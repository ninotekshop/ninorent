package com.ninotek.ninorent.utils

import android.util.Log
import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.ninotek.ninorent.model.BankAccountInfo
import com.ninotek.ninorent.model.CartItem
import com.ninotek.ninorent.model.Customer
import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.UserAccount
import com.ninotek.ninorent.model.UserRole
import com.ninotek.ninorent.model.defaultBankAccountInfo
import com.ninotek.ninorent.model.defaultLessorInfo
import com.ninotek.ninorent.model.parseUserRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.util.UUID

private fun logD(tag: String, msg: String) {
    try {
        Log.d(tag, msg)
    } catch (_: Throwable) {
        println("$tag: $msg")
    }
}

private fun logE(tag: String, msg: String, tr: Throwable? = null) {
    try {
        if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
    } catch (_: Throwable) {
        println("$tag: $msg ${tr?.message ?: ""}")
    }
}

fun normalizeStoreIdFromPhone(phoneOrStoreId: String): String {
    val trimmed = phoneOrStoreId.trim().replace(" ", "")
    if (trimmed.isBlank()) return ""

    if (trimmed == "0901992349" || trimmed.endsWith("0901992349")) {
        return "SHOP_0901992349"
    }

    if (trimmed.startsWith("SHOP_", ignoreCase = true)) {
        val suffix = trimmed.substring(5)
        if (suffix == "0901992349" || suffix.endsWith("0901992349")) {
            return "SHOP_0901992349"
        }
        if (suffix.startsWith("0")) {
            return "SHOP_" + suffix
        } else if (suffix.length == 9 && suffix.all { it.isDigit() }) {
            return "SHOP_0" + suffix
        }
        return "SHOP_" + suffix
    }

    val cleanPhone = trimmed.removePrefix("+84")
    return when {
        cleanPhone.startsWith("0") -> "SHOP_$cleanPhone"
        cleanPhone.length == 9 && cleanPhone.all { it.isDigit() } -> "SHOP_0$cleanPhone"
        else -> "SHOP_$cleanPhone"
    }
}

@Keep
@Serializable
data class StaffAccountDto(
    val id: String = UUID.randomUUID().toString(),
    val full_name: String,
    val phone: String,
    val email: String,
    val password: String,
    val role: String,
    val staff_role_detail: String,
    val biometric_enabled: Boolean = false,
    @SerialName("store_id") val storeId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Keep
@Serializable
data class CustomerDto(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val address: String,
    val customer_type: String = "Cá nhân",
    val cccd_number: String = "",
    val issue_date: String = "",
    val email: String = "",
    val id_card_front_photo_uri: String? = null,
    val id_card_back_photo_uri: String? = null,
    @SerialName("store_id") val storeId: String? = null
)

fun Customer.toDto() = CustomerDto(
    id = id.ifBlank { UUID.randomUUID().toString() },
    name = name,
    phone = phone,
    address = address,
    customer_type = type,
    cccd_number = idNumber,
    issue_date = idIssueDate,
    email = email,
    id_card_front_photo_uri = idCardFrontPhotoUri,
    id_card_back_photo_uri = idCardBackPhotoUri,
    storeId = if (storeId.isNotBlank()) storeId else SupabaseManager.currentStoreId
)

fun CustomerDto.toDomain() = Customer(
    id = id.ifBlank { UUID.randomUUID().toString() },
    name = name,
    phone = phone,
    address = address,
    type = customer_type,
    email = email,
    idNumber = cccd_number,
    idIssueDate = issue_date,
    idCardFrontPhotoUri = id_card_front_photo_uri,
    idCardBackPhotoUri = id_card_back_photo_uri,
    storeId = storeId ?: ""
)

object StringOrNumberSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringOrNumberSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                if (element is JsonNull || element.content == "null") return ""
                return element.content
            }
            return element.toString()
        }
        return decoder.decodeString()
    }
}

@Keep
@Serializable
data class EquipmentDto(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "",
    val category_subtitle: String? = "",
    @Serializable(with = StringOrNumberSerializer::class) val daily_price: String = "",
    val status: String? = "Sẵn sàng",
    val serial_number: String? = "",
    @SerialName("store_id") val storeId: String? = null
)

fun getIconForCategory(category: String): ImageVector {
    return when (category.lowercase()) {
        "máy ảnh" -> Icons.Rounded.CameraAlt
        "máy quay" -> Icons.Rounded.Videocam
        "điện thoại" -> Icons.Rounded.Smartphone
        "laptop" -> Icons.Rounded.Computer
        else -> Icons.Rounded.Devices
    }
}

fun Equipment.toDto() = EquipmentDto(
    id = id.ifBlank { UUID.randomUUID().toString() },
    name = name,
    category = category,
    category_subtitle = categorySubtitle,
    daily_price = pricePerDay,
    status = status,
    serial_number = serialNumber,
    storeId = if (storeId.isNotBlank()) storeId else SupabaseManager.currentStoreId
)

fun EquipmentDto.toDomain() = Equipment(
    id = id.ifBlank { UUID.randomUUID().toString() },
    name = name,
    category = category,
    categorySubtitle = category_subtitle ?: "",
    pricePerDay = daily_price,
    status = status ?: "Sẵn sàng",
    icon = getIconForCategory(category),
    serialNumber = serial_number ?: "",
    storeId = storeId ?: ""
)

