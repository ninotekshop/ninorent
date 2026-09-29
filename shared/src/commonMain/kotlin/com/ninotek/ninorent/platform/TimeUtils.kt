@file:OptIn(ExperimentalTime::class)

package com.ninotek.ninorent.platform

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

private fun Int.pad2() = toString().padStart(2, '0')

private fun Long.toLocal(): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())

/** dd/MM/yyyy theo múi giờ hiện tại. */
fun formatDate(epochMillis: Long): String {
    val t = epochMillis.toLocal()
    return "${t.dayOfMonth.pad2()}/${t.monthNumber.pad2()}/${t.year.toString().padStart(4, '0')}"
}

/** HH:mm theo múi giờ hiện tại. */
fun formatTime(epochMillis: Long): String {
    val t = epochMillis.toLocal()
    return "${t.hour.pad2()}:${t.minute.pad2()}"
}

/** Cộng thêm số phút vào một mốc thời gian. */
fun addMinutes(epochMillis: Long, minutes: Long): Long = epochMillis + minutes * 60_000L

private val timeThenDate = Regex("""^(\d{1,2}):(\d{2})\s+(\d{1,2})/(\d{1,2})/(\d{4})$""")
private val dateThenTime = Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2})$""")
private val dateOnly = Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})$""")

private fun toMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long? = try {
    LocalDateTime(year, month, day, hour, minute, second)
        .toInstant(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
} catch (_: Exception) {
    null
}

/** Phân tích "HH:mm dd/MM/yyyy". */
fun parseTimeThenDate(text: String): Long? {
    val m = timeThenDate.find(text.trim()) ?: return null
    val (h, min, d, mo, y) = m.destructured
    return toMillis(y.toInt(), mo.toInt(), d.toInt(), h.toInt(), min.toInt())
}

/** Phân tích "dd/MM/yyyy HH:mm". */
fun parseDateThenTime(text: String): Long? {
    val m = dateThenTime.find(text.trim()) ?: return null
    val (d, mo, y, h, min) = m.destructured
    return toMillis(y.toInt(), mo.toInt(), d.toInt(), h.toInt(), min.toInt())
}

/** Phân tích "dd/MM/yyyy" và trả về 23:59:59 của ngày đó. */
fun parseDateAtEndOfDay(text: String): Long? {
    val m = dateOnly.find(text.trim()) ?: return null
    val (d, mo, y) = m.destructured
    return toMillis(y.toInt(), mo.toInt(), d.toInt(), 23, 59, 59)
}
