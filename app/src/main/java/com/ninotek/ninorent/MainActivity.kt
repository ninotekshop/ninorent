package com.ninotek.ninorent

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.ninotek.ninorent.model.BankAccountInfo
import com.ninotek.ninorent.model.CreateRentalOrderState
import com.ninotek.ninorent.model.Customer
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.NotificationItem
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.model.UserRole
import com.ninotek.ninorent.model.defaultBankAccountInfo
import com.ninotek.ninorent.model.defaultDevicesList
import com.ninotek.ninorent.model.defaultLessorInfo
import com.ninotek.ninorent.model.loadBankAccountInfoFromPrefs
import com.ninotek.ninorent.model.loadLessorInfoFromPrefs
import com.ninotek.ninorent.model.saveBankAccountInfoToPrefs
import com.ninotek.ninorent.model.saveLessorInfoToPrefs
import com.ninotek.ninorent.ui.auth.ForgotPasswordScreen
import com.ninotek.ninorent.ui.auth.LoginScreen
import com.ninotek.ninorent.ui.auth.RegisterScreen
import com.ninotek.ninorent.ui.auth.SplashScreen
import com.ninotek.ninorent.ui.dashboard.*
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.NotificationHelper
import com.ninotek.ninorent.utils.SupabaseManager
import com.ninotek.ninorent.utils.reconcileEquipmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class AppScreen {
    Splash,
    Login,
    Register,
    ForgotPassword,
    Dashboard
}

enum class DashboardTab(val title: String, val icon: ImageVector) {
    Home("Trang chủ", Icons.Rounded.Home),
    Devices("Thiết bị", Icons.Rounded.Devices),
    Orders("Đơn thuê", Icons.AutoMirrored.Rounded.Assignment),
    Customers("Khách hàng", Icons.Rounded.People),
    More("Thêm", Icons.Rounded.MoreHoriz)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NinoRentTheme {
                NinoRentApp()
            }
        }
    }
}

