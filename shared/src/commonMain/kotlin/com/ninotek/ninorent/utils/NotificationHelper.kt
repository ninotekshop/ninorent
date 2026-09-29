package com.ninotek.ninorent.utils

import com.ninotek.ninorent.platform.PlatformContext
import com.ninotek.ninorent.platform.postLocalNotification

object NotificationHelper {
    fun sendOverdueNotification(
        context: PlatformContext,
        title: String,
        message: String,
        notificationId: Int = 1001,
    ) {
        postLocalNotification(context, title, message, notificationId)
    }
}
