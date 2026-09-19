package com.ninotek.ninorent

import com.ninotek.ninorent.utils.EmailSender
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailSenderTest {

    @Test
    fun testSimulatedEmailSending() = runTest {
        // Clear SMTP pass to ensure simulated mode is used and no real email is sent during tests
        EmailSender.smtpPass = ""

        val result = EmailSender.sendOtp("test@example.com", "5678")
        assertTrue("Email send result should be success", result.isSuccess)
        assertFalse("Simulated email should return false for real sending", result.getOrThrow())
    }
}
