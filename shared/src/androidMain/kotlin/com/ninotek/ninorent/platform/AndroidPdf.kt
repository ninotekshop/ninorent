package com.ninotek.ninorent.platform

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private class AndroidPdfPage(private val canvas: Canvas) : PdfPage {
    private fun textPaint(size: Float, bold: Boolean, align: PdfAlign, color: Long, underline: Boolean) =
        Paint().apply {
            this.color = color.toInt()
            isAntiAlias = true
            textSize = size
            if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isUnderlineText = underline
            textAlign = when (align) {
                PdfAlign.Left -> Paint.Align.LEFT
                PdfAlign.Center -> Paint.Align.CENTER
                PdfAlign.Right -> Paint.Align.RIGHT
            }
        }

    override fun drawText(
        text: String, x: Float, y: Float, size: Float,
        bold: Boolean, align: PdfAlign, color: Long, underline: Boolean
    ) {
        canvas.drawText(text, x, y, textPaint(size, bold, align, color, underline))
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, strokeWidth: Float) {
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
        }
        canvas.drawLine(x1, y1, x2, y2, paint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, fillColor: Long?, strokeWidth: Float) {
        if (fillColor != null) {
            val fill = Paint().apply {
                color = fillColor.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(left, top, right, bottom, fill)
        }
        if (strokeWidth > 0f) {
            val stroke = Paint().apply {
                color = android.graphics.Color.BLACK
                this.strokeWidth = strokeWidth
                style = Paint.Style.STROKE
            }
            canvas.drawRect(left, top, right, bottom, stroke)
        }
    }

    override fun drawImage(imageBytes: ByteArray, x: Float, y: Float, height: Float) {
        val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return
        val width = height * original.width.toFloat() / original.height.toFloat()
        val scaled = Bitmap.createScaledBitmap(original, width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1), true)
        canvas.drawBitmap(scaled, x, y, null)
    }
}

actual fun buildPdf(pageWidth: Int, pageHeight: Int, draw: (PdfPage) -> Unit): ByteArray {
    val document = PdfDocument()
    try {
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        page.canvas.drawColor(android.graphics.Color.WHITE)
        draw(AndroidPdfPage(page.canvas))
        document.finishPage(page)
        val out = ByteArrayOutputStream()
        document.writeTo(out)
        return out.toByteArray()
    } finally {
        document.close()
    }
}

private class BytesPrintAdapter(private val jobName: String, private val pdfBytes: ByteArray) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("$jobName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        try {
            FileOutputStream(destination?.fileDescriptor).use { it.write(pdfBytes) }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        }
    }
}

actual fun printPdf(context: PlatformContext, jobName: String, pdfBytes: ByteArray) {
    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
    printManager.print(jobName, BytesPrintAdapter(jobName, pdfBytes), PrintAttributes.Builder().build())
}

actual fun sharePdf(context: PlatformContext, fileName: String, pdfBytes: ByteArray, chooserTitle: String) {
    val file = File(context.cacheDir, fileName)
    FileOutputStream(file).use { it.write(pdfBytes) }
    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(shareIntent, chooserTitle)
    // Context có thể là Application (không phải Activity) nên cần cờ NEW_TASK.
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
