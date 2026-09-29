package com.ninotek.ninorent.platform

enum class PdfAlign { Left, Center, Right }

/** Trang PDF tối giản: đủ để vẽ hợp đồng. Toạ độ tính bằng point (1/72 inch), gốc ở góc trên trái. */
interface PdfPage {
    /** Vẽ chữ với đường cơ sở (baseline) tại [y]. */
    fun drawText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        bold: Boolean = false,
        align: PdfAlign = PdfAlign.Left,
        color: Long = 0xFF000000,
        underline: Boolean = false
    )

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, strokeWidth: Float)

    /** Vẽ hình chữ nhật: tô màu [fillColor] (nếu có) và/hoặc viền [strokeWidth] (nếu > 0). */
    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, fillColor: Long? = null, strokeWidth: Float = 0f)

    /** Vẽ ảnh (JPEG/PNG) căn trái tại ([x], [y]) với chiều cao [height], giữ tỉ lệ. */
    fun drawImage(imageBytes: ByteArray, x: Float, y: Float, height: Float)
}

/** Dựng một PDF 1 trang và trả về nội dung file. */
expect fun buildPdf(pageWidth: Int, pageHeight: Int, draw: (PdfPage) -> Unit): ByteArray

/** Mở hộp thoại in hệ thống cho file PDF. */
expect fun printPdf(context: PlatformContext, jobName: String, pdfBytes: ByteArray)

/** Mở hộp thoại chia sẻ hệ thống cho file PDF. */
expect fun sharePdf(context: PlatformContext, fileName: String, pdfBytes: ByteArray, chooserTitle: String)
