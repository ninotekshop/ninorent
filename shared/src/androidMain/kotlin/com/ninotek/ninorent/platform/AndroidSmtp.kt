package com.ninotek.ninorent.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

actual suspend fun sendHtmlEmail(config: SmtpConfig, toEmail: String, subject: String, htmlBody: String) {
    withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            if (config.port == 465) {
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.socketFactory.fallback", "false")
            } else {
                put("mail.smtp.starttls.enable", "true")
            }
            put("mail.smtp.ssl.trust", config.host)
            put("mail.transport.protocol", "smtp")
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication =
                PasswordAuthentication(config.user, config.password)
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.fromEmail, config.fromName))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            this.subject = subject
            setContent(htmlBody, "text/html; charset=utf-8")
        }
        Transport.send(message)
    }
}