@Keep
@Serializable
data class CartItemDto(
    val id: String = UUID.randomUUID().toString(),
    val equipment_name: String,
    val price_per_day: String,
    val serial_number: String = ""
)

fun CartItem.toDto() = CartItemDto(id.ifBlank { UUID.randomUUID().toString() }, equipmentName, pricePerDay, serialNumber)
fun CartItemDto.toDomain() = CartItem(id.ifBlank { UUID.randomUUID().toString() }, equipment_name, price_per_day, serial_number)

@Keep
@Serializable
data class RentalOrderContractJson(
    val serial_number: String = "",
    val discount_amount: String = "0",
    val price: String = "",
    val net_total: String = "",
    val equipment_items: List<CartItemDto> = emptyList(),
    val bank_name: String = "",
    val bank_account_number: String = "",
    val bank_account_holder: String = "",
    val lessor_name: String = "",
    val lessor_address: String = "",
    val lessor_representative: String = "",
    val lessor_phone: String = "",
    val lessee_name: String = "",
    val lessee_address: String = "",
    val lessee_phone: String = "",
    val lessee_id_number: String = "",
    val lessee_id_issue_date: String = "",
    val advance_payment_amount: String = "0",
    val collateral_cccd: Boolean = false,
    val collateral_gplx: Boolean = false,
    val collateral_asset_description: String = "",
    val collateral_cash_amount: String = "0",
    val id_card_front_photo_uri: String? = null,
    val id_card_back_photo_uri: String? = null,
    val contract_location: String = "Quy Nhơn",
    val contract_date: String = "",
    val date_range: String = ""
)

@Keep
@Serializable
data class RentalOrderDto(
    val id: String = UUID.randomUUID().toString(),
    val order_code: String = "",
    val customer_name: String = "",
    val customer_phone: String = "",
    val equipment_summary: String = "",
    val total_amount: String = "",
    val advance_payment: String = "0",
    val status: String = "Đang thuê",
    val start_date: String = "",
    val end_date: String = "",
    val contract_json: String = "{}",
    @SerialName("store_id") val storeId: String? = null
)

private val jsonFormat = Json { ignoreUnknownKeys = true }

fun RentalOrder.toDto(): RentalOrderDto {
    val dates = dateRange.split("-").map { it.trim() }
    val startDate = dates.getOrElse(0) { "" }
    val endDate = dates.getOrElse(1) { "" }

    val contractObj = RentalOrderContractJson(
        serial_number = serialNumber,
        discount_amount = discountAmount,
        price = price,
        net_total = netTotal,
        equipment_items = equipmentItems.map { it.toDto() },
        bank_name = bankName,
        bank_account_number = bankAccountNumber,
        bank_account_holder = bankAccountHolder,
        lessor_name = lessorName,
        lessor_address = lessorAddress,
        lessor_representative = lessorRepresentative,
        lessor_phone = lessorPhone,
        lessee_name = lesseeName,
        lessee_address = lesseeAddress,
        lessee_phone = lesseePhone,
        lessee_id_number = lesseeIdNumber,
        lessee_id_issue_date = lesseeIdIssueDate,
        advance_payment_amount = advancePaymentAmount,
        collateral_cccd = collateralCccd,
        collateral_gplx = collateralGplx,
        collateral_asset_description = collateralAssetDescription,
        collateral_cash_amount = collateralCashAmount,
        id_card_front_photo_uri = idCardFrontPhotoUri,
        id_card_back_photo_uri = idCardBackPhotoUri,
        contract_location = contractLocation,
        contract_date = contractDate,
        date_range = dateRange
    )
    val contractJsonStr = try {
        jsonFormat.encodeToString(contractObj)
    } catch (e: Exception) {
        "{}"
    }

    val totalAmt = netTotal.ifBlank { price }
    val custName = customerName.ifBlank { lesseeName }
    val custPhone = lesseePhone

    val validId = id.ifBlank { UUID.randomUUID().toString() }

    return RentalOrderDto(
        id = validId,
        order_code = validId,
        customer_name = custName,
        customer_phone = custPhone,
        equipment_summary = equipmentName,
        total_amount = totalAmt,
        advance_payment = advancePaymentAmount,
        status = status,
        start_date = startDate,
        end_date = endDate,
        contract_json = contractJsonStr,
        storeId = if (storeId.isNotBlank()) storeId else SupabaseManager.currentStoreId
    )
}

