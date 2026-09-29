package com.ninotek.ninorent.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ninotek.ninorent.platform.CccdCameraViewfinder
import com.ninotek.ninorent.platform.rememberCameraPermissionState
import com.ninotek.ninorent.utils.CccdData
import com.ninotek.ninorent.utils.parseCccdQrPayload

private val PrimaryOrange = Color(0xFFFF6D00)
private val DarkOverlayBg = Color(0x99000000)

/**
 * Bottom Sheet / Dialog quét mã QR CCCD bằng CameraX và ML Kit Barcode Scanning.
 *
 * @param onDismiss Đóng dialog quét
 * @param onCccdScanned Callback nhận kết quả dữ liệu [CccdData] khi quét thành công
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CccdCameraScannerSheet(
    onDismiss: () -> Unit,
    onCccdScanned: (CccdData) -> Unit
) {
    val cameraPermission = rememberCameraPermissionState()
    val hasCameraPermission = cameraPermission.granted

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermission.request()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    CccdCameraViewfinder(
                        onCccdScanned = onCccdScanned,
                        modifier = Modifier.fillMaxSize()
                    )
                    ScannerOverlayUI(
                        onDismiss = onDismiss
                    )
                } else {
                    CameraPermissionDeniedUI(
                        onRequestPermission = {
                            cameraPermission.request()
                        },
                        onDismiss = onDismiss,
                        onCccdScanned = onCccdScanned
                    )
                }
            }
        }
    }
}

/**
 * Giao diện đè (Overlay) gồm khung quét QR trung tâm, các góc định vị, tia laser quét và nút điều hướng.
 */
@Composable
private fun ScannerOverlayUI(
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val translateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryOrange.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Quét mã QR thẻ CCCD",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Tự động trích xuất thông tin chủ thẻ",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Đóng",
                    tint = Color.White
                )
            }
        }

        // Central Scanner Framing Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Semi-transparent overlay surrounding central box
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(DarkOverlayBg)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(DarkOverlayBg)
                    )
                    // Clear Center Box
                    Spacer(modifier = Modifier.width(260.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(DarkOverlayBg)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(DarkOverlayBg)
                )
            }

            // Bounding square frame and corner brackets
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val cornerLen = 26.dp.toPx()
                    val cornerColor = PrimaryOrange

                    // Top-Left Corner
                    drawLine(cornerColor, start = Offset(0f, 0f), end = Offset(cornerLen, 0f), strokeWidth = strokeWidth)
                    drawLine(cornerColor, start = Offset(0f, 0f), end = Offset(0f, cornerLen), strokeWidth = strokeWidth)

                    // Top-Right Corner
                    drawLine(cornerColor, start = Offset(size.width, 0f), end = Offset(size.width - cornerLen, 0f), strokeWidth = strokeWidth)
                    drawLine(cornerColor, start = Offset(size.width, 0f), end = Offset(size.width, cornerLen), strokeWidth = strokeWidth)

                    // Bottom-Left Corner
                    drawLine(cornerColor, start = Offset(0f, size.height), end = Offset(cornerLen, size.height), strokeWidth = strokeWidth)
                    drawLine(cornerColor, start = Offset(0f, size.height), end = Offset(0f, size.height - cornerLen), strokeWidth = strokeWidth)

                    // Bottom-Right Corner
                    drawLine(cornerColor, start = Offset(size.width, size.height), end = Offset(size.width - cornerLen, size.height), strokeWidth = strokeWidth)
                    drawLine(cornerColor, start = Offset(size.width, size.height), end = Offset(size.width, size.height - cornerLen), strokeWidth = strokeWidth)

                    // Animated scanning line
                    val lineY = translateY * size.height
                    drawLine(
                        color = cornerColor,
                        start = Offset(6.dp.toPx(), lineY),
                        end = Offset(size.width - 6.dp.toPx(), lineY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }

            Text(
                text = "Đặt mã QR góc trên bên phải CCCD vào trong khung",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 32.dp, end = 32.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * Giao diện hiển thị khi người dùng chưa cấp quyền truy cập Camera.
 */
@Composable
private fun CameraPermissionDeniedUI(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onCccdScanned: (CccdData) -> Unit
) {
    val sampleQr1 = "052095001234|123456789|Nguyễn Văn Nam|15051995|Nam|123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định|15052021"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(PrimaryOrange.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Yêu cầu quyền truy cập Camera",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ứng dụng cần sử dụng máy ảnh để quét mã QR Code trên thẻ CCCD nhằm tự động trích xuất thông tin khách thuê.",
            color = Color.LightGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cấp quyền Máy ảnh", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                val parsed = parseCccdQrPayload(sampleQr1)
                if (parsed != null) {
                    onCccdScanned(parsed)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCodeScanner,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dùng dữ liệu CCCD mẫu (Không cần Camera)", color = Color.White, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Đóng",
                tint = Color.Gray
            )
        }
    }
}
