package com.ninotek.ninorent.platform

/** Đọc toàn bộ nội dung từ một URI/đường dẫn (file://, content://...). Trả null nếu không đọc được. */
expect fun readUriBytes(context: PlatformContext, uri: String): ByteArray?
