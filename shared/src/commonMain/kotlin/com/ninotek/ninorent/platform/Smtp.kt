package com.ninotek.ninorent.platform

/** Thông tin kết nối SMTP. */
class SmtpConfig(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val fromEmail: String,
    val fromName: String
)

/**
 * Gửi một email HTML qua SMTP. Ném exception nếu thất bại.
 * Android dùng JavaMail; iOS dùng client SMTP tự viết trên ktor-network.
 */
expect suspend fun sendHtmlEmail(config: SmtpConfig, toEmail: String, subject: String, htmlBody: String)
