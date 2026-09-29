package com.ninotek.ninorent.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ninotek.ninorent.utils.CccdData

/** Trạng thái quyền Camera. */
interface CameraPermissionState {
    val granted: Boolean
    fun request()
}

@Composable
expect fun rememberCameraPermissionState(): CameraPermissionState

/**
 * Khung xem camera sau, tự quét mã QR CCCD.
 * Gọi [onCccdScanned] khi đọc được dữ liệu CCCD hợp lệ.
 */
@Composable
expect fun CccdCameraViewfinder(onCccdScanned: (CccdData) -> Unit, modifier: Modifier)
