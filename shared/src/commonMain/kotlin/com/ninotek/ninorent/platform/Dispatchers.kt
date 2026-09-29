package com.ninotek.ninorent.platform

import kotlinx.coroutines.CoroutineDispatcher

/** Dispatcher cho tác vụ I/O (Dispatchers.IO không có trong commonMain). */
expect val ioDispatcher: CoroutineDispatcher
