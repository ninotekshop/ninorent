package com.ninotek.ninorent.ui.auth

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.R
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.AuthValidation
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    prefilledIdentifier: String = "",
    onLoginSuccess: () -> Unit,
    onNavigateRegister: () -> Unit = {},
    onNavigateForgotPassword: () -> Unit = {},
    isBiometricEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var identifier by remember { mutableStateOf(prefilledIdentifier) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var identifierError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var showBiometricAuthDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun launchBiometricPrompt() {
        val biometricAccount = UserAccountStore.getBiometricAccount(context)
        if (biometricAccount == null) {
            Toast.makeText(
                context,
                "Vui lòng đăng ký tài khoản, đăng nhập thành công và bật Đăng nhập bằng vân tay trong Cài đặt.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            UserAccountStore.setCurrentSession(context, biometricAccount)
                            onLoginSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                showBiometricAuthDialog = true
                            }
                        }
                    }
                )
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Đăng nhập bằng vân tay")
                    .setSubtitle("Xác thực vân tay để vào ứng dụng NinoRent")
                    .setNegativeButtonText("Hủy")
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                showBiometricAuthDialog = true
            }
        } else {
            showBiometricAuthDialog = true
        }
    }

    fun validateAndSubmit() {
        var isValid = true
        val trimmedIdentifier = identifier.trim()
        val trimmedPassword = password.trim()

        generalError = null

        if (trimmedIdentifier.isEmpty()) {
            identifierError = "Vui lòng nhập số điện thoại hoặc email"
            isValid = false
        } else if (trimmedIdentifier.contains("@")) {
            if (!AuthValidation.isValidEmail(trimmedIdentifier)) {
                identifierError = "Địa chỉ email không hợp lệ"
                isValid = false
            } else {
                identifierError = null
            }
        } else {
            if (!AuthValidation.isValidPhone(trimmedIdentifier)) {
                identifierError = "Số điện thoại không hợp lệ (ví dụ: 0901234567)"
                isValid = false
            } else {
                identifierError = null
            }
        }

        if (trimmedPassword.isEmpty()) {
            passwordError = "Vui lòng nhập mật khẩu"
            isValid = false
        } else if (trimmedPassword.length < 6) {
            passwordError = "Mật khẩu phải từ 6 ký tự trở lên"
            isValid = false
        } else {
            passwordError = null
        }

        if (isValid) {
            focusManager.clearFocus()
            val validatedAccount = UserAccountStore.validateCredentials(context, trimmedIdentifier, trimmedPassword)
            if (validatedAccount != null) {
                UserAccountStore.setCurrentSession(context, validatedAccount)
                onLoginSuccess()
            } else {
                generalError = "Tài khoản hoặc mật khẩu không chính xác."
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Header & Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CHO THUÊ MÁY MÓC, THIẾT BỊ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.2.sp
                )
            }

            // Middle: Login Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Đăng nhập",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chào mừng bạn trở lại!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                if (generalError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = generalError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Phone / Email field
                OutlinedTextField(
                    value = identifier,
                    onValueChange = {
                        identifier = it
                        if (identifierError != null) identifierError = null
                        if (generalError != null) generalError = null
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
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOrange,
                        cursorColor = PrimaryOrange
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) passwordError = null
                        if (generalError != null) generalError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("Mật khẩu") },
                    placeholder = { Text("Nhập mật khẩu") },
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
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { validateAndSubmit() }),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOrange,
                        cursorColor = PrimaryOrange
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Forgot Password link
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Quên mật khẩu?",
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { onNavigateForgotPassword() }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Login Button
                Button(
                    onClick = { validateAndSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(
                        text = "Đăng nhập",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (isBiometricEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { launchBiometricPrompt() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, PrimaryOrange),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = "Vân tay",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đăng nhập bằng vân tay",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom: Register link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chưa có tài khoản? ",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Đăng ký ngay",
                    color = PrimaryOrange,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onNavigateRegister() }
                )
            }
        }

        if (showBiometricAuthDialog) {
            AlertDialog(
                onDismissRequest = { showBiometricAuthDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(56.dp)
                    )
                },
                title = { Text("Xác thực vân tay", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Chạm vào cảm biến vân tay trên thiết bị để hoàn tất đăng nhập nhanh vào NinoRent.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBiometricAuthDialog = false
                            val bioAcc = UserAccountStore.getBiometricAccount(context)
                            if (bioAcc != null) {
                                UserAccountStore.setCurrentSession(context, bioAcc)
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Chưa có tài khoản nào bật đăng nhập vân tay.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Xác thực thành công", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBiometricAuthDialog = false }) {
                        Text("Hủy", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    NinoRentTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
