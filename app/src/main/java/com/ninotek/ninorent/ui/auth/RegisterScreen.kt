package com.ninotek.ninorent.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.ninotek.ninorent.utils.normalizeStoreIdFromPhone
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.R
import com.ninotek.ninorent.model.UserAccount
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.model.UserRole
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.AuthValidation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (String) -> Unit,
    onNavigateLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isAgreed by remember { mutableStateOf(false) }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var supabaseErrorDialog by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    fun validateAndSubmit() {
        var isValid = true

        val trimmedName = fullName.trim()
        val trimmedPhone = phone.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isEmpty()) {
            fullNameError = "Vui lòng nhập họ và tên"
            isValid = false
        } else {
            fullNameError = null
        }

        if (trimmedPhone.isEmpty()) {
            phoneError = "Vui lòng nhập số điện thoại"
            isValid = false
        } else if (!AuthValidation.isValidPhone(trimmedPhone)) {
            phoneError = "Số điện thoại không hợp lệ (ví dụ: 0901234567)"
            isValid = false
        } else {
            phoneError = null
        }

        if (trimmedEmail.isEmpty()) {
            emailError = "Vui lòng nhập địa chỉ email"
            isValid = false
        } else if (!AuthValidation.isValidEmail(trimmedEmail)) {
            emailError = "Địa chỉ email không hợp lệ"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "Vui lòng nhập mật khẩu"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Mật khẩu phải từ 6 ký tự trở lên"
            isValid = false
        } else {
            passwordError = null
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "Vui lòng xác nhận mật khẩu"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordError = "Mật khẩu xác nhận không khớp"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        if (!isAgreed) {
            termsError = "Bạn cần đồng ý với Điều khoản dịch vụ & Chính sách bảo mật"
            isValid = false
        } else {
            termsError = null
        }

        if (isValid) {
            focusManager.clearFocus()
            isLoading = true
            supabaseErrorDialog = null
            val generatedStoreId = normalizeStoreIdFromPhone(trimmedPhone)
            val newAccount = UserAccount(
                fullName = trimmedName,
                phone = trimmedPhone,
                email = trimmedEmail,
                password = password,
                biometricEnabled = false,
                role = UserRole.OWNER,
                storeId = generatedStoreId
            )
            coroutineScope.launch {
                try {
                    UserAccountStore.registerAccount(context, newAccount)
                    isLoading = false
                    showSuccessDialog = true
                } catch (e: Exception) {
                    isLoading = false
                    supabaseErrorDialog = e.message ?: "Lỗi lưu tài khoản lên Supabase: Lỗi không xác định. Vui lòng kiểm tra kết nối hoặc cấu hình RLS trên Supabase!"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Đăng ký tài khoản",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_ninorent_logo),
                    contentDescription = "NinoRent Logo",
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "NinoRent",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "Tạo tài khoản mới",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tham gia NinoRent để quản lý cho thuê dễ dàng",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Field 1: Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    if (fullNameError != null) fullNameError = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("Họ và tên") },
                placeholder = { Text("Nhập họ và tên đầy đủ") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = PrimaryOrange
                    )
                },
                isError = fullNameError != null,
                supportingText = fullNameError?.let {
                    { Text(text = it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    cursorColor = PrimaryOrange
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Field 2: Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    if (phoneError != null) phoneError = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("Số điện thoại") },
                placeholder = { Text("Nhập số điện thoại (10 chữ số)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                        tint = PrimaryOrange
                    )
                },
                isError = phoneError != null,
                supportingText = phoneError?.let {
                    { Text(text = it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    cursorColor = PrimaryOrange
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Field 3: Email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("Email") },
                placeholder = { Text("Nhập địa chỉ email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = PrimaryOrange
                    )
                },
                isError = emailError != null,
                supportingText = emailError?.let {
                    { Text(text = it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    cursorColor = PrimaryOrange
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Field 4: Password
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("Mật khẩu") },
                placeholder = { Text("Nhập mật khẩu (tối thiểu 6 ký tự)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = PrimaryOrange
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                            tint = Color.Gray
                        )
                    }
                },
                isError = passwordError != null,
                supportingText = passwordError?.let {
                    { Text(text = it, color = MaterialTheme.colorScheme.error) }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    cursorColor = PrimaryOrange
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Field 5: Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (confirmPasswordError != null) confirmPasswordError = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("Xác nhận mật khẩu") },
                placeholder = { Text("Nhập lại mật khẩu") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = PrimaryOrange
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                            tint = Color.Gray
                        )
                    }
                },
                isError = confirmPasswordError != null,
                supportingText = confirmPasswordError?.let {
                    { Text(text = it, color = MaterialTheme.colorScheme.error) }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { validateAndSubmit() }),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    cursorColor = PrimaryOrange
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Checkbox Terms
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAgreed,
                    onCheckedChange = {
                        isAgreed = it
                        if (it) termsError = null
                    },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tôi đồng ý với Điều khoản dịch vụ & Chính sách bảo mật",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (termsError != null) {
                Text(
                    text = termsError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Register Button
            Button(
                onClick = { validateAndSubmit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Đăng ký",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Already have an account link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đã có tài khoản? ",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Đăng nhập ngay",
                    color = PrimaryOrange,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onNavigateLogin() }
                )
            }
        }
    }

    // Registration Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng ký tài khoản thành công!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Đăng ký tài khoản thành công! Vui lòng đăng nhập để tiếp tục."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onRegisterSuccess(phone.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Đăng nhập ngay", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Supabase / Registration Error Dialog
    if (supabaseErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { supabaseErrorDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lỗi đăng ký tài khoản", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(supabaseErrorDialog ?: "")
            },
            confirmButton = {
                Button(
                    onClick = { supabaseErrorDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Đóng", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    NinoRentTheme {
        RegisterScreen(onRegisterSuccess = {}, onNavigateLogin = {})
    }
}
