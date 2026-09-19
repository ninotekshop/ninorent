package com.ninotek.ninorent.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun formatCurrencyAmount(amount: Long): String {
    val symbols = DecimalFormatSymbols(Locale.forLanguageTag("vi-VN")).apply {
        groupingSeparator = '.'
    }
    return DecimalFormat("#,###", symbols).format(amount)
}

fun formatCurrencyAmount(amount: Double): String {
    return formatCurrencyAmount(amount.toLong())
}

fun formatCurrencyAmount(amount: String?): String {
    if (amount.isNullOrBlank()) return "0"
    val cleaned = amount
        .replace("đ", "", ignoreCase = true)
        .replace("₫", "")
        .replace("VND", "", ignoreCase = true)
        .replace("VNĐ", "", ignoreCase = true)
        .replace(".", "")
        .replace(",", "")
        .trim()
    val digitsOnly = cleaned.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) return "0"
    return try {
        formatCurrencyAmount(digitsOnly.toLong())
    } catch (_: Exception) {
        digitsOnly
    }
}

fun parseCurrencyToLong(amount: String?): Long {
    if (amount.isNullOrBlank()) return 0L
    val cleaned = amount
        .replace("đ", "", ignoreCase = true)
        .replace("₫", "")
        .replace("VND", "", ignoreCase = true)
        .replace("VNĐ", "", ignoreCase = true)
        .replace(".", "")
        .replace(",", "")
        .trim()
    val digitsOnly = cleaned.filter { it.isDigit() }
    return digitsOnly.toLongOrNull() ?: 0L
}

fun formatCurrencyInput(input: String): String {
    val digitsOnly = input.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) return ""
    val longVal = digitsOnly.toLongOrNull() ?: return input
    return formatCurrencyAmount(longVal)
}
