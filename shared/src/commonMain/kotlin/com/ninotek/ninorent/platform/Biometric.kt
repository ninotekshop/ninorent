package com.ninotek.ninorent.platform

import androidx.compose.runtime.Composable

enum class BiometricResult {
    /** Xác thực thành công. */
    Success,

    /** Người dùng chủ động huỷ — không cần hiện phương án dự phòng. */
    Cancelled,

    /** Lỗi xác thực hoặc thiết bị không hỗ trợ/chưa đăng ký sinh trắc học. */
    Failed
}

interface BiometricAuthenticator {
    fun authenticate(
        title: String,
        subtitle: String,
        cancelText: String,
        onResult: (BiometricResult) -> Unit
    )
}

/** Vân tay/Face ID: BiometricPrompt trên Android, LocalAuthentication trên iOS. */
@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
