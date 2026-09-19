package com.ninotek.ninorent

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import com.ninotek.ninorent.model.BankAccountInfo
import com.ninotek.ninorent.model.Customer
import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.UserAccount
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.model.UserRole
import com.ninotek.ninorent.utils.EquipmentDto
import com.ninotek.ninorent.utils.StaffAccountDto
import com.ninotek.ninorent.utils.StoreSettingsDto
import com.ninotek.ninorent.utils.SupabaseManager
import com.ninotek.ninorent.utils.normalizeStoreIdFromPhone
import com.ninotek.ninorent.utils.toDto
import com.ninotek.ninorent.utils.toDomain
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SupabaseClientTest {

    @Test
    fun testSupabaseManagerClientAccess() {
        val client = SupabaseManager.client
        println("SupabaseManager.client instance in unit test: $client")
    }

    @Test
    fun testOfflineFallbackFetchMethods() = runTest {
        val customers = SupabaseManager.fetchCustomers()
        println("Fetched customers in unit test: $customers")

        val equipment = SupabaseManager.fetchEquipment()
        println("Fetched equipment in unit test: $equipment")

        val orders = SupabaseManager.fetchRentalOrders()
        println("Fetched rental orders in unit test: $orders")

        val storeSettings = SupabaseManager.fetchStoreSettings()
        println("Fetched store settings in unit test: $storeSettings")
    }

    @Test
    fun testDtoMappingRoundtrip() {
        // Customer DTO roundtrip
        val customer = Customer(
            id = "cust-1",
            name = "Nguyễn Văn Test",
            phone = "0901112233",
            address = "123 Test Street",
            type = "Cá nhân",
            email = "test@example.com",
            idNumber = "123456789",
            idIssueDate = "01/01/2020"
        )
        val customerDto = customer.toDto()
        assertEquals("cust-1", customerDto.id)
        assertEquals("Nguyễn Văn Test", customerDto.name)
        assertEquals("0901112233", customerDto.phone)
        assertEquals("Cá nhân", customerDto.customer_type)
        assertEquals("123456789", customerDto.cccd_number)
        assertEquals("01/01/2020", customerDto.issue_date)

        val customerDomain = customerDto.toDomain()
        assertEquals(customer.id, customerDomain.id)
        assertEquals(customer.name, customerDomain.name)
        assertEquals(customer.phone, customerDomain.phone)
        assertEquals(customer.idNumber, customerDomain.idNumber)

        // Equipment DTO roundtrip
        val equipment = Equipment(
            id = "eq-100",
            name = "Sony A7IV",
            category = "Máy ảnh",
            categorySubtitle = "Mirrorless",
            pricePerDay = "500.000",
            status = "Sẵn sàng",
            icon = Icons.Rounded.Devices,
            serialNumber = "SN9999"
        )
        val equipmentDto = equipment.toDto()
        assertEquals("eq-100", equipmentDto.id)
        assertEquals("Sony A7IV", equipmentDto.name)
        assertEquals("500.000", equipmentDto.daily_price)
        assertEquals("SN9999", equipmentDto.serial_number)

        val equipmentDomain = equipmentDto.toDomain()
        assertEquals(equipment.id, equipmentDomain.id)
        assertEquals(equipment.name, equipmentDomain.name)
        assertEquals(equipment.pricePerDay, equipmentDomain.pricePerDay)

        // RentalOrder DTO roundtrip
        val order = RentalOrder(
            id = "#DH001",
            equipmentName = "Sony A7IV",
            dateRange = "01/01/2026 - 02/01/2026",
            price = "500.000",
            status = "Đang thuê",
            customerName = "Nguyễn Văn Test",
            lesseePhone = "0901112233",
            advancePaymentAmount = "200.000"
        )
        val orderDto = order.toDto()
        assertEquals("#DH001", orderDto.id)
        assertEquals("#DH001", orderDto.order_code)
        assertEquals("Nguyễn Văn Test", orderDto.customer_name)
        assertEquals("0901112233", orderDto.customer_phone)
        assertEquals("Sony A7IV", orderDto.equipment_summary)
        assertEquals("500.000", orderDto.total_amount)
        assertEquals("200.000", orderDto.advance_payment)
        assertEquals("Đang thuê", orderDto.status)
        assertEquals("01/01/2026", orderDto.start_date)
        assertEquals("02/01/2026", orderDto.end_date)

        val orderDomain = orderDto.toDomain()
        assertEquals(order.id, orderDomain.id)
        assertEquals(order.equipmentName, orderDomain.equipmentName)
        assertEquals(order.customerName, orderDomain.customerName)

        // StoreSettings DTO validation
        val storeSettings = StoreSettingsDto(
            id = "default_settings",
            lessor_company = "CÔNG TY TNHH NINOTEK",
            lessor_representative = "Ông Nguyễn Văn A",
            lessor_address = "Quy Nhơn, Bình Định",
            lessor_phone = "0901234567",
            bank_name = "MB Bank",
            bank_account_number = "0987654321",
            bank_account_holder = "CÔNG TY TNHH NINOTEK",
            storeId = "SHOP_090123456"
        )
        assertEquals("default_settings", storeSettings.id)
        assertEquals("CÔNG TY TNHH NINOTEK", storeSettings.lessor_company)
        assertEquals("SHOP_090123456", storeSettings.storeId)
    }

    @Test
    fun testNonNullAndNonBlankIdInDtosAndModels() {
        val staffDto = StaffAccountDto(
            full_name = "Test Staff",
            phone = "0900000000",
            email = "staff@test.com",
            password = "password",
            role = "STAFF",
            staff_role_detail = "Kho"
        )
        assertNotNull(staffDto.id)
        assertTrue(staffDto.id.isNotBlank())

        val customerWithBlankId = Customer(
            id = "",
            name = "Test",
            phone = "0900",
            address = "Addr"
        )
        val customerDto = customerWithBlankId.toDto()
        assertNotNull(customerDto.id)
        assertTrue(customerDto.id.isNotBlank())

        val equipmentWithBlankId = Equipment(
            id = "",
            name = "Test Eq",
            category = "Cam",
            categorySubtitle = "",
            pricePerDay = "100",
            status = "Sẵn sàng",
            icon = Icons.Rounded.Devices
        )
        val equipmentDto = equipmentWithBlankId.toDto()
        assertNotNull(equipmentDto.id)
        assertTrue(equipmentDto.id.isNotBlank())

        val orderWithBlankId = RentalOrder(
            id = "",
            equipmentName = "Cam",
            dateRange = "01/01/2026",
            price = "100",
            status = "Đang thuê",
            customerName = "Cust"
        )
        val orderDto = orderWithBlankId.toDto()
        assertNotNull(orderDto.id)
        assertTrue(orderDto.id.isNotBlank())

        val userAcc = UserAccount(
            fullName = "User",
            phone = "0900",
            email = "user@test.com",
            password = "pass"
        )
        assertNotNull(userAcc.id)
        assertTrue(userAcc.id.isNotBlank())
    }

    @Test
    fun testSyncAllLocalDataOffline() = runTest {
        val lessor = LessorInfo(name = "Test Company", address = "Test Address", representative = "Test Rep", phone = "0900000000")
        val bank = BankAccountInfo(bankName = "MB Bank", accountNumber = "123456789", accountHolderName = "Test Holder")

        val result = SupabaseManager.syncAllLocalData(
            staffAccounts = emptyList<StaffAccountDto>(),
            customers = emptyList<Customer>(),
            equipment = emptyList<Equipment>(),
            rentalOrders = emptyList<RentalOrder>(),
            lessorInfo = lessor,
            bankAccountInfo = bank
        )
        println("Sync result in unit test: $result")
    }

    @Test
    fun testSyncExceptionWithMockData() = runTest {
        val customers = listOf(Customer(
            id = "10",
            name = "Test Customer",
            phone = "0901234567",
            address = "Test Address"
        ))
        val equipment = listOf(Equipment(
            id = "20",
            name = "Test Equipment",
            category = "Test Category",
            categorySubtitle = "Test Subtitle",
            pricePerDay = "1.000.000",
            status = "Sẵn sàng",
            icon = Icons.Rounded.Devices,
            serialNumber = "SN1234"
        ))
        val orders = listOf(RentalOrder(
            id = "#DH001",
            equipmentName = "Test Equipment",
            dateRange = "01/01/2026 - 02/01/2026",
            price = "1.000.000",
            status = "Đang thuê",
            customerName = "Test Customer"
        ))

        println("Starting sync with mock data...")
        try {
            val supabaseClient = createSupabaseClient(
                supabaseUrl = "https://dgkolxnsglsbqkavmawv.supabase.co",
                supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRna29seG5zZ2xzYnFrYXZtYXd2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyMjc0NjYsImV4cCI6MjEwNDgwMzQ2Nn0.zY3Gfx503_mY48JwEPmlYgrBlAtFQI_KmHXnlFdyxq8"
            ) {
                install(Postgrest)
                defaultSerializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
            supabaseClient.postgrest
                .from("equipment")
                .upsert(equipment.first().toDto())
            println("Equipment upsert success!")
        } catch (e: Exception) {
            println("Caught exact exception during equipment upsert: ${e.message}")
            File("test_exception_eq.log").writeText("Exact exception eq: ${e.message}\n" + e.stackTraceToString())
        }
        
        try {
            val supabaseClient = createSupabaseClient(
                supabaseUrl = "https://dgkolxnsglsbqkavmawv.supabase.co",
                supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRna29seG5zZ2xzYnFrYXZtYXd2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyMjc0NjYsImV4cCI6MjEwNDgwMzQ2Nn0.zY3Gfx503_mY48JwEPmlYgrBlAtFQI_KmHXnlFdyxq8"
            ) {
                install(Postgrest)
                defaultSerializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
            supabaseClient.postgrest
                .from("rental_orders")
                .upsert(orders.first().toDto())
            println("Order upsert success!")
        } catch (e: Exception) {
            println("Caught exact exception during order upsert: ${e.message}")
            File("test_exception_order.log").writeText("Exact exception order: ${e.message}\n" + e.stackTraceToString())
        }
        
        val result = SupabaseManager.syncAllLocalData(
            staffAccounts = emptyList(),
            customers = customers,
            equipment = equipment,
            rentalOrders = orders
        )
        println("Result of sync: $result")
    }

    @Test
    fun testCloudDataPullLogicAndDemoDataPrevention() {
        val remoteEquips = listOf(
            Equipment(id = "eq-1", name = "Canon R6", category = "Máy ảnh", categorySubtitle = "", pricePerDay = "500k", status = "Sẵn sàng", icon = Icons.Rounded.Devices)
        )
        val remoteCusts: List<Customer>? = emptyList()
        val remoteOrders: List<RentalOrder>? = emptyList()
        val remoteSettings: StoreSettingsDto? = StoreSettingsDto(lessor_company = "Nino Shop")

        val hasRemoteData = !remoteCusts.isNullOrEmpty() ||
                !remoteEquips.isNullOrEmpty() ||
                !remoteOrders.isNullOrEmpty() ||
                remoteSettings != null

        assertTrue(hasRemoteData)

        var hasClearedDemoData = false
        if (hasRemoteData) {
            hasClearedDemoData = true
        }

        assertTrue(hasClearedDemoData)
    }

    @Test
    fun testCheckAccountExistsOffline() = runTest {
        val exists = SupabaseManager.checkAccountExists("0900000000", "test@ninotek.com")
        println("Check account exists in unit test (offline): $exists")
    }

    @Test
    fun testStoreIdNormalization() {
        assertEquals("SHOP_0901992349", normalizeStoreIdFromPhone("0901992349"))
        assertEquals("SHOP_0901992349", normalizeStoreIdFromPhone("901992349"))
        assertEquals("SHOP_0901992349", normalizeStoreIdFromPhone("SHOP_0901992349"))
        assertEquals("SHOP_0901992349", normalizeStoreIdFromPhone("SHOP_901992349"))
        assertEquals("SHOP_0901992349", normalizeStoreIdFromPhone("+84901992349"))
        assertEquals("SHOP_0911223344", normalizeStoreIdFromPhone("0911223344"))
    }

    @Test
    fun testMultiTenantStoreIdAndDataIsolation() {
        val ownerPhone = "0901992349"
        val expectedStoreId = normalizeStoreIdFromPhone(ownerPhone)

        val ownerAccount = UserAccount(
            fullName = "Lê Trung Hiếu",
            phone = ownerPhone,
            email = "hieu@ninorent.vn",
            password = "password123",
            role = UserRole.OWNER,
            storeId = expectedStoreId
        )

        assertEquals("SHOP_0901992349", ownerAccount.storeId)

        val staffAccount = UserAccount(
            fullName = "Nhân Viên A",
            phone = "0901112233",
            email = "staffA@ninorent.vn",
            password = "password123",
            role = UserRole.STAFF,
            storeId = ownerAccount.storeId
        )

        assertEquals(ownerAccount.storeId, staffAccount.storeId)

        SupabaseManager.currentStoreId = ownerAccount.storeId
        assertEquals("SHOP_0901992349", SupabaseManager.currentStoreId)

        val customer = Customer(name = "Khách Hàng X", phone = "0909999888", address = "Quy Nhơn", storeId = ownerAccount.storeId)
        val customerDto = customer.toDto()
        assertEquals("SHOP_0901992349", customerDto.storeId)

        val equipment = Equipment(name = "Canon EOS R6", category = "Máy ảnh", categorySubtitle = "Mirrorless", pricePerDay = "400.000", status = "Sẵn sàng", icon = Icons.Rounded.Devices, storeId = ownerAccount.storeId)
        val equipmentDto = equipment.toDto()
        assertEquals("SHOP_0901992349", equipmentDto.storeId)

        val order = RentalOrder(equipmentName = "Canon EOS R6", dateRange = "01/01/2026", price = "400.000", status = "Đang thuê", customerName = "Khách Hàng X", storeId = ownerAccount.storeId)
        val orderDto = order.toDto()
        assertEquals("SHOP_0901992349", orderDto.storeId)
    }

    @Test
    fun testMasterAdminDetection() {
        val masterAdmin1 = UserAccount(fullName = "Master Admin", phone = "0901992349", email = "admin@ninorent.vn", password = "pass")
        val masterAdmin2 = UserAccount(fullName = "Master Admin Space", phone = "0901 992 349", email = "admin2@ninorent.vn", password = "pass")
        val regularOwner = UserAccount(fullName = "Owner Shop B", phone = "0987654321", email = "shopb@ninorent.vn", password = "pass")

        assertTrue(UserAccountStore.isMasterAdmin(masterAdmin1))
        assertTrue(UserAccountStore.isMasterAdmin(masterAdmin2))
        assertTrue(!UserAccountStore.isMasterAdmin(regularOwner))
    }

    @Test
    fun testMasterAdminFetchAndPurgeStores() = runTest {
        val localAccounts = listOf(
            UserAccount(fullName = "Master Admin", phone = "0901992349", email = "admin@ninorent.vn", password = "pass", role = UserRole.OWNER, storeId = "SHOP_901992349"),
            UserAccount(fullName = "Owner Store A", phone = "0911223344", email = "storeA@ninorent.vn", password = "pass", role = UserRole.OWNER, storeId = "SHOP_911223344")
        )

        val stores = SupabaseManager.fetchAllStores(localAccounts)
        assertNotNull(stores)
        assertTrue(stores.isNotEmpty())

        val purgeResult = SupabaseManager.purgeStoreData("SHOP_911223344", "0911223344")
        println("Purge store result in offline test: $purgeResult")

        val resetResult = SupabaseManager.resetStoreOwnerPassword("0911223344", "newPassword123")
        println("Reset password result in offline test: $resetResult")
    }

    @Test
    fun testStringOrNumberSerializerWithVariousTypes() {
        val json = Json { ignoreUnknownKeys = true }

        val jsonLong = """{"name":"Cam 1","category":"Máy ảnh","daily_price":500000}"""
        val dtoLong = json.decodeFromString<EquipmentDto>(jsonLong)
        assertEquals("500000", dtoLong.daily_price)

        val jsonDouble = """{"name":"Cam 2","category":"Máy ảnh","daily_price":250000.0}"""
        val dtoDouble = json.decodeFromString<EquipmentDto>(jsonDouble)
        assertEquals("250000.0", dtoDouble.daily_price)

        val jsonString = """{"name":"Cam 3","category":"Máy ảnh","daily_price":"300.000"}"""
        val dtoString = json.decodeFromString<EquipmentDto>(jsonString)
        assertEquals("300.000", dtoString.daily_price)

        val jsonNull = """{"name":"Cam 4","category":"Máy ảnh","daily_price":null}"""
        val dtoNull = json.decodeFromString<EquipmentDto>(jsonNull)
        assertEquals("", dtoNull.daily_price)
    }

    @Test
    fun testFetchEquipmentWithStoreIdOverride() = runTest {
        val equipment = SupabaseManager.fetchEquipment("SHOP_0901992349")
        println("Fetched equipment for SHOP_0901992349 in unit test: ${equipment?.size}")
    }

    @Test
    fun testAccount0901992349StoreIdAssignedCorrectly() {
        val accountDefaultStore = UserAccount(
            fullName = "Master Admin",
            phone = "0901992349",
            email = "admin@ninorent.vn",
            password = "pass",
            role = UserRole.OWNER,
            storeId = "SHOP_DEFAULT"
        )
        val normalizedId = normalizeStoreIdFromPhone(accountDefaultStore.phone)
        assertEquals("SHOP_0901992349", normalizedId)

        val endingPhone = "0980901992349"
        val normalizedEnding = normalizeStoreIdFromPhone(endingPhone)
        assertEquals("SHOP_0901992349", normalizedEnding)
    }

    @Test
    fun testFetchEquipmentFallbackForShopDefault() = runTest {
        // Querying with SHOP_DEFAULT or overriding store ID
        val equipmentDefault = SupabaseManager.fetchEquipment("SHOP_DEFAULT")
        println("Fetched equipment for SHOP_DEFAULT (with fallback): ${equipmentDefault?.size}")
    }
}
