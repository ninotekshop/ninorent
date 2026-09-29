package com.ninotek.ninorent.platform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private class ToastEvent(val message: String, val durationMs: Long)

private var currentToast by mutableStateOf<ToastEvent?>(null)

/** Hiển thị thông báo ngắn (thay cho android.widget.Toast) — dùng chung cho Android và iOS. */
@Suppress("UNUSED_PARAMETER")
fun showToast(context: PlatformContext?, message: String, long: Boolean = false) {
    currentToast = ToastEvent(message, if (long) 3500L else 2000L)
}

/** Đặt một lần ở gốc UI để vẽ toast lên trên cùng. */
@Composable
fun ToastHost() {
    val event = currentToast
    LaunchedEffect(event) {
        if (event != null) {
            delay(event.durationMs)
            if (currentToast === event) currentToast = null
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 72.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(visible = event != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = event?.message.orEmpty(),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .background(Color(0xE6323232), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}
