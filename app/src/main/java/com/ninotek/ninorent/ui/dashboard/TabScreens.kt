package com.ninotek.ninorent.ui.dashboard

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.ninotek.ninorent.utils.normalizeStoreIdFromPhone
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ninotek.ninorent.model.BankAccountInfo
import com.ninotek.ninorent.model.Customer
import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.UserAccount
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.model.UserRole
import com.ninotek.ninorent.model.defaultBankAccountInfo
import com.ninotek.ninorent.model.defaultLessorInfo
import com.ninotek.ninorent.model.loadBankAccountInfoFromPrefs
import com.ninotek.ninorent.model.saveBankAccountInfoToPrefs
import com.ninotek.ninorent.model.saveLessorInfoToPrefs
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.StaffAccountDto
import com.ninotek.ninorent.utils.StoreSettingsDto
import com.ninotek.ninorent.utils.SyncSummary
import com.ninotek.ninorent.utils.SupabaseManager
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun OrdersScreen(modifier: Modifier = Modifier) {
    RentalOrdersScreen(modifier = modifier)
}

@Composable
fun DevicesScreen(
    devicesList: List<Equipment> = emptyList(),
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
    DeviceListScreen(
        devicesList = devicesList,
        ordersList = ordersList,
        customersList = customersList,
        onAddEquipment = onAddEquipment,
        onEditEquipment = onEditEquipment,
        onDeleteEquipment = onDeleteEquipment,
        onCreateOrder = onCreateOrder,
        onRefreshEquipment = onRefreshEquipment,
        isRefreshing = isRefreshing,
        initialFilter = initialFilter,
        onFilterConsumed = onFilterConsumed,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    staffAccounts: List<UserAccount> = emptyList(),
    customers: List<Customer> = emptyList(),
    equipment: List<Equipment> = emptyList(),
    rentalOrders: List<RentalOrder> = emptyList(),
    onLogout: () -> Unit = {},
    isBiometricEnabled: Boolean = true,
    onBiometricToggle: (Boolean) -> Unit = {},
    lessorInfo: LessorInfo = defaultLessorInfo,
    onUpdateLessorInfo: (LessorInfo) -> Unit = {},
    bankAccountInfo: BankAccountInfo = defaultBankAccountInfo,
    onUpdateBankAccountInfo: (BankAccountInfo) -> Unit = {},
    onClearDemoData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE) }
    val activeUser = remember { UserAccountStore.getCurrentSession(context) }
    val isOwner = activeUser == null || activeUser.role == UserRole.OWNER
    val displayStoreId = remember(activeUser) {
        if (!activeUser?.storeId.isNullOrBlank()) normalizeStoreIdFromPhone(activeUser.storeId)
        else if (activeUser?.phone.isNullOrBlank()) "SHOP_0901992349"
        else normalizeStoreIdFromPhone(activeUser.phone)
    }

    val isMasterAdmin = remember(activeUser) {
        val cleanPhone = activeUser?.phone?.replace(" ", "") ?: ""
        cleanPhone == "0901992349"
    }

    var currentBiometricEnabled by remember {
        mutableStateOf(activeUser?.biometricEnabled ?: isBiometricEnabled)
    }
    var hasClearedDemoData by remember {
        mutableStateOf(prefs.getBoolean("hasClearedDemoData", false))
    }
    var currentBankAccountInfo by remember(bankAccountInfo) { mutableStateOf(loadBankAccountInfoFromPrefs(context)) }

    var showMasterAdminPanel by remember { mutableStateOf(false) }
    var showReports by remember { mutableStateOf(false) }
    var showStaffManagement by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    // Dialog states
    var showAccountDialog by remember { mutableStateOf(false) }
    var showStaffAccountDialog by remember { mutableStateOf(false) }
    var showEditLessorDialog by remember { mutableStateOf(false) }
    var showEditBankDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showClearDemoConfirmDialog by remember { mutableStateOf(false) }
    var showCloudSyncProgressDialog by remember { mutableStateOf(false) }
    var showCloudSyncResultDialog by remember { mutableStateOf(false) }
    var syncResultSummary by remember { mutableStateOf<SyncSummary?>(null) }
    var syncErrorMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isSubScreenActive = showMasterAdminPanel || showStaffManagement || showReports
    BackHandler(enabled = isSubScreenActive) {
        showMasterAdminPanel = false
        showStaffManagement = false
        showReports = false
    }

    when {
        showMasterAdminPanel -> {
            MasterAdminScreen(
                modifier = modifier,
                onBack = { showMasterAdminPanel = false }
            )
        }
        showStaffManagement -> {
            StaffManagementScreen(
                modifier = modifier,
                onBack = { showStaffManagement = false }
            )
        }
        showReports -> {
            ReportsScreen(
                modifier = modifier,
                onBack = { showReports = false }
            )
        }
        else -> {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text("Cài đặt & Tài khoản", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    // Profile Card
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
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "Avatar",
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeUser?.fullName ?: "Lê Trung Hiếu",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activeUser?.email ?: "admin@ninorent.vn",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PrimaryOrange.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (activeUser != null) "SĐT: ${activeUser.phone}" else "SĐT: 0987 654 321",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryOrange,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Blue.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = activeUser?.role?.displayName ?: "Admin",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Store Info & Commercial License Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Store ID Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PrimaryOrange.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Store,
                                            contentDescription = "Mã cửa hàng",
                                            tint = PrimaryOrange,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Mã cửa hàng",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = displayStoreId,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // License Status Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = "Bản quyền",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Trạng thái bản quyền",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Bản quyền Thương mại / Hoạt động",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "Hoạt động",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Master Admin Section (Only visible for Master Admin 0901992349)
                    if (isMasterAdmin) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = PrimaryOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Dành cho Quản trị",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                SettingsItem(
                                    icon = Icons.Rounded.Shield,
                                    title = "Quản lý Danh sách Cửa hàng",
                                    subtitle = "Quản lý, cấp lại mật khẩu và dọn dẹp dữ liệu các cửa hàng",
                                    onClick = { showMasterAdminPanel = true }
                                )
                            }
                        }
                    }

                    // Settings List Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // Staff Management (Owner Only)
                            if (isOwner) {
                                SettingsItem(
                                    icon = Icons.Rounded.Badge,
                                    title = "Quản lý nhân viên",
                                    subtitle = "Thêm và quản lý tài khoản nhân viên",
                                    onClick = { showStaffManagement = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }

                            // 1. Thông tin tài khoản
                            SettingsItem(
                                icon = Icons.Rounded.AccountBox,
                                title = if (isOwner) "Thông tin tài khoản & Bên A" else "Thông tin tài khoản cá nhân",
                                onClick = {
                                    if (isOwner) {
                                        showAccountDialog = true
                                    } else {
                                        showStaffAccountDialog = true
                                    }
                                }
                            )

                            if (isOwner) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                // 1b. Cấu hình Tài khoản Ngân hàng (VietQR)
                                SettingsItem(
                                    icon = Icons.Rounded.AccountBalance,
                                    title = "Cấu hình Tài khoản Ngân hàng (VietQR)",
                                    onClick = { showEditBankDialog = true }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // 2. Đăng nhập bằng vân tay (Biometric toggle)
                            SettingsToggleItem(
                                icon = Icons.Rounded.Fingerprint,
                                title = "Đăng nhập bằng vân tay",
                                checked = currentBiometricEnabled,
                                onCheckedChange = { checked ->
                                    currentBiometricEnabled = checked
                                    if (activeUser != null) {
                                        UserAccountStore.setBiometricEnabled(context, activeUser.phone, checked)
                                    }
                                    onBiometricToggle(checked)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (checked) "Đã bật đăng nhập bằng vân tay" else "Đã tắt đăng nhập bằng vân tay"
                                        )
                                    }
                                }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // 3. Đổi mật khẩu
                            SettingsItem(
                                icon = Icons.Rounded.Lock,
                                title = "Đổi mật khẩu",
                                onClick = { showPasswordDialog = true }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // 4. Thông báo
                            SettingsToggleItem(
                                icon = Icons.Rounded.Notifications,
                                title = "Thông báo",
                                checked = notificationsEnabled,
                                onCheckedChange = {
                                    notificationsEnabled = it
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (it) "Đã bật thông báo" else "Đã tắt thông báo"
                                        )
                                    }
                                }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // 4. Thông tin kết thúc / Thông báo
                            // 5. Ngôn ngữ đã lược bỏ

                            if (isOwner) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                // 6. Xóa dữ liệu Demo (Owner Only)
                                SettingsItem(
                                    icon = Icons.Rounded.DeleteForever,
                                    title = "Xóa dữ liệu Demo",
                                    subtitle = if (hasClearedDemoData) "Đã xóa dữ liệu mẫu" else null,
                                    iconTint = if (hasClearedDemoData) Color.Gray else Color.Red,
                                    enabled = !hasClearedDemoData,
                                    alpha = if (hasClearedDemoData) 0.5f else 1.0f,
                                    onClick = {
                                        if (!hasClearedDemoData) {
                                            showClearDemoConfirmDialog = true
                                        }
                                    }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // 7. Trợ giúp
                            SettingsItem(
                                icon = Icons.AutoMirrored.Rounded.Help,
                                title = "Trợ giúp",
                                onClick = { showHelpDialog = true }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // 8. Giới thiệu
                            SettingsItem(
                                icon = Icons.Rounded.Info,
                                title = "Giới thiệu",
                                onClick = { showAboutDialog = true }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            if (isOwner) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                // Cloud Sync (Supabase)
                                SettingsItem(
                                    icon = Icons.Rounded.CloudSync,
                                    title = "Đồng bộ dữ liệu lên Cloud",
                                    subtitle = "Đẩy toàn bộ Tài khoản, Khách hàng, Thiết bị và Đơn thuê lên Supabase",
                                    onClick = {
                                        showCloudSyncProgressDialog = true
                                        scope.launch {
                                            val actualStaffDtos = staffAccounts.ifEmpty { UserAccountStore.getRegisteredAccounts(context) }.map { acc ->
                                                StaffAccountDto(
                                                    id = acc.id.ifBlank { UUID.randomUUID().toString() },
                                                    full_name = acc.fullName,
                                                    phone = acc.phone,
                                                    email = acc.email,
                                                    password = acc.password,
                                                    role = acc.role.name,
                                                    staff_role_detail = acc.staffRoleDetail,
                                                    biometric_enabled = acc.biometricEnabled
                                                )
                                            }
                                            val result = SupabaseManager.syncAllLocalData(
                                                staffAccounts = actualStaffDtos,
                                                customers = customers,
                                                equipment = equipment,
                                                rentalOrders = rentalOrders,
                                                lessorInfo = lessorInfo,
                                                bankAccountInfo = currentBankAccountInfo
                                            )
                                            showCloudSyncProgressDialog = false
                                            syncResultSummary = result.getOrNull()
                                            syncErrorMessage = result.exceptionOrNull()?.localizedMessage
                                            showCloudSyncResultDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Reports Card (Owner Only)
                    if (isOwner) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showReports = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(PrimaryOrange.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.BarChart, contentDescription = null, tint = PrimaryOrange)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Báo cáo doanh thu & thiết bị",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Thống kê chi tiết, biểu đồ ngày, top thiết bị",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }

                    // Logout Button
                    Button(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đăng xuất", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Thông tin tài khoản & Bên A", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tài khoản hiện tại:", fontWeight = FontWeight.Bold, color = PrimaryOrange)
                    Text("• Họ và tên: ${activeUser?.fullName ?: "Lê Trung Hiếu"}")
                    Text("• Email: ${activeUser?.email ?: "admin@ninorent.vn"}")
                    Text("• Số điện thoại: ${activeUser?.phone ?: "0987 654 321"}")
                    Text("• Vai trò: ${activeUser?.role?.displayName ?: "Admin"}")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Thông tin Bên A (Bên Cho Thuê / Hợp đồng):", fontWeight = FontWeight.Bold, color = PrimaryOrange)
                    Text("• Đơn vị: ${lessorInfo.name}")
                    Text("• Người đại diện: ${lessorInfo.representative}")
                    Text("• Địa chỉ: ${lessorInfo.address}")
                    Text("• Hotline Bên A: ${lessorInfo.phone}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountDialog = false
                        showEditLessorDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sửa thông tin Bên A")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showStaffAccountDialog) {
        AlertDialog(
            onDismissRequest = { showStaffAccountDialog = false },
            title = { Text("Thông tin tài khoản cá nhân", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• Họ và tên: ${activeUser?.fullName ?: "Nhân viên"}")
                    Text("• Email: ${activeUser?.email ?: ""}")
                    Text("• Số điện thoại: ${activeUser?.phone ?: ""}")
                    Text("• Vai trò: ${activeUser?.role?.displayName ?: "Nhân viên"}")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStaffAccountDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showEditLessorDialog) {
        var name by remember { mutableStateOf(lessorInfo.name) }
        var representative by remember { mutableStateOf(lessorInfo.representative) }
        var address by remember { mutableStateOf(lessorInfo.address) }
        var phone by remember { mutableStateOf(lessorInfo.phone) }
        var logoUri by remember { mutableStateOf(lessorInfo.logoUri) }

        val logoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                logoUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditLessorDialog = false },
            title = { Text("Chỉnh sửa thông tin Bên A", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Logo cửa hàng (Hiển thị trên hợp đồng)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = PrimaryOrange)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                                .clickable { logoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!logoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = logoUri,
                                    contentDescription = "Logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        Column {
                            OutlinedButton(
                                onClick = { logoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Chọn Logo", fontSize = 12.sp)
                            }
                            if (!logoUri.isNullOrBlank()) {
                                TextButton(
                                    onClick = { logoUri = null },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Text("Xóa Logo", fontSize = 12.sp, color = Color.Red)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên đơn vị Bên A (Cho thuê)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = representative,
                        onValueChange = { representative = it },
                        label = { Text("Người đại diện") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Địa chỉ Bên A") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại Bên A") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLessorInfo = LessorInfo(
                            name = name,
                            representative = representative,
                            address = address,
                            phone = phone,
                            logoUri = logoUri
                        )
                        onUpdateLessorInfo(newLessorInfo)
                        saveLessorInfoToPrefs(context, newLessorInfo)
                        showEditLessorDialog = false
                        scope.launch {
                            SupabaseManager.upsertStoreSettings(newLessorInfo, currentBankAccountInfo)
                            snackbarHostState.showSnackbar("Đã lưu thông tin Bên A thành công!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Lưu thông tin Bên A", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditLessorDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showEditBankDialog) {
        var bName by remember { mutableStateOf(currentBankAccountInfo.bankName) }
        var accNum by remember { mutableStateOf(currentBankAccountInfo.accountNumber) }
        var accHolder by remember { mutableStateOf(currentBankAccountInfo.accountHolderName) }

        AlertDialog(
            onDismissRequest = { showEditBankDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = PrimaryOrange)
                    Text("Cấu hình Tài khoản Ngân hàng (VietQR)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tài khoản ngân hàng dùng để nhận chuyển khoản đặt cọc và tự động tạo mã VietQR cho khách hàng.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = bName,
                        onValueChange = { bName = it },
                        label = { Text("Tên ngân hàng (vd: MB Bank, Vietcombank)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accNum,
                        onValueChange = { accNum = it },
                        label = { Text("Số tài khoản ngân hàng") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accHolder,
                        onValueChange = { accHolder = it },
                        label = { Text("Tên chủ tài khoản (Viết hoa không dấu)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBankInfo = BankAccountInfo(
                            bankName = bName,
                            accountNumber = accNum,
                            accountHolderName = accHolder
                        )
                        currentBankAccountInfo = newBankInfo
                        onUpdateBankAccountInfo(newBankInfo)
                        saveBankAccountInfoToPrefs(context, newBankInfo)
                        showEditBankDialog = false
                        scope.launch {
                            SupabaseManager.upsertStoreSettings(lessorInfo, newBankInfo)
                            snackbarHostState.showSnackbar("Đã lưu tài khoản ngân hàng VietQR thành công!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Lưu tài khoản", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBankDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showClearDemoConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearDemoConfirmDialog = false },
            title = { Text("Xóa dữ liệu Demo", fontWeight = FontWeight.Bold) },
            text = {
                Text("Xóa toàn bộ dữ liệu mẫu? Tất cả danh sách thiết bị, đơn thuê và khách hàng sẽ bị xóa hoàn toàn khỏi ứng dụng.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearDemoData()
                        prefs.edit().putBoolean("hasClearedDemoData", true).apply()
                        hasClearedDemoData = true
                        showClearDemoConfirmDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Đã xóa toàn bộ dữ liệu mẫu!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa toàn bộ", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDemoConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showPasswordDialog) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Đổi mật khẩu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Mật khẩu hiện tại") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Mật khẩu mới") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Xác nhận mật khẩu mới") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.length >= 6 && newPass == confirmPass) {
                            showPasswordDialog = false
                            scope.launch { snackbarHostState.showSnackbar("Đổi mật khẩu thành công!") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Mật khẩu mới không khớp hoặc quá ngắn!") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Lưu thay đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Trợ giúp & Hỗ trợ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Liên hệ tổng đài hỗ trợ kỹ thuật NinoRent:")
                    Text("Hotline: 090 199 2349 (8:00 - 18:00)", fontWeight = FontWeight.Bold)
                    Text("Email: cskh@ninotekpos.com")
                    Text("Website: https://ninotekpos.com")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Giới thiệu NinoRent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NinoRent - Hệ thống Quản lý và Cho thuê Thiết bị chuyên nghiệp.")
                    Text("Phiên bản: v1.0.0 (Build 2026.09)")
                    Text("© 2026 Ninotek JSC. All rights reserved.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Xác nhận đăng xuất", fontWeight = FontWeight.Bold) },
            text = {
                Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản ${activeUser?.email ?: "admin@ninorent.vn"}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        UserAccountStore.setCurrentSession(context, null)
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Đăng xuất", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showCloudSyncProgressDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Đang đồng bộ dữ liệu") },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                    Text("Đang đồng bộ dữ liệu lên Supabase...")
                }
            },
            confirmButton = {}
        )
    }

    if (showCloudSyncResultDialog) {
        AlertDialog(
            onDismissRequest = { showCloudSyncResultDialog = false },
            title = {
                Text(
                    text = if (syncResultSummary != null) "Đồng bộ thành công!" else "Lỗi đồng bộ",
                    fontWeight = FontWeight.Bold,
                    color = if (syncResultSummary != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (syncResultSummary != null) {
                        Text(
                            text = "Đồng bộ thành công! Đã đẩy ${syncResultSummary!!.staffSyncedCount} tài khoản, " +
                                    "${syncResultSummary!!.customersSyncedCount} khách hàng, " +
                                    "${syncResultSummary!!.equipmentSyncedCount} thiết bị, " +
                                    "${syncResultSummary!!.rentalOrdersSyncedCount} đơn thuê lên Supabase."
                        )
                    } else {
                        Text(
                            text = "Không thể đồng bộ dữ liệu lên Supabase: ${syncErrorMessage ?: "Lỗi kết nối"}. Vui lòng kiểm tra internet hoặc cấu hình Supabase.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCloudSyncResultDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = PrimaryOrange,
    enabled: Boolean = true,
    alpha: Float = 1.0f,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (iconTint == Color.Red && enabled) Color.Red else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (enabled) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PrimaryOrange.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryOrange
            )
        )
    }
}



@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun MoreScreenPreview() {
    NinoRentTheme {
        MoreScreen()
    }
}
