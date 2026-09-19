package com.ninotek.ninorent.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.model.CreateRentalOrderState
import com.ninotek.ninorent.model.Customer
import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.defaultLessorInfo
import com.ninotek.ninorent.ui.theme.*
import com.ninotek.ninorent.utils.formatCurrencyAmount

val defaultOrdersList = listOf(
    RentalOrder(
        id = "#DH001",
        equipmentName = "Sony Alpha A7C + Ống kính 24-70mm",
        dateRange = "10/09 - 12/09/2026",
        price = "6.000.000",
        status = "Đang thuê",
        customerName = "Nguyễn Văn Nam",
        lesseeName = "Nguyễn Văn Nam",
        lesseePhone = "090 123 4567",
        lesseeAddress = "123 Lê Lợi, TP. Quy Nhơn, Gia Lai",
        lesseeIdNumber = "012345678901",
        lesseeIdIssueDate = "01/01/2021",
        advancePaymentAmount = "2.000.000",
        collateralCccd = true,
        collateralAssetDescription = "Xe máy Honda Vision BKS 77F1-123.45",
        collateralCashAmount = "5.000.000",
        contractLocation = "TP. Quy Nhơn, Bình Định",
        contractDate = "10/09/2026"
    ),
    RentalOrder(
        id = "#DH002",
        equipmentName = "MacBook Pro M2 Max 16 inch",
        dateRange = "08/09 - 15/09/2026",
        price = "4.200.000",
        status = "Đang thuê",
        customerName = "Lê Thị Mai",
        lesseeName = "Lê Thị Mai",
        lesseePhone = "098 765 4321",
        lesseeAddress = "45 Nguyễn Trãi, TP. Quy Nhơn, Bình Định",
        lesseeIdNumber = "052200001234",
        lesseeIdIssueDate = "15/08/2022",
        advancePaymentAmount = "1.500.000",
        collateralGplx = true
    ),
    RentalOrder("#DH003", "iPhone 15 Pro Max", "01/09 - 05/09/2026", "2.500.000", "Đã trả", "Hoàng Minh Tuấn"),
    RentalOrder("#DH004", "DJI Mini 4 Pro Fly More Combo", "25/08 - 28/08/2026", "3.000.000", "Quá hạn", "Công ty Media Pro"),
    RentalOrder("#DH005", "Canon EOS R6 Mark II", "11/09 - 13/09/2026", "5.000.000", "Đang thuê", "Trần Quốc Bảo"),
    RentalOrder("#DH006", "iPad Pro M4 13 inch", "02/09 - 06/09/2026", "1.800.000", "Đã trả", "Phạm Văn Đức")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalOrdersScreen(
    ordersList: List<RentalOrder> = defaultOrdersList,
    devicesList: List<Equipment> = emptyList(),
    customersList: List<Customer> = emptyList(),
    lessorInfo: LessorInfo = defaultLessorInfo,
    createRentalOrderState: CreateRentalOrderState? = null,
    externalSelectedSubScreen: String? = null,
    onExternalSubScreenChange: ((String?) -> Unit)? = null,
    isAdmin: Boolean = false,
    onCreateOrder: (RentalOrder) -> Unit = {},
    onUpdateOrder: (RentalOrder) -> Unit = {},
    onDeleteOrder: (RentalOrder) -> Unit = {},
    onPayOrder: (String) -> Unit = {},
    initialFilter: String? = null,
    onFilterConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var internalSubScreen by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSubScreen = externalSelectedSubScreen ?: internalSubScreen
    val updateSubScreen: (String?) -> Unit = { value ->
        internalSubScreen = value
        onExternalSubScreenChange?.invoke(value)
    }
    var previousSubScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedOrderForPrint by remember { mutableStateOf<RentalOrder?>(null) }
    var orderToDelete by remember { mutableStateOf<RentalOrder?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialFilter) {
        if (initialFilter != null) {
            selectedTab = when (initialFilter) {
                "Đang thuê" -> 1
                "Đã trả" -> 2
                "Quá hạn" -> 3
                else -> 0
            }
            onFilterConsumed()
        }
    }

    val totalCount = ordersList.size
    val rentedCount = ordersList.count { it.status == "Đang thuê" }
    val paidCount = ordersList.count { it.status == "Đã trả" }
    val overdueCount = ordersList.count { it.status == "Quá hạn" }

    val tabs = listOf(
        "Tất cả ($totalCount)",
        "Đang thuê ($rentedCount)",
        "Đã trả ($paidCount)",
        "Quá hạn ($overdueCount)"
    )

    val filteredOrders = ordersList.filter { order ->
        when (selectedTab) {
            1 -> order.status == "Đang thuê"
            2 -> order.status == "Đã trả"
            3 -> order.status == "Quá hạn"
            else -> true
        }
    }

    val rentalState = createRentalOrderState ?: remember { CreateRentalOrderState(devicesList) }

    BackHandler(enabled = selectedSubScreen != null) {
        if (selectedSubScreen == "print" && (previousSubScreen == "create" || previousSubScreen == null)) {
            rentalState.currentStep = 4
            updateSubScreen("create")
        } else if (selectedSubScreen == "create") {
            if (rentalState.currentStep > 1) {
                rentalState.currentStep -= 1
            } else {
                updateSubScreen(null)
            }
        } else {
            updateSubScreen(null)
        }
    }

    when {
        selectedSubScreen == "create" -> {
            CreateRentalScreen(
                onBack = { updateSubScreen(null) },
                onSuccess = { updateSubScreen(null) },
                state = rentalState,
                lessorInfo = lessorInfo,
                devicesList = devicesList,
                customersList = customersList,
                onCreateOrder = { newOrder ->
                    onCreateOrder(newOrder)
                    updateSubScreen(null)
                },
                onPrintContract = { order ->
                    selectedOrderForPrint = order
                    previousSubScreen = "create"
                    updateSubScreen("print")
                },
                modifier = modifier
            )
        }
        selectedSubScreen == "print" -> {
            ContractPrintScreen(
                order = selectedOrderForPrint ?: ordersList.firstOrNull() ?: sampleRentalOrder,
                lessorInfo = lessorInfo,
                onBack = {
                    if (previousSubScreen == "create" || previousSubScreen == null) {
                        rentalState.currentStep = 4
                        updateSubScreen("create")
                    } else {
                        updateSubScreen(previousSubScreen)
                    }
                },
                modifier = modifier
            )
        }
        selectedSubScreen != null -> {
            val currentOrder = ordersList.find { it.id == selectedSubScreen }
                ?: ordersList.firstOrNull()
                ?: sampleRentalOrder

            RentalOrderDetailScreen(
                order = currentOrder,
                onBack = { updateSubScreen(null) },
                lessorInfo = lessorInfo,
                isAdmin = isAdmin,
                onPrintContract = {
                    selectedOrderForPrint = currentOrder
                    previousSubScreen = currentOrder.id
                    updateSubScreen("print")
                },
                onExtendOrder = { updatedOrder ->
                    onUpdateOrder(updatedOrder)
                },
                onEditOrder = { updatedOrder ->
                    onUpdateOrder(updatedOrder)
                },
                onDeleteOrder = { order ->
                    onDeleteOrder(order)
                    updateSubScreen(null)
                },
                onPayOrder = { orderId ->
                    onPayOrder(orderId)
                },
                modifier = modifier
            )
        }
        else -> {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text("Quản lý Đơn thuê", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                        actions = {
                            IconButton(onClick = { updateSubScreen("create") }) {
                                Icon(Icons.Rounded.AddCircle, contentDescription = "Tạo đơn", tint = PrimaryOrange)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { updateSubScreen("create") },
                        containerColor = PrimaryOrange,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Tạo đơn thuê")
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Filter Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = PrimaryOrange,
                        edgePadding = 16.dp
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selectedContentColor = PrimaryOrange,
                                unselectedContentColor = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
                    ) {
                        items(filteredOrders) { order ->
                            val statusColor = when (order.status) {
                                "Đang thuê" -> StatusRented
                                "Đã trả" -> StatusReady
                                "Quá hạn" -> StatusOverdue
                                else -> Color.Gray
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { updateSubScreen(order.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${order.id} • ${order.customerName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryOrange
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = statusColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = order.status,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }
                                            if (isAdmin) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { orderToDelete = order },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Delete,
                                                        contentDescription = "Xóa đơn",
                                                        tint = Color.Red,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = order.equipmentName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CalendarToday,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = order.dateRange,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = formatCurrencyAmount(order.netTotal.ifBlank { order.price }),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryOrange
                                            )
                                            IconButton(
                                                onClick = {
                                                    selectedOrderForPrint = order
                                                    updateSubScreen("print")
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Print,
                                                    contentDescription = "In hợp đồng",
                                                    tint = PrimaryOrange,
                                                    modifier = Modifier.size(18.dp)
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
            
            if (orderToDelete != null) {
                AlertDialog(
                    onDismissRequest = { orderToDelete = null },
                    title = { Text("Xác nhận xóa") },
                    text = { Text("Xác nhận xóa đơn thuê ${orderToDelete?.id}? Thao tác này sẽ xóa đơn vĩnh viễn và chuyển các thiết bị về trạng thái 'Sẵn sàng'.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                orderToDelete?.let { onDeleteOrder(it) }
                                orderToDelete = null
                            }
                        ) {
                            Text("Xóa", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { orderToDelete = null }) {
                            Text("Hủy", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RentalOrdersScreenPreview() {
    NinoRentTheme {
        RentalOrdersScreen()
    }
}
