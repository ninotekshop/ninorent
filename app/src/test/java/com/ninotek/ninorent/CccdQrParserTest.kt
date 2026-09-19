package com.ninotek.ninorent

import com.ninotek.ninorent.utils.parseCccdQrPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CccdQrParserTest {

    @Test
    fun parseCccdQrPayload_valid7FieldPayload_returnsCccdDataWithFormattedDates() {
        val rawPayload = "052095001234|123456789|Nguyễn Văn Nam|15051995|Nam|123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định|15052021"
        val cccdData = parseCccdQrPayload(rawPayload)

        assertNotNull(cccdData)
        assertEquals("052095001234", cccdData?.cccdNumber)
        assertEquals("123456789", cccdData?.oldCmndNumber)
        assertEquals("Nguyễn Văn Nam", cccdData?.fullName)
        assertEquals("15/05/1995", cccdData?.dateOfBirth)
        assertEquals("Nam", cccdData?.gender)
        assertEquals("123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định", cccdData?.address)
        assertEquals("15/05/2021", cccdData?.issueDate)
    }

    @Test
    fun parseCccdQrPayload_validFemalePayload_returnsCorrectData() {
        val rawPayload = "052200001234|987654321|Trần Thị Mai|20101998|Nữ|456 Trần Hưng Đạo, TP. Quy Nhơn, Bình Định|20102022"
        val cccdData = parseCccdQrPayload(rawPayload)

        assertNotNull(cccdData)
        assertEquals("052200001234", cccdData?.cccdNumber)
        assertEquals("987654321", cccdData?.oldCmndNumber)
        assertEquals("Trần Thị Mai", cccdData?.fullName)
        assertEquals("20/10/1998", cccdData?.dateOfBirth)
        assertEquals("Nữ", cccdData?.gender)
        assertEquals("456 Trần Hưng Đạo, TP. Quy Nhơn, Bình Định", cccdData?.address)
        assertEquals("20/10/2022", cccdData?.issueDate)
    }

    @Test
    fun parseCccdQrPayload_invalidFieldCount_returnsNull() {
        val invalidPayload = "052095001234|123456789|Nguyễn Văn Nam|15051995"
        val cccdData = parseCccdQrPayload(invalidPayload)

        assertNull(cccdData)
    }

    @Test
    fun parseCccdQrPayload_emptyString_returnsNull() {
        assertNull(parseCccdQrPayload(""))
    }
}
