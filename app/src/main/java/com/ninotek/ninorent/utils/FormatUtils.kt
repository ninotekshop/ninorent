package com.ninotek.ninorent.utils

import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.RentalOrder
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

fun parseEndDateTimeFromOrder(order: RentalOrder): Date? {
    val range = order.dateRange
    if (range.isBlank()) return null

    val sdfFull = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
    val sdfFullAlt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val sdfDateOnly = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val parts = range.split("-").map { it.trim() }
    val endPart = parts.getOrNull(1) ?: parts.getOrNull(0) ?: return null
    val cleanEnd = endPart.substringBefore("(").trim()

    try {
        val date = sdfFull.parse(cleanEnd)
        if (date != null) return date
    } catch (_: Exception) {}

    try {
        val date = sdfFullAlt.parse(cleanEnd)
        if (date != null) return date
    } catch (_: Exception) {}

    try {
        val dateOnly = sdfDateOnly.parse(cleanEnd)
        if (dateOnly != null) {
            val cal = Calendar.getInstance()
            cal.time = dateOnly
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            return cal.time
        }
    } catch (_: Exception) {}

    return null
}

fun isOrderOverdue24h(order: RentalOrder, now: Date = Date()): Boolean {
    if (order.status != "Đang thuê") return false
    val endDateTime = parseEndDateTimeFromOrder(order) ?: return false
    return now.after(endDateTime)
}

fun updateOrdersOverdueStatus24h(orders: List<RentalOrder>): List<RentalOrder> {
    val now = Date()
    return orders.map { order ->
        if (order.status == "Đang thuê" && isOrderOverdue24h(order, now)) {
            order.copy(status = "Quá hạn")
        } else {
            order
        }
    }
}
