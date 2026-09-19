package com.ninotek.ninorent.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.PaperSize
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.defaultLessorInfo
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.formatCurrencyAmount
import com.ninotek.ninorent.utils.formatDailyPrice
import com.ninotek.ninorent.utils.formatRentalDuration
import com.ninotek.ninorent.utils.formatVietnameseContractDate
import com.ninotek.ninorent.utils.getOrderEquipmentItems
import com.ninotek.ninorent.utils.printContract
import com.ninotek.ninorent.utils.shareContractPdf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractPrintScreen(
    modifier: Modifier = Modifier,
    order: RentalOrder = sampleRentalOrder,
    lessorInfo: LessorInfo = defaultLessorInfo,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var selectedPaperSize by remember { mutableStateOf(PaperSize.A4) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Xem trước & In Hợp đồng", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { shareContractPdf(context, order, selectedPaperSize) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, PrimaryOrange),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chia sẻ / PDF", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { printContract(context, order, selectedPaperSize) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("In hợp đồng", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        containerColor = Color(0xFFEFEFEF) // Gray workspace background to mimic doc viewer
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Paper Size Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null, tint = PrimaryOrange)
                        Column {
                            Text("Khổ giấy in", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (selectedPaperSize == PaperSize.A4) "Tiêu chuẩn A4 (210 x 297 mm)" else "Nhỏ gọn A5 (148 x 210 mm)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    // Segmented Filter Chips for Paper Size Selection
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedPaperSize == PaperSize.A4,
                            onClick = { selectedPaperSize = PaperSize.A4 },
                            label = { Text("Khổ A4", fontWeight = FontWeight.Bold) },
                            leadingIcon = if (selectedPaperSize == PaperSize.A4) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryOrange,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedPaperSize == PaperSize.A5,
                            onClick = { selectedPaperSize = PaperSize.A5 },
                            label = { Text("Khổ A5", fontWeight = FontWeight.Bold) },
                            leadingIcon = if (selectedPaperSize == PaperSize.A5) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryOrange,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
            }

            // Paper Document Card styled identically to Mau-Hop-dong-thue-may-moc-thiet-bi.docx
            PaperDocumentCard(
                order = order,
                paperSize = selectedPaperSize,
                lessorInfo = lessorInfo,
                modifier = Modifier
                    .padding(horizontal = if (selectedPaperSize == PaperSize.A4) 12.dp else 24.dp)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun PaperDocumentCard(
    order: RentalOrder,
    paperSize: PaperSize,
    lessorInfo: LessorInfo = defaultLessorInfo,
    modifier: Modifier = Modifier
) {
    val scale = if (paperSize == PaperSize.A4) 1.0f else 0.84f
    val paddingDp = if (paperSize == PaperSize.A4) 18.dp else 12.dp
    val textPrimaryColor = Color(0xFF111111)

    fun scaledSp(baseSp: Float): TextUnit = (baseSp * scale).sp

    val lName = order.lessorName.ifBlank { lessorInfo.name }.ifBlank { "Công ty TNHH Công nghệ & Dịch vụ NINOTEK" }
    val lAddr = order.lessorAddress.ifBlank { lessorInfo.address }
    val lRep = order.lessorRepresentative.ifBlank { lessorInfo.representative }
    val lPhone = order.lessorPhone.ifBlank { lessorInfo.phone }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(4.dp))
            .animateContentSize(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingDp),
            verticalArrangement = Arrangement.spacedBy((8 * scale).dp)
        ) {
            // Header Row: Logo NINOTEK & National Motto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Header: NINOTEK Brand
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PrecisionManufacturing,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size((22 * scale).dp)
                        )
                        Text(
                            text = "NINOTEK",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = scaledSp(16f),
                            color = PrimaryOrange
                        )
                    }
                    Text(
                        text = lName,
                        fontWeight = FontWeight.Bold,
                        fontSize = scaledSp(10f),
                        color = textPrimaryColor
                    )
                    Text(
                        text = "SĐT: $lPhone",
                        fontSize = scaledSp(9f),
                        color = Color.DarkGray
                    )
                }

                // Right Header: National Motto (Quốc hiệu & Tiêu ngữ)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM",
                        fontWeight = FontWeight.Bold,
                        fontSize = scaledSp(10.5f),
                        color = textPrimaryColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height((2 * scale).dp))
                    Text(
                        text = "Độc lập – Tự do – Hạnh phúc",
                        fontWeight = FontWeight.Bold,
                        fontSize = scaledSp(10f),
                        textDecoration = TextDecoration.Underline,
                        color = textPrimaryColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Contract Date Line
            val formattedDateLine = formatVietnameseContractDate(order.contractLocation, order.contractDate)
            Text(
                text = formattedDateLine,
                fontSize = scaledSp(10f),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                color = textPrimaryColor
            )

            Spacer(modifier = Modifier.height((2 * scale).dp))

            // Contract Title
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HỢP ĐỒNG CHO THUÊ THIẾT BỊ",
                    fontWeight = FontWeight.Bold,
                    fontSize = scaledSp(16f),
                    color = textPrimaryColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height((2 * scale).dp))
                Text(
                    text = "Số: ${order.id.replace("#", "")}/HĐ-NINOTEK",
                    fontSize = scaledSp(10f),
                    fontStyle = FontStyle.Italic,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height((4 * scale).dp))

            // I. BÊN A (BÊN CHO THUÊ)
            Column(verticalArrangement = Arrangement.spacedBy((2 * scale).dp)) {
                Text(
                    text = "I. BÊN A (BÊN CHO THUÊ):",
                    fontWeight = FontWeight.Bold,
                    fontSize = scaledSp(11f),
                    color = textPrimaryColor
                )
                Text(text = "• Đơn vị: $lName", fontSize = scaledSp(10f), color = textPrimaryColor)
                Text(text = "• Địa chỉ: $lAddr", fontSize = scaledSp(10f), color = textPrimaryColor)
                Text(text = "• Đại diện: $lRep", fontSize = scaledSp(10f), color = textPrimaryColor)
                Text(text = "• Điện thoại: $lPhone", fontSize = scaledSp(10f), color = textPrimaryColor)
            }

            Spacer(modifier = Modifier.height((3 * scale).dp))

            // II. BÊN B (BÊN THUÊ)
            val lesseeName = if (order.lesseeName.isNotBlank()) order.lesseeName else order.customerName
            val cccdStr = if (order.lesseeIdNumber.isNotBlank()) order.lesseeIdNumber else "012345678901"
            val issueStr = if (order.lesseeIdIssueDate.isNotBlank()) " (Cấp ngày: ${order.lesseeIdIssueDate})" else ""
            val phoneStr = if (order.lesseePhone.isNotBlank()) order.lesseePhone else "090 123 4567"
            val addrStr = if (order.lesseeAddress.isNotBlank()) order.lesseeAddress else "123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định"

            Column(verticalArrangement = Arrangement.spacedBy((2 * scale).dp)) {
                Text(
                    text = "II. BÊN B (BÊN THUÊ):",
                    fontWeight = FontWeight.Bold,
                    fontSize = scaledSp(11f),
                    color = textPrimaryColor
                )
                Text(text = "• Ông/Bà: $lesseeName", fontSize = scaledSp(10f), color = textPrimaryColor)
                Text(text = "• Số CCCD/CMND: $cccdStr$issueStr", fontSize = scaledSp(10f), color = textPrimaryColor)
                Text(text = "• Địa chỉ: $addrStr", fontSize = scaledSp(10f), color = textPrimaryColor)
                Text(text = "• Điện thoại: $phoneStr", fontSize = scaledSp(10f), color = textPrimaryColor)
            }

            Spacer(modifier = Modifier.height((3 * scale).dp))

            // III. NỘI DUNG, ĐỐI TƯỢNG VÀ GIÁ TRỊ CỦA HỢP ĐỒNG
            Text(
                text = "III. NỘI DUNG, ĐỐI TƯỢNG VÀ GIÁ TRỊ CỦA HỢP ĐỒNG:",
                fontWeight = FontWeight.Bold,
                fontSize = scaledSp(11f),
                color = textPrimaryColor
            )

            val formattedDuration = formatRentalDuration(order.dateRange)
            val finalTotalPrice = if (order.netTotal.isNotBlank()) formatCurrencyAmount(order.netTotal) else formatCurrencyAmount(order.price)
            val items = getOrderEquipmentItems(order)

            // 5-Column Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
            ) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEEEEEE)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(text = "Stt", weight = 0.7f, isHeader = true, scale = scale, textAlign = TextAlign.Center)
                    TableCell(text = "Tên máy móc, thiết bị", weight = 3.1f, isHeader = true, scale = scale, textAlign = TextAlign.Center)
                    TableCell(text = "SL", weight = 1.0f, isHeader = true, scale = scale, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                    TableCell(text = "Thời gian thuê", weight = 2.7f, isHeader = true, scale = scale, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                    TableCell(text = "Giá thuê/ngày", weight = 2.5f, isHeader = true, scale = scale, maxLines = 1, softWrap = false, textAlign = TextAlign.End, showRightBorder = false)
                }

                items.forEachIndexed { index, item ->
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                    val equipDisplayName = if (item.serialNumber.isNotBlank()) "${item.equipmentName}\n(Seri: ${item.serialNumber})" else item.equipmentName
                    val itemDailyPrice = if (item.pricePerDay.isNotBlank()) formatCurrencyAmount(item.pricePerDay) else formatDailyPrice(order.price, order.dateRange)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell(text = "${index + 1}", weight = 0.7f, scale = scale, textAlign = TextAlign.Center)
                        TableCell(text = equipDisplayName, weight = 3.1f, scale = scale, textAlign = TextAlign.Start)
                        TableCell(text = "01 bộ", weight = 1.0f, scale = scale, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                        TableCell(text = formattedDuration, weight = 2.7f, scale = scale, maxLines = Int.MAX_VALUE, softWrap = true, textAlign = TextAlign.Center)
                        TableCell(text = itemDailyPrice, weight = 2.5f, scale = scale, maxLines = 1, softWrap = false, textAlign = TextAlign.End, showRightBorder = false)
                    }
                }

                HorizontalDivider(color = Color.Black, thickness = 1.dp)

                // Total Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9F9)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val labelText = if (order.discountAmount.isNotBlank() && order.discountAmount != "0") {
                        "Tổng cộng giá trị (Đã giảm ${formatCurrencyAmount(order.discountAmount)}):"
                    } else {
                        "Tổng cộng giá trị thanh toán:"
                    }
                    TableCell(
                        text = labelText,
                        weight = 7.5f,
                        scale = scale,
                        isHeader = true,
                        textAlign = TextAlign.Start,
                        showRightBorder = true
                    )
                    TableCell(
                        text = finalTotalPrice,
                        weight = 2.5f,
                        scale = scale,
                        isHeader = true,
                        textAlign = TextAlign.End,
                        showRightBorder = false
                    )
                }
            }

            Spacer(modifier = Modifier.height((3 * scale).dp))

            // IV. ĐIỀU KHOẢN THANH TOÁN VÀ THẾ CHẤP
            val formattedAdvance = formatCurrencyAmount(if (order.advancePaymentAmount.isNotBlank()) order.advancePaymentAmount else "2.000.000")
            val hasCash = order.collateralCashAmount.isNotBlank() && order.collateralCashAmount != "0"
            val cashBox = if (hasCash) "☑" else "☐"
            val formattedCash = if (hasCash) formatCurrencyAmount(order.collateralCashAmount) else "5.000.000"

            Column(verticalArrangement = Arrangement.spacedBy((2 * scale).dp)) {
                Text(
                    text = "IV. ĐIỀU KHOẢN THANH TOÁN VÀ THẾ CHẤP:",
                    fontWeight = FontWeight.Bold,
                    fontSize = scaledSp(11f),
                    color = textPrimaryColor
                )
                Text(
                    text = "1. Số tiền thanh toán trước (đặt cọc thuê): $formattedAdvance",
                    fontSize = scaledSp(10f),
                    color = textPrimaryColor,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "2. Danh mục tài sản & giấy tờ thế chấp (giữ lại):",
                    fontSize = scaledSp(10f),
                    fontWeight = FontWeight.Medium,
                    color = textPrimaryColor
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (8 * scale).dp),
                    horizontalArrangement = Arrangement.spacedBy((16 * scale).dp)
                ) {
                    val cccdBox = if (order.collateralCccd) "☑" else "☐"
                    val gplxBox = if (order.collateralGplx) "☑" else "☐"
                    Text("$cccdBox CCCD gốc", fontSize = scaledSp(9.5f), color = textPrimaryColor)
                    Text("$gplxBox GPLX gốc", fontSize = scaledSp(9.5f), color = textPrimaryColor)
                }

                val hasAsset = order.collateralAssetDescription.isNotBlank()
                val assetBox = if (hasAsset) "☑" else "☐"
                val assetStr = if (hasAsset) order.collateralAssetDescription else "Xe máy / Giấy tờ khác"
                Text(
                    text = "  $assetBox Tài sản khác: $assetStr",
                    fontSize = scaledSp(9.5f),
                    color = textPrimaryColor,
                    modifier = Modifier.padding(start = (4 * scale).dp)
                )

                Text(
                    text = "  $cashBox Tiền đặt cọc thế chấp: $formattedCash",
                    fontSize = scaledSp(9.5f),
                    color = textPrimaryColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = (4 * scale).dp)
                )
            }

            Spacer(modifier = Modifier.height((3 * scale).dp))

            // V. TRÁCH NHIỆM VÀ BỒI THƯỜNG
            Column(verticalArrangement = Arrangement.spacedBy((2 * scale).dp)) {
                Text(
                    text = "V. TRÁCH NHIỆM VÀ BỒI THƯỜNG TRONG QUÁ TRÌNH THUÊ:",
                    fontWeight = FontWeight.Bold,
                    fontSize = scaledSp(11f),
                    color = textPrimaryColor
                )
                Text(
                    text = "1. Bên B có trách nhiệm kiểm tra kỹ tình trạng máy móc, thiết bị trước khi nhận bàn giao. Thiết bị được giao ở trạng thái hoạt động tốt, đầy đủ phụ kiện kèm theo.",
                    fontSize = scaledSp(9.5f),
                    color = textPrimaryColor
                )
                Text(
                    text = "2. Trong thời gian thuê, Bên B có trách nhiệm bảo quản thiết bị cẩn thận. Nếu xảy ra mất mát, hư hỏng hoặc sự cố do lỗi Bên B, Bên B cam kết bồi thường 100% chi phí sửa chữa hoặc hoàn trả toàn bộ giá trị thiết bị theo giá thị trường tại thời điểm bồi thường.",
                    fontSize = scaledSp(9.5f),
                    color = textPrimaryColor
                )
                Text(
                    text = "3. Bên B cam kết bàn giao lại thiết bị đúng thời hạn quy định. Quá hạn sẽ tính phí phát sinh theo giá niêm yết.",
                    fontSize = scaledSp(9.5f),
                    color = textPrimaryColor
                )
            }

            Spacer(modifier = Modifier.height((4 * scale).dp))

            // VI. CHỮ KÝ CÁC BÊN
            Text(
                text = "VI. CHỮ KÝ CÁC BÊN:",
                fontWeight = FontWeight.Bold,
                fontSize = scaledSp(11f),
                color = textPrimaryColor
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (2 * scale).dp, start = (20 * scale).dp, end = (20 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bên A Signature Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ĐẠI DIỆN BÊN A", fontWeight = FontWeight.Bold, fontSize = scaledSp(10.5f), color = textPrimaryColor)
                    Text("(Ký, ghi rõ họ tên & đóng dấu)", fontSize = scaledSp(8.5f), fontStyle = FontStyle.Italic, color = Color.Gray)
                    Spacer(modifier = Modifier.height((32 * scale).dp))
                    Text(
                        lRep.substringBefore("-").trim(),
                        fontWeight = FontWeight.Bold,
                        fontSize = scaledSp(10f),
                        color = textPrimaryColor
                    )
                }

                // Bên B Signature Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ĐẠI DIỆN BÊN B", fontWeight = FontWeight.Bold, fontSize = scaledSp(10.5f), color = textPrimaryColor)
                    Text("(Ký & ghi rõ họ tên)", fontSize = scaledSp(8.5f), fontStyle = FontStyle.Italic, color = Color.Gray)
                    Spacer(modifier = Modifier.height((32 * scale).dp))
                    Text(
                        lesseeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = scaledSp(10f),
                        color = textPrimaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    scale: Float,
    isHeader: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    textAlign: TextAlign = if (isHeader) TextAlign.Center else TextAlign.Start,
    showRightBorder: Boolean = true
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .drawBehind {
                if (showRightBorder) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .padding((4 * scale).dp),
        contentAlignment = when (textAlign) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            fontSize = ((if (isHeader) 9.5f else 9f) * scale).sp,
            color = Color.Black,
            textAlign = textAlign,
            maxLines = maxLines,
            softWrap = softWrap
        )
    }
}

