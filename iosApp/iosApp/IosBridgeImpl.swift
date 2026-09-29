import AVFoundation
import Foundation
import LocalAuthentication
import UIKit
import UserNotifications
import Shared

// MARK: - Chuyển đổi giữa Data (Swift) và KotlinByteArray

extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(count))
        for (index, byte) in enumerated() {
            array.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return array
    }
}

extension KotlinByteArray {
    func toData() -> Data {
        var data = Data(count: Int(size))
        for index in 0..<Int(size) {
            data[index] = UInt8(bitPattern: get(index: Int32(index)))
        }
        return data
    }
}

// MARK: - Tiện ích UIKit

private func documentsDirectory() -> URL {
    FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
}

private func topViewController() -> UIViewController? {
    let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
    let window = scenes.flatMap { $0.windows }.first { $0.isKeyWindow } ?? scenes.first?.windows.first
    var top = window?.rootViewController
    while let presented = top?.presentedViewController {
        top = presented
    }
    return top
}

// MARK: - Cầu nối chính

final class IosBridgeImpl: NSObject, IosBridge {
    private var photoPicker: PhotoPicker?

    // Chọn ảnh / chụp ảnh

    func pickImage(useCamera: Bool, filePrefix: String, onResult: @escaping (String?) -> Void) {
        guard let presenter = topViewController() else {
            onResult(nil)
            return
        }
        let source: UIImagePickerController.SourceType =
            (useCamera && UIImagePickerController.isSourceTypeAvailable(.camera)) ? .camera : .photoLibrary

        let picker = UIImagePickerController()
        picker.sourceType = source
        let handler = PhotoPicker(filePrefix: filePrefix) { [weak self] path in
            self?.photoPicker = nil
            onResult(path)
        }
        photoPicker = handler // delegate của UIImagePickerController là weak nên phải giữ tham chiếu
        picker.delegate = handler
        presenter.present(picker, animated: true)
    }

    // Sinh trắc học

    func authenticateBiometric(
        title: String,
        reason: String,
        cancelText: String,
        onResult: @escaping (String) -> Void
    ) {
        let context = LAContext()
        context.localizedCancelTitle = cancelText
        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            onResult("failed")
            return
        }
        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, evalError in
            DispatchQueue.main.async {
                if success {
                    onResult("success")
                } else if let laError = evalError as? LAError,
                          laError.code == .userCancel || laError.code == .appCancel || laError.code == .systemCancel {
                    onResult("cancelled")
                } else {
                    onResult("failed")
                }
            }
        }
    }

    // Camera quét QR

    func isCameraAuthorized() -> Bool {
        AVCaptureDevice.authorizationStatus(for: .video) == .authorized
    }

    func requestCameraPermission(onResult: @escaping (String) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async {
                onResult(granted ? "granted" : "denied")
            }
        }
    }

    func makeQrScannerView(onCode: @escaping (String) -> Void) -> UIView {
        QrScannerView(onCode: onCode)
    }

    func stopQrScannerView(view: UIView) {
        (view as? QrScannerView)?.stop()
    }

    // PDF

    func createPdf(width: Int32, height: Int32, draw: @escaping (PdfPage) -> Void) -> KotlinByteArray? {
        let bounds = CGRect(x: 0, y: 0, width: CGFloat(width), height: CGFloat(height))
        let renderer = UIGraphicsPDFRenderer(bounds: bounds)
        let data = renderer.pdfData { context in
            context.beginPage()
            UIColor.white.setFill()
            UIRectFill(bounds)
            draw(SwiftPdfPage())
        }
        return data.toKotlinByteArray()
    }

    func printPdf(jobName: String, pdfBytes: KotlinByteArray) {
        let data = pdfBytes.toData()
        guard UIPrintInteractionController.canPrint(data) else { return }
        let info = UIPrintInfo(dictionary: nil)
        info.outputType = .general
        info.jobName = jobName
        let controller = UIPrintInteractionController.shared
        controller.printInfo = info
        controller.printingItem = data
        controller.present(animated: true, completionHandler: nil)
    }

    func sharePdf(fileName: String, pdfBytes: KotlinByteArray) {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        do {
            try pdfBytes.toData().write(to: url, options: .atomic)
        } catch {
            return
        }
        guard let presenter = topViewController() else { return }
        let activity = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        if let popover = activity.popoverPresentationController {
            // iPad bắt buộc phải có điểm neo cho popover.
            popover.sourceView = presenter.view
            popover.sourceRect = CGRect(x: presenter.view.bounds.midX, y: presenter.view.bounds.midY, width: 0, height: 0)
            popover.permittedArrowDirections = []
        }
        presenter.present(activity, animated: true)
    }

    // File

    func readFileBytes(uriOrPath: String) -> KotlinByteArray? {
        var path = uriOrPath
        if let url = URL(string: uriOrPath), url.isFileURL {
            path = url.path
        }
        if !FileManager.default.fileExists(atPath: path) {
            // Đường dẫn container của app có thể đổi sau khi cập nhật; thử lại theo tên file trong Documents.
            let candidate = documentsDirectory().appendingPathComponent((path as NSString).lastPathComponent).path
            guard FileManager.default.fileExists(atPath: candidate) else { return nil }
            path = candidate
        }
        return FileManager.default.contents(atPath: path)?.toKotlinByteArray()
    }

    // Thông báo

    func postNotification(title: String, body: String, id: Int32) {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            guard granted else { return }
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.sound = .default
            let request = UNNotificationRequest(
                identifier: "overdue_\(id)",
                content: content,
                trigger: UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
            )
            center.add(request, withCompletionHandler: nil)
        }
    }
}

// MARK: - Chọn / chụp ảnh

