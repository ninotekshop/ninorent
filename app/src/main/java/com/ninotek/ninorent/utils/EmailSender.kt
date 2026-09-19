package com.ninotek.ninorent.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {
    var smtpHost: String = "smtp.hostinger.com"
    var smtpPort: String = "465"
    var smtpUser: String = "cskh@ninotekpos.com"
    var smtpPass: String = "Sony@0906" // Configurable SMTP password for cskh@ninotekpos.com
    var fromEmail: String = "cskh@ninotekpos.com"

    /**
     * Sends an OTP email asynchronously on Dispatchers.IO.
     * Returns Result.success(true) if real email sent successfully,
     * Result.success(false) if simulated (due to missing SMTP credentials),
     * or Result.failure if an error occurred during sending.
     */
    suspend fun sendOtp(toEmail: String, otpCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (smtpPass.isBlank()) {
                // Graceful simulation fallback if SMTP credentials are not configured
                delay(800L) // Simulate network latency
                println("EmailSender: SMTP credentials (smtpPass) not configured. Simulated sending OTP $otpCode to $toEmail")
                return@withContext Result.success(false)
            }

            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.host", smtpHost)
                put("mail.smtp.port", smtpPort)
                if (smtpPort == "465") {
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.socketFactory.fallback", "false")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                }
                put("mail.smtp.ssl.trust", smtpHost)
                put("mail.transport.protocol", "smtp")
                put("mail.smtp.connectiontimeout", "15000")
                put("mail.smtp.timeout", "15000")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(smtpUser, smtpPass)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail, "NinoRent CSKH"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "[NinoRent] Mã xác thực khôi phục mật khẩu (OTP)"
                setContent(
                    """
                    <div style="font-family: Arial, sans-serif; padding: 24px; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px;">
                        <h2 style="color: #FF6F00; margin-top: 0;">NinoRent - Khôi phục mật khẩu</h2>
                        <p>Xin chào quý khách,</p>
                        <p>Bạn nhận được email này vì đã yêu cầu khôi phục mật khẩu cho tài khoản ứng dụng cho thuê thiết bị NinoRent.</p>
                        <p>Mã xác thực OTP của bạn là:</p>
                        <div style="text-align: center; margin: 24px 0;">
                            <span style="background: #FFF3E0; color: #FF6F00; font-size: 32px; font-weight: bold; padding: 12px 24px; display: inline-block; border-radius: 10px; letter-spacing: 6px;">$otpCode</span>
                        </div>
                        <p>Mã này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai để đảm bảo an toàn tài khoản.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                        <p style="font-size: 12px; color: #777;">Trân trọng,<br/><b>Đội ngũ Chăm sóc Khách hàng NinoRent</b><br/>Email: cskh@ninotekpos.com</p>
                    </div>
                    """.trimIndent(),
                    "text/html; charset=utf-8"
                )
            }

            Transport.send(message)
            println("EmailSender: Real OTP email successfully sent via SMTP to $toEmail")
            Result.success(true)
        } catch (e: Exception) {
            println("EmailSender: Failed to send email via SMTP, falling back to simulation: ${e.message}")
            Result.failure(e)
        }
    }
}
