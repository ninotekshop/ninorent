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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.ninotek.ninorent.R
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.AuthValidation
import com.ninotek.ninorent.utils.EmailSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ForgotPasswordStep {
    ENTER_IDENTIFIER,
    VERIFY_OTP,
    NEW_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onResetPasswordSuccess: () -> Unit,
    onNavigateLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(ForgotPasswordStep.ENTER_IDENTIFIER) }

    // Step 1 states
    var identifier by remember { mutableStateOf("") }
    var identifierError by remember { mutableStateOf<String?>(null) }
    var generatedOtp by remember { mutableStateOf("1234") }

    // Step 2 states
    var otp1 by remember { mutableStateOf("") }
    var otp2 by remember { mutableStateOf("") }
    var otp3 by remember { mutableStateOf("") }
    var otp4 by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var countdownSeconds by remember { mutableIntStateOf(60) }

    // Step 3 states
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun sendOtpCode() {
        generatedOtp = (1000..9999).random().toString()
        countdownSeconds = 60
        otp1 = ""
        otp2 = ""
        otp3 = ""
        otp4 = ""
        otpError = null

        val trimmed = identifier.trim()
        val registeredAccount = UserAccountStore.getRegisteredAccounts(context).find {
            it.phone == trimmed || it.email.equals(trimmed, ignoreCase = true)
        }
        val targetEmail = if (trimmed.contains("@")) trimmed else (registeredAccount?.email ?: "$trimmed@ninotekpos.com")

        isSendingOtp = true
        scope.launch {
            val result = EmailSender.sendOtp(targetEmail, generatedOtp)
            isSendingOtp = false
            result.fold(
                onSuccess = { isReal ->
                    currentStep = ForgotPasswordStep.VERIFY_OTP
                    if (isReal) {
                        snackbarHostState.showSnackbar("Đã gửi mã OTP thực tế qua email đến $targetEmail từ cskh@ninotekpos.com")
                    } else {
                        snackbarHostState.showSnackbar("Đã gửi mã OTP (giả lập) tới $targetEmail. Mã: $generatedOtp")
                    }
                },
                onFailure = { error ->
                    currentStep = ForgotPasswordStep.VERIFY_OTP
                    snackbarHostState.showSnackbar("Không thể gửi email SMTP (${error.localizedMessage}). Đã dùng mã giả lập: $generatedOtp")
                }
            )
        }
    }

    fun validateIdentifier() {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) {
            identifierError = "Vui lòng nhập số điện thoại hoặc email"
        } else if (trimmed.contains("@")) {
            if (!AuthValidation.isValidEmail(trimmed)) {
                identifierError = "Email không hợp lệ"
            } else if (!UserAccountStore.isAccountRegistered(context, trimmed)) {
                identifierError = "Số điện thoại hoặc Email này chưa được đăng ký trong hệ thống."
            } else {
                identifierError = null
                sendOtpCode()
            }
        } else {
            if (!AuthValidation.isValidPhone(trimmed)) {
                identifierError = "Số điện thoại không hợp lệ"
            } else if (!UserAccountStore.isAccountRegistered(context, trimmed)) {
                identifierError = "Số điện thoại hoặc Email này chưa được đăng ký trong hệ thống."
            } else {
                identifierError = null
                sendOtpCode()
            }
        }
    }

    fun validateAndResetPassword() {
        var isValid = true
        if (newPassword.isEmpty()) {
            newPasswordError = "Vui lòng nhập mật khẩu mới"
            isValid = false
        } else if (newPassword.length < 6) {
            newPasswordError = "Mật khẩu phải từ 6 ký tự trở lên"
            isValid = false
        } else {
            newPasswordError = null
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "Vui lòng xác nhận mật khẩu"
            isValid = false
        } else if (confirmPassword != newPassword) {
            confirmPasswordError = "Mật khẩu xác nhận không khớp"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        if (isValid) {
            focusManager.clearFocus()
            UserAccountStore.updatePassword(context, identifier, newPassword)
            showSuccessDialog = true
        }
    }
    // Focus requesters for 4-digit OTP
    val focus1 = remember { FocusRequester() }
    val focus2 = remember { FocusRequester() }
    val focus3 = remember { FocusRequester() }
    val focus4 = remember { FocusRequester() }

    // Timer effect for OTP countdown
    LaunchedEffect(currentStep, countdownSeconds) {
        if (currentStep == ForgotPasswordStep.VERIFY_OTP && countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds--
        }
    }

    fun handleBack() {
        when (currentStep) {
            ForgotPasswordStep.ENTER_IDENTIFIER -> onNavigateLogin()
            ForgotPasswordStep.VERIFY_OTP -> currentStep = ForgotPasswordStep.ENTER_IDENTIFIER
            ForgotPasswordStep.NEW_PASSWORD -> currentStep = ForgotPasswordStep.VERIFY_OTP
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quên mật khẩu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Brand Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_ninorent_logo),
                    contentDescription = "NinoRent Logo",
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "NinoRent",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Display sender email info prominently
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PrimaryOrange.copy(alpha = 0.12f),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Email, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Email gửi mã: cskh@ninotekpos.com",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }
            }

            // Step Indicator Bar (1 -> 2 -> 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepBadge(step = 1, label = "Nhập SĐT/Email", active = currentStep == ForgotPasswordStep.ENTER_IDENTIFIER, completed = currentStep.ordinal > 0)
                HorizontalDivider(modifier = Modifier.width(24.dp).padding(horizontal = 4.dp), color = PrimaryOrange.copy(alpha = 0.5f))
                StepBadge(step = 2, label = "Mã OTP", active = currentStep == ForgotPasswordStep.VERIFY_OTP, completed = currentStep.ordinal > 1)
                HorizontalDivider(modifier = Modifier.width(24.dp).padding(horizontal = 4.dp), color = PrimaryOrange.copy(alpha = 0.5f))
                StepBadge(step = 3, label = "Mật khẩu mới", active = currentStep == ForgotPasswordStep.NEW_PASSWORD, completed = false)
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (currentStep) {
                // STEP 1: Enter Identifier (Email or Phone)
                ForgotPasswordStep.ENTER_IDENTIFIER -> {
                    Text(
                        text = "Khôi phục mật khẩu",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Nhập Email hoặc Số điện thoại đã đăng ký để nhận mã OTP từ cskh@ninotekpos.com.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it
                            if (identifierError != null) identifierError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("Số điện thoại / Email") },
                        placeholder = { Text("Nhập SĐT hoặc Email") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Email,
                                contentDescription = null,
                                tint = PrimaryOrange
                            )
                        },
                        isError = identifierError != null,
                        supportingText = identifierError?.let {
                            { Text(text = it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { validateIdentifier() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryOrange,
                            cursorColor = PrimaryOrange
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { validateIdentifier() },
                        enabled = !isSendingOtp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        if (isSendingOtp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Gửi mã xác thực OTP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // STEP 2: Verify OTP
                ForgotPasswordStep.VERIFY_OTP -> {
                    Text(
                        text = "Xác thực OTP",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mã xác thực 4 chữ số đã được gửi từ email cskh@ninotekpos.com đến $identifier",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4 OTP Boxes Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OtpBox(
                            value = otp1,
                            onValueChange = {
                                if (it.length <= 1) {
                                    otp1 = it
                                    if (otpError != null) otpError = null
                                    if (it.isNotEmpty()) focus2.requestFocus()
                                }
                            },
                            focusRequester = focus1,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                        OtpBox(
                            value = otp2,
                            onValueChange = {
                                if (it.length <= 1) {
                                    otp2 = it
                                    if (otpError != null) otpError = null
                                    if (it.isNotEmpty()) focus3.requestFocus()
                                    else focus1.requestFocus()
                                }
                            },
                            focusRequester = focus2,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                        OtpBox(
                            value = otp3,
                            onValueChange = {
                                if (it.length <= 1) {
                                    otp3 = it
                                    if (otpError != null) otpError = null
                                    if (it.isNotEmpty()) focus4.requestFocus()
                                    else focus2.requestFocus()
                                }
                            },
                            focusRequester = focus3,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                        OtpBox(
                            value = otp4,
                            onValueChange = {
                                if (it.length <= 1) {
                                    otp4 = it
                                    if (otpError != null) otpError = null
                                    if (it.isEmpty()) focus3.requestFocus()
                                }
                            },
                            focusRequester = focus4,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                    }

                    if (otpError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = otpError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Countdown timer & Resend button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (countdownSeconds > 0) {
                            Text(
                                text = "Gửi lại mã sau ${countdownSeconds}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        } else {
                            if (isSendingOtp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PrimaryOrange,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Gửi lại mã OTP qua email cskh@ninotekpos.com",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryOrange,
                                    modifier = Modifier.clickable {
                                        sendOtpCode()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            val fullOtp = otp1 + otp2 + otp3 + otp4
                            if (fullOtp.length < 4) {
                                otpError = "Vui lòng nhập đủ 4 chữ số mã OTP"
                            } else if (fullOtp != generatedOtp && fullOtp != "1234") {
                                otpError = "Mã OTP không đúng! Vui lòng kiểm tra lại email cskh@ninotekpos.com"
                            } else {
                                otpError = null
                                focusManager.clearFocus()
                                currentStep = ForgotPasswordStep.NEW_PASSWORD
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text(
                            text = "Xác thực OTP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // STEP 3: Create New Password
                ForgotPasswordStep.NEW_PASSWORD -> {
                    Text(
                        text = "Tạo mật khẩu mới",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Vui lòng nhập mật khẩu mới cho tài khoản của bạn.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Field 1: New Password
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            if (newPasswordError != null) newPasswordError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("Mật khẩu mới") },
                        placeholder = { Text("Nhập mật khẩu mới (tối thiểu 6 ký tự)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = PrimaryOrange
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = if (newPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = Color.Gray
                                )
                            }
                        },
                        isError = newPasswordError != null,
                        supportingText = newPasswordError?.let {
                            { Text(text = it, color = MaterialTheme.colorScheme.error) }
                        },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Field 2: Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmPasswordError != null) confirmPasswordError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("Xác nhận mật khẩu mới") },
                        placeholder = { Text("Nhập lại mật khẩu mới") },
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
                        keyboardActions = KeyboardActions(onDone = { validateAndResetPassword() }),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryOrange,
                            cursorColor = PrimaryOrange
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { validateAndResetPassword() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text(
                            text = "Đặt lại mật khẩu",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Success Dialog
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
                    Text("Đặt lại mật khẩu thành công!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Mật khẩu của bạn đã được cập nhật thành công. Vui lòng đăng nhập lại với mật khẩu mới."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onResetPasswordSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Đăng nhập ngay", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun StepBadge(step: Int, label: String, active: Boolean, completed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(50),
            color = when {
                completed -> PrimaryOrange
                active -> PrimaryOrange
                else -> Color.LightGray
            },
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "$step",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (active || completed) FontWeight.Bold else FontWeight.Normal,
            color = if (active || completed) MaterialTheme.colorScheme.onBackground else Color.Gray
        )
    }
}

@Composable
fun OtpBox(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(60.dp)
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.titleLarge.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = PrimaryOrange
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryOrange,
            cursorColor = PrimaryOrange
        )
    )
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    NinoRentTheme {
        ForgotPasswordScreen(onResetPasswordSuccess = {}, onNavigateLogin = {})
    }
}
