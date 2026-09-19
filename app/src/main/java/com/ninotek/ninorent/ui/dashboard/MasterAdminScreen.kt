package com.ninotek.ninorent.ui.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.RegisteredStoreInfo
import com.ninotek.ninorent.utils.SupabaseManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterAdminScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = remember { UserAccountStore.getCurrentSession(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var storesList by remember { mutableStateOf<List<RegisteredStoreInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Reset Password Dialog state
    var selectedStoreForReset by remember { mutableStateOf<RegisteredStoreInfo?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var resetErrorMsg by remember { mutableStateOf<String?>(null) }

    // Delete Store Dialog state
    var selectedStoreForDelete by remember { mutableStateOf<RegisteredStoreInfo?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    BackHandler {
        when {
            selectedStoreForReset != null -> selectedStoreForReset = null
            selectedStoreForDelete != null && !isDeleting -> selectedStoreForDelete = null
            else -> onBack()
        }
    }

    val loadStores: () -> Unit = {
        scope.launch {
            isLoading = true
            try {
                val localAccounts = UserAccountStore.getRegisteredAccounts(context)
                val result = SupabaseManager.fetchAllStores(localAccounts)
                storesList = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadStores()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Danh sách Cửa hàng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = loadStores) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Tải lại", tint = PrimaryOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryOrange)
            }
        } else if (storesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Store,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "Chưa có cửa hàng nào được đăng ký",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Button(
                        onClick = loadStores,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tải lại danh sách")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryOrange.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = PrimaryOrange
                                )
                                Text(
                                    text = "Bảng Điều Khiển Master Admin",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PrimaryOrange
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PrimaryOrange
                            ) {
                                Text(
                                    text = "${storesList.size} cửa hàng",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                items(storesList, key = { it.storeId }) { store ->
                    val cleanOwnerPhone = store.ownerPhone.replace(" ", "")
                    val isMasterAdminStore = store.storeId == "SHOP_0901992349" ||
                            cleanOwnerPhone == "0901992349" ||
                            cleanOwnerPhone.endsWith("0901992349") ||
                            (currentUser != null && (
                                (currentUser.storeId.isNotBlank() && store.storeId == currentUser.storeId) ||
                                (currentUser.phone.isNotBlank() && cleanOwnerPhone == currentUser.phone.replace(" ", "")) ||
                                (currentUser.email.isNotBlank() && store.ownerEmail.equals(currentUser.email, ignoreCase = true))
                            ))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header: Store Name & ID
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryOrange.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Storefront,
                                            contentDescription = null,
                                            tint = PrimaryOrange,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = store.storeName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "ID: ${store.storeId}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Hoạt động",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // Store Details
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Chủ cửa hàng: ${store.ownerName.ifBlank { "Chưa cập nhật" }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Phone,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "SĐT Chủ: ${store.ownerPhone.ifBlank { "Chưa cập nhật" }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (store.ownerEmail.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Email,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Email: ${store.ownerEmail}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Event,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Ngày đăng ký: ${store.registrationDate.ifBlank { "Hệ thống" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedStoreForReset = store
                                        newPassword = ""
                                        passwordVisible = false
                                        resetErrorMsg = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cấp lại mật khẩu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (isMasterAdminStore) {
                                            Toast.makeText(context, "Không thể tự xóa chính tài khoản Quản trị Hệ thống đang đăng nhập!", Toast.LENGTH_SHORT).show()
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Không thể tự xóa chính tài khoản Quản trị Hệ thống đang đăng nhập!")
                                            }
                                        } else {
                                            selectedStoreForDelete = store
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isMasterAdminStore) Color.Gray else MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteForever,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xóa cửa hàng", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Reset Password Dialog
    selectedStoreForReset?.let { store ->
        AlertDialog(
            onDismissRequest = { selectedStoreForReset = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = null,
                        tint = PrimaryOrange
                    )
                    Text("Cấp lại mật khẩu chủ cửa hàng", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Cửa hàng: ${store.storeName}\nSĐT Chủ: ${store.ownerPhone}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (!resetErrorMsg.isNullOrEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = resetErrorMsg ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Mật khẩu mới (tối thiểu 6 ký tự)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.trim().length < 6) {
                            resetErrorMsg = "Mật khẩu phải chứa ít nhất 6 ký tự!"
                            return@Button
                        }
                        scope.launch {
                            val successRemote = SupabaseManager.resetStoreOwnerPassword(store.ownerPhone, newPassword.trim())
                            val successLocal = UserAccountStore.updatePassword(context, store.ownerPhone, newPassword.trim())
                            if (successRemote || successLocal) {
                                Toast.makeText(context, "Đã cấp lại mật khẩu mới cho ${store.ownerPhone}", Toast.LENGTH_SHORT).show()
                                snackbarHostState.showSnackbar("Đã cấp lại mật khẩu thành công!")
                                selectedStoreForReset = null
                                loadStores()
                            } else {
                                resetErrorMsg = "Không thể cập nhật mật khẩu. Vui lòng thử lại!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Cập nhật mật khẩu", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedStoreForReset = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog 2: Delete Store Confirmation Warning Dialog
    selectedStoreForDelete?.let { store ->
        val cleanOwnerPhone = store.ownerPhone.replace(" ", "")
        val isMasterAdminStore = store.storeId == "SHOP_0901992349" ||
                cleanOwnerPhone == "0901992349" ||
                cleanOwnerPhone.endsWith("0901992349") ||
                (currentUser != null && (
                    (currentUser.storeId.isNotBlank() && store.storeId == currentUser.storeId) ||
                    (currentUser.phone.isNotBlank() && cleanOwnerPhone == currentUser.phone.replace(" ", "")) ||
                    (currentUser.email.isNotBlank() && store.ownerEmail.equals(currentUser.email, ignoreCase = true))
                ))
        if (isMasterAdminStore) {
            Toast.makeText(context, "Không thể tự xóa chính tài khoản Quản trị Hệ thống đang đăng nhập!", Toast.LENGTH_SHORT).show()
            selectedStoreForDelete = null
            return@let
        }
        AlertDialog(
            onDismissRequest = { if (!isDeleting) selectedStoreForDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("CẢNH BÁO XÓA CỬA HÀNG", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bạn có chắc chắn muốn XÓA VĨNH VIỄN cửa hàng \"${store.storeName}\" (Mã ID: ${store.storeId})?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hành động này sẽ XÓA TOÀN BỘ dữ liệu bao gồm:\n" +
                                "• Cấu hình cửa hàng (store_settings)\n" +
                                "• Tất cả tài khoản nhân viên (staff_accounts)\n" +
                                "• Danh sách khách hàng (customers)\n" +
                                "• Danh sách thiết bị (equipment)\n" +
                                "• Tất cả hợp đồng & đơn thuê (rental_orders)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ Thao tác này KHÔNG THỂ KHÔI PHỤC!",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            try {
                                SupabaseManager.purgeStoreData(store.storeId, store.ownerPhone)
                                UserAccountStore.purgeLocalStoreData(context, store.storeId, store.ownerPhone)
                                Toast.makeText(context, "Đã xóa toàn bộ dữ liệu cửa hàng ${store.storeName}", Toast.LENGTH_SHORT).show()
                                snackbarHostState.showSnackbar("Đã xóa cửa hàng ${store.storeName} thành công!")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lỗi khi xóa cửa hàng: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isDeleting = false
                                selectedStoreForDelete = null
                                loadStores()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Xác nhận XÓA", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedStoreForDelete = null },
                    enabled = !isDeleting
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}