fun RentalOrderDto.toDomain(): RentalOrder {
    val contractObj = try {
        jsonFormat.decodeFromString<RentalOrderContractJson>(contract_json)
    } catch (e: Exception) {
        RentalOrderContractJson()
    }

    val itemsList = contractObj.equipment_items.map { it.toDomain() }
    val computedDateRange = when {
        contractObj.date_range.isNotBlank() -> contractObj.date_range
        start_date.isNotBlank() && end_date.isNotBlank() -> "$start_date - $end_date"
        else -> start_date
    }

    val validId = id.ifBlank { order_code.ifBlank { UUID.randomUUID().toString() } }

    return RentalOrder(
        id = validId,
        equipmentName = equipment_summary,
        dateRange = computedDateRange,
        price = contractObj.price.ifBlank { total_amount },
        status = status,
        customerName = customer_name.ifBlank { contractObj.lessee_name },
        serialNumber = contractObj.serial_number,
        discountAmount = contractObj.discount_amount,
        netTotal = total_amount.ifBlank { contractObj.net_total },
        equipmentItems = itemsList,
        bankName = contractObj.bank_name.ifBlank { "MB Bank" },
        bankAccountNumber = contractObj.bank_account_number.ifBlank { "0987654321" },
        bankAccountHolder = contractObj.bank_account_holder.ifBlank { "CÔNG TY TNHH NINOTEK" },
        lessorName = contractObj.lessor_name.ifBlank { "Công ty TNHH Công nghệ & Dịch vụ NINOTEK" },
        lessorAddress = contractObj.lessor_address.ifBlank { "125/2 Hai Bà Trưng, Phường Quy Nhơn, Tỉnh Gia Lai" },
        lessorRepresentative = contractObj.lessor_representative.ifBlank { "Ông Lê Trung Hiếu - Giám Đốc" },
        lessorPhone = contractObj.lessor_phone.ifBlank { "0901992349" },
        lesseeName = contractObj.lessee_name.ifBlank { customer_name },
        lesseeAddress = contractObj.lessee_address,
        lesseePhone = customer_phone.ifBlank { contractObj.lessee_phone },
        lesseeIdNumber = contractObj.lessee_id_number,
        lesseeIdIssueDate = contractObj.lessee_id_issue_date,
        advancePaymentAmount = advance_payment.ifBlank { contractObj.advance_payment_amount },
        collateralCccd = contractObj.collateral_cccd,
        collateralGplx = contractObj.collateral_gplx,
        collateralAssetDescription = contractObj.collateral_asset_description,
        collateralCashAmount = contractObj.collateral_cash_amount,
        idCardFrontPhotoUri = contractObj.id_card_front_photo_uri,
        idCardBackPhotoUri = contractObj.id_card_back_photo_uri,
        contractLocation = contractObj.contract_location,
        contractDate = contractObj.contract_date,
        storeId = storeId ?: ""
    )
}

