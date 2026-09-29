import com.ninotek.ninorent.model.CartItem
import com.ninotek.ninorent.model.CreateRentalOrderState
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.PaperSize
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.defaultBankAccountInfo
import com.ninotek.ninorent.model.defaultDevicesList
import com.ninotek.ninorent.model.defaultLessorInfo
import com.ninotek.ninorent.model.parseCccdQrCode
import com.ninotek.ninorent.ui.dashboard.defaultCustomersList
import com.ninotek.ninorent.ui.dashboard.defaultOrdersList
import com.ninotek.ninorent.utils.formatCurrencyAmount
import com.ninotek.ninorent.utils.formatDailyPrice
import com.ninotek.ninorent.utils.formatRentalDuration
import com.ninotek.ninorent.utils.formatVietnameseContractDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractUnitTest {

    @Test
    fun paperSizeEnum_hasCorrectLabels() {
        assertEquals("Khổ A4", PaperSize.A4.label)
        assertEquals("Khổ A5", PaperSize.A5.label)
    }

    @Test
    fun defaultLessorInfo_hasDefaultValues() {
        val info = defaultLessorInfo
        assertEquals("", info.name)
        assertEquals("", info.representative)
        assertEquals("", info.address)
        assertEquals("", info.phone)
    }

    @Test
    fun defaultBankAccountInfo_hasDefaultValues() {
        val bankInfo = defaultBankAccountInfo
        assertEquals("", bankInfo.bankName)
        assertEquals("", bankInfo.accountNumber)
        assertEquals("", bankInfo.accountHolderName)
    }

    @Test
    fun rentalOrder_hasSerialNumberAndDiscount() {
        val order = RentalOrder(
            id = "#DH007",
            equipmentName = "Sony Alpha A7C",
            dateRange = "10/09/2026 - 12/09/2026 (3 ngày)",
            price = "6.000.000₫",
            status = "Đang thuê",
            customerName = "Nguyễn Văn Nam",
            serialNumber = "SN-2026-NINO88",
            discountAmount = "500.000₫",
            netTotal = "5.500.000₫",
            bankName = "MB Bank",
            bankAccountNumber = "0987654321",
            bankAccountHolder = "Công ty TNHH Công nghệ & Dịch vụ NINOTEK"
        )

        assertEquals("SN-2026-NINO88", order.serialNumber)
        assertEquals("500.000₫", order.discountAmount)
        assertEquals("5.500.000₫", order.netTotal)
        assertEquals("MB Bank", order.bankName)
    }

    @Test
    fun customLessorInfo_canBeUpdated() {
        val customInfo = LessorInfo(
            name = "CÔNG TY THIẾT BỊ NINO",
            address = "456 Lê Hồng Phong, Quy Nhơn",
            representative = "Bà Trần Thị B",
            phone = "0988888888"
        )
        assertEquals("CÔNG TY THIẾT BỊ NINO", customInfo.name)
        assertEquals("Bà Trần Thị B", customInfo.representative)
    }

    @Test
    fun rentalOrder_hasContractDetails() {
        val order = RentalOrder(
            id = "#DH001",
            equipmentName = "Sony Alpha A7C",
            dateRange = "10/09/2026 - 12/09/2026 (3 ngày)",
            price = "6.000.000₫",
            status = "Đang thuê",
            customerName = "Nguyễn Văn Nam",
            lessorName = "Công ty TNHH Công nghệ & Dịch vụ NINOTEK",
            lessorAddress = "125/2 Hai Bà Trưng, Phường Quy Nhơn, Tỉnh Gia Lai",
            lessorRepresentative = "Ông Lê Trung Hiếu - Giám Đốc",
            lessorPhone = "0901992349",
            lesseeName = "Nguyễn Văn Nam",
            lesseeAddress = "123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định",
            lesseePhone = "090 123 4567",
            lesseeIdNumber = "012345678901",
            lesseeIdIssueDate = "01/01/2021",
            advancePaymentAmount = "2.000.000₫",
            collateralCccd = true,
            collateralAssetDescription = "Xe máy Honda Vision BKS 77F1-123.45",
            collateralCashAmount = "5.000.000₫"
        )

        assertEquals("#DH001", order.id)
        assertEquals("Công ty TNHH Công nghệ & Dịch vụ NINOTEK", order.lessorName)
        assertEquals("Nguyễn Văn Nam", order.lesseeName)
        assertEquals("123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định", order.lesseeAddress)
        assertTrue(order.collateralCccd)
        assertEquals("5.000.000₫", order.collateralCashAmount)
    }

    @Test
    fun parseCccdQrCode_parsesCorrectly() {
        val rawQr = "012345678901|123456789|Nguyễn Văn Nam|15051995|Nam|123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định|01012021"
        val parsed = parseCccdQrCode(rawQr)

        assertNotNull(parsed)
        assertEquals("012345678901", parsed?.idNumber)
        assertEquals("Nguyễn Văn Nam", parsed?.name)
        assertEquals("123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định", parsed?.address)
        assertEquals("01/01/2021", parsed?.issueDate)
    }

    @Test
    fun parseCccdQrCode_parsesPresetFallbackStringCorrectly() {
        val rawPresetQr = "052095001234|123456789|Nguyễn Văn Nam|15051995|Nam|123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định|15052021"
        val parsed = parseCccdQrCode(rawPresetQr)

        assertNotNull(parsed)
        assertEquals("052095001234", parsed?.idNumber)
        assertEquals("Nguyễn Văn Nam", parsed?.name)
        assertEquals("123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định", parsed?.address)
        assertEquals("15/05/2021", parsed?.issueDate)
    }

    @Test
    fun formatCurrencyAmount_cleansTrailingCharacters() {
        assertEquals("5.000.000", formatCurrencyAmount("5.000.000đ"))
        assertEquals("5.000.000", formatCurrencyAmount("5.000.000đđ"))
        assertEquals("5.000.000", formatCurrencyAmount("5000000"))
        assertEquals("2.000.000", formatCurrencyAmount("2.000.000₫"))
    }

    @Test
    fun formatRentalDuration_formatsCorrectly() {
        assertEquals("10/09 - 12/09/2026 (3 ngày)", formatRentalDuration("10/09 - 12/09/2026 (3 ngày)"))
        assertEquals("10/09 - 12/09/2026 (3 ngày)", formatRentalDuration("10/09/2026 - 12/09/2026 (3 ngày)"))
    }

    @Test
    fun formatDailyPrice_calculatesCorrectly() {
        assertEquals("2.000.000", formatDailyPrice("6.000.000", "10/09/2026 - 12/09/2026 (3 ngày)"))
    }

    @Test
    fun formatVietnameseContractDate_formatsOfficialStandard() {
        assertEquals("Quy Nhơn, ngày 10 tháng 09 năm 2026", formatVietnameseContractDate("TP. Quy Nhơn, Bình Định", "10/09/2026"))
    }

    @Test
    fun createRentalOrderState_startsEmptyAndResetsCorrectly() {
        val state = CreateRentalOrderState()
        assertEquals(1, state.currentStep)
        assertEquals("", state.lesseeName)
        assertEquals("", state.lesseePhone)
        assertEquals("", state.lesseeAddress)
        assertEquals("", state.lesseeIdNumber)
        assertEquals("", state.lesseeIdIssueDate)
        assertEquals(false, state.discountTypeIsPercent)

        // Modify state
        state.currentStep = 3
        state.lesseeName = "Trần Thị Mai"
        state.lesseePhone = "0988776655"
        state.discountTypeIsPercent = true
        state.discountValueText = "10"

        // Reset state
        state.reset()
        assertEquals(1, state.currentStep)
        assertEquals("", state.lesseeName)
        assertEquals("", state.lesseePhone)
        assertEquals(false, state.discountTypeIsPercent)
    }

    @Test
    fun clearedDemoData_flagCheck_evaluatesCorrectly() {
        val hasClearedDemoData = true
        val devicesList = if (hasClearedDemoData) emptyList() else defaultDevicesList
        val ordersList = if (hasClearedDemoData) emptyList() else defaultOrdersList
        val customersList = if (hasClearedDemoData) emptyList() else defaultCustomersList

        assertTrue(devicesList.isEmpty())
        assertTrue(ordersList.isEmpty())
        assertTrue(customersList.isEmpty())
    }

    @Test
    fun multiEquipmentCart_sumsPricesAndCombinesSerialNumbers() {
        val state = CreateRentalOrderState()
        state.cartItems.clear()
        state.cartItems.add(CartItem(equipmentName = "Sony Alpha A7C", pricePerDay = "2000000", serialNumber = "SN001"))
        state.cartItems.add(CartItem(equipmentName = "Canon EOS R6", pricePerDay = "2500000", serialNumber = "SN002"))

        val dailyTotal = state.cartItems.sumOf { it.pricePerDay.toLong() }
        val namesCombined = state.cartItems.joinToString(" + ") { it.equipmentName }
        val serialsCombined = state.cartItems.joinToString(", ") { it.serialNumber }

        assertEquals(4500000L, dailyTotal)
        assertEquals("Sony Alpha A7C + Canon EOS R6", namesCombined)
        assertEquals("SN001, SN002", serialsCombined)
    }

    @Test
    fun discountCalculation_supportsAmountAndPercentage() {
        val grossTotal = 6000000L

        // VNĐ Discount
        val amountDiscount = 500000L
        val netVnd = (grossTotal - amountDiscount).coerceAtLeast(0L)
        assertEquals(5500000L, netVnd)

        // Percent Discount (10%)
        val percent = 10.0
        val rawPercentDiscount = (grossTotal * (percent / 100.0)).toLong()
        val netPercent = (grossTotal - rawPercentDiscount).coerceAtLeast(0L)
        assertEquals(600000L, rawPercentDiscount)
        assertEquals(5400000L, netPercent)
    }

    @Test
    fun defaultDevicesList_containsAllNinotekPriceListItems() {
        assertEquals(103, defaultDevicesList.size)
        assertTrue(defaultDevicesList.all { it.status == "Sẵn sàng" })
        assertTrue(defaultDevicesList.all { it.storeId == "SHOP_0901992349" })

        val sonyCount = defaultDevicesList.count { it.name.startsWith("Sony") && it.category == "Máy ảnh" }
        val fujiCount = defaultDevicesList.count { it.name.startsWith("Fujifilm") }
        val nikonCount = defaultDevicesList.count { it.name.startsWith("Nikon") }
        val canonCount = defaultDevicesList.count { it.name.startsWith("Canon") }
        val gameCount = defaultDevicesList.count { it.category == "Máy Game & Phụ kiện" }
        val videoCamCount = defaultDevicesList.count { it.category == "Máy quay" }
        val accessoryCount = defaultDevicesList.count { it.category == "Phụ kiện" }
        val filmCount = defaultDevicesList.count { it.categorySubtitle == "Máy ảnh Film" }

        assertEquals(20, sonyCount)
        assertEquals(12, fujiCount)
        assertEquals(17, nikonCount)
        assertEquals(37, canonCount)
        assertEquals(10, gameCount)
        assertEquals(4, videoCamCount)
        assertEquals(2, accessoryCount)
        assertEquals(1, filmCount)
    }
}
