package com.ninotek.ninorent.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.UIKitView
import com.ninotek.ninorent.utils.CccdData
import com.ninotek.ninorent.utils.parseCccdQrPayload
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Image
import platform.Foundation.NSUserDefaults

actual abstract class PlatformContext

/** Trên iOS không có Context; đối tượng này chỉ là giá trị giữ chỗ. */
object IosPlatformContext : PlatformContext()

// ---------------------------------------------------------------- Prefs

private class IosPrefs(private val name: String) : Prefs {
    private val defaults = NSUserDefaults.standardUserDefaults

    private fun scoped(key: String) = "$name.$key"

    override fun getString(key: String, default: String?): String? =
        defaults.stringForKey(scoped(key)) ?: default

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(scoped(key)) == null) default else defaults.boolForKey(scoped(key))

    override fun contains(key: String): Boolean = defaults.objectForKey(scoped(key)) != null

    override fun edit(): PrefsEditor = Editor()

    private inner class Editor : PrefsEditor {
        private val operations = mutableListOf<() -> Unit>()

        override fun putString(key: String, value: String?): PrefsEditor {
            operations += {
                if (value == null) defaults.removeObjectForKey(scoped(key))
                else defaults.setObject(value, forKey = scoped(key))
            }
            return this
        }

        override fun putBoolean(key: String, value: Boolean): PrefsEditor {
            operations += { defaults.setBool(value, forKey = scoped(key)) }
            return this
        }

        override fun remove(key: String): PrefsEditor {
            operations += { defaults.removeObjectForKey(scoped(key)) }
            return this
        }

        override fun apply() {
            operations.forEach { it() }
            operations.clear()
        }
    }
}

actual fun PlatformContext.openPrefs(name: String): Prefs = IosPrefs(name)

// ---------------------------------------------------------------- Logging / threading

actual fun logDebug(tag: String, message: String) {
    println("D/$tag: $message")
}

actual fun logWarn(tag: String, message: String) {
    println("W/$tag: $message")
}

actual fun logError(tag: String, message: String, error: Throwable?) {
    println("E/$tag: $message ${error?.message ?: ""}")
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

actual fun <T> runBlockingIO(block: suspend CoroutineScope.() -> T): T = runBlocking(Dispatchers.Default, block)

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS không có nút Back hệ thống; các màn hình đã có nút quay lại trên giao diện.
}

// ---------------------------------------------------------------- Files / notifications / SMTP

actual fun readUriBytes(context: PlatformContext, uri: String): ByteArray? = iosBridge.readFileBytes(uri)

actual fun postLocalNotification(context: PlatformContext, title: String, message: String, notificationId: Int) {
    iosBridge.postNotification(title, message, notificationId)
}

actual suspend fun sendHtmlEmail(config: SmtpConfig, toEmail: String, subject: String, htmlBody: String) {
    // TODO: chưa có client SMTP an toàn cho iOS (Kotlin/Native không xác thực chứng chỉ TLS của ktor-network-tls).
    // Nên chuyển việc gửi OTP sang server (Supabase Edge Function). Màn Quên mật khẩu tự chuyển sang OTP giả lập khi lỗi.
    throw UnsupportedOperationException("Gửi email SMTP chưa được hỗ trợ trên iOS")
}

// ---------------------------------------------------------------- Photos

private fun pickIosPhoto(useCamera: Boolean, filePrefix: String, onPhoto: (PickedPhoto) -> Unit) {
    val bridge = iosBridge
    bridge.pickImage(useCamera, filePrefix) { path ->
        if (path != null) {
            val preview = if (useCamera) {
                bridge.readFileBytes(path)?.let { bytes ->
                    try {
                        Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    } catch (_: Throwable) {
                        null
                    }
                }
            } else {
                null
            }
            onPhoto(PickedPhoto("file://$path", preview))
        }
    }
}

@Composable
actual fun rememberGalleryPicker(filePrefix: String, onPhoto: (PickedPhoto) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPhoto)
    return remember(filePrefix) { { pickIosPhoto(false, filePrefix) { callback.value(it) } } }
}

@Composable
actual fun rememberCameraCapture(filePrefix: String, onPhoto: (PickedPhoto) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPhoto)
    return remember(filePrefix) { { pickIosPhoto(true, filePrefix) { callback.value(it) } } }
}

// ---------------------------------------------------------------- Biometric

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator = remember {
    object : BiometricAuthenticator {
        override fun authenticate(
            title: String,
            subtitle: String,
            cancelText: String,
            onResult: (BiometricResult) -> Unit
        ) {
            iosBridge.authenticateBiometric(title, subtitle, cancelText) { code ->
                onResult(
                    when (code) {
                        "success" -> BiometricResult.Success
                        "cancelled" -> BiometricResult.Cancelled
                        else -> BiometricResult.Failed
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Camera scanner

@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {
    var isGranted by remember { mutableStateOf(iosBridge.isCameraAuthorized()) }
    return remember(isGranted) {
        val current = isGranted
        object : CameraPermissionState {
            override val granted: Boolean = current
            override fun request() {
                iosBridge.requestCameraPermission { result -> isGranted = result == "granted" }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun CccdCameraViewfinder(onCccdScanned: (CccdData) -> Unit, modifier: Modifier) {
    val callback = rememberUpdatedState(onCccdScanned)
    val delivered = remember { booleanArrayOf(false) }
    UIKitView(
        factory = {
            iosBridge.makeQrScannerView { text ->
                if (!delivered[0]) {
                    val data = parseCccdQrPayload(text)
                    if (data != null) {
                        delivered[0] = true
                        callback.value(data)
                    }
                }
            }
        },
        modifier = modifier,
        onRelease = { view -> iosBridge.stopQrScannerView(view) }
    )
}

// ---------------------------------------------------------------- PDF

actual fun buildPdf(pageWidth: Int, pageHeight: Int, draw: (PdfPage) -> Unit): ByteArray =
    iosBridge.createPdf(pageWidth, pageHeight, draw) ?: ByteArray(0)

actual fun printPdf(context: PlatformContext, jobName: String, pdfBytes: ByteArray) {
    iosBridge.printPdf(jobName, pdfBytes)
}

actual fun sharePdf(context: PlatformContext, fileName: String, pdfBytes: ByteArray, chooserTitle: String) {
    iosBridge.sharePdf(fileName, pdfBytes)
}