private final class PhotoPicker: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    private let filePrefix: String
    private let onResult: (String?) -> Void

    init(filePrefix: String, onResult: @escaping (String?) -> Void) {
        self.filePrefix = filePrefix
        self.onResult = onResult
    }

    func imagePickerController(
        _ picker: UIImagePickerController,
        didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
    ) {
        let image = info[.originalImage] as? UIImage
        picker.dismiss(animated: true) { [self] in
            onResult(image.flatMap { save($0) })
        }
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true) { [self] in
            onResult(nil)
        }
    }

    /// Lưu JPEG vào Documents của app (giống filesDir trên Android) và trả về đường dẫn tuyệt đối.
    private func save(_ image: UIImage) -> String? {
        guard let data = normalized(image).jpegData(compressionQuality: 0.9) else { return nil }
        let millis = Int64(Date().timeIntervalSince1970 * 1000)
        let url = documentsDirectory().appendingPathComponent("\(filePrefix)_\(millis).jpg")
        do {
            try data.write(to: url, options: .atomic)
            return url.path
        } catch {
            return nil
        }
    }

    /// Vẽ lại ảnh theo hướng "Up" để các bộ giải mã bỏ qua EXIF vẫn hiển thị đúng chiều.
    private func normalized(_ image: UIImage) -> UIImage {
        if image.imageOrientation == .up { return image }
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = image.scale
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: image.size, format: format)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: image.size))
        }
    }
}

// MARK: - Quét QR bằng AVFoundation

private final class QrScannerView: UIView, AVCaptureMetadataOutputObjectsDelegate {
    private let session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private let onCode: (String) -> Void

    init(onCode: @escaping (String) -> Void) {
        self.onCode = onCode
        super.init(frame: .zero)
        backgroundColor = .black
        configure()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func configure() {
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else { return }
        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else { return }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        output.metadataObjectTypes = [.qr]

        let preview = AVCaptureVideoPreviewLayer(session: session)
        preview.videoGravity = .resizeAspectFill
        layer.addSublayer(preview)
        previewLayer = preview

        let captureSession = self.session
        DispatchQueue.global(qos: .userInitiated).async {
            captureSession.startRunning()
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }

    func stop() {
        let captureSession = self.session
        DispatchQueue.global(qos: .userInitiated).async {
            if captureSession.isRunning { captureSession.stopRunning() }
        }
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        for object in metadataObjects {
            if let code = object as? AVMetadataMachineReadableCodeObject, let text = code.stringValue {
                onCode(text)
            }
        }
    }
}

// MARK: - Vẽ PDF

private final class SwiftPdfPage: NSObject, PdfPage {
    private static func uiColor(_ argb: Int64) -> UIColor {
        let value = UInt32(truncatingIfNeeded: argb)
        return UIColor(
            red: CGFloat((value >> 16) & 0xFF) / 255,
            green: CGFloat((value >> 8) & 0xFF) / 255,
            blue: CGFloat(value & 0xFF) / 255,
            alpha: CGFloat((value >> 24) & 0xFF) / 255
        )
    }

    func drawText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        bold: Bool,
        align: PdfAlign,
        color: Int64,
        underline: Bool
    ) {
        let font = bold ? UIFont.boldSystemFont(ofSize: CGFloat(size)) : UIFont.systemFont(ofSize: CGFloat(size))
        let uiColor = SwiftPdfPage.uiColor(color)
        let attributes: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: uiColor]
        let string = text as NSString
        let width = string.size(withAttributes: attributes).width

        var originX = CGFloat(x)
        if align == PdfAlign.center {
            originX -= width / 2
        } else if align == PdfAlign.right {
            originX -= width
        }
        // Android truyền baseline; UIKit vẽ từ mép trên của dòng chữ.
        let originY = CGFloat(y) - font.ascender
        string.draw(at: CGPoint(x: originX, y: originY), withAttributes: attributes)

        if underline {
            let path = UIBezierPath()
            path.move(to: CGPoint(x: originX, y: CGFloat(y) + 2))
            path.addLine(to: CGPoint(x: originX + width, y: CGFloat(y) + 2))
            path.lineWidth = max(0.5, CGFloat(size) / 16)
            uiColor.setStroke()
            path.stroke()
        }
    }

    func drawLine(x1: Float, y1: Float, x2: Float, y2: Float, strokeWidth: Float) {
        let path = UIBezierPath()
        path.move(to: CGPoint(x: CGFloat(x1), y: CGFloat(y1)))
        path.addLine(to: CGPoint(x: CGFloat(x2), y: CGFloat(y2)))
        path.lineWidth = CGFloat(strokeWidth)
        UIColor.black.setStroke()
        path.stroke()
    }

    func drawRect(left: Float, top: Float, right: Float, bottom: Float, fillColor: KotlinLong?, strokeWidth: Float) {
        let rect = CGRect(
            x: CGFloat(left),
            y: CGFloat(top),
            width: CGFloat(right - left),
            height: CGFloat(bottom - top)
        )
        if let fillColor = fillColor {
            SwiftPdfPage.uiColor(fillColor.int64Value).setFill()
            UIBezierPath(rect: rect).fill()
        }
        if strokeWidth > 0 {
            let path = UIBezierPath(rect: rect)
            path.lineWidth = CGFloat(strokeWidth)
            UIColor.black.setStroke()
            path.stroke()
        }
    }

    func drawImage(imageBytes: KotlinByteArray, x: Float, y: Float, height: Float) {
        guard let image = UIImage(data: imageBytes.toData()), image.size.height > 0 else { return }
        let drawHeight = CGFloat(height)
        let drawWidth = drawHeight * image.size.width / image.size.height
        image.draw(in: CGRect(x: CGFloat(x), y: CGFloat(y), width: drawWidth, height: drawHeight))
    }
}
