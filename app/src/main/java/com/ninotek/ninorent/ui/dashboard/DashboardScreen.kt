package com.ninotek.ninorent.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.DashboardTab
import com.ninotek.ninorent.R
import com.ninotek.ninorent.model.Customer
import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.NotificationItem
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.ui.theme.*
import com.ninotek.ninorent.utils.formatCurrencyAmount
import com.ninotek.ninorent.utils.getOrderEquipmentItems
import com.ninotek.ninorent.utils.parseCurrencyToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    devicesList: List<Equipment> = emptyList(),
    ordersList: List<RentalOrder> = emptyList(),
    notificationsList: List<NotificationItem> = emptyList(),
    customersList: List<Customer> = emptyList(),
    onNotificationRead: (String) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    onStatClick: (String) -> Unit = {},
    onNavigateToTab: (DashboardTab) -> Unit = {},
    onCreateOrder: (RentalOrder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeUser = remember { UserAccountStore.getCurrentSession(context) }
    val greetingName = activeUser?.fullName?.substringAfterLast(" ") ?: "Hiếu"

    val scrollState = rememberScrollState()

    var showSearchDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var selectedEquipmentForRental by remember { mutableStateOf<Equipment?>(null) }
    var showQuickRentalSheet by remember { mutableStateOf(false) }

    val rentedCount = ordersList.count { it.status == "Đang thuê" }
    val readyCount = devicesList.count { it.status == "Sẵn sàng" }
    val maintenanceCount = devicesList.count { it.status == "Tạm ngưng" }
    val overdueCount = ordersList.count { it.status == "Quá hạn" }
    val unreadNotifCount = notificationsList.count { !it.isRead }

    // Real Analytics Calculations
    val todayRevenue = ordersList.filter {
        it.contractDate == "10/09/2026" || it.dateRange.contains("10/09")
    }.sumOf { parseCurrencyToLong(it.netTotal.ifEmpty { it.price }) }

    val monthlyRevenue = ordersList.sumOf { parseCurrencyToLong(it.netTotal.ifEmpty { it.price }) }

    val customerGroup = ordersList.groupBy { if (it.lesseeName.isNotBlank()) it.lesseeName else it.customerName }
    val topCustomerEntry = customerGroup.maxByOrNull { entry -> entry.value.sumOf { parseCurrencyToLong(it.netTotal.ifEmpty { it.price }) } }
    val topCustomerName = topCustomerEntry?.key ?: "Chưa có dữ liệu"
    val topCustomerOrderCount = topCustomerEntry?.value?.size ?: 0
    val topCustomerTotalSpent = topCustomerEntry?.value?.sumOf { parseCurrencyToLong(it.netTotal.ifEmpty { it.price }) } ?: 0L

    val equipmentMap = mutableMapOf<String, Pair<Int, Long>>()
    ordersList.forEach { order ->
        val items = getOrderEquipmentItems(order)
        val orderRev = parseCurrencyToLong(order.netTotal.ifEmpty { order.price })
        val itemRev = if (items.isNotEmpty()) orderRev / items.size else orderRev
        items.forEach { item ->
            val name = item.equipmentName.trim()
            val current = equipmentMap[name] ?: Pair(0, 0L)
            equipmentMap[name] = Pair(current.first + 1, current.second + itemRev)
        }
    }
    val topEquipmentEntry = equipmentMap.maxByOrNull { it.value.first }
    val topEquipmentName = topEquipmentEntry?.key ?: "Chưa có dữ liệu"
    val topEquipmentCount = topEquipmentEntry?.value?.first ?: 0
    val topEquipmentRevenue = topEquipmentEntry?.value?.second ?: 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ninorent_logo),
                        contentDescription = "NinoRent Logo",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "NinoRent",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                IconButton(onClick = { showSearchDialog = true }) {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = "Tìm kiếm")
                }
                BadgedBox(
                    badge = {
                        if (unreadNotifCount > 0) {
                            Badge(
                                containerColor = PrimaryOrange,
                                contentColor = Color.White
                            ) {
                                Text(unreadNotifCount.toString())
                            }
                        }
                    }
                ) {
                    IconButton(onClick = { showNotificationsDialog = true }) {
                        Icon(imageVector = Icons.Rounded.Notifications, contentDescription = "Thông báo")
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange.copy(alpha = 0.2f))
                        .clickable { showProfileDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Hồ sơ",
                        tint = PrimaryOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Main content scrollable area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Greeting Banner
            Column {
                Text(
                    text = "Xin chào, $greetingName!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Chúc bạn một ngày làm việc hiệu quả!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Statistics 2x2 Grid Cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Đang thuê",
                        count = rentedCount.toString(),
                        icon = Icons.Rounded.Assignment,
                        iconTint = StatusRented,
                        onClick = { onStatClick("Đang thuê") },
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Sẵn sàng",
                        count = readyCount.toString(),
                        icon = Icons.Rounded.CheckCircle,
                        iconTint = StatusReady,
                        onClick = { onStatClick("Sẵn sàng") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Tạm ngưng",
                        count = maintenanceCount.toString(),
                        icon = Icons.Rounded.Build,
                        iconTint = StatusMaintenance,
                        onClick = { onStatClick("Tạm ngưng") },
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Quá hạn",
                        count = overdueCount.toString(),
                        icon = Icons.Rounded.Warning,
                        iconTint = StatusOverdue,
                        onClick = { onStatClick("Quá hạn") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Real Revenue Cards Section: Doanh thu ngày & Doanh thu tháng
            Text("Phân tích doanh thu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Today, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                            Text("Doanh thu ngày", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatCurrencyAmount(todayRevenue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.DateRange, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Text("Doanh thu tháng", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatCurrencyAmount(monthlyRevenue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Real Top Customer Card Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Khách hàng thuê nhiều nhất", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(topCustomerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("$topCustomerOrderCount đơn thuê • Tổng chi: ${formatCurrencyAmount(topCustomerTotalSpent)}", style = MaterialTheme.typography.bodySmall, color = PrimaryOrange, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Real Top Equipment Card Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sản phẩm thuê nhiều nhất", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(topEquipmentName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("$topEquipmentCount lần thuê • Doanh thu: ${formatCurrencyAmount(topEquipmentRevenue)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Featured Equipment Section (Thiết bị nổi bật)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thiết bị nổi bật",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Xem tất cả",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToTab(DashboardTab.Devices) }
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val featuredList = devicesList.take(4)

                    featuredList.forEach { equip ->
                        EquipmentCard(
                            equipment = equip,
                            onClick = {
                                selectedEquipmentForRental = equip
                                showQuickRentalSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Interactive Search Dialog
    if (showSearchDialog) {
        var query by remember { mutableStateOf("") }
        val searchResults = if (query.isBlank()) emptyList() else devicesList.filter {
            it.name.contains(query, ignoreCase = true) || it.categorySubtitle.contains(query, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Tìm kiếm thiết bị...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (query.isBlank()) {
                        Text("Nhập từ khóa để tìm kiếm thiết bị nhanh...", color = Color.Gray, fontSize = 12.sp)
                    } else if (searchResults.isEmpty()) {
                        Text("Không tìm thấy kết quả nào cho '$query'", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(searchResults) { equip ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedEquipmentForRental = equip
                                            showSearchDialog = false
                                            showQuickRentalSheet = true
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(equip.icon, contentDescription = null, tint = PrimaryOrange)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(equip.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(formatCurrencyAmount(equip.pricePerDay), color = PrimaryOrange, fontSize = 11.sp)
                                        }
                                        Text(equip.status, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Interactive Notifications Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thông báo hệ thống", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (notificationsList.any { !it.isRead }) {
                        TextButton(onClick = { onMarkAllNotificationsRead() }) {
                            Text("Đọc tất cả", fontSize = 11.sp, color = PrimaryOrange)
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notificationsList.isEmpty()) {
                        Text("Không có thông báo nào.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(notificationsList) { notif ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNotificationRead(notif.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!notif.isRead) PrimaryOrange.copy(alpha = 0.08f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(notif.time, fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(notif.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Profile Dialog
    if (showProfileDialog) {
        val user = remember { UserAccountStore.getCurrentSession(context) }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text(user?.fullName ?: "Nguyễn Trung Hiếu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Thành viên NinoRent", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Email: ${user?.email ?: "hieunino@ninorent.com"}", fontSize = 12.sp)
                    Text("• Số điện thoại: ${user?.phone ?: "0901234567"}", fontSize = 12.sp)
                    Text("• Chi nhánh: Quy Nhơn, Bình Định", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Quick Rental Sheet
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

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EquipmentCard(
    equipment: Equipment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = equipment.icon,
                    contentDescription = equipment.name,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                text = equipment.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                softWrap = false,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrencyAmount(equipment.pricePerDay),
                    color = PrimaryOrange,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StatusReady.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = equipment.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusReady
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    NinoRentTheme {
        DashboardScreen()
    }
}
