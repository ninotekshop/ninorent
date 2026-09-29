package com.ninotek.ninorent.utils

import com.ninotek.ninorent.model.CartItem
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.PaperSize
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.loadLessorInfoFromPrefs
import com.ninotek.ninorent.platform.PdfAlign
import com.ninotek.ninorent.platform.PdfPage
import com.ninotek.ninorent.platform.PlatformContext
import com.ninotek.ninorent.platform.buildPdf
import com.ninotek.ninorent.platform.printPdf
import com.ninotek.ninorent.platform.readUriBytes
import com.ninotek.ninorent.platform.sharePdf
import com.ninotek.ninorent.platform.showToast

fun printContract(context: PlatformContext, order: RentalOrder, paperSize: PaperSize) {
    val jobName = "NinoRent_HopDong_${order.id.replace("#", "")}"
    printPdf(context, jobName, generateContractPdf(order, paperSize, context))
}

fun shareContractPdf(context: PlatformContext, order: RentalOrder, paperSize: PaperSize) {
    try {
        val pdfBytes = generateContractPdf(order, paperSize, context)
        sharePdf(context, "Hop_Dong_${order.id.replace("#", "")}.pdf", pdfBytes, "Chia sẻ Hợp đồng PDF")
    } catch (e: Exception) {
        showToast(context, "Lỗi xuất file PDF: ${e.message}")
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
        formatGroupedNumber(number)
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
            return formatGroupedNumber(daily)
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

/** Dựng file PDF hợp đồng (1 trang) và trả về nội dung. */
fun generateContractPdf(order: RentalOrder, paperSize: PaperSize, context: PlatformContext? = null): ByteArray {
    val savedLessor = context?.let { loadLessorInfoFromPrefs(it) }

    // Page dimensions in points (1 pt = 1/72 inch)
    val pageWidth = if (paperSize == PaperSize.A4) 595 else 420
    val pageHeight = if (paperSize == PaperSize.A4) 842 else 595
    val scale = if (paperSize == PaperSize.A4) 1.0f else 0.72f
    val margin = (28 * scale).toInt()

    return buildPdf(pageWidth, pageHeight) { page ->
        drawContract(page, order, savedLessor, context, pageWidth, scale, margin)
    }
}

private const val COLOR_ORANGE = 0xFFFF6600L
private const val COLOR_TABLE_HEADER = 0xFFEEEEEEL

private fun drawContract(
    page: PdfPage,
    order: RentalOrder,
    savedLessor: LessorInfo?,
    context: PlatformContext?,
    pageWidth: Int,
    scale: Float,
    margin: Int
) {
    val textSize = 9.5f * scale
    val boldSize = 10.5f * scale

    fun text(t: String, x: Float, y: Float, size: Float = textSize, align: PdfAlign = PdfAlign.Left) =
        page.drawText(t, x, y, size, align = align)

    fun bold(t: String, x: Float, y: Float, size: Float = boldSize, align: PdfAlign = PdfAlign.Left) =
        page.drawText(t, x, y, size, bold = true, align = align)

    val lineWidth = 1f * scale
    var y = margin.toFloat() + 8f * scale

    // 1. Header: Logo (Left) & Motto (Right)
    val rightHeaderWidth = 230f * scale
    val mottoCenterX = pageWidth - margin - (rightHeaderWidth / 2f)

    // Draw Motto (Right Side)
    bold("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", mottoCenterX, y, textSize, PdfAlign.Center)

    val yMottoLine2 = y + 13f * scale
    page.drawText("Độc lập – Tự do – Hạnh phúc", mottoCenterX, yMottoLine2, textSize, bold = true, align = PdfAlign.Center, underline = true)

    val yDateLine = yMottoLine2 + 22f * scale

    // Draw Date Line (Right Side)
    val dateLine = formatVietnameseContractDate(order.contractLocation, order.contractDate)
    text(dateLine, (pageWidth - margin).toFloat(), yDateLine, align = PdfAlign.Right)

    // Draw Logo (Left Side)
    val logoUriString = savedLessor?.logoUri
    val logoBytes = if (!logoUriString.isNullOrBlank() && context != null) readUriBytes(context, logoUriString) else null
    if (logoBytes != null) {
        val availableHeight = yDateLine - (margin.toFloat() + 8f * scale)
        val maxLogoHeight = availableHeight + 10f * scale // slight allowance
        page.drawImage(logoBytes, margin.toFloat(), margin.toFloat() + 4f * scale, maxLogoHeight)
    } else {
        // Fallback text if no logo configured or it fails to load
        page.drawText("NINOTEK", margin.toFloat(), y, 12f * scale, bold = true, color = COLOR_ORANGE)
    }

    y = yDateLine + 22f * scale

    // Title
    bold("HỢP ĐỒNG CHO THUÊ THIẾT BỊ", (pageWidth / 2).toFloat(), y, 15f * scale, PdfAlign.Center)

    y += 14f * scale
    text("Số: ${order.id.replace("#", "")}/HĐ-NINOTEK", (pageWidth / 2).toFloat(), y, align = PdfAlign.Center)

    y += 20f * scale

    val lName = if (order.lessorName.isNotBlank()) order.lessorName else (savedLessor?.name ?: "CÔNG TY TNHH NINOTEK")
    val lPhone = if (order.lessorPhone.isNotBlank()) order.lessorPhone else (savedLessor?.phone ?: "0901234567")
    val lAddr = if (order.lessorAddress.isNotBlank()) order.lessorAddress else (savedLessor?.address ?: "Số 123 Nguyễn Huệ, TP. Quy Nhơn, Tỉnh Bình Định")
    val lRep = if (order.lessorRepresentative.isNotBlank()) order.lessorRepresentative else (savedLessor?.representative ?: "Ông Nguyễn Văn A - Giám Đốc")

    val indentX = margin + 10 * scale

    // I. BÊN A
    bold("I. BÊN A (BÊN CHO THUÊ):", margin.toFloat(), y)
    y += 13f * scale
    text("• Đơn vị: $lName", indentX, y)
    y += 12f * scale
    text("• Địa chỉ: $lAddr", indentX, y)
    y += 12f * scale
    text("• Đại diện: $lRep", indentX, y)
    y += 12f * scale
    text("• Điện thoại: $lPhone", indentX, y)

    y += 16f * scale

    // II. BÊN B
    bold("II. BÊN B (BÊN THUÊ):", margin.toFloat(), y)
    y += 13f * scale
    val lesseeName = if (order.lesseeName.isNotBlank()) order.lesseeName else order.customerName
    val phoneStr = if (order.lesseePhone.isNotBlank()) order.lesseePhone else "090 123 4567"
    val addrStr = if (order.lesseeAddress.isNotBlank()) order.lesseeAddress else "123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định"
    val cccdStr = if (order.lesseeIdNumber.isNotBlank()) order.lesseeIdNumber else "012345678901"
    val issueStr = if (order.lesseeIdIssueDate.isNotBlank()) " (Cấp ngày: ${order.lesseeIdIssueDate})" else ""

    text("• Ông/Bà: $lesseeName", indentX, y)
    y += 12f * scale
    text("• Số CCCD/CMND: $cccdStr$issueStr", indentX, y)
    y += 12f * scale
    text("• Địa chỉ: $addrStr", indentX, y)
    y += 12f * scale
    text("• Điện thoại: $phoneStr", indentX, y)

    y += 16f * scale

    // III. NỘI DUNG VÀ BẢNG THIẾT BỊ
    bold("III. NỘI DUNG, ĐỐI TƯỢNG VÀ GIÁ TRỊ CỦA HỢP ĐỒNG:", margin.toFloat(), y)
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
    page.drawRect(tableLeft, y, tableRight, y + headerHeight, fillColor = COLOR_TABLE_HEADER)
    page.drawRect(tableLeft, y, tableRight, y + headerHeight, strokeWidth = lineWidth)

    val headers = arrayOf("Stt", "Tên máy móc, thiết bị", "SL", "Thời gian thuê", "Giá thuê/ngày")
    val aligns = arrayOf(PdfAlign.Center, PdfAlign.Left, PdfAlign.Center, PdfAlign.Center, PdfAlign.Right)

    fun cellX(curX: Float, index: Int): Float = when (aligns[index]) {
        PdfAlign.Center -> curX + (colWidths[index] / 2f)
        PdfAlign.Right -> curX + colWidths[index] - (4f * scale)
        PdfAlign.Left -> curX + (4f * scale)
    }

    var curX = tableLeft
    for (i in headers.indices) {
        bold(headers[i], cellX(curX, i), y + 12f * scale, align = aligns[i])
        curX += colWidths[i]
        if (i < headers.size - 1) {
            page.drawLine(curX, y, curX, y + headerHeight, lineWidth)
        }
    }

    y += headerHeight

    // Table Data Rows
    val items = getOrderEquipmentItems(order)
    val formattedDuration = formatRentalDuration(order.dateRange)
    val finalTotalPrice = if (order.netTotal.isNotBlank()) formatCurrencyAmount(order.netTotal) else formatCurrencyAmount(order.price)

    items.forEachIndexed { index, item ->
        val rowHeight = 20f * scale
        page.drawRect(tableLeft, y, tableRight, y + rowHeight, strokeWidth = lineWidth)

        val itemDailyPrice = if (item.pricePerDay.isNotBlank()) formatCurrencyAmount(item.pricePerDay) else formatDailyPrice(order.price, order.dateRange)
        val equipDisplayName = if (item.serialNumber.isNotBlank()) "${item.equipmentName} (Seri: ${item.serialNumber})" else item.equipmentName

        curX = tableLeft
        val rowData = arrayOf("${index + 1}", equipDisplayName, "01 bộ", formattedDuration, itemDailyPrice)
        for (i in rowData.indices) {
            text(rowData[i], cellX(curX, i), y + 14f * scale, align = aligns[i])
            curX += colWidths[i]
            if (i < rowData.size - 1) {
                page.drawLine(curX, y, curX, y + rowHeight, lineWidth)
            }
        }
        y += rowHeight
    }

    // Total Row
    val totalRowHeight = 20f * scale
    page.drawRect(tableLeft, y, tableRight, y + totalRowHeight, strokeWidth = lineWidth)
    val totalDividerX = tableLeft + colStt + colName + colQty + colTime
    page.drawLine(totalDividerX, y, totalDividerX, y + totalRowHeight, lineWidth)
    val totalLabel = if (order.discountAmount.isNotBlank() && order.discountAmount != "0") {
        "Tổng cộng thanh toán (Đã giảm ${formatCurrencyAmount(order.discountAmount)}):"
    } else {
        "Tổng cộng giá trị thanh toán:"
    }
    bold(totalLabel, tableLeft + 8f * scale, y + 14f * scale)
    bold(finalTotalPrice, tableRight - 8f * scale, y + 14f * scale, align = PdfAlign.Right)

    y += totalRowHeight + 14f * scale

    // IV. ĐIỀU KHOẢN THANH TOÁN & THẾ CHẤP
    val advanceStr = formatCurrencyAmount(if (order.advancePaymentAmount.isNotBlank()) order.advancePaymentAmount else "2.000.000")
    bold("IV. ĐIỀU KHOẢN THANH TOÁN VÀ THẾ CHẤP:", margin.toFloat(), y)
    y += 13f * scale
    text("1. Số tiền thanh toán trước (đặt cọc thuê): $advanceStr", indentX, y)
    y += 12f * scale
    text("2. Danh mục tài sản & giấy tờ thế chấp (giữ lại):", indentX, y)
    y += 12f * scale

    val cccdCheck = if (order.collateralCccd) "[X] CCCD gốc" else "[  ] CCCD gốc"
    val gplxCheck = if (order.collateralGplx) "[X] GPLX gốc" else "[  ] GPLX gốc"
    val hasAsset = order.collateralAssetDescription.isNotBlank()
    val assetCheck = if (hasAsset) "[X] Tài sản khác: ${order.collateralAssetDescription}" else "[  ] Tài sản khác: Xe máy / Giấy tờ khác"
    val hasCash = order.collateralCashAmount.isNotBlank() && order.collateralCashAmount != "0"
    val cashStr = if (hasCash) formatCurrencyAmount(order.collateralCashAmount) else "5.000.000"
    val cashCheck = if (hasCash) "[X] Tiền đặt cọc thế chấp: $cashStr" else "[  ] Tiền đặt cọc thế chấp: $cashStr"

    text("  $cccdCheck     $gplxCheck", indentX, y)
    y += 12f * scale
    text("  $assetCheck", indentX, y)
    y += 12f * scale
    text("  $cashCheck", indentX, y)

    y += 16f * scale

    // V. TRÁCH NHIỆM & BỒI THƯỜNG
    bold("V. TRÁCH NHIỆM VÀ BỒI THƯỜNG TRONG QUÁ TRÌNH THUÊ:", margin.toFloat(), y)
    y += 13f * scale
    text("1. Bên B có trách nhiệm kiểm tra kỹ tình trạng máy móc, thiết bị trước khi nhận bàn giao.", indentX, y)
    y += 12f * scale
    text("2. Trong thời gian thuê, nếu hư hỏng hoặc mất mát do lỗi Bên B phải bồi thường 100%.", indentX, y)
    y += 12f * scale
    text("3. Bên B cam kết bàn giao lại thiết bị đúng thời hạn quy định. Quá hạn sẽ tính phí phát sinh.", indentX, y)

    y += 20f * scale

    // VI. CHỮ KÝ CÁC BÊN
    bold("VI. CHỮ KÝ CÁC BÊN:", margin.toFloat(), y)
    y += 15f * scale

    val colA = margin + 90f * scale
    val colB = pageWidth - margin - 90f * scale

    bold("ĐẠI DIỆN BÊN A", colA, y, align = PdfAlign.Center)
    bold("ĐẠI DIỆN BÊN B", colB, y, align = PdfAlign.Center)

    y += 11f * scale
    text("(Ký, ghi rõ họ tên & đóng dấu)", colA, y, 8f * scale, align = PdfAlign.Center)
    text("(Ký & ghi rõ họ tên)", colB, y, 8f * scale, align = PdfAlign.Center)

    y += 38f * scale
    bold(lRep.substringBefore("-").trim(), colA, y, textSize, PdfAlign.Center)
    bold(lesseeName, colB, y, textSize, PdfAlign.Center)
}
