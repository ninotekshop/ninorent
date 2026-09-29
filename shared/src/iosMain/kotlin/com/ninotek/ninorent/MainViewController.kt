package com.ninotek.ninorent

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.ninotek.ninorent.platform.IosPlatformContext
import com.ninotek.ninorent.platform.LocalPlatformContext
import platform.UIKit.UIViewController

/** Điểm vào của UI trên iOS; SwiftUI nhúng view controller này (xem iosApp/iosApp/ContentView.swift). */
fun MainViewController(): UIViewController = ComposeUIViewController {
    CompositionLocalProvider(LocalPlatformContext provides IosPlatformContext) {
        NinoRentRoot()
    }
}