@Composable
fun NinoRentApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE) }
    val hasCompletedOnboarding = remember { prefs.getBoolean("has_completed_onboarding", false) }
    val hasClearedDemoData = remember { prefs.getBoolean("hasClearedDemoData", false) }
    val activeSession = remember { UserAccountStore.getCurrentSession(context) }
    val isOwner = activeSession == null || activeSession.role == UserRole.OWNER
    var currentScreen by remember {
        mutableStateOf(
            if (activeSession != null) AppScreen.Dashboard
            else if (hasCompletedOnboarding) AppScreen.Login
            else AppScreen.Splash
        )
    }
    var currentTab by remember { mutableStateOf(DashboardTab.Home) }
    var prefilledLoginIdentifier by remember { mutableStateOf("") }

    val isLoggedIn = currentScreen == AppScreen.Dashboard

    // Shared global states across the app
    var isBiometricEnabled by remember { mutableStateOf(true) }
    var lessorInfo by remember { mutableStateOf<LessorInfo>(loadLessorInfoFromPrefs(context)) }
    var bankAccountInfo by remember { mutableStateOf<BankAccountInfo>(loadBankAccountInfoFromPrefs(context)) }

    var devicesList by remember { mutableStateOf(if (hasClearedDemoData) emptyList() else defaultDevicesList) }
    var ordersList by remember { mutableStateOf(if (hasClearedDemoData) emptyList() else defaultOrdersList) }
    var customersList by remember { mutableStateOf(if (hasClearedDemoData) emptyList() else defaultCustomersList) }
    val createRentalOrderState = remember { CreateRentalOrderState(devicesList) }
    var notificationsList by remember {
        mutableStateOf(
            if (hasClearedDemoData) emptyList()
            else listOf(
                NotificationItem("1", "Cảnh báo đơn thuê quá hạn: #DH004", "DJI Mini 4 Pro Fly More Combo - Công ty Media Pro đã quá hạn 2 ngày!", "10 phút trước", false),
                NotificationItem("2", "Đơn thuê mới #DH005", "Khách hàng Trần Quốc Bảo vừa đặt thuê Canon EOS R6 Mark II.", "1 giờ trước", false),
                NotificationItem("3", "Bảo trì thiết bị", "Fujifilm X-T5 đã đến lịch bảo trì định kỳ.", "2 giờ trước", true)
            )
        )
    }

    var targetDeviceFilter by remember { mutableStateOf<String?>(null) }
    var orderSubScreen by remember { mutableStateOf<String?>(null) }
    var targetOrderFilter by remember { mutableStateOf<String?>(null) }

    var isRefreshingDevices by remember { mutableStateOf(false) }

    val refreshEquipmentFromSupabase = remember {
        { showToast: Boolean ->
            scope.launch(Dispatchers.IO) {
                isRefreshingDevices = true
                try {
                    val activeSession = UserAccountStore.getCurrentSession(context)
                    val activeStoreId = SupabaseManager.currentStoreId
                        ?: UserAccountStore.activeStoreId.takeIf { it.isNotBlank() }
                        ?: activeSession?.storeId?.takeIf { it.isNotBlank() }
                        ?: "SHOP_0901992349"
                    val fetched = SupabaseManager.fetchEquipment(activeStoreId)
                    if (fetched != null) {
                        withContext(Dispatchers.Main) {
                            devicesList = fetched
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (showToast) {
                                Toast.makeText(context, "Không thể tải dữ liệu từ Supabase", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (showToast) {
                            Toast.makeText(context, "Lỗi khi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isRefreshingDevices = false
                    }
                }
            }
            Unit
        }
    }

    // Cloud Data Pull on App Launch & Login
    LaunchedEffect(isLoggedIn) {
        withContext(Dispatchers.IO) {
            try {
                val activeSession = UserAccountStore.getCurrentSession(context)
                val activeStoreId = SupabaseManager.currentStoreId
                    ?: UserAccountStore.activeStoreId.takeIf { it.isNotBlank() }
                    ?: activeSession?.storeId?.takeIf { it.isNotBlank() }
                    ?: "SHOP_0901992349"
                val custsDeferred = async { SupabaseManager.fetchCustomers() }
                val equipsDeferred = async { SupabaseManager.fetchEquipment(activeStoreId) }
                val ordersDeferred = async { SupabaseManager.fetchRentalOrders() }
                val settingsDeferred = async { SupabaseManager.fetchStoreSettings() }

                val remoteCusts = custsDeferred.await()
                val remoteEquips = equipsDeferred.await()
                val remoteOrders = ordersDeferred.await()
                val remoteSettings = settingsDeferred.await()

                val hasRemoteData = !remoteCusts.isNullOrEmpty() ||
                        !remoteEquips.isNullOrEmpty() ||
                        !remoteOrders.isNullOrEmpty() ||
                        remoteSettings != null

                val isRemoteEmpty = (remoteCusts != null && remoteCusts.isEmpty()) &&
                        (remoteEquips != null && remoteEquips.isEmpty()) &&
                        (remoteOrders != null && remoteOrders.isEmpty()) &&
                        (remoteSettings == null)

                withContext(Dispatchers.Main) {
                    if (hasRemoteData) {
                        if (remoteCusts != null) customersList = remoteCusts
                        if (remoteEquips != null) devicesList = remoteEquips
                        if (remoteOrders != null) ordersList = remoteOrders

                        if (remoteSettings != null) {
                            val newLessorInfo = LessorInfo(
                                name = remoteSettings.lessor_company.ifBlank { defaultLessorInfo.name },
                                address = remoteSettings.lessor_address.ifBlank { defaultLessorInfo.address },
                                representative = remoteSettings.lessor_representative.ifBlank { defaultLessorInfo.representative },
                                phone = remoteSettings.lessor_phone.ifBlank { defaultLessorInfo.phone }
                            )
                            val newBankInfo = BankAccountInfo(
                                bankName = remoteSettings.bank_name.ifBlank { defaultBankAccountInfo.bankName },
                                accountNumber = remoteSettings.bank_account_number.ifBlank { defaultBankAccountInfo.accountNumber },
                                accountHolderName = remoteSettings.bank_account_holder.ifBlank { defaultBankAccountInfo.accountHolderName }
                            )
                            lessorInfo = newLessorInfo
                            saveLessorInfoToPrefs(context, newLessorInfo)

                            bankAccountInfo = newBankInfo
                            saveBankAccountInfoToPrefs(context, newBankInfo)
                        }

                        prefs.edit().putBoolean("hasClearedDemoData", true).apply()
                    } else if (isRemoteEmpty) {
                        val currentlyCleared = prefs.getBoolean("hasClearedDemoData", false)
                        if (!currentlyCleared) {
                            devicesList = defaultDevicesList
                            ordersList = defaultOrdersList
                            customersList = defaultCustomersList
                        } else {
                            devicesList = emptyList()
                            ordersList = emptyList()
                            customersList = emptyList()
                        }
                    }
                }
            } catch (_: Exception) {
                // Offline status or network error fallback: keep local/demo state intact
            }
        }
    }

    // Automatically reconcile equipment status with active rental orders
    LaunchedEffect(devicesList, ordersList) {
        if (devicesList.isNotEmpty()) {
            val reconciled = reconcileEquipmentStatus(devicesList, ordersList)
            if (reconciled != devicesList) {
                val changedDevices = reconciled.filterIndexed { index, eq ->
                    index >= devicesList.size || eq.status != devicesList[index].status
                }
                devicesList = reconciled
                withContext(Dispatchers.IO) {
                    changedDevices.forEach { eq ->
                        SupabaseManager.updateEquipment(eq)
                    }
                }
            }
        }
    }

    // Send System Push Notifications for overdue orders
    LaunchedEffect(ordersList) {
        val overdueOrders = ordersList.filter { it.status == "Quá hạn" }
        overdueOrders.forEachIndexed { index, order ->
            val title = "Cảnh báo đơn thuê quá hạn: ${order.id}"
            val msg = "${order.id} - ${order.equipmentName} (${order.customerName}) đã quá hạn thanh toán!"
            NotificationHelper.sendOverdueNotification(
                context = context,
                title = title,
                message = msg,
                notificationId = 1000 + index
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            AppScreen.Splash -> {
                SplashScreen(
                    onNavigateToLogin = {
                        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                        currentScreen = AppScreen.Login
                    }
                )
            }
            AppScreen.Login -> {
                LoginScreen(
                    prefilledIdentifier = prefilledLoginIdentifier,
                    onLoginSuccess = {
                        currentScreen = AppScreen.Dashboard
                        currentTab = DashboardTab.Home
                    },
                    onNavigateRegister = { currentScreen = AppScreen.Register },
                    onNavigateForgotPassword = { currentScreen = AppScreen.ForgotPassword },
                    isBiometricEnabled = isBiometricEnabled
                )
            }
            AppScreen.Register -> {
                RegisterScreen(
                    onRegisterSuccess = { registeredIdentifier ->
                        prefilledLoginIdentifier = registeredIdentifier
                        currentScreen = AppScreen.Login
                    },
                    onNavigateLogin = { currentScreen = AppScreen.Login }
                )
            }
            AppScreen.ForgotPassword -> {
                ForgotPasswordScreen(
                    onResetPasswordSuccess = { currentScreen = AppScreen.Login },
                    onNavigateLogin = { currentScreen = AppScreen.Login }
                )
            }
            AppScreen.Dashboard -> {
                BackHandler(enabled = currentTab != DashboardTab.Home) {
                    currentTab = DashboardTab.Home
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = PrimaryOrange,
                            tonalElevation = 8.dp
                        ) {
                            DashboardTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    alwaysShowLabel = true,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryOrange,
                                        selectedTextColor = PrimaryOrange,
                                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val innerModifier = Modifier.padding(innerPadding)
                    val handleCreateOrder: (RentalOrder) -> Unit = { newOrder ->
                        ordersList = listOf(newOrder) + ordersList
                        devicesList = devicesList.map {
                            if (it.name.contains(newOrder.equipmentName.substringBefore("+").trim(), ignoreCase = true)) {
                                val updatedEq = it.copy(status = "Đang thuê")
                                scope.launch(Dispatchers.IO) { SupabaseManager.updateEquipment(updatedEq) }
                                updatedEq
                            } else it
                        }
                        scope.launch(Dispatchers.IO) { SupabaseManager.insertRentalOrder(newOrder) }

                        val custName = newOrder.lesseeName.ifBlank { newOrder.customerName }
                        val custPhone = newOrder.lesseePhone
                        if (custName.isNotBlank() && !customersList.any { it.phone == custPhone || it.name.equals(custName, ignoreCase = true) }) {
                            val newCust = Customer(
                                id = UUID.randomUUID().toString(),
                                name = custName,
                                phone = custPhone,
                                address = newOrder.lesseeAddress,
                                idNumber = newOrder.lesseeIdNumber,
                                idIssueDate = newOrder.lesseeIdIssueDate,
                                storeId = SupabaseManager.currentStoreId ?: ""
                            )
                            customersList = listOf(newCust) + customersList
                            scope.launch(Dispatchers.IO) { SupabaseManager.insertCustomer(newCust) }
                        }
                    }

                    when (currentTab) {
                        DashboardTab.Home -> DashboardScreen(
                            devicesList = devicesList,
                            ordersList = ordersList,
                            notificationsList = notificationsList,
                            customersList = customersList,
                            onNotificationRead = { notifId ->
                                notificationsList = notificationsList.map {
                                    if (it.id == notifId) it.copy(isRead = true) else it
                                }
                            },
                            onMarkAllNotificationsRead = {
                                notificationsList = notificationsList.map { it.copy(isRead = true) }
                            },
                            onStatClick = { statType ->
                                when (statType) {
                                    "Đang thuê" -> {
                                        targetOrderFilter = "Đang thuê"
                                        currentTab = DashboardTab.Orders
                                    }
                                    "Sẵn sàng" -> {
                                        targetDeviceFilter = "Sẵn sàng"
                                        currentTab = DashboardTab.Devices
                                    }
                                    "Tạm ngưng" -> {
                                        targetDeviceFilter = "Tạm ngưng"
                                        currentTab = DashboardTab.Devices
                                    }
                                    "Quá hạn" -> {
                                        targetOrderFilter = "Quá hạn"
                                        currentTab = DashboardTab.Orders
                                    }
                                }
                            },
                            onNavigateToTab = { tab -> currentTab = tab },
                            onCreateOrder = handleCreateOrder,
                            modifier = innerModifier
                        )
                        DashboardTab.Devices -> DeviceListScreen(
                            devicesList = devicesList,
                            ordersList = ordersList,
                            customersList = customersList,
                            onAddEquipment = { newEquip ->
                                devicesList = listOf(newEquip) + devicesList
                                scope.launch(Dispatchers.IO) { SupabaseManager.insertEquipment(newEquip) }
                            },
                            onEditEquipment = { updatedEquip ->
                                devicesList = devicesList.map { if (it.id == updatedEquip.id) updatedEquip else it }
                                scope.launch(Dispatchers.IO) { SupabaseManager.updateEquipment(updatedEquip) }
                            },
                            onDeleteEquipment = { deletedEquip ->
                                devicesList = devicesList.filter { it.id != deletedEquip.id }
                                scope.launch(Dispatchers.IO) { SupabaseManager.deleteEquipment(deletedEquip.id) }
                            },
                            onCreateOrder = handleCreateOrder,
                            onRefreshEquipment = { refreshEquipmentFromSupabase(true) },
                            isRefreshing = isRefreshingDevices,
                            initialFilter = targetDeviceFilter,
                            onFilterConsumed = { targetDeviceFilter = null },
                            modifier = innerModifier
                        )
                        DashboardTab.Orders -> RentalOrdersScreen(
                            ordersList = ordersList,
                            devicesList = devicesList,
                            customersList = customersList,
                            lessorInfo = lessorInfo,
                            createRentalOrderState = createRentalOrderState,
                            externalSelectedSubScreen = orderSubScreen,
                            onExternalSubScreenChange = { orderSubScreen = it },
                            isAdmin = isOwner,
                            onClearDemoData = {
                                devicesList = emptyList()
                                ordersList = emptyList()
                                customersList = emptyList()
                                notificationsList = emptyList()
                                prefs.edit().putBoolean("hasClearedDemoData", true).apply()
                            },
                            onCreateOrder = handleCreateOrder,
                            onUpdateOrder = { updatedOrder ->
                                val oldOrder = ordersList.find { it.id == updatedOrder.id }
                                ordersList = ordersList.map { if (it.id == updatedOrder.id) updatedOrder else it }
                                if (oldOrder != null && oldOrder.status != updatedOrder.status) {
                                    val newEquipmentStatus = when (updatedOrder.status) {
                                        "Đã trả" -> "Sẵn sàng"
                                        else -> "Đang thuê"
                                    }
                                    devicesList = devicesList.map {
                                        if (it.name.contains(updatedOrder.equipmentName.substringBefore("+").trim(), ignoreCase = true)) {
                                            val updatedEq = it.copy(status = newEquipmentStatus)
                                            scope.launch(Dispatchers.IO) { SupabaseManager.updateEquipment(updatedEq) }
                                            updatedEq
                                        } else it
                                    }
                                }
                                scope.launch(Dispatchers.IO) { SupabaseManager.insertRentalOrder(updatedOrder) }
                                Toast.makeText(context, "Cập nhật đơn thành công", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteOrder = { orderToDelete ->
                                ordersList = ordersList.filter { it.id != orderToDelete.id }
                                devicesList = devicesList.map {
                                    if (it.name.contains(orderToDelete.equipmentName.substringBefore("+").trim(), ignoreCase = true)) {
                                        val updatedEq = it.copy(status = "Sẵn sàng")
                                        scope.launch(Dispatchers.IO) { SupabaseManager.updateEquipment(updatedEq) }
                                        updatedEq
                                    } else it
                                }
                                scope.launch(Dispatchers.IO) { SupabaseManager.deleteRentalOrder(orderToDelete.id) }
                                Toast.makeText(context, "Đã xóa đơn thuê thành công", Toast.LENGTH_SHORT).show()
                            },
                            onPayOrder = { orderId ->
                                val paidOrder = ordersList.find { it.id == orderId }
                                ordersList = ordersList.map {
                                    if (it.id == orderId) it.copy(status = "Đã trả") else it
                                }
                                if (paidOrder != null) {
                                    devicesList = devicesList.map {
                                        if (it.name.contains(paidOrder.equipmentName.substringBefore("+").trim(), ignoreCase = true)) {
                                            val updatedEq = it.copy(status = "Sẵn sàng")
                                            scope.launch(Dispatchers.IO) { SupabaseManager.updateEquipment(updatedEq) }
                                            updatedEq
                                        } else it
                                    }
                                }
                                scope.launch(Dispatchers.IO) { SupabaseManager.updateRentalOrderStatus(orderId, "Đã trả") }
                            },
                            initialFilter = targetOrderFilter,
                            onFilterConsumed = { targetOrderFilter = null },
                            modifier = innerModifier
                        )
                        DashboardTab.Customers -> CustomersScreen(
                            customerList = customersList,
                            onAddCustomer = { newCust ->
                                customersList = listOf(newCust) + customersList
                                scope.launch(Dispatchers.IO) { SupabaseManager.insertCustomer(newCust) }
                            },
                            onDeleteCustomer = { custToDelete ->
                                customersList = customersList.filter { it.id != custToDelete.id }
                                scope.launch(Dispatchers.IO) { SupabaseManager.deleteCustomer(custToDelete.id) }
                            },
                            modifier = innerModifier
                        )
                        DashboardTab.More -> MoreScreen(
                            staffAccounts = UserAccountStore.getRegisteredAccounts(context),
                            customers = customersList,
                            equipment = devicesList,
                            rentalOrders = ordersList,
                            onLogout = {
                                UserAccountStore.setCurrentSession(context, null)
                                currentScreen = AppScreen.Login
                            },
                            isBiometricEnabled = isBiometricEnabled,
                            onBiometricToggle = { isBiometricEnabled = it },
                            lessorInfo = lessorInfo,
                            onUpdateLessorInfo = { updated ->
                                lessorInfo = updated
                                saveLessorInfoToPrefs(context, updated)
                            },
                            bankAccountInfo = bankAccountInfo,
                            onUpdateBankAccountInfo = { updated ->
                                bankAccountInfo = updated
                                saveBankAccountInfoToPrefs(context, updated)
                            },
                            onClearDemoData = {
                                devicesList = emptyList()
                                ordersList = emptyList()
                                customersList = emptyList()
                                notificationsList = emptyList()
                            },
                            modifier = innerModifier
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NinoRentAppPreview() {
    NinoRentTheme {
        NinoRentApp()
    }
}
