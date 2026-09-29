package com.ninotek.ninorent.platform

/** Gửi thông báo cục bộ (NotificationManager trên Android, UNUserNotificationCenter trên iOS). */
expect fun postLocalNotification(context: PlatformContext, title: String, message: String, notificationId: Int)
