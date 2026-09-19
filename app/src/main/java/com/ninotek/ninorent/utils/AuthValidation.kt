package com.ninotek.ninorent.utils

object AuthValidation {
    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        return trimmed.matches(emailRegex.toRegex())
    }

    fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        val phoneRegex = "^0[0-9]{9,10}\$"
        return cleanPhone.matches(phoneRegex.toRegex())
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isPasswordMatching(password: String, confirm: String): Boolean {
        return password == confirm && password.isNotEmpty()
    }

    fun isValidOtp(otp: String): Boolean {
        return otp.length == 4 && otp.all { it.isDigit() }
    }
}
