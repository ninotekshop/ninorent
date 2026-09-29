package com.ninotek.ninorent.platform

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Handle tới môi trường nền tảng: `android.content.Context` trên Android, đối tượng rỗng trên iOS.
 * Code dùng chung chỉ truyền nó xuống các hàm platform (prefs, file, PDF...).
 */
expect abstract class PlatformContext

val LocalPlatformContext = staticCompositionLocalOf<PlatformContext> {
    error("PlatformContext chưa được cung cấp. Hãy bọc UI trong CompositionLocalProvider(LocalPlatformContext provides ...).")
}
