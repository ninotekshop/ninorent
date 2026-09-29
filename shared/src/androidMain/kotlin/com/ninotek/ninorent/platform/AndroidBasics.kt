package com.ninotek.ninorent.platform

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

actual typealias PlatformContext = Context

private class AndroidPrefs(private val sp: SharedPreferences) : Prefs {
    override fun getString(key: String, default: String?): String? = sp.getString(key, default)
    override fun getBoolean(key: String, default: Boolean): Boolean = sp.getBoolean(key, default)
    override fun contains(key: String): Boolean = sp.contains(key)
    override fun edit(): PrefsEditor = AndroidPrefsEditor(sp.edit())
}

private class AndroidPrefsEditor(private val editor: SharedPreferences.Editor) : PrefsEditor {
    override fun putString(key: String, value: String?): PrefsEditor = apply { editor.putString(key, value) }
    override fun putBoolean(key: String, value: Boolean): PrefsEditor = apply { editor.putBoolean(key, value) }
    override fun remove(key: String): PrefsEditor = apply { editor.remove(key) }
    override fun apply() = editor.apply()
}

actual fun PlatformContext.openPrefs(name: String): Prefs =
    AndroidPrefs(getSharedPreferences(name, Context.MODE_PRIVATE))

actual fun logDebug(tag: String, message: String) {
    try {
        Log.d(tag, message)
    } catch (_: Throwable) {
        // Unit test JVM: android.util.Log không khả dụng.
        println("$tag: $message")
    }
}

actual fun logWarn(tag: String, message: String) {
    try {
        Log.w(tag, message)
    } catch (_: Throwable) {
        println("$tag: $message")
    }
}

actual fun <T> runBlockingIO(block: suspend CoroutineScope.() -> T): T = runBlocking(Dispatchers.IO, block)

actual fun logError(tag: String, message: String, error: Throwable?) {
    try {
        if (error != null) Log.e(tag, message, error) else Log.e(tag, message)
    } catch (_: Throwable) {
        println("$tag: $message ${error?.message ?: ""}")
    }
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}

actual fun readUriBytes(context: PlatformContext, uri: String): ByteArray? = try {
    context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
} catch (_: Exception) {
    null
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
