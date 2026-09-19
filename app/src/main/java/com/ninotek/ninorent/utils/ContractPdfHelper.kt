package com.ninotek.ninorent.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
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
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ninotek.ninorent.model.CartItem
import com.ninotek.ninorent.model.PaperSize
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.loadLessorInfoFromPrefs
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

class ContractPrintAdapter(
    private val context: Context,
    private val order: RentalOrder,
    private val paperSize: PaperSize
) : PrintDocumentAdapter() {

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

        val info = PrintDocumentInfo.Builder("Hop_Dong_Thue_${order.id.replace("#", "")}.pdf")
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
        val pdfDocument = generateContractPdf(order, paperSize, context)
        try {
            FileOutputStream(destination?.fileDescriptor).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        } finally {
            pdfDocument.close()
        }
    }
}

fun printContract(context: Context, order: RentalOrder, paperSize: PaperSize) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "NinoRent_HopDong_${order.id.replace("#", "")}"
    printManager.print(jobName, ContractPrintAdapter(context, order, paperSize), PrintAttributes.Builder().build())
}

fun shareContractPdf(context: Context, order: RentalOrder, paperSize: PaperSize) {
    try {
        val pdfDocument = generateContractPdf(order, paperSize, context)
        val file = File(context.cacheDir, "Hop_Dong_${order.id.replace("#", "")}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ Hợp đồng PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Lỗi xuất file PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun getOrderEquipmentItems(order: RentalOrder): List<CartItem> {
    if (order.equipmentItems.isNotEmpty()) return order.equipmentItems
    val names = order.equipmentName.split(" + ")
    val serials = order.serialNumber.split(", ")
    return names.mapIndexed { idx, name ->
        val serial = serials.getOrNull(idx) ?: order.serialNumber
        CartItem(
            equipmentName = name.trim(),
            pricePerDay = if (names.size == 1) order.price else "",
            serialNumber = serial.trim()
        )
    }
}

fun formatCurrencyAmount(amount: String): String {
    if (amount.isBlank()) return "0"
    val cleaned = amount
        .replace("đ", "", ignoreCase = true)
        .replace("₫", "")
        .replace("VND", "", ignoreCase = true)
        .replace("VNĐ", "", ignoreCase = true)
        .replace(".", "")
        .replace(",", "")
        .trim()

    val digitsOnly = cleaned.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) return "0"

    return try {
        val number = digitsOnly.toLong()
        NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(number)
    } catch (_: Exception) {
        cleaned
    }
}

fun formatRentalDuration(dateRange: String): String {
    if (dateRange.isBlank()) return "10/09 - 12/09/2026 (1 ngày)"
    val regex = Regex("""^(\d{2}/\d{2})/\d{4}\s*-\s*(\d{2}/\d{2}/\d{4}\s*\(\d+\s*ngày\))$""")
    val match = regex.find(dateRange)
    if (match != null) {
        return "${match.groupValues[1]} - ${match.groupValues[2]}"
    }
    return dateRange
}

fun formatDailyPrice(totalPrice: String, dateRange: String): String {
    val digitsOnly = totalPrice.filter { it.isDigit() }
    val daysRegex = Regex("""\((\d+)\s*ngày\)""", RegexOption.IGNORE_CASE)
    val match = daysRegex.find(dateRange)
    val days = match?.groupValues?.get(1)?.toIntOrNull() ?: 1

    if (digitsOnly.isNotEmpty()) {
        try {
            val total = digitsOnly.toLong()
            val daily = total / days
            return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(daily)
        } catch (_: Exception) {
            // fallback
        }
    }
    return formatCurrencyAmount(totalPrice)
}

fun formatVietnameseContractDate(location: String, dateStr: String): String {
    val city = when {
        location.contains("Quy Nhơn", ignoreCase = true) -> "Quy Nhơn"
        location.isNotBlank() -> location.substringBefore(",").trim()
        else -> "Quy Nhơn"
    }

    if (dateStr.contains("ngày", ignoreCase = true)) {
        return if (dateStr.startsWith(city)) dateStr else "$city, $dateStr"
    }

    val parts = dateStr.split("/", "-")
    if (parts.size >= 3) {
        val day = parts[0].padStart(2, '0')
        val month = parts[1].padStart(2, '0')
        val year = parts[2]
        return "$city, ngày $day tháng $month năm $year"
    }

    return "$city, ngày 10 tháng 09 năm 2026"
}

fun generateContractPdf(order: RentalOrder, paperSize: PaperSize, context: Context? = null): PdfDocument {
    val pdfDocument = PdfDocument()
    val savedLessor = context?.let { loadLessorInfoFromPrefs(it) }

    // Page dimensions in points (1 pt = 1/72 inch)
    val pageWidth = if (paperSize == PaperSize.A4) 595 else 420
    val pageHeight = if (paperSize == PaperSize.A4) 842 else 595
    val scale = if (paperSize == PaperSize.A4) 1.0f else 0.72f
    val margin = (28 * scale).toInt()

    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    // Background white
    canvas.drawColor(Color.WHITE)

    val paintText = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        textSize = 9.5f * scale
    }

    val paintBold = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 10.5f * scale
    }

    val paintTitle = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 15f * scale
    }

    val paintLine = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1f * scale
        style = Paint.Style.STROKE
    }

    var y = margin.toFloat() + 8f * scale

    // 1. Header: Logo (Left) & Motto (Right)
    val rightHeaderWidth = 230f * scale
    val mottoCenterX = pageWidth - margin - (rightHeaderWidth / 2f)

    // Draw Motto (Right Side)
    paintBold.textSize = 9.5f * scale
    paintBold.textAlign = Paint.Align.CENTER
    canvas.drawText("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", mottoCenterX, y, paintBold)

    val yMottoLine2 = y + 13f * scale
    paintBold.textSize = 9.5f * scale
    paintBold.isUnderlineText = true
    canvas.drawText("Độc lập – Tự do – Hạnh phúc", mottoCenterX, yMottoLine2, paintBold)
    paintBold.isUnderlineText = false
    paintBold.textAlign = Paint.Align.LEFT

    val yDateLine = yMottoLine2 + 22f * scale

    // Draw Date Line (Right Side)
    val dateLine = formatVietnameseContractDate(order.contractLocation, order.contractDate)
    paintText.textSize = 9.5f * scale
    paintText.textAlign = Paint.Align.RIGHT
    canvas.drawText(dateLine, (pageWidth - margin).toFloat(), yDateLine, paintText)
    paintText.textAlign = Paint.Align.LEFT

    // Draw Logo (Left Side) replacing the old text
    val logoUriString = savedLessor?.logoUri
    if (!logoUriString.isNullOrBlank() && context != null) {
        try {
            val uri = Uri.parse(logoUriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                val availableHeight = yDateLine - (margin.toFloat() + 8f * scale)
                val maxLogoHeight = availableHeight + 10f * scale // slight allowance

                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val targetHeight = maxLogoHeight
                val targetWidth = targetHeight * ratio

                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth.toInt(), targetHeight.toInt(), true)

                // Align left
                val logoX = margin.toFloat()
                val logoY = margin.toFloat() + 4f * scale

                canvas.drawBitmap(scaledBitmap, logoX, logoY, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback text if logo fails to load
            paintBold.textSize = 12f * scale
            paintBold.color = Color.rgb(255, 102, 0)
            canvas.drawText("NINOTEK", margin.toFloat(), y, paintBold)
            paintBold.color = Color.BLACK
        }
    } else {
        // Fallback text if no logo configured
        paintBold.textSize = 12f * scale
        paintBold.color = Color.rgb(255, 102, 0)
        canvas.drawText("NINOTEK", margin.toFloat(), y, paintBold)
        paintBold.color = Color.BLACK
    }

    y = yDateLine + 22f * scale

    // Title
    paintTitle.textAlign = Paint.Align.CENTER
    canvas.drawText("HỢP ĐỒNG CHO THUÊ THIẾT BỊ", (pageWidth / 2).toFloat(), y, paintTitle)
    paintTitle.textAlign = Paint.Align.LEFT

    y += 14f * scale
    paintText.textAlign = Paint.Align.CENTER
    canvas.drawText("Số: ${order.id.replace("#", "")}/HĐ-NINOTEK", (pageWidth / 2).toFloat(), y, paintText)
    paintText.textAlign = Paint.Align.LEFT

    y += 20f * scale

    val lName = if (order.lessorName.isNotBlank()) order.lessorName else (savedLessor?.name ?: "CÔNG TY TNHH NINOTEK")
    val lPhone = if (order.lessorPhone.isNotBlank()) order.lessorPhone else (savedLessor?.phone ?: "0901234567")
    val lAddr = if (order.lessorAddress.isNotBlank()) order.lessorAddress else (savedLessor?.address ?: "Số 123 Nguyễn Huệ, TP. Quy Nhơn, Tỉnh Bình Định")
    val lRep = if (order.lessorRepresentative.isNotBlank()) order.lessorRepresentative else (savedLessor?.representative ?: "Ông Nguyễn Văn A - Giám Đốc")

    // I. BÊN A
    paintBold.textSize = 10.5f * scale
    canvas.drawText("I. BÊN A (BÊN CHO THUÊ):", margin.toFloat(), y, paintBold)
    y += 13f * scale
    paintText.textSize = 9.5f * scale
    canvas.drawText("• Đơn vị: $lName", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("• Địa chỉ: $lAddr", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("• Đại diện: $lRep", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("• Điện thoại: $lPhone", (margin + 10 * scale).toFloat(), y, paintText)

    y += 16f * scale

    // II. BÊN B
    canvas.drawText("II. BÊN B (BÊN THUÊ):", margin.toFloat(), y, paintBold)
    y += 13f * scale
    val lesseeName = if (order.lesseeName.isNotBlank()) order.lesseeName else order.customerName
    val phoneStr = if (order.lesseePhone.isNotBlank()) order.lesseePhone else "090 123 4567"
    val addrStr = if (order.lesseeAddress.isNotBlank()) order.lesseeAddress else "123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định"
    val cccdStr = if (order.lesseeIdNumber.isNotBlank()) order.lesseeIdNumber else "012345678901"
    val issueStr = if (order.lesseeIdIssueDate.isNotBlank()) " (Cấp ngày: ${order.lesseeIdIssueDate})" else ""

    canvas.drawText("• Ông/Bà: $lesseeName", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("• Số CCCD/CMND: $cccdStr$issueStr", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("• Địa chỉ: $addrStr", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("• Điện thoại: $phoneStr", (margin + 10 * scale).toFloat(), y, paintText)

    y += 16f * scale

    // III. NỘI DUNG VÀ BẢNG THIẾT BỊ
    canvas.drawText("III. NỘI DUNG, ĐỐI TƯỢNG VÀ GIÁ TRỊ CỦA HỢP ĐỒNG:", margin.toFloat(), y, paintBold)
    y += 13f * scale

    // 5-Column Table
    val tableLeft = margin.toFloat()
    val tableRight = (pageWidth - margin).toFloat()
    val tableWidth = tableRight - tableLeft

    val colStt = 30f * scale
    val colName = 175f * scale
    val colQty = 45f * scale
    val colTime = 145f * scale
    val colPrice = tableWidth - (colStt + colName + colQty + colTime)
    val colWidths = floatArrayOf(colStt, colName, colQty, colTime, colPrice)

    val headerHeight = 18f * scale
    val paintTableBg = Paint().apply {
        color = Color.rgb(238, 238, 238)
        style = Paint.Style.FILL
    }
    canvas.drawRect(tableLeft, y, tableRight, y + headerHeight, paintTableBg)
    canvas.drawRect(tableLeft, y, tableRight, y + headerHeight, paintLine)

    var curX = tableLeft
    val headers = arrayOf("Stt", "Tên máy móc, thiết bị", "SL", "Thời gian thuê", "Giá thuê/ngày")
    val aligns = arrayOf(Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.RIGHT)

    for (i in headers.indices) {
        paintBold.textAlign = aligns[i]
        val textX = when (aligns[i]) {
            Paint.Align.CENTER -> curX + (colWidths[i] / 2f)
            Paint.Align.RIGHT -> curX + colWidths[i] - (4f * scale)
            else -> curX + (4f * scale)
        }
        canvas.drawText(headers[i], textX, y + 12f * scale, paintBold)
        curX += colWidths[i]
        if (i < headers.size - 1) {
            canvas.drawLine(curX, y, curX, y + headerHeight, paintLine)
        }
    }
    paintBold.textAlign = Paint.Align.LEFT

    y += headerHeight

    // Table Data Rows
    val items = getOrderEquipmentItems(order)
    val formattedDuration = formatRentalDuration(order.dateRange)
    val finalTotalPrice = if (order.netTotal.isNotBlank()) formatCurrencyAmount(order.netTotal) else formatCurrencyAmount(order.price)

    items.forEachIndexed { index, item ->
        val rowHeight = 20f * scale
        canvas.drawRect(tableLeft, y, tableRight, y + rowHeight, paintLine)

        val itemDailyPrice = if (item.pricePerDay.isNotBlank()) formatCurrencyAmount(item.pricePerDay) else formatDailyPrice(order.price, order.dateRange)
        val equipDisplayName = if (item.serialNumber.isNotBlank()) "${item.equipmentName} (Seri: ${item.serialNumber})" else item.equipmentName

        curX = tableLeft
        val rowData = arrayOf("${index + 1}", equipDisplayName, "01 bộ", formattedDuration, itemDailyPrice)
        for (i in rowData.indices) {
            paintText.textAlign = aligns[i]
            val textX = when (aligns[i]) {
                Paint.Align.CENTER -> curX + (colWidths[i] / 2f)
                Paint.Align.RIGHT -> curX + colWidths[i] - (4f * scale)
                else -> curX + (4f * scale)
            }
            canvas.drawText(rowData[i], textX, y + 14f * scale, paintText)
            curX += colWidths[i]
            if (i < rowData.size - 1) {
                canvas.drawLine(curX, y, curX, y + rowHeight, paintLine)
            }
        }
        paintText.textAlign = Paint.Align.LEFT
        y += rowHeight
    }

    // Total Row
    val totalRowHeight = 20f * scale
    canvas.drawRect(tableLeft, y, tableRight, y + totalRowHeight, paintLine)
    val totalDividerX = tableLeft + colStt + colName + colQty + colTime
    canvas.drawLine(totalDividerX, y, totalDividerX, y + totalRowHeight, paintLine)
    paintBold.textAlign = Paint.Align.LEFT
    val totalLabel = if (order.discountAmount.isNotBlank() && order.discountAmount != "0") {
        "Tổng cộng thanh toán (Đã giảm ${formatCurrencyAmount(order.discountAmount)}):"
    } else {
        "Tổng cộng giá trị thanh toán:"
    }
    canvas.drawText(totalLabel, tableLeft + 8f * scale, y + 14f * scale, paintBold)
    paintBold.textAlign = Paint.Align.RIGHT
    canvas.drawText(finalTotalPrice, tableRight - 8f * scale, y + 14f * scale, paintBold)
    paintBold.textAlign = Paint.Align.LEFT

    y += totalRowHeight + 14f * scale

    // IV. ĐIỀU KHOẢN THANH TOÁN & THẾ CHẤP
    val advanceStr = formatCurrencyAmount(if (order.advancePaymentAmount.isNotBlank()) order.advancePaymentAmount else "2.000.000")
    canvas.drawText("IV. ĐIỀU KHOẢN THANH TOÁN VÀ THẾ CHẤP:", margin.toFloat(), y, paintBold)
    y += 13f * scale
    canvas.drawText("1. Số tiền thanh toán trước (đặt cọc thuê): $advanceStr", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("2. Danh mục tài sản & giấy tờ thế chấp (giữ lại):", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale

    val cccdCheck = if (order.collateralCccd) "[X] CCCD gốc" else "[  ] CCCD gốc"
    val gplxCheck = if (order.collateralGplx) "[X] GPLX gốc" else "[  ] GPLX gốc"
    val hasAsset = order.collateralAssetDescription.isNotBlank()
    val assetCheck = if (hasAsset) "[X] Tài sản khác: ${order.collateralAssetDescription}" else "[  ] Tài sản khác: Xe máy / Giấy tờ khác"
    val hasCash = order.collateralCashAmount.isNotBlank() && order.collateralCashAmount != "0"
    val cashStr = if (hasCash) formatCurrencyAmount(order.collateralCashAmount) else "5.000.000"
    val cashCheck = if (hasCash) "[X] Tiền đặt cọc thế chấp: $cashStr" else "[  ] Tiền đặt cọc thế chấp: $cashStr"

    canvas.drawText("  $cccdCheck     $gplxCheck", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("  $assetCheck", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("  $cashCheck", (margin + 10 * scale).toFloat(), y, paintText)

    y += 16f * scale

    // V. TRÁCH NHIỆM & BỒI THƯỜNG
    canvas.drawText("V. TRÁCH NHIỆM VÀ BỒI THƯỜNG TRONG QUÁ TRÌNH THUÊ:", margin.toFloat(), y, paintBold)
    y += 13f * scale
    canvas.drawText("1. Bên B có trách nhiệm kiểm tra kỹ tình trạng máy móc, thiết bị trước khi nhận bàn giao.", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("2. Trong thời gian thuê, nếu hư hỏng hoặc mất mát do lỗi Bên B phải bồi thường 100%.", (margin + 10 * scale).toFloat(), y, paintText)
    y += 12f * scale
    canvas.drawText("3. Bên B cam kết bàn giao lại thiết bị đúng thời hạn quy định. Quá hạn sẽ tính phí phát sinh.", (margin + 10 * scale).toFloat(), y, paintText)

    y += 20f * scale

    // VI. CHỮ KÝ CÁC BÊN
    canvas.drawText("VI. CHỮ KÝ CÁC BÊN:", margin.toFloat(), y, paintBold)
    y += 15f * scale

    val colA = margin + 90f * scale
    val colB = pageWidth - margin - 90f * scale

    paintBold.textAlign = Paint.Align.CENTER
    canvas.drawText("ĐẠI DIỆN BÊN A", colA, y, paintBold)
    canvas.drawText("ĐẠI DIỆN BÊN B", colB, y, paintBold)

    y += 11f * scale
    paintText.textSize = 8f * scale
    paintText.textAlign = Paint.Align.CENTER
    canvas.drawText("(Ký, ghi rõ họ tên & đóng dấu)", colA, y, paintText)
    canvas.drawText("(Ký & ghi rõ họ tên)", colB, y, paintText)

    y += 38f * scale
    paintBold.textSize = 9.5f * scale
    canvas.drawText(lRep.substringBefore("-").trim(), colA, y, paintBold)
    canvas.drawText(lesseeName, colB, y, paintBold)

    paintBold.textAlign = Paint.Align.LEFT
    paintText.textAlign = Paint.Align.LEFT

    pdfDocument.finishPage(page)
    return pdfDocument
}
