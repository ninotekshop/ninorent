package com.ninotek.ninorent.platform

import androidx.compose.runtime.Composable

/** Chặn nút Back (Android). Trên iOS không có nút Back hệ thống nên không làm gì. */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
