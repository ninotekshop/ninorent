package com.ninotek.ninorent

import com.ninotek.ninorent.utils.AuthValidation
import com.ninotek.ninorent.utils.EmailSender
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUnitTest {

    @Test
    fun isValidEmail_validatesCorrectly() {
        assertTrue(AuthValidation.isValidEmail("user@example.com"))
        assertTrue(AuthValidation.isValidEmail("admin.ninorent@ninotek.vn"))
        assertFalse(AuthValidation.isValidEmail("invalid-email"))
        assertFalse(AuthValidation.isValidEmail("user@"))
        assertFalse(AuthValidation.isValidEmail("@domain.com"))
    }

    @Test
    fun isValidPhone_validatesCorrectly() {
        assertTrue(AuthValidation.isValidPhone("0901234567"))
        assertTrue(AuthValidation.isValidPhone("091-234-5678"))
        assertTrue(AuthValidation.isValidPhone("098 765 4321"))
        assertFalse(AuthValidation.isValidPhone("1234567")) // Too short
        assertFalse(AuthValidation.isValidPhone("abc0901234")) // Non-digits
    }

    @Test
    fun isValidPassword_validatesMinLength() {
        assertTrue(AuthValidation.isValidPassword("123456"))
        assertTrue(AuthValidation.isValidPassword("password123"))
        assertFalse(AuthValidation.isValidPassword("12345"))
        assertFalse(AuthValidation.isValidPassword(""))
    }

    @Test
    fun isPasswordMatching_validatesMatchingPasswords() {
        assertTrue(AuthValidation.isPasswordMatching("secret123", "secret123"))
        assertFalse(AuthValidation.isPasswordMatching("secret123", "secret456"))
        assertFalse(AuthValidation.isPasswordMatching("", ""))
    }

    @Test
    fun isValidOtp_validatesFourDigits() {
        assertTrue(AuthValidation.isValidOtp("1234"))
        assertTrue(AuthValidation.isValidOtp("0000"))
        assertFalse(AuthValidation.isValidOtp("123"))
        assertFalse(AuthValidation.isValidOtp("12345"))
        assertFalse(AuthValidation.isValidOtp("abcd"))
    }

    @Test
    fun emailSender_simulatesWhenCredentialsBlank() = runTest {
        EmailSender.smtpPass = ""
        val result = EmailSender.sendOtp("test@example.com", "1234")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }
}
