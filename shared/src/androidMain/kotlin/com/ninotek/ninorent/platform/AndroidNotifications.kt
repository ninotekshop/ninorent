package com.ninotek.ninorent.platform

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

private const val CHANNEL_ID = "overdue_orders"
private const val CHANNEL_NAME = "Thông báo đơn quá hạn"

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Kênh thông báo cảnh báo đơn thuê bị quá hạn"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

@SuppressLint("MissingPermission", "NotificationPermission")
actual fun postLocalNotification(context: PlatformContext, title: String, message: String, notificationId: Int) {
    createNotificationChannel(context)
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(context.applicationInfo.icon)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    try {
        manager.notify(notificationId, builder.build())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
