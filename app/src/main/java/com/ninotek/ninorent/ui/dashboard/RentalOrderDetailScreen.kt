package com.ninotek.ninorent.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.defaultLessorInfo
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.ui.theme.StatusReady
import com.ninotek.ninorent.ui.theme.StatusRented
import com.ninotek.ninorent.utils.formatCurrencyAmount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalOrderDetailScreen(
    order: RentalOrder = sampleRentalOrder,
    onBack: () -> Unit,
    lessorInfo: LessorInfo = defaultLessorInfo,
    isAdmin: Boolean = false,
    onPrintContract: () -> Unit = {},
    onExtendOrder: (RentalOrder) -> Unit = {},
    onCreateExtensionOrder: (RentalOrder, Double) -> Unit = { _, _ -> },
    onEditOrder: (RentalOrder) -> Unit = {},
    onDeleteOrder: (RentalOrder) -> Unit = {},
    onPayOrder: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showExtendDialog by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOverdueDialog by remember { mutableStateOf(false) }

    BackHandler {
        when {
            showEditDialog -> showEditDialog = false
            showExtendDialog -> showExtendDialog = false
            showOverdueDialog -> showOverdueDialog = false
            showPayDialog -> showPayDialog = false
            else -> onBack()
        }
    }

    val statusColor = when (order.status) {
        "Đang thuê" -> StatusRented
        "Đã trả" -> StatusReady
        else -> Color.Red
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết đơn thuê ${order.id}", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Chỉnh sửa đơn", tint = PrimaryOrange)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Xóa đơn", tint = Color.Red)
                        }
                    }
                    IconButton(onClick = onPrintContract) {
                        Icon(Icons.Rounded.Print, contentDescription = "In hợp đồng", tint = PrimaryOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (order.status != "Đã trả") {
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
                        if (order.status == "Quá hạn") {
                            OutlinedButton(
                                onClick = { showPayDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Rounded.TaskAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ngoài ý muốn (Miễn phí)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showOverdueDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Phát sinh thêm (Tính phí)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showExtendDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                            ) {
                                Icon(Icons.Rounded.Update, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gia hạn", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showPayDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Icon(Icons.Rounded.Payment, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Thanh toán", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = order.id,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Thời gian: ${order.dateRange}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = order.status,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = statusColor
                        )
                    }
                }
            }

            // Contract Parties Card (Bên A & Bên B)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thông tin Hợp đồng & Bên B",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE3F2FD)
                        ) {
                            Text(
                                text = "Đã ký",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                    HorizontalDivider()

                    val nameToShow = if (order.lesseeName.isNotBlank()) order.lesseeName else order.customerName
                    val phoneToShow = if (order.lesseePhone.isNotBlank()) order.lesseePhone else "090 123 4567"
                    val cccdToShow = if (order.lesseeIdNumber.isNotBlank()) order.lesseeIdNumber else "012345678901"

                    Text("Bên B (Bên Thuê):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                        Text(text = nameToShow, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Text(text = phoneToShow, style = MaterialTheme.typography.bodyMedium, color = PrimaryOrange, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Badge, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Text(text = "CCCD/GPLX: $cccdToShow", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (order.lesseeAddress.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Text(text = order.lesseeAddress, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Bên A (Bên Cho Thuê):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val lName = order.lessorName.ifBlank { lessorInfo.name }
                    val lRep = order.lessorRepresentative.ifBlank { lessorInfo.representative }
                    val lAddr = order.lessorAddress.ifBlank { lessorInfo.address }
                    Text(lName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Text("Đại diện: $lRep", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("Địa chỉ: $lAddr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // Print & Preview Contract Banner Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPrintContract() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryOrange.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, PrimaryOrange)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("Xem trước & In hợp đồng", fontWeight = FontWeight.Bold, color = PrimaryOrange, fontSize = 15.sp)
                            Text("Tùy chọn khổ A4 & A5, xuất PDF", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = PrimaryOrange)
                }
            }

            // Retained Documents & Collateral Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Giấy tờ & Tài sản thế chấp (Giữ lại)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()

                    if (order.collateralCccd) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Text("Giữ CCCD gốc", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (order.collateralGplx) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Text("Giữ GPLX gốc", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (order.collateralAssetDescription.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Text("Tài sản khác: ${order.collateralAssetDescription}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (order.collateralCashAmount.isNotBlank() && order.collateralCashAmount != "0") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Text("Tiền đặt cọc thế chấp: ${formatCurrencyAmount(order.collateralCashAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Equipment Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Thiết bị thuê",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = PrimaryOrange,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = order.equipmentName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mã đơn: ${order.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Price & Payment Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Chi tiết thanh toán",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Tổng giá thuê:", color = Color.Gray)
                        Text(text = formatCurrencyAmount(order.netTotal.ifBlank { order.price }), fontWeight = FontWeight.Bold, color = PrimaryOrange)
                    }
                    if (order.advancePaymentAmount.isNotBlank() && order.advancePaymentAmount != "0") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Đã cọc trước:", color = Color.Gray)
                            Text(text = formatCurrencyAmount(order.advancePaymentAmount), fontWeight = FontWeight.Medium, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }
    }

    // Gia hạn Dialog
    if (showExtendDialog) {
        var extraDays by remember { mutableStateOf("3") }

        AlertDialog(
            onDismissRequest = { showExtendDialog = false },
            title = { Text("Gia hạn hợp đồng thuê", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thiết bị: ${order.equipmentName}", fontSize = 13.sp)
                    Text("Thời gian hiện tại: ${order.dateRange}", fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = extraDays,
                        onValueChange = { extraDays = it },
                        label = { Text("Số ngày gia hạn thêm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = extraDays == "1", onClick = { extraDays = "1" }, label = { Text("+1 ngày") })
                        FilterChip(selected = extraDays == "3", onClick = { extraDays = "3" }, label = { Text("+3 ngày") })
                        FilterChip(selected = extraDays == "7", onClick = { extraDays = "7" }, label = { Text("+7 ngày") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val daysToAdd = extraDays.toIntOrNull() ?: 1
                        val updatedOrder = order.copy(
                            dateRange = "${order.dateRange.substringBefore("(").trim()} (đã gia hạn +$daysToAdd ngày)",
                            price = "${(order.price.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 6000000) + (daysToAdd * 1000000)}đ"
                        )
                        onExtendOrder(updatedOrder)
                        showExtendDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Xác nhận gia hạn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtendDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Thanh toán Dialog
    if (showPayDialog) {
        var selectedPaymentMethod by remember { mutableStateOf("Chuyển khoản QR") }
        var returnCollateralConfirmed by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            title = { Text("Thanh toán & Đóng đơn thuê", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Mã đơn thuê: ${order.id}", fontWeight = FontWeight.Bold, color = PrimaryOrange)
                    Text("Khách hàng: ${order.customerName}")
                    Text("Tổng số tiền thanh toán: ${order.price}", fontWeight = FontWeight.Bold)

                    HorizontalDivider()

                    Text("Phương thức thanh toán:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedPaymentMethod == "Chuyển khoản QR",
                            onClick = { selectedPaymentMethod = "Chuyển khoản QR" },
                            label = { Text("Chuyển khoản QR") }
                        )
                        FilterChip(
                            selected = selectedPaymentMethod == "Tiền mặt",
                            onClick = { selectedPaymentMethod = "Tiền mặt" },
                            label = { Text("Tiền mặt") }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { returnCollateralConfirmed = !returnCollateralConfirmed }
                    ) {
                        Checkbox(
                            checked = returnCollateralConfirmed,
                            onCheckedChange = { returnCollateralConfirmed = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đã bàn giao đầy đủ giấy tờ & cọc lại cho khách hàng", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPayOrder(order.id)
                        showPayDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Hoàn tất thanh toán")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showOverdueDialog) {
        var extraDays by remember { mutableDoubleStateOf(0.5) }

        AlertDialog(
            onDismissRequest = { showOverdueDialog = false },
            title = { Text("Tính phí phát sinh", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thiết bị: ${order.equipmentName}", fontSize = 13.sp)
                    Text("Chọn số ngày quá hạn (phát sinh):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (extraDays % 1.0 == 0.0) "${extraDays.toInt()}" else "$extraDays",
                            onValueChange = {
                                val newVal = it.replace(",", ".").toDoubleOrNull()
                                if (newVal != null && newVal >= 0.0) {
                                    extraDays = newVal
                                } else if (it.isBlank()) {
                                    extraDays = 0.0
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { if (extraDays >= 0.5) extraDays -= 0.5 },
                                modifier = Modifier.background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).size(40.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Giảm")
                            }
                            IconButton(
                                onClick = { extraDays += 0.5 },
                                modifier = Modifier.background(PrimaryOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).size(40.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Tăng", tint = PrimaryOrange)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateExtensionOrder(order, extraDays)
                        showOverdueDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Tạo đơn phát sinh")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverdueDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (showEditDialog) {
        EditRentalOrderDialog(
            order = order,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedOrder ->
                onEditOrder(updatedOrder)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Xác nhận xóa đơn thuê ${order.id}? Thao tác này sẽ xóa đơn vĩnh viễn và chuyển các thiết bị về trạng thái 'Sẵn sàng'.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteOrder(order)
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text("Xóa", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy", color = Color.Gray)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RentalOrderDetailScreenPreview() {
    NinoRentTheme {
        RentalOrderDetailScreen(onBack = {})
    }
}