@Keep
@Serializable
data class StoreSettingsDto(
    val id: String = "default_settings",
    val lessor_company: String = "",
    val lessor_representative: String = "",
    val lessor_address: String = "",
    val lessor_phone: String = "",
    val bank_name: String = "",
    val bank_account_number: String = "",
    val bank_account_holder: String = "",
    @SerialName("store_id") val storeId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Keep
@Serializable
data class RegisteredStoreInfo(
    val storeId: String,
    val storeName: String,
    val ownerName: String,
    val ownerPhone: String,
    val ownerEmail: String,
    val registrationDate: String = ""
)

@Keep
@Serializable
data class SyncSummary(
    val staffSyncedCount: Int,
    val customersSyncedCount: Int,
    val equipmentSyncedCount: Int,
    val rentalOrdersSyncedCount: Int,
    val storeSettingsSynced: Boolean = false
) {
    val totalSynced: Int
        get() = staffSyncedCount + customersSyncedCount + equipmentSyncedCount + rentalOrdersSyncedCount + (if (storeSettingsSynced) 1 else 0)
}

object SupabaseManager {
    private var _currentStoreId: String? = null
    var currentStoreId: String?
        get() = _currentStoreId
        set(value) {
            _currentStoreId = value?.let { normalizeStoreIdFromPhone(it) }
        }

    private const val DEFAULT_SUPABASE_URL = "https://dgkolxnsglsbqkavmawv.supabase.co"
    private const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRna29seG5zZ2xzYnFrYXZtYXd2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyMjc0NjYsImV4cCI6MjEwNDgwMzQ2Nn0.zY3Gfx503_mY48JwEPmlYgrBlAtFQI_KmHXnlFdyxq8"

    val client: SupabaseClient? by lazy {
        try {
            createSupabaseClient(
                supabaseUrl = DEFAULT_SUPABASE_URL,
                supabaseKey = DEFAULT_SUPABASE_ANON_KEY
            ) {
                install(Postgrest)
                install(Auth)
                defaultSerializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        } catch (e: Throwable) {
            File("client_init_error.log").writeText("Init error: ${e.message}\n${e.stackTraceToString()}")
            logE("SupabaseManager", "Failed to create Supabase client: ${e.message}", e)
            null
        }
    }

    suspend fun fetchStaffAccounts(): List<StaffAccountDto>? {
        val storeId = currentStoreId
        return try {
            val response = client?.postgrest
                ?.from("staff_accounts")
                ?.select {
                    if (!storeId.isNullOrBlank()) {
                        filter {
                            eq("store_id", storeId)
                        }
                    }
                }
                ?.decodeList<StaffAccountDto>()
            logD("SupabaseManager", "fetchStaffAccounts: successfully fetched ${response?.size ?: 0} staff accounts for storeId=$storeId")
            response
        } catch (e: Exception) {
            logE("SupabaseManager", "Failed to fetch staff accounts from Supabase: ${e.message}", e)
            null
        }
    }

    suspend fun registerAccount(account: StaffAccountDto) {
        val supabaseClient = client ?: throw Exception("Supabase client chưa được khởi tạo")
        val storeIdToAttach = if (!account.storeId.isNullOrBlank()) account.storeId else currentStoreId
        val validAccount = if (account.id.isBlank()) {
            account.copy(id = UUID.randomUUID().toString(), storeId = storeIdToAttach)
        } else {
            account.copy(storeId = storeIdToAttach)
        }
        try {
            logD("SupabaseManager", "registerAccount: upserting account for phone=${validAccount.phone}, email=${validAccount.email}, storeId=${validAccount.storeId}")
            supabaseClient.postgrest
                .from("staff_accounts")
                .upsert(validAccount)
            logD("SupabaseManager", "registerAccount: successfully upserted account for phone=${validAccount.phone}")
        } catch (e: Exception) {
            logE("SupabaseManager", "Failed to register/upsert account on Supabase for phone=${account.phone}: ${e.message}", e)
            throw Exception("Lỗi lưu tài khoản lên Supabase: ${e.localizedMessage ?: e.message}. Vui lòng kiểm tra kết nối hoặc cấu hình RLS trên Supabase!")
        }
    }

    suspend fun checkAccountExists(phone: String, email: String): Boolean {
        return try {
            val accounts = fetchStaffAccounts()
            accounts?.any { it.phone == phone || it.email.equals(email, ignoreCase = true) } ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun insertStaffAccount(account: StaffAccountDto): Boolean {
        return try {
            registerAccount(account)
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "insertStaffAccount caught exception for phone=${account.phone}: ${e.message}", e)
            false
        }
    }

    suspend fun deleteStaffAccountByPhone(phone: String): Boolean {
        return try {
            logD("SupabaseManager", "deleteStaffAccountByPhone: deleting staff account for phone=$phone")
            client?.postgrest
                ?.from("staff_accounts")
                ?.delete {
                    filter {
                        eq("phone", phone)
                    }
                }
            logD("SupabaseManager", "deleteStaffAccountByPhone: successfully deleted staff account for phone=$phone")
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "Failed to delete staff account for phone=$phone: ${e.message}", e)
            false
        }
    }

    suspend fun updateBiometricStatus(phone: String, enabled: Boolean): Boolean {
        return try {
            logD("SupabaseManager", "updateBiometricStatus: updating biometric_enabled=$enabled for phone=$phone")
            client?.postgrest
                ?.from("staff_accounts")
                ?.update(
                    mapOf("biometric_enabled" to enabled)
                ) {
                    filter {
                        eq("phone", phone)
                    }
                }
            logD("SupabaseManager", "updateBiometricStatus: successfully updated biometric status for phone=$phone")
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "Failed to update biometric status for phone=$phone: ${e.message}", e)
            false
        }
    }

    // --- Customers ---
    private suspend fun fetchCustomersByStoreId(storeIdFilter: String): List<CustomerDto>? {
        return try {
            client?.postgrest
                ?.from("customers")
                ?.select {
                    filter { eq("store_id", storeIdFilter) }
                }
                ?.decodeList<CustomerDto>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchCustomers(overrideStoreId: String? = null): List<Customer>? {
        val targetStoreId = overrideStoreId ?: currentStoreId ?: "SHOP_0901992349"
        val normalizedTarget = normalizeStoreIdFromPhone(targetStoreId)

        var primaryDtos = fetchCustomersByStoreId(targetStoreId)
        if (primaryDtos.isNullOrEmpty() && normalizedTarget.isNotBlank() && normalizedTarget != targetStoreId) {
            primaryDtos = fetchCustomersByStoreId(normalizedTarget)
        }

        if (!primaryDtos.isNullOrEmpty()) {
            return primaryDtos.map { it.toDomain() }
        }

        if (targetStoreId != "SHOP_0901992349" && normalizedTarget != "SHOP_0901992349") {
            val fallbackStoreIds = listOf("SHOP_0901992349", "SHOP_901992349")
            for (fallbackId in fallbackStoreIds) {
                val fallbackDtos = fetchCustomersByStoreId(fallbackId)
                if (!fallbackDtos.isNullOrEmpty()) {
                    return fallbackDtos.map { it.toDomain() }
                }
            }
        }

        return try {
            client?.postgrest
                ?.from("customers")
                ?.select()
                ?.decodeList<CustomerDto>()
                ?.map { it.toDomain() }
        } catch (e: Exception) {
            primaryDtos?.map { it.toDomain() }
        }
    }

    suspend fun insertCustomer(customer: Customer): Boolean {
        return try {
            val dto = customer.toDto()
            val finalDto = if (dto.storeId.isNullOrBlank()) dto.copy(storeId = currentStoreId) else dto
            client?.postgrest
                ?.from("customers")
                ?.upsert(finalDto)
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "insertCustomer failed: ${e.message}", e)
            false
        }
    }

    suspend fun deleteCustomer(id: String): Boolean {
        return try {
            client?.postgrest
                ?.from("customers")
                ?.delete {
                    filter { eq("id", id) }
                }
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Equipment ---
    private suspend fun fetchEquipmentByStoreId(storeIdFilter: String): List<EquipmentDto>? {
        val allDtos = mutableListOf<EquipmentDto>()
        var page = 0
        val pageSize = 100
        val maxPages = 50
        while (page < maxPages) {
            val from = (page * pageSize).toLong()
            val to = from + pageSize - 1
            val batch = client?.postgrest
                ?.from("equipment")
                ?.select {
                    filter {
                        eq("store_id", storeIdFilter)
                    }
                    range(from, to)
                }
                ?.decodeList<EquipmentDto>() ?: return null

            allDtos.addAll(batch)
            if (batch.size < pageSize) {
                break
            }
            page++
        }
        return allDtos
    }

    suspend fun fetchEquipment(overrideStoreId: String? = null): List<Equipment>? {
        val targetStoreId = overrideStoreId ?: currentStoreId ?: "SHOP_0901992349"
        val normalizedTarget = normalizeStoreIdFromPhone(targetStoreId)

        logD("SupabaseManager", "fetchEquipment: querying with storeId=$targetStoreId (normalized=$normalizedTarget)")

        var primaryDtos: List<EquipmentDto>? = null
        try {
            primaryDtos = fetchEquipmentByStoreId(targetStoreId)
            if (primaryDtos.isNullOrEmpty() && normalizedTarget.isNotBlank() && normalizedTarget != targetStoreId) {
                primaryDtos = fetchEquipmentByStoreId(normalizedTarget)
            }
        } catch (e: Exception) {
            logE("SupabaseManager", "fetchEquipment primary query failed for storeId=$targetStoreId: ${e.message}", e)
        }

        if (!primaryDtos.isNullOrEmpty()) {
            logD("SupabaseManager", "fetchEquipment: found ${primaryDtos.size} equipment items using storeId=$targetStoreId")
            return primaryDtos.map { it.toDomain() }
        }

        // If querying with storeId returns 0 items (or null), check if storeId is "SHOP_DEFAULT" or different from "SHOP_0901992349", and fallback to query with "SHOP_0901992349"
        if (targetStoreId != "SHOP_0901992349" && normalizedTarget != "SHOP_0901992349") {
            logD("SupabaseManager", "fetchEquipment: initial query for storeId=$targetStoreId returned 0 items. Falling back to SHOP_0901992349...")
            val fallbackStoreIds = listOf("SHOP_0901992349", "SHOP_901992349")
            for (fallbackId in fallbackStoreIds) {
                val fallbackDtos = try {
                    fetchEquipmentByStoreId(fallbackId)
                } catch (e: Exception) {
                    logE("SupabaseManager", "fetchEquipment fallback query failed for storeId=$fallbackId: ${e.message}", e)
                    null
                }
                if (!fallbackDtos.isNullOrEmpty()) {
                    logD("SupabaseManager", "fetchEquipment: fallback to $fallbackId succeeded, found ${fallbackDtos.size} items")
                    return fallbackDtos.map { it.toDomain() }
                }
            }
        }

        logD("SupabaseManager", "fetchEquipment: returned ${primaryDtos?.size ?: 0} items for storeId=$targetStoreId")
        return primaryDtos?.map { it.toDomain() }
    }

    suspend fun insertEquipment(equipment: Equipment): Boolean {
        return try {
            val dto = equipment.toDto()
            val finalDto = if (dto.storeId.isNullOrBlank()) dto.copy(storeId = currentStoreId) else dto
            client?.postgrest
                ?.from("equipment")
                ?.upsert(finalDto)
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "insertEquipment failed: ${e.message}", e)
            false
        }
    }

    suspend fun updateEquipment(equipment: Equipment): Boolean {
        return try {
            val dto = equipment.toDto()
            val finalDto = if (dto.storeId.isNullOrBlank()) dto.copy(storeId = currentStoreId) else dto
            client?.postgrest
                ?.from("equipment")
                ?.update(finalDto) {
                    filter { eq("id", equipment.id) }
                }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteEquipment(id: String): Boolean {
        return try {
            client?.postgrest
                ?.from("equipment")
                ?.delete {
                    filter { eq("id", id) }
                }
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Rental Orders ---
    private suspend fun fetchRentalOrdersByStoreId(storeIdFilter: String): List<RentalOrderDto>? {
        return try {
            client?.postgrest
                ?.from("rental_orders")
                ?.select {
                    filter { eq("store_id", storeIdFilter) }
                }
                ?.decodeList<RentalOrderDto>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchRentalOrders(overrideStoreId: String? = null): List<RentalOrder>? {
        val targetStoreId = overrideStoreId ?: currentStoreId ?: "SHOP_0901992349"
        val normalizedTarget = normalizeStoreIdFromPhone(targetStoreId)

        var primaryDtos = fetchRentalOrdersByStoreId(targetStoreId)
        if (primaryDtos.isNullOrEmpty() && normalizedTarget.isNotBlank() && normalizedTarget != targetStoreId) {
            primaryDtos = fetchRentalOrdersByStoreId(normalizedTarget)
        }

        if (!primaryDtos.isNullOrEmpty()) {
            return primaryDtos.map { it.toDomain() }
        }

        if (targetStoreId != "SHOP_0901992349" && normalizedTarget != "SHOP_0901992349") {
            val fallbackStoreIds = listOf("SHOP_0901992349", "SHOP_901992349")
            for (fallbackId in fallbackStoreIds) {
                val fallbackDtos = fetchRentalOrdersByStoreId(fallbackId)
                if (!fallbackDtos.isNullOrEmpty()) {
                    return fallbackDtos.map { it.toDomain() }
                }
            }
        }

        return try {
            client?.postgrest
                ?.from("rental_orders")
                ?.select()
                ?.decodeList<RentalOrderDto>()
                ?.map { it.toDomain() }
        } catch (e: Exception) {
            primaryDtos?.map { it.toDomain() }
        }
    }

    suspend fun insertRentalOrder(order: RentalOrder): Boolean {
        return try {
            val dto = order.toDto()
            val finalDto = if (dto.storeId.isNullOrBlank()) dto.copy(storeId = currentStoreId) else dto
            client?.postgrest
                ?.from("rental_orders")
                ?.upsert(finalDto)
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "insertRentalOrder failed: ${e.message}", e)
            false
        }
    }

    suspend fun deleteRentalOrder(id: String): Boolean {
        return try {
            client?.postgrest
                ?.from("rental_orders")
                ?.delete {
                    filter { eq("id", id) }
                }
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "deleteRentalOrder failed: ${e.message}", e)
            false
        }
    }

    suspend fun updateRentalOrderStatus(id: String, status: String): Boolean {
        return try {
            client?.postgrest
                ?.from("rental_orders")
                ?.update(
                    mapOf("status" to status)
                ) {
                    filter { eq("id", id) }
                }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateRentalOrderExtension(order: RentalOrder): Boolean {
        return try {
            val dto = order.toDto()
            val finalDto = if (dto.storeId.isNullOrBlank()) dto.copy(storeId = currentStoreId) else dto
            client?.postgrest
                ?.from("rental_orders")
                ?.upsert(finalDto)
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Store Settings ---
    suspend fun fetchStoreSettings(): StoreSettingsDto? {
        val storeId = currentStoreId
        return try {
            val response = client?.postgrest
                ?.from("store_settings")
                ?.select {
                    if (!storeId.isNullOrBlank()) {
                        filter { eq("store_id", storeId) }
                    }
                }
                ?.decodeList<StoreSettingsDto>()
                ?.firstOrNull()
            logD("SupabaseManager", "fetchStoreSettings: successfully fetched store settings for storeId=$storeId")
            response
        } catch (e: Exception) {
            logE("SupabaseManager", "Failed to fetch store settings from Supabase: ${e.message}", e)
            null
        }
    }

    suspend fun upsertStoreSettings(settings: StoreSettingsDto): Boolean {
        val supabaseClient = client ?: return false
        val finalSettings = if (settings.storeId.isNullOrBlank()) settings.copy(storeId = currentStoreId) else settings
        return try {
            supabaseClient.postgrest
                .from("store_settings")
                .upsert(finalSettings)
            logD("SupabaseManager", "upsertStoreSettings: successfully upserted store settings for storeId=${finalSettings.storeId}")
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "Failed to upsert store settings on Supabase: ${e.message}", e)
            false
        }
    }

    suspend fun upsertStoreSettings(lessorInfo: LessorInfo, bankAccountInfo: BankAccountInfo): Boolean {
        val settingsToPush = StoreSettingsDto(
            id = "default_settings",
            lessor_company = lessorInfo.name,
            lessor_representative = lessorInfo.representative,
            lessor_address = lessorInfo.address,
            lessor_phone = lessorInfo.phone,
            bank_name = bankAccountInfo.bankName,
            bank_account_number = bankAccountInfo.accountNumber,
            bank_account_holder = bankAccountInfo.accountHolderName,
            storeId = currentStoreId
        )
        return upsertStoreSettings(settingsToPush)
    }

    suspend fun syncAllLocalData(
        staffAccounts: List<StaffAccountDto>,
        customers: List<Customer>,
        equipment: List<Equipment>,
        rentalOrders: List<RentalOrder>,
        lessorInfo: LessorInfo? = null,
        bankAccountInfo: BankAccountInfo? = null
    ): Result<SyncSummary> {
        return try {
            val supabaseClient = client ?: throw Exception("Supabase client chưa được khởi tạo")

            var staffCount = 0
            for (staff in staffAccounts) {
                try {
                    val storeIdToAttach = if (!staff.storeId.isNullOrBlank()) staff.storeId else currentStoreId
                    val validStaff = if (staff.id.isBlank()) staff.copy(id = UUID.randomUUID().toString(), storeId = storeIdToAttach) else staff.copy(storeId = storeIdToAttach)
                    supabaseClient.postgrest
                        .from("staff_accounts")
                        .upsert(validStaff)
                    staffCount++
                } catch (e: Exception) {
                    logE("SupabaseManager", "Failed to sync staff ${staff.phone}: ${e.message}", e)
                }
            }

            var customerCount = 0
            for (cust in customers) {
                try {
                    val dto = cust.toDto()
                    val finalDto = if (dto.storeId.isNullOrBlank()) dto.copy(storeId = currentStoreId) else dto
                    supabaseClient.postgrest
                        .from("customers")
                        .upsert(finalDto)
                    customerCount++
                } catch (e: Exception) {
                    logE("SupabaseManager", "Failed to sync customer ${cust.id}: ${e.message}", e)
                }
            }

            var equipmentCount = 0
            for (eq in equipment) {
                try {
                    val dto = eq.toDto()
                    val finalDto = if (dto.storeId.isNullOrBlank()) dto.copy(storeId = currentStoreId) else dto
                    supabaseClient.postgrest
                        .from("equipment")
                        .upsert(finalDto)
                    equipmentCount++
                } catch (e: Exception) {
                    logE("SupabaseManager", "Failed to sync equipment ${eq.id}: ${e.message}", e)
                }
            }

            var orderCount = 0
            for (order in rentalOrders) {
                try {
                    val dto = order.toDto()
                    val finalDto = if (dto.storeId.isNullOrBlank()) dto.copy(storeId = currentStoreId) else dto
                    supabaseClient.postgrest
                        .from("rental_orders")
                        .upsert(finalDto)
                    orderCount++
                } catch (e: Exception) {
                    logE("SupabaseManager", "Failed to sync order ${order.id}: ${e.message}", e)
                }
            }

            var storeSettingsSynced = false
            val storeSettingsToPush = StoreSettingsDto(
                id = "default_settings",
                lessor_company = lessorInfo?.name ?: defaultLessorInfo.name,
                lessor_representative = lessorInfo?.representative ?: defaultLessorInfo.representative,
                lessor_address = lessorInfo?.address ?: defaultLessorInfo.address,
                lessor_phone = lessorInfo?.phone ?: defaultLessorInfo.phone,
                bank_name = bankAccountInfo?.bankName ?: defaultBankAccountInfo.bankName,
                bank_account_number = bankAccountInfo?.accountNumber ?: defaultBankAccountInfo.accountNumber,
                bank_account_holder = bankAccountInfo?.accountHolderName ?: defaultBankAccountInfo.accountHolderName,
                storeId = currentStoreId
            )
            try {
                storeSettingsSynced = upsertStoreSettings(storeSettingsToPush)
                if (storeSettingsSynced) {
                    logD("SupabaseManager", "Successfully synced store settings to Supabase")
                } else {
                    logE("SupabaseManager", "Failed to sync store_settings: upsertStoreSettings returned false")
                }
            } catch (e: Exception) {
                logE("SupabaseManager", "Failed to sync store_settings: ${e.message}", e)
            }

            try {
                val fetchedSettings = fetchStoreSettings()
                if (fetchedSettings != null) {
                    logD("SupabaseManager", "Successfully fetched store settings during sync: $fetchedSettings")
                }
            } catch (e: Exception) {
                logE("SupabaseManager", "Failed to fetch store settings during sync: ${e.message}", e)
            }

            Result.success(SyncSummary(staffCount, customerCount, equipmentCount, orderCount, storeSettingsSynced))
        } catch (e: Exception) {
            logE("SupabaseManager", "syncAllLocalData failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAllStores(localAccounts: List<UserAccount> = emptyList()): List<RegisteredStoreInfo> {
        val resultList = mutableListOf<RegisteredStoreInfo>()
        val storeMap = mutableMapOf<String, RegisteredStoreInfo>()

        try {
            val remoteAccounts = client?.postgrest
                ?.from("staff_accounts")
                ?.select()
                ?.decodeList<StaffAccountDto>() ?: emptyList()

            val remoteSettings = client?.postgrest
                ?.from("store_settings")
                ?.select()
                ?.decodeList<StoreSettingsDto>() ?: emptyList()

            val settingsMap = remoteSettings.mapNotNull { s ->
                s.storeId?.let { it to s }
            }.toMap()

            val groupedAccounts = remoteAccounts.groupBy { dto ->
                if (!dto.storeId.isNullOrBlank()) normalizeStoreIdFromPhone(dto.storeId)
                else if (parseUserRole(dto.role) == UserRole.OWNER) normalizeStoreIdFromPhone(dto.phone)
                else ""
            }

            for ((storeId, accounts) in groupedAccounts) {
                if (storeId.isBlank()) continue
                val ownerAccount = accounts.find { parseUserRole(it.role) == UserRole.OWNER } ?: accounts.firstOrNull() ?: continue
                val settings = settingsMap[storeId]

                val storeName = settings?.lessor_company?.takeIf { it.isNotBlank() }
                    ?: settings?.lessor_representative?.takeIf { it.isNotBlank() }
                    ?: ownerAccount.full_name.takeIf { it.isNotBlank() }
                    ?: "Cửa hàng $storeId"

                val rawDate = ownerAccount.createdAt?.takeIf { it.isNotBlank() } ?: settings?.createdAt?.takeIf { it.isNotBlank() } ?: ""
                val formattedDate = formatRegistrationDate(rawDate)

                val storeInfo = RegisteredStoreInfo(
                    storeId = storeId,
                    storeName = storeName,
                    ownerName = ownerAccount.full_name,
                    ownerPhone = ownerAccount.phone,
                    ownerEmail = ownerAccount.email,
                    registrationDate = formattedDate
                )
                storeMap[storeId] = storeInfo
            }

            for (settings in remoteSettings) {
                val sId = settings.storeId ?: continue
                if (sId.isNotBlank() && !storeMap.containsKey(sId)) {
                    val rawDate = settings.createdAt ?: ""
                    storeMap[sId] = RegisteredStoreInfo(
                        storeId = sId,
                        storeName = settings.lessor_company.ifBlank { "Cửa hàng $sId" },
                        ownerName = settings.lessor_representative,
                        ownerPhone = settings.lessor_phone,
                        ownerEmail = "",
                        registrationDate = formatRegistrationDate(rawDate)
                    )
                }
            }
        } catch (e: Exception) {
            logE("SupabaseManager", "fetchAllStores remote fetch failed: ${e.message}", e)
        }

        for (acc in localAccounts) {
            val effStoreId = if (acc.storeId.isNotBlank()) normalizeStoreIdFromPhone(acc.storeId)
            else if (acc.role == UserRole.OWNER) normalizeStoreIdFromPhone(acc.phone)
            else ""

            if (effStoreId.isNotBlank() && !storeMap.containsKey(effStoreId)) {
                storeMap[effStoreId] = RegisteredStoreInfo(
                    storeId = effStoreId,
                    storeName = "Cửa hàng ${acc.fullName}",
                    ownerName = acc.fullName,
                    ownerPhone = acc.phone,
                    ownerEmail = acc.email,
                    registrationDate = "Hệ thống"
                )
            }
        }

        resultList.addAll(storeMap.values)
        logD("SupabaseManager", "fetchAllStores: found ${resultList.size} stores")
        return resultList
    }

    private fun formatRegistrationDate(rawDate: String): String {
        if (rawDate.isBlank()) return "Hệ thống"
        return try {
            if (rawDate.contains("T")) {
                val datePart = rawDate.split("T")[0]
                val parts = datePart.split("-")
                if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else datePart
            } else rawDate
        } catch (_: Exception) {
            rawDate
        }
    }

    suspend fun purgeStoreData(storeId: String, ownerPhone: String? = null): Boolean {
        val cleanPhone = ownerPhone?.replace(" ", "") ?: if (storeId.startsWith("SHOP_")) storeId.removePrefix("SHOP_") else null
        if (storeId == "SHOP_0901992349" || cleanPhone == "0901992349" || cleanPhone?.endsWith("0901992349") == true) {
            logE("SupabaseManager", "purgeStoreData blocked: Cannot purge Master Admin store!")
            return false
        }
        val supabaseClient = client ?: return false
        val normalizedId = normalizeStoreIdFromPhone(storeId)
        val altId = if (normalizedId.startsWith("SHOP_0")) "SHOP_" + normalizedId.removePrefix("SHOP_0") else normalizedId
        val targetIds = listOf(storeId, normalizedId, altId).filter { it.isNotBlank() }.distinct()
        return try {
            logD("SupabaseManager", "purgeStoreData: purging storeIds=$targetIds, ownerPhone=$ownerPhone")
            for (sId in targetIds) {
                supabaseClient.postgrest.from("store_settings").delete { filter { eq("store_id", sId) } }
                supabaseClient.postgrest.from("staff_accounts").delete { filter { eq("store_id", sId) } }
                supabaseClient.postgrest.from("customers").delete { filter { eq("store_id", sId) } }
                supabaseClient.postgrest.from("equipment").delete { filter { eq("store_id", sId) } }
                supabaseClient.postgrest.from("rental_orders").delete { filter { eq("store_id", sId) } }
            }

            val cleanPhone = ownerPhone?.replace(" ", "") ?: if (storeId.startsWith("SHOP_")) storeId.removePrefix("SHOP_") else null
            if (!cleanPhone.isNullOrBlank()) {
                val fullPhoneWithZero = if (!cleanPhone.startsWith("0")) "0$cleanPhone" else cleanPhone
                val rawPhoneWithoutZero = cleanPhone.removePrefix("0")
                try {
                    supabaseClient.postgrest.from("staff_accounts").delete { filter { eq("phone", fullPhoneWithZero) } }
                    supabaseClient.postgrest.from("staff_accounts").delete { filter { eq("phone", rawPhoneWithoutZero) } }
                } catch (e: Exception) {
                    logE("SupabaseManager", "purgeStoreData phone delete warning: ${e.message}")
                }
            }
            logD("SupabaseManager", "purgeStoreData: successfully purged storeId=$storeId")
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "purgeStoreData failed for storeId=$storeId: ${e.message}", e)
            false
        }
    }

    suspend fun resetStoreOwnerPassword(phone: String, newPassword: String): Boolean {
        val supabaseClient = client ?: return false
        val cleanPhone = phone.replace(" ", "")
        val fullPhoneWithZero = if (!cleanPhone.startsWith("0")) "0$cleanPhone" else cleanPhone
        val rawPhoneWithoutZero = cleanPhone.removePrefix("0")
        return try {
            supabaseClient.postgrest.from("staff_accounts").update(
                mapOf("password" to newPassword)
            ) { filter { eq("phone", fullPhoneWithZero) } }

            supabaseClient.postgrest.from("staff_accounts").update(
                mapOf("password" to newPassword)
            ) { filter { eq("phone", rawPhoneWithoutZero) } }

            logD("SupabaseManager", "resetStoreOwnerPassword: password reset for phone=$phone")
            true
        } catch (e: Exception) {
            logE("SupabaseManager", "resetStoreOwnerPassword failed for phone=$phone: ${e.message}", e)
            false
        }
    }
}
