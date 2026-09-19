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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.model.UserAccount
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.model.UserRole
import com.ninotek.ninorent.model.parseUserRole
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.SupabaseManager
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = remember { UserAccountStore.getCurrentSession(context) }
    var staffList by remember {
        mutableStateOf(UserAccountStore.getStaffAccountsForActiveStore(context).filter {
            it.phone != currentUser?.phone && it.email != currentUser?.email
        })
    }

    // Add dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var addFullName by remember { mutableStateOf("") }
    var addPhone by remember { mutableStateOf("") }
    var addEmail by remember { mutableStateOf("") }
    var addPassword by remember { mutableStateOf("") }
    var addRole by remember { mutableStateOf(UserRole.STAFF) }
    var addErrorMessage by remember { mutableStateOf<String?>(null) }

    // Edit dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var staffToEdit by remember { mutableStateOf<UserAccount?>(null) }
    var editFullName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf(UserRole.STAFF) }
    var editErrorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        when {
            showAddDialog -> showAddDialog = false
            showEditDialog -> showEditDialog = false
            else -> onBack()
        }
    }

    val openAddDialog: () -> Unit = {
        addFullName = ""
        addPhone = ""
        addEmail = ""
        addPassword = ""
        addRole = UserRole.STAFF
        addErrorMessage = null
        showAddDialog = true
    }

    val openEditDialog: (UserAccount) -> Unit = { staff ->
        staffToEdit = staff
        editFullName = staff.fullName
        editPhone = staff.phone
        editEmail = staff.email
        editPassword = staff.password
        editRole = staff.role
        editErrorMessage = null
        showEditDialog = true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val attemptDeleteStaff: (UserAccount) -> Unit = { staffToDelete ->
        val isSelf = currentUser != null && (
            (currentUser.phone.isNotBlank() && staffToDelete.phone == currentUser.phone) ||
            (currentUser.email.isNotBlank() && staffToDelete.email.equals(currentUser.email, ignoreCase = true))
        )
        if (isSelf) {
            Toast.makeText(context, "Không thể tự xóa chính tài khoản Admin đang đăng nhập!", Toast.LENGTH_SHORT).show()
            scope.launch {
                snackbarHostState.showSnackbar("Không thể tự xóa chính tài khoản Admin đang đăng nhập!")
            }
        } else {
            UserAccountStore.deleteAccountByPhone(context, staffToDelete.phone)
                    staffList = UserAccountStore.getStaffAccountsForActiveStore(context).filter {
                        it.phone != currentUser?.phone && it.email != currentUser?.email
                    }
            scope.launch {
                SupabaseManager.deleteStaffAccountByPhone(staffToDelete.phone)
                Toast.makeText(context, "Đã xóa nhân viên ${staffToDelete.fullName}", Toast.LENGTH_SHORT).show()
                snackbarHostState.showSnackbar("Đã xóa nhân viên ${staffToDelete.fullName}")
            }
        }
    }

    // Sync with Supabase on launch
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val remoteStaff = SupabaseManager.fetchStaffAccounts()
                if (!remoteStaff.isNullOrEmpty()) {
                    remoteStaff.forEach { dto ->
                        val parsedRole = parseUserRole(dto.role)
                        val acc = UserAccount(
                            id = dto.id.ifBlank { UUID.randomUUID().toString() },
                            fullName = dto.full_name,
                            phone = dto.phone,
                            email = dto.email,
                            password = dto.password,
                            role = parsedRole,
                            staffRoleDetail = dto.staff_role_detail.ifBlank { parsedRole.displayName },
                            storeId = dto.storeId ?: ""
                        )
                        UserAccountStore.registerAccount(context, acc)
                    }
                            staffList = UserAccountStore.getStaffAccountsForActiveStore(context).filter {
                        it.phone != currentUser?.phone && it.email != currentUser?.email
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Quản lý nhân viên", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = openAddDialog) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = "Thêm nhân viên", tint = PrimaryOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = openAddDialog,
                containerColor = PrimaryOrange,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Thêm nhân viên")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (staffList.isEmpty()) {
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
                        imageVector = Icons.Rounded.Badge,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "Chưa có tài khoản nhân viên nào",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Button(
                        onClick = openAddDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thêm nhân viên mới")
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
                items(staffList) { staff ->
                    val isCurrentUser = currentUser != null && (
                        (currentUser.phone.isNotBlank() && staff.phone == currentUser.phone) ||
                        (currentUser.email.isNotBlank() && staff.email.equals(currentUser.email, ignoreCase = true))
                    )
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
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (staff.role == UserRole.OWNER) PrimaryOrange.copy(alpha = 0.15f)
                                        else Color(0xFF1976D2).copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (staff.role == UserRole.OWNER) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = if (staff.role == UserRole.OWNER) PrimaryOrange else Color(0xFF1976D2),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = staff.fullName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "SĐT: ${staff.phone} • Email: ${staff.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (staff.role == UserRole.OWNER) PrimaryOrange.copy(alpha = 0.15f) else Color(0xFF1976D2).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = staff.role.displayName,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (staff.role == UserRole.OWNER) PrimaryOrange else Color(0xFF1976D2)
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { openEditDialog(staff) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Chỉnh sửa",
                                        tint = PrimaryOrange
                                    )
                                }
                                if (!isCurrentUser) {
                                    IconButton(
                                        onClick = { attemptDeleteStaff(staff) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Xóa",
                                            tint = MaterialTheme.colorScheme.error
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

    // Add Staff Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm tài khoản nhân viên", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!addErrorMessage.isNullOrEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = addErrorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = addFullName,
                        onValueChange = { addFullName = it },
                        label = { Text("Họ và tên") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = addPhone,
                        onValueChange = { addPhone = it },
                        label = { Text("Số điện thoại") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = addEmail,
                        onValueChange = { addEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = addPassword,
                        onValueChange = { addPassword = it },
                        label = { Text("Mật khẩu (tối thiểu 6 ký tự)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Vai trò tài khoản:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = addRole == UserRole.OWNER,
                            onClick = { addRole = UserRole.OWNER },
                            label = { Text("Admin") },
                            leadingIcon = if (addRole == UserRole.OWNER) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = addRole == UserRole.STAFF,
                            onClick = { addRole = UserRole.STAFF },
                            label = { Text("Nhân viên") },
                            leadingIcon = if (addRole == UserRole.STAFF) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addFullName.isNotBlank() && addPhone.isNotBlank() && addEmail.isNotBlank() && addPassword.length >= 6) {
                            addErrorMessage = null
                            val currentOwner = UserAccountStore.getCurrentSession(context)
                            val ownerStoreId = currentOwner?.storeId ?: UserAccountStore.activeStoreId
                            val newStaff = UserAccount(
                                fullName = addFullName.trim(),
                                phone = addPhone.trim(),
                                email = addEmail.trim(),
                                password = addPassword,
                                role = addRole,
                                staffRoleDetail = addRole.displayName,
                                storeId = ownerStoreId
                            )
                            scope.launch {
                                try {
                                    UserAccountStore.registerAccount(context, newStaff)
                                            staffList = UserAccountStore.getStaffAccountsForActiveStore(context).filter {
                        it.phone != currentUser?.phone && it.email != currentUser?.email
                    }
                                    showAddDialog = false

                                    addFullName = ""
                                    addPhone = ""
                                    addEmail = ""
                                    addPassword = ""
                                    addErrorMessage = null
                                    Toast.makeText(context, "Thêm nhân viên và đồng bộ Supabase thành công!", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("Thêm nhân viên và đồng bộ Supabase thành công!")
                                } catch (e: Exception) {
                                    addErrorMessage = e.message ?: "Lỗi lưu tài khoản lên Supabase. Vui lòng kiểm tra kết nối hoặc RLS!"
                                }
                            }
                        } else {
                            addErrorMessage = "Vui lòng điền đầy đủ thông tin (Mật khẩu từ 6 ký tự)!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Thêm nhân viên", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Edit Staff Dialog
    if (showEditDialog && staffToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Chỉnh sửa tài khoản nhân viên", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!editErrorMessage.isNullOrEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = editErrorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = { Text("Họ và tên") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Số điện thoại") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("Mật khẩu (tối thiểu 6 ký tự)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Vai trò tài khoản:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = editRole == UserRole.OWNER,
                            onClick = { editRole = UserRole.OWNER },
                            label = { Text("Admin") },
                            leadingIcon = if (editRole == UserRole.OWNER) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = editRole == UserRole.STAFF,
                            onClick = { editRole = UserRole.STAFF },
                            label = { Text("Nhân viên") },
                            leadingIcon = if (editRole == UserRole.STAFF) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentStaff = staffToEdit
                        if (currentStaff != null && editFullName.isNotBlank() && editPhone.isNotBlank() && editEmail.isNotBlank() && editPassword.length >= 6) {
                            editErrorMessage = null
                            val updatedStaff = currentStaff.copy(
                                fullName = editFullName.trim(),
                                phone = editPhone.trim(),
                                email = editEmail.trim(),
                                password = editPassword,
                                role = editRole,
                                staffRoleDetail = editRole.displayName
                            )
                            scope.launch {
                                try {
                                    UserAccountStore.updateAccount(context, updatedStaff, oldPhone = currentStaff.phone)
                                            staffList = UserAccountStore.getStaffAccountsForActiveStore(context).filter {
                        it.phone != currentUser?.phone && it.email != currentUser?.email
                    }
                                    showEditDialog = false
                                    Toast.makeText(context, "Cập nhật thông tin nhân viên thành công!", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("Cập nhật thông tin nhân viên thành công!")
                                } catch (e: Exception) {
                                    editErrorMessage = e.message ?: "Lỗi lưu tài khoản lên Supabase. Vui lòng kiểm tra kết nối hoặc RLS!"
                                }
                            }
                        } else {
                            editErrorMessage = "Vui lòng điền đầy đủ thông tin (Mật khẩu từ 6 ký tự)!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
