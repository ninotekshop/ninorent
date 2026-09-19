package com.ninotek.ninorent.utils

import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.RentalOrder
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

fun reconcileEquipmentStatus(
    devices: List<Equipment>,
    orders: List<RentalOrder>
): List<Equipment> {
    val activeOrders = orders.filter { it.status == "Đang thuê" || it.status == "Quá hạn" }
    val rentedIdentifiers = mutableSetOf<String>()

    activeOrders.forEach { order ->
        val itemsList = getOrderEquipmentItems(order)
        itemsList.forEach { item ->
            val name = item.equipmentName.trim().lowercase()
            if (name.isNotBlank()) rentedIdentifiers.add(name)
        }
        val mainName = order.equipmentName.substringBefore("+").trim().lowercase()
        if (mainName.isNotBlank()) {
            rentedIdentifiers.add(mainName)
        }
    }

    return devices.map { device ->
        val deviceNameClean = device.name.trim().lowercase()
        val isDeviceRented = rentedIdentifiers.any { id ->
            deviceNameClean.contains(id) || id.contains(deviceNameClean)
        }

        if (!isDeviceRented && (device.status == "Đang thuê" || device.status == "Quá hạn")) {
            device.copy(status = "Sẵn sàng")
        } else if (isDeviceRented && device.status == "Sẵn sàng") {
            device.copy(status = "Đang thuê")
        } else {
            device
        }
    }
}
