package com.ninotek.ninorent.utils

import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.platform.currentTimeMillis
import com.ninotek.ninorent.platform.parseDateAtEndOfDay
import com.ninotek.ninorent.platform.parseDateThenTime
import com.ninotek.ninorent.platform.parseTimeThenDate

/** Định dạng số nguyên với dấu chấm ngăn cách hàng nghìn (kiểu vi-VN): 1234567 -> "1.234.567". */
fun formatGroupedNumber(amount: Long): String {
    val negative = amount < 0
    val digits = if (amount == Long.MIN_VALUE) amount.toString().removePrefix("-") else kotlin.math.abs(amount).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return if (negative) "-$grouped" else grouped
}

fun formatCurrencyAmount(amount: Long): String = formatGroupedNumber(amount)

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

/** Thời điểm kết thúc của đơn thuê (epoch millis), hoặc null nếu không phân tích được. */
fun parseEndDateTimeFromOrder(order: RentalOrder): Long? {
    val range = order.dateRange
    if (range.isBlank()) return null

    val parts = range.split("-").map { it.trim() }
    val endPart = parts.getOrNull(1) ?: parts.getOrNull(0) ?: return null
    val cleanEnd = endPart.substringBefore("(").trim()

    return parseTimeThenDate(cleanEnd)
        ?: parseDateThenTime(cleanEnd)
        ?: parseDateAtEndOfDay(cleanEnd)
}

fun isOrderOverdue24h(order: RentalOrder, nowMillis: Long = currentTimeMillis()): Boolean {
    if (order.status != "Đang thuê") return false
    val endDateTime = parseEndDateTimeFromOrder(order) ?: return false
    return nowMillis > endDateTime
}

fun updateOrdersOverdueStatus24h(orders: List<RentalOrder>): List<RentalOrder> {
    val now = currentTimeMillis()
    return orders.map { order ->
        if (order.status == "Đang thuê" && isOrderOverdue24h(order, now)) {
            order.copy(status = "Quá hạn")
        } else {
            order
        }
    }
}
