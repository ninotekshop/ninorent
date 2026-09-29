package com.ninotek.ninorent.platform

import platform.UIKit.UIView

/**
 * Cầu nối sang các API riêng của iOS (UIKit, AVFoundation, LocalAuthentication, UserNotifications).
 * Phần cài đặt nằm ở Swift (iosApp/iosApp/IosBridgeImpl.swift) và được đăng ký lúc khởi động app
 * bằng [installIosBridge]. Viết bằng Swift để tránh phải map các enum/constant Objective-C sang Kotlin.
 *
 * Mọi callback đều được gọi trên main thread. Kết quả dạng chuỗi để tránh boxing kiểu nguyên thủy.
 */
interface IosBridge {
    /** Mở camera hoặc thư viện ảnh. Trả về đường dẫn tuyệt đối của file JPEG đã lưu, hoặc null nếu huỷ/lỗi. */
    fun pickImage(useCamera: Boolean, filePrefix: String, onResult: (String?) -> Unit)

    /** Xác thực sinh trắc học. Kết quả: "success", "cancelled" hoặc "failed". */
    fun authenticateBiometric(title: String, reason: String, cancelText: String, onResult: (String) -> Unit)

    fun isCameraAuthorized(): Boolean

    /** Xin quyền camera. Kết quả: "granted" hoặc "denied". */
    fun requestCameraPermission(onResult: (String) -> Unit)

    /** Tạo view camera quét QR; gọi [onCode] với nội dung mã mỗi khi đọc được. */
    fun makeQrScannerView(onCode: (String) -> Unit): UIView

    fun stopQrScannerView(view: UIView)

    /** Dựng PDF 1 trang: gọi [draw] với trang để vẽ, trả về nội dung file PDF. */
    fun createPdf(width: Int, height: Int, draw: (PdfPage) -> Unit): ByteArray?

    fun printPdf(jobName: String, pdfBytes: ByteArray)

    fun sharePdf(fileName: String, pdfBytes: ByteArray)

    /** Đọc nội dung file từ URI hoặc đường dẫn. */
    fun readFileBytes(uriOrPath: String): ByteArray?

    fun postNotification(title: String, body: String, id: Int)
}

private var installedBridge: IosBridge? = null

/** Được Swift gọi một lần khi app khởi động, trước khi hiển thị UI. */
fun installIosBridge(bridge: IosBridge) {
    installedBridge = bridge
}

internal val iosBridge: IosBridge
    get() = installedBridge ?: error("IosBridge chưa được cài đặt. Gọi IosBridgeKt.installIosBridge(...) trong iOSApp.swift.")
