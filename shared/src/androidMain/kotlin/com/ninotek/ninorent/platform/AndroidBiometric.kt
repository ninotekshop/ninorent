package com.ninotek.ninorent.platform

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val context = LocalContext.current
    return remember(context) { AndroidBiometricAuthenticator(context) }
}

private class AndroidBiometricAuthenticator(private val context: Context) : BiometricAuthenticator {
    override fun authenticate(
        title: String,
        subtitle: String,
        cancelText: String,
        onResult: (BiometricResult) -> Unit
    ) {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            onResult(BiometricResult.Failed)
            return
        }
        val canAuthenticate = BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult(BiometricResult.Failed)
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(BiometricResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    onResult(if (cancelled) BiometricResult.Cancelled else BiometricResult.Failed)
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(cancelText)
            .build()
        prompt.authenticate(info)
    }
}
