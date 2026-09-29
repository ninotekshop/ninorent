package com.ninotek.ninorent.platform

/** Giữ tên `Keep` như `androidx.annotation.Keep` để code cũ không đổi; không có tác dụng trên iOS. */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.BINARY)
annotation class Keep
