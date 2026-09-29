package com.ninotek.ninorent.platform

import kotlinx.coroutines.CoroutineScope

expect fun logDebug(tag: String, message: String)

expect fun logWarn(tag: String, message: String)

expect fun logError(tag: String, message: String, error: Throwable? = null)

/**
 * Chạy khối suspend trên ioDispatcher và chặn luồng gọi cho tới khi xong.
 * Chỉ dùng cho các hàm đồng bộ cũ (validateCredentials...) chưa chuyển sang suspend.
 */
expect fun <T> runBlockingIO(block: suspend CoroutineScope.() -> T): T