val sampleRentalOrder = RentalOrder(
    id = "#DH001",
    equipmentName = "Sony Alpha A7C + Ống kính 24-70mm",
    dateRange = "10/09/2026 - 12/09/2026 (3 ngày)",
    price = "6.000.000₫",
    status = "Đang thuê",
    customerName = "Nguyễn Văn Nam",
    lessorName = "Công ty TNHH Công nghệ & Dịch vụ NINOTEK",
    lessorAddress = "125/2 Hai Bà Trưng, Phường Quy Nhơn, Tỉnh Gia Lai",
    lessorRepresentative = "Ông Lê Trung Hiếu - Giám Đốc",
    lessorPhone = "0901992349",
    lesseeName = "Nguyễn Văn Nam",
    lesseePhone = "090 123 4567",
    lesseeAddress = "123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định",
    lesseeIdNumber = "012345678901",
    lesseeIdIssueDate = "01/01/2021",
    advancePaymentAmount = "2.000.000₫",
    collateralCccd = true,
    collateralGplx = false,
    collateralAssetDescription = "Xe máy Honda Vision BKS 77F1-123.45",
    collateralCashAmount = "5.000.000₫",
    contractLocation = "TP. Quy Nhơn, Bình Định",
    contractDate = "10/09/2026"
)

@Preview(showBackground = true)
@Composable
fun ContractPrintScreenPreview() {
    NinoRentTheme {
        ContractPrintScreen()
    }
}
