package com.ninotek.ninorent.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.model.Customer
import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.defaultDevicesList
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.style.TextAlign
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.formatCurrencyAmount
import com.ninotek.ninorent.utils.formatCurrencyInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    devicesList: List<Equipment> = defaultDevicesList,
    ordersList: List<RentalOrder> = emptyList(),
    customersList: List<Customer> = emptyList(),
    onAddEquipment: (Equipment) -> Unit = {},
    onEditEquipment: (Equipment) -> Unit = {},
    onDeleteEquipment: (Equipment) -> Unit = {},
    onCreateOrder: (RentalOrder) -> Unit = {},
    onRefreshEquipment: () -> Unit = {},
    isRefreshing: Boolean = false,
    initialFilter: String? = null,
    onFilterConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(initialFilter ?: "Tất cả") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedEquipmentForEdit by remember { mutableStateOf<Equipment?>(null) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteBlockedAlert by remember { mutableStateOf(false) }
    var selectedEquipmentForDelete by remember { mutableStateOf<Equipment?>(null) }

    var selectedEquipmentForRental by remember { mutableStateOf<Equipment?>(null) }
    var showQuickRentalSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefreshEquipment()
    }

    LaunchedEffect(initialFilter) {
        if (initialFilter != null) {
            selectedCategory = initialFilter
            onFilterConsumed()
        }
    }

    val categories = listOf("Tất cả", "Sẵn sàng", "Đang thuê", "Tạm ngưng", "Máy ảnh", "Máy Game & Phụ kiện", "Máy quay", "Phụ kiện", "Điện thoại", "Laptop")

    val filteredDevices = devicesList.filter { device ->
        val matchesSearch = device.name.contains(searchQuery, ignoreCase = true) ||
                device.categorySubtitle.contains(searchQuery, ignoreCase = true) ||
                device.serialNumber.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedCategory) {
            "Sẵn sàng" -> device.status == "Sẵn sàng"
            "Đang thuê" -> device.status == "Đang thuê"
            "Tạm ngưng" -> device.status == "Tạm ngưng"
            "Máy ảnh" -> device.category == "Máy ảnh"
            "Máy Game & Phụ kiện" -> device.category == "Máy Game & Phụ kiện" || device.category.contains("Game", ignoreCase = true)
            "Máy quay" -> device.category == "Máy quay"
            "Phụ kiện" -> device.category == "Phụ kiện" || device.category.contains("Phụ kiện", ignoreCase = true)
            "Điện thoại" -> device.category == "Điện thoại"
            "Laptop" -> device.category == "Laptop"
            else -> true
        }
        matchesSearch && matchesCategory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm tên, loại, mã serial...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryOrange,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                } else {
                    Text("Thiết bị", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            actions = {
                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = "Tìm kiếm"
                    )
                }
                IconButton(
                    onClick = { onRefreshEquipment() },
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PrimaryOrange,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới từ Supabase",
                            tint = PrimaryOrange
                        )
                    }
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm thiết bị", tint = PrimaryOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Category & Status Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                        selectedLabelColor = PrimaryOrange
                    )
                )
            }
        }

        // Equipment List with PullToRefresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefreshEquipment() },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (devicesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = PrimaryOrange
                        )
                        Text(
                            text = "Chưa có dữ liệu thiết bị",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Danh sách thiết bị hiện đang trống. Bấm nút bên dưới để tải lại dữ liệu từ Supabase.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onRefreshEquipment() },
                            enabled = !isRefreshing,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang tải dữ liệu...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tải dữ liệu từ Supabase", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            } else if (filteredDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.Gray.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Không tìm thấy thiết bị phù hợp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hãy thử đổi từ khóa tìm kiếm, chọn danh mục khác, hoặc bấm nút bên dưới để tải lại dữ liệu từ Supabase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onRefreshEquipment() },
                            enabled = !isRefreshing,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang tải dữ liệu...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tải dữ liệu từ Supabase", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredDevices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedEquipmentForRental = device
                                    showQuickRentalSheet = true
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = device.icon,
                                            contentDescription = device.name,
                                            tint = PrimaryOrange,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${device.categorySubtitle}${if (device.serialNumber.isNotBlank()) " • Serial: ${device.serialNumber}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatCurrencyAmount(device.pricePerDay),
                                            color = PrimaryOrange,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                val statusColor = when (device.status) {
                                    "Sẵn sàng" -> Color(0xFF2E7D32)
                                    "Đang thuê" -> Color(0xFFEF6C00)
                                    "Tạm ngưng" -> Color(0xFFC62828)
                                    else -> Color.Gray
                                }
                                val statusBg = statusColor.copy(alpha = 0.12f)

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = statusBg
                                    ) {
                                        Text(
                                            text = device.status,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                selectedEquipmentForEdit = device
                                                showEditDialog = true
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Sửa thiết bị",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                selectedEquipmentForDelete = device
                                                val hasOrders = ordersList.any { order ->
                                                    order.equipmentName.contains(device.name, ignoreCase = true) ||
                                                    order.equipmentItems.any { it.equipmentName.contains(device.name, ignoreCase = true) }
                                                }
                                                if (hasOrders) {
                                                    showDeleteBlockedAlert = true
                                                } else {
                                                    showDeleteConfirmDialog = true
                                                }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Xóa thiết bị",
                                                tint = Color(0xFFD32F2F),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                selectedEquipmentForRental = device
                                                showQuickRentalSheet = true
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AddCircle,
                                                contentDescription = "Tạo đơn nhanh",
                                                tint = PrimaryOrange,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Equipment Dialog ("Thêm thiết bị mới")
    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("Máy ảnh") }
        var newSubtitle by remember { mutableStateOf("Máy ảnh - Mirrorless") }
        var newPrice by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm thiết bị mới", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Tên thiết bị *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSubtitle,
                        onValueChange = { newSubtitle = it },
                        label = { Text("Loại chi tiết (VD: Máy ảnh - Mirrorless)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPrice,
                        onValueChange = { newPrice = formatCurrencyInput(it) },
                        label = { Text("Giá thuê (VNĐ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val newItem = Equipment(
                                id = (devicesList.size + 10).toString(),
                                name = newName,
                                category = newCategory,
                                categorySubtitle = newSubtitle.ifBlank { "Thiết bị điện tử" },
                                pricePerDay = formatCurrencyAmount(newPrice).ifBlank { "0" },
                                status = "Sẵn sàng",
                                icon = Icons.Rounded.Devices,
                                serialNumber = ""
                            )
                            onAddEquipment(newItem)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Thêm thiết bị")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Edit Equipment Dialog ("Chỉnh sửa thiết bị")
    if (showEditDialog && selectedEquipmentForEdit != null) {
        val equip = selectedEquipmentForEdit!!
        var editName by remember { mutableStateOf(equip.name) }
        var editCategory by remember { mutableStateOf(equip.category) }
        var editSubtitle by remember { mutableStateOf(equip.categorySubtitle) }
        var editPrice by remember { mutableStateOf(formatCurrencyAmount(equip.pricePerDay)) }
        var editSerial by remember { mutableStateOf(equip.serialNumber) }
        var editStatus by remember { mutableStateOf(equip.status) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Chỉnh sửa thiết bị", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Tên thiết bị *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Danh mục (vd: Máy ảnh, Máy quay...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSubtitle,
                        onValueChange = { editSubtitle = it },
                        label = { Text("Loại chi tiết (vd: Máy ảnh - Mirrorless)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = formatCurrencyInput(it) },
                        label = { Text("Giá thuê (VNĐ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSerial,
                        onValueChange = { editSerial = it },
                        label = { Text("Mã Serial thiết bị") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Trạng thái thiết bị:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statusOptions = listOf("Sẵn sàng", "Đang thuê", "Tạm ngưng")
                        statusOptions.forEach { st ->
                            FilterChip(
                                selected = editStatus == st,
                                onClick = { editStatus = st },
                                label = { Text(st, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            val updated = equip.copy(
                                name = editName,
                                category = editCategory,
                                categorySubtitle = editSubtitle,
                                pricePerDay = formatCurrencyAmount(editPrice),
                                serialNumber = editSerial,
                                status = editStatus
                            )
                            onEditEquipment(updated)
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Lưu thay đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Delete Equipment Protection Alert (Blocked Delete)
    if (showDeleteBlockedAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteBlockedAlert = false },
            title = { Text("Không thể xóa", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = {
                Text("Không thể xóa thiết bị này vì đã phát sinh đơn thuê!")
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteBlockedAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Đóng")
                }
            }
        )
    }

    // Confirm Delete Dialog
    if (showDeleteConfirmDialog && selectedEquipmentForDelete != null) {
        val equip = selectedEquipmentForDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Xác nhận xóa thiết bị", fontWeight = FontWeight.Bold) },
            text = {
                Text("Bạn có chắc chắn muốn xóa thiết bị '${equip.name}' không?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEquipment(equip)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Xóa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Quick Rental Creation Bottom Sheet
    if (showQuickRentalSheet && selectedEquipmentForRental != null) {
        val equipment = selectedEquipmentForRental!!
        var customerName by remember { mutableStateOf("") }
        var customerPhone by remember { mutableStateOf("") }
        var lesseeIdNumber by remember { mutableStateOf("") }
        var rentalDays by remember { mutableStateOf("") }
        var matchedCustomerAddress by remember { mutableStateOf("") }

        var lastMatchedQuickCustId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(customerPhone, lesseeIdNumber) {
            if (customerPhone.isNotBlank() || lesseeIdNumber.isNotBlank()) {
                val matched = customersList.find { cust ->
                    (cust.phone.isNotBlank() && cust.phone.replace(" ", "") == customerPhone.replace(" ", "")) ||
                    (cust.idNumber.isNotBlank() && cust.idNumber.replace(" ", "") == lesseeIdNumber.replace(" ", ""))
                }
                if (matched != null && matched.id != lastMatchedQuickCustId) {
                    lastMatchedQuickCustId = matched.id
                    customerName = matched.name
                    customerPhone = matched.phone
                    lesseeIdNumber = matched.idNumber
                    matchedCustomerAddress = matched.address
                    Toast.makeText(context, "Đây là Khách hàng cũ, đã có sẵn thông tin cá nhân", Toast.LENGTH_SHORT).show()
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showQuickRentalSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tạo đơn thuê chọn nhanh",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    IconButton(onClick = { showQuickRentalSheet = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng")
                    }
                }

                HorizontalDivider()

                // Selected Equipment Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(equipment.icon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(36.dp))
                        Column {
                            Text(equipment.name, fontWeight = FontWeight.Bold)
                            Text("${formatCurrencyAmount(equipment.pricePerDay)} • Tình trạng: ${equipment.status}", color = PrimaryOrange, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Khách hàng thuê") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = lesseeIdNumber,
                    onValueChange = { lesseeIdNumber = it },
                    label = { Text("Số CCCD / GPLX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = rentalDays,
                    onValueChange = { rentalDays = it },
                    label = { Text("Số ngày thuê") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Button(
                    onClick = {
                        val daysInt = rentalDays.toIntOrNull() ?: 1
                        val unitPrice = equipment.pricePerDay.filter { it.isDigit() }.toLongOrNull() ?: 2000000L
                        val calcPrice = formatCurrencyAmount(daysInt * unitPrice)
                        val newOrder = RentalOrder(
                            id = "#DH00${(10..99).random()}",
                            equipmentName = equipment.name,
                            dateRange = "10/09 - ${10 + daysInt}/09/2026 ($daysInt ngày)",
                            price = calcPrice,
                            netTotal = calcPrice,
                            status = "Đang thuê",
                            customerName = customerName,
                            lesseeName = customerName,
                            lesseePhone = customerPhone,
                            lesseeIdNumber = lesseeIdNumber,
                            lesseeAddress = matchedCustomerAddress.ifBlank { "123 Lê Lợi, TP. Quy Nhơn, Gia Lai" },
                            contractDate = "10/09/2026"
                        )
                        onCreateOrder(newOrder)
                        showQuickRentalSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xác nhận tạo đơn thuê", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceListScreenPreview() {
    NinoRentTheme {
        DeviceListScreen()
    }
}
