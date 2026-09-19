package com.ninotek.ninorent.model

import android.content.Context
import android.util.Log
import com.ninotek.ninorent.utils.StaffAccountDto
import com.ninotek.ninorent.utils.SupabaseManager
import com.ninotek.ninorent.utils.normalizeStoreIdFromPhone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import androidx.annotation.Keep
import java.util.UUID

@Keep
enum class UserRole(val displayName: String) {
    OWNER("Admin"),
    STAFF("Nhân viên")
}

fun parseUserRole(roleStr: String): UserRole {
    return try {
        when (roleStr.uppercase().trim()) {
            "OWNER", "ADMIN", "QUẢN TRỊ VIÊN" -> UserRole.OWNER
            "STAFF", "NHÂN VIÊN", "NHÂN VIÊN NGHIỆP VỤ" -> UserRole.STAFF
            else -> UserRole.valueOf(roleStr)
        }
    } catch (e: Exception) {
        UserRole.OWNER
    }
}

@Keep
data class UserAccount(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val phone: String,
    val email: String,
    val password: String,
    val biometricEnabled: Boolean = false,
    val role: UserRole = UserRole.OWNER,
    val staffRoleDetail: String = "Admin",
    val storeId: String = ""
)

object UserAccountStore {
    private const val PREF_NAME = "registered_users"
    private const val KEY_ACCOUNTS = "accounts_json"
    private const val KEY_CURRENT_SESSION = "current_session_identifier"

    private var _activeStoreId: String = ""
    var activeStoreId: String
        get() = _activeStoreId
        set(value) {
            _activeStoreId = if (value.isNotBlank()) normalizeStoreIdFromPhone(value) else ""
        }

    private fun isAccount0901992349(phone: String): Boolean {
        val clean = phone.trim().replace(" ", "")
        return clean == "0901992349" || clean.endsWith("0901992349")
    }

    fun getRegisteredAccounts(context: Context): List<UserAccount> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        val list = mutableListOf<UserAccount>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val roleStr = obj.optString("role", "OWNER")
                val role = parseUserRole(roleStr)
                val rawId = obj.optString("id", "")
                val id = if (rawId.isBlank()) UUID.randomUUID().toString() else rawId
                val detailStr = obj.optString("staffRoleDetail", "")
                val phone = obj.optString("phone", "")
                val rawStoreId = obj.optString("storeId", "")
                val storeId = if (isAccount0901992349(phone)) "SHOP_0901992349" else rawStoreId
                list.add(
                    UserAccount(
                        id = id,
                        fullName = obj.optString("fullName", ""),
                        phone = phone,
                        email = obj.optString("email", ""),
                        password = obj.optString("password", ""),
                        biometricEnabled = obj.optBoolean("biometricEnabled", false),
                        role = role,
                        staffRoleDetail = if (detailStr.isBlank()) role.displayName else detailStr,
                        storeId = storeId
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveAccounts(context: Context, accounts: List<UserAccount>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (acc in accounts) {
            val validId = acc.id.ifBlank { UUID.randomUUID().toString() }
            val obj = JSONObject().apply {
                put("id", validId)
                put("fullName", acc.fullName)
                put("phone", acc.phone)
                put("email", acc.email)
                put("password", acc.password)
                put("biometricEnabled", acc.biometricEnabled)
                put("role", acc.role.name)
                put("staffRoleDetail", acc.staffRoleDetail)
                put("storeId", acc.storeId)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_ACCOUNTS, jsonArray.toString()).apply()
    }

    suspend fun registerAccount(context: Context, account: UserAccount) {
        val accounts = getRegisteredAccounts(context).toMutableList()
        
        // Check if phone or email already exists in local storage or Supabase
        val existingLocal = accounts.find { it.phone == account.phone || it.email.equals(account.email, ignoreCase = true) }
        val existsRemote = try {
            SupabaseManager.checkAccountExists(account.phone, account.email)
        } catch (_: Exception) {
            false
        }

        val validAccountId = account.id.ifBlank { UUID.randomUUID().toString() }
        val accountWithId = account.copy(id = validAccountId)

        val currentSession = getCurrentSession(context)
        val inheritedStoreId = when {
            isAccount0901992349(accountWithId.phone) -> "SHOP_0901992349"
            accountWithId.storeId.isNotBlank() -> normalizeStoreIdFromPhone(accountWithId.storeId)
            accountWithId.role == UserRole.OWNER -> normalizeStoreIdFromPhone(accountWithId.phone)
            else -> currentSession?.storeId?.ifBlank { activeStoreId } ?: activeStoreId
        }

        // First registered account defaults to OWNER, subsequent can be STAFF or OWNER
        val assignedAccount = if (isAccount0901992349(accountWithId.phone)) {
            accountWithId.copy(
                role = if (accounts.isEmpty() && !existsRemote) UserRole.OWNER else accountWithId.role,
                storeId = "SHOP_0901992349"
            )
        } else if (accounts.isEmpty() && !existsRemote) {
            accountWithId.copy(role = UserRole.OWNER, storeId = inheritedStoreId.ifBlank { normalizeStoreIdFromPhone(accountWithId.phone) })
        } else if (existingLocal != null) {
            val existingStoreId = existingLocal.storeId.ifBlank { inheritedStoreId }
            accountWithId.copy(id = existingLocal.id.ifBlank { validAccountId }, storeId = normalizeStoreIdFromPhone(existingStoreId))
        } else {
            accountWithId.copy(storeId = normalizeStoreIdFromPhone(inheritedStoreId))
        }

        val dto = StaffAccountDto(
            id = assignedAccount.id.ifBlank { UUID.randomUUID().toString() },
            full_name = assignedAccount.fullName,
            phone = assignedAccount.phone,
            email = assignedAccount.email,
            password = assignedAccount.password,
            role = assignedAccount.role.name,
            staff_role_detail = assignedAccount.staffRoleDetail.ifBlank { assignedAccount.role.displayName },
            biometric_enabled = assignedAccount.biometricEnabled,
            storeId = assignedAccount.storeId
        )

        // Require successful upsert into Supabase staff_accounts table
        try {
            Log.d("UserAccountStore", "registerAccount: upserting account to Supabase staff_accounts for phone=${assignedAccount.phone}, email=${assignedAccount.email}, storeId=${assignedAccount.storeId}")
            SupabaseManager.registerAccount(dto)
            Log.d("UserAccountStore", "registerAccount: successfully upserted account to Supabase for phone=${assignedAccount.phone}")
        } catch (e: Exception) {
            Log.e("UserAccountStore", "registerAccount: Supabase sync failed for phone=${assignedAccount.phone}: ${e.message}", e)
            val errorMsg = e.localizedMessage ?: e.message ?: "Lỗi không xác định"
            val formattedError = if (errorMsg.contains("Lỗi lưu tài khoản lên Supabase")) {
                errorMsg
            } else {
                "Lỗi lưu tài khoản lên Supabase: $errorMsg. Vui lòng kiểm tra kết nối hoặc cấu hình RLS trên Supabase!"
            }
            throw Exception(formattedError)
        }

        accounts.removeAll { it.id == assignedAccount.id || it.phone == assignedAccount.phone || it.email.equals(assignedAccount.email, ignoreCase = true) }
        accounts.add(assignedAccount)
        saveAccounts(context, accounts)
        Log.d("UserAccountStore", "registerAccount: saved/updated account locally for phone=${assignedAccount.phone}")
    }

    suspend fun updateAccount(context: Context, updatedAccount: UserAccount, oldPhone: String? = null) {
        val accounts = getRegisteredAccounts(context).toMutableList()
        val validAccountId = updatedAccount.id.ifBlank { UUID.randomUUID().toString() }
        val effectiveStoreId = if (isAccount0901992349(updatedAccount.phone)) "SHOP_0901992349" else updatedAccount.storeId
        val accountWithId = updatedAccount.copy(
            id = validAccountId,
            staffRoleDetail = updatedAccount.role.displayName,
            storeId = effectiveStoreId
        )

        val dto = StaffAccountDto(
            id = accountWithId.id,
            full_name = accountWithId.fullName,
            phone = accountWithId.phone,
            email = accountWithId.email,
            password = accountWithId.password,
            role = accountWithId.role.name,
            staff_role_detail = accountWithId.staffRoleDetail,
            biometric_enabled = accountWithId.biometricEnabled,
            storeId = accountWithId.storeId
        )

        try {
            Log.d("UserAccountStore", "updateAccount: upserting account to Supabase staff_accounts for phone=${accountWithId.phone}")
            SupabaseManager.registerAccount(dto)
            Log.d("UserAccountStore", "updateAccount: successfully upserted account to Supabase for phone=${accountWithId.phone}")
        } catch (e: Exception) {
            Log.e("UserAccountStore", "updateAccount: Supabase sync failed for phone=${accountWithId.phone}: ${e.message}", e)
            val errorMsg = e.localizedMessage ?: e.message ?: "Lỗi không xác định"
            val formattedError = if (errorMsg.contains("Lỗi lưu tài khoản lên Supabase")) {
                errorMsg
            } else {
                "Lỗi lưu tài khoản lên Supabase: $errorMsg. Vui lòng kiểm tra kết nối hoặc cấu hình RLS trên Supabase!"
            }
            throw Exception(formattedError)
        }

        if (!oldPhone.isNullOrBlank() && oldPhone != accountWithId.phone) {
            try {
                SupabaseManager.deleteStaffAccountByPhone(oldPhone)
            } catch (e: Exception) {
                Log.w("UserAccountStore", "Failed to delete old phone record $oldPhone from Supabase: ${e.message}")
            }
        }

        accounts.removeAll { it.id == accountWithId.id || it.phone == accountWithId.phone || (!oldPhone.isNullOrBlank() && it.phone == oldPhone) }
        accounts.add(accountWithId)
        saveAccounts(context, accounts)

        val currentSession = getCurrentSession(context)
        if (currentSession != null && (!oldPhone.isNullOrBlank() && (currentSession.phone == oldPhone || currentSession.id == accountWithId.id))) {
            setCurrentSession(context, accountWithId)
        }
        Log.d("UserAccountStore", "updateAccount: updated local account for phone=${accountWithId.phone}")
    }

    fun deleteAccountByPhone(context: Context, phone: String) {
        val cleanPhone = phone.replace(" ", "")
        val currentSession = getCurrentSession(context)
        if (isAccount0901992349(cleanPhone) || (currentSession != null && (currentSession.phone == phone || (currentSession.phone.isNotBlank() && cleanPhone == currentSession.phone.replace(" ", ""))))) {
            Log.w("UserAccountStore", "Blocked deleteAccountByPhone for active logged-in user or master admin: $phone")
            return
        }
        val accounts = getRegisteredAccounts(context).toMutableList()
        accounts.removeAll { it.phone == phone }
        saveAccounts(context, accounts)
        Log.d("UserAccountStore", "deleteAccountByPhone: deleted local account for phone=$phone")
    }

    fun validateCredentials(context: Context, identifier: String, password: String): UserAccount? {
        val trimmedIdentifier = identifier.trim()
        Log.d("UserAccountStore", "validateCredentials: starting validation for identifier=$trimmedIdentifier")
        
        // 1. Try querying Supabase staff_accounts (with local fallback if offline)
        try {
            val remoteAccounts = runBlocking(Dispatchers.IO) {
                SupabaseManager.fetchStaffAccounts()
            }
            if (remoteAccounts != null) {
                Log.d("UserAccountStore", "validateCredentials: fetched ${remoteAccounts.size} remote staff accounts from Supabase")
                val domainAccounts = remoteAccounts.map { dto ->
                    val role = parseUserRole(dto.role)
                    val storeId = if (isAccount0901992349(dto.phone)) "SHOP_0901992349" else (dto.storeId ?: "")
                    UserAccount(
                        id = dto.id.ifBlank { UUID.randomUUID().toString() },
                        fullName = dto.full_name,
                        phone = dto.phone,
                        email = dto.email,
                        password = dto.password,
                        biometricEnabled = dto.biometric_enabled,
                        role = role,
                        staffRoleDetail = dto.staff_role_detail.ifBlank { role.displayName },
                        storeId = storeId
                    )
                }
                // Merge with local accounts
                val localAccounts = getRegisteredAccounts(context)
                val mergedMap = LinkedHashMap<String, UserAccount>()
                for (acc in domainAccounts) {
                    mergedMap[acc.phone] = acc
                }
                for (acc in localAccounts) {
                    if (!mergedMap.containsKey(acc.phone)) {
                        mergedMap[acc.phone] = acc
                    }
                }
                val mergedList = mergedMap.values.toList()
                saveAccounts(context, mergedList)

                val matched = mergedList.find {
                    (it.phone == trimmedIdentifier || it.email.equals(trimmedIdentifier, ignoreCase = true) || (isAccount0901992349(trimmedIdentifier) && isAccount0901992349(it.phone))) &&
                    it.password == password
                }
                if (matched != null) {
                    val finalMatched = if (isAccount0901992349(matched.phone) || isAccount0901992349(trimmedIdentifier)) {
                        matched.copy(storeId = "SHOP_0901992349")
                    } else if (matched.role == UserRole.OWNER) {
                        matched.copy(storeId = if (matched.storeId.isNotBlank()) normalizeStoreIdFromPhone(matched.storeId) else normalizeStoreIdFromPhone(matched.phone))
                    } else {
                        if (matched.storeId.isNotBlank()) matched.copy(storeId = normalizeStoreIdFromPhone(matched.storeId)) else matched
                    }
                    setCurrentSession(context, finalMatched)
                    Log.d("UserAccountStore", "validateCredentials: matched account in remote/merged data for identifier=$trimmedIdentifier, storeId=${finalMatched.storeId}")
                    return finalMatched
                } else {
                    Log.w("UserAccountStore", "validateCredentials: no matching account found in remote/merged data for identifier=$trimmedIdentifier")
                }
            } else {
                Log.w("UserAccountStore", "validateCredentials: remote fetchStaffAccounts returned null (offline or network error). Falling back to local accounts.")
            }
        } catch (e: Exception) {
            Log.e("UserAccountStore", "validateCredentials: exception while fetching/processing remote staff accounts: ${e.message}", e)
        }

        // 2. Local fallback if offline or remote query returned null/exception
        Log.d("UserAccountStore", "validateCredentials: falling back to local storage for identifier=$trimmedIdentifier")
        val accounts = getRegisteredAccounts(context)
        val localMatched = accounts.find {
            (it.phone == trimmedIdentifier || it.email.equals(trimmedIdentifier, ignoreCase = true) || (isAccount0901992349(trimmedIdentifier) && isAccount0901992349(it.phone))) &&
            it.password == password
        }
        if (localMatched != null) {
            val finalMatched = if (isAccount0901992349(localMatched.phone) || isAccount0901992349(trimmedIdentifier)) {
                localMatched.copy(storeId = "SHOP_0901992349")
            } else if (localMatched.role == UserRole.OWNER) {
                localMatched.copy(storeId = if (localMatched.storeId.isNotBlank()) normalizeStoreIdFromPhone(localMatched.storeId) else normalizeStoreIdFromPhone(localMatched.phone))
            } else {
                if (localMatched.storeId.isNotBlank()) localMatched.copy(storeId = normalizeStoreIdFromPhone(localMatched.storeId)) else localMatched
            }
            setCurrentSession(context, finalMatched)
            Log.d("UserAccountStore", "validateCredentials: matched account in local storage for identifier=$trimmedIdentifier, storeId=${finalMatched.storeId}")
            return finalMatched
        } else {
            Log.w("UserAccountStore", "validateCredentials: no matching account found in local storage for identifier=$trimmedIdentifier")
        }
        return null
    }

    fun isAccountRegistered(context: Context, identifier: String): Boolean {
        val trimmedIdentifier = identifier.trim()
        try {
            val remoteAccounts = runBlocking(Dispatchers.IO) {
                SupabaseManager.fetchStaffAccounts()
            }
            if (remoteAccounts != null) {
                val domainAccounts = remoteAccounts.map { dto ->
                    val role = parseUserRole(dto.role)
                    val storeId = if (isAccount0901992349(dto.phone)) "SHOP_0901992349" else (dto.storeId ?: "")
                    UserAccount(
                        id = dto.id.ifBlank { UUID.randomUUID().toString() },
                        fullName = dto.full_name,
                        phone = dto.phone,
                        email = dto.email,
                        password = dto.password,
                        biometricEnabled = dto.biometric_enabled,
                        role = role,
                        staffRoleDetail = dto.staff_role_detail.ifBlank { role.displayName },
                        storeId = storeId
                    )
                }
                val localAccounts = getRegisteredAccounts(context)
                val mergedMap = LinkedHashMap<String, UserAccount>()
                for (acc in domainAccounts) {
                    mergedMap[acc.phone] = acc
                }
                for (acc in localAccounts) {
                    if (!mergedMap.containsKey(acc.phone)) {
                        mergedMap[acc.phone] = acc
                    }
                }
                saveAccounts(context, mergedMap.values.toList())
                return mergedMap.values.any { it.phone == trimmedIdentifier || it.email.equals(trimmedIdentifier, ignoreCase = true) || (isAccount0901992349(trimmedIdentifier) && isAccount0901992349(it.phone)) }
            }
        } catch (e: Exception) {
            Log.e("UserAccountStore", "isAccountRegistered: remote fetch error: ${e.message}", e)
        }

        val accounts = getRegisteredAccounts(context)
        return accounts.any { it.phone == trimmedIdentifier || it.email.equals(trimmedIdentifier, ignoreCase = true) || (isAccount0901992349(trimmedIdentifier) && isAccount0901992349(it.phone)) }
    }

    fun updatePassword(context: Context, identifier: String, newPassword: String): Boolean {
        val trimmedIdentifier = identifier.trim()
        val accounts = getRegisteredAccounts(context).toMutableList()
        var updated = false
        for (i in accounts.indices) {
            val acc = accounts[i]
            if (acc.phone == trimmedIdentifier || acc.email.equals(trimmedIdentifier, ignoreCase = true) || (isAccount0901992349(trimmedIdentifier) && isAccount0901992349(acc.phone))) {
                accounts[i] = acc.copy(password = newPassword)
                updated = true
            }
        }
        if (updated) {
            saveAccounts(context, accounts)
        }
        return updated
    }

    fun setBiometricEnabled(context: Context, userIdentifier: String, enabled: Boolean) {
        val accounts = getRegisteredAccounts(context).toMutableList()
        var updated = false
        var targetPhone: String? = null
        for (i in accounts.indices) {
            val acc = accounts[i]
            if (acc.phone == userIdentifier || acc.email.equals(userIdentifier, ignoreCase = true) || (isAccount0901992349(userIdentifier) && isAccount0901992349(acc.phone))) {
                accounts[i] = acc.copy(biometricEnabled = enabled)
                targetPhone = acc.phone
                updated = true
            }
        }
        if (updated) {
            saveAccounts(context, accounts)
            if (targetPhone != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        SupabaseManager.updateBiometricStatus(targetPhone, enabled)
                        Log.d("UserAccountStore", "Successfully pushed biometricEnabled=$enabled to Supabase for phone=$targetPhone")
                    } catch (e: Exception) {
                        Log.e("UserAccountStore", "Failed to push biometric status to Supabase for phone=$targetPhone: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun getBiometricAccount(context: Context): UserAccount? {
        val accounts = getRegisteredAccounts(context)
        return accounts.find { it.biometricEnabled }
    }

    fun setCurrentSession(context: Context, account: UserAccount?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_SESSION, account?.phone ?: account?.email).apply()
        if (account != null) {
            val effectiveStoreId = if (isAccount0901992349(account.phone)) {
                "SHOP_0901992349"
            } else if (account.storeId.isNotBlank()) {
                normalizeStoreIdFromPhone(account.storeId)
            } else if (account.role == UserRole.OWNER) {
                normalizeStoreIdFromPhone(account.phone)
            } else ""
            activeStoreId = effectiveStoreId
            SupabaseManager.currentStoreId = effectiveStoreId
        } else {
            activeStoreId = ""
            SupabaseManager.currentStoreId = null
        }
    }

    fun getCurrentSession(context: Context): UserAccount? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val identifier = prefs.getString(KEY_CURRENT_SESSION, null) ?: return null
        val accounts = getRegisteredAccounts(context)
        val account = accounts.find { it.phone == identifier || it.email.equals(identifier, ignoreCase = true) || (isAccount0901992349(identifier) && isAccount0901992349(it.phone)) }
        if (account != null) {
            val effectiveStoreId = if (isAccount0901992349(account.phone) || isAccount0901992349(identifier)) {
                "SHOP_0901992349"
            } else if (account.storeId.isNotBlank()) {
                normalizeStoreIdFromPhone(account.storeId)
            } else if (account.role == UserRole.OWNER) {
                normalizeStoreIdFromPhone(account.phone)
            } else ""
            val updatedAccount = account.copy(storeId = effectiveStoreId)
            activeStoreId = effectiveStoreId
            SupabaseManager.currentStoreId = effectiveStoreId
            return updatedAccount
        }
        return null
    }

    fun isMasterAdmin(account: UserAccount?): Boolean {
        if (account == null) return false
        return isAccount0901992349(account.phone)
    }

    fun getStaffAccountsForActiveStore(context: Context): List<UserAccount> {
        val currentSession = getCurrentSession(context)
        val sessionStoreId = currentSession?.storeId?.ifBlank {
            if (currentSession.role == UserRole.OWNER) normalizeStoreIdFromPhone(currentSession.phone) else activeStoreId
        } ?: activeStoreId

        val allAccounts = getRegisteredAccounts(context)
        return allAccounts.filter { acc ->
            if (isAccount0901992349(acc.phone)) return@filter false

            val accStoreId = if (acc.storeId.isNotBlank()) normalizeStoreIdFromPhone(acc.storeId)
            else if (acc.role == UserRole.OWNER) normalizeStoreIdFromPhone(acc.phone)
            else ""

            sessionStoreId.isBlank() || accStoreId == sessionStoreId
        }
    }

    fun purgeLocalStoreData(context: Context, storeId: String, ownerPhone: String = "") {
        val cleanOwnerPhone = ownerPhone.replace(" ", "")
        if (storeId == "SHOP_0901992349" || cleanOwnerPhone == "0901992349" || cleanOwnerPhone.endsWith("0901992349")) {
            Log.w("UserAccountStore", "Blocked purgeLocalStoreData for Master Admin store $storeId / $ownerPhone")
            return
        }
        val accounts = getRegisteredAccounts(context).toMutableList()
        val normStoreId = normalizeStoreIdFromPhone(storeId)
        accounts.removeAll { acc ->
            val cleanAccPhone = acc.phone.replace(" ", "")
            val accStoreId = if (acc.storeId.isNotBlank()) normalizeStoreIdFromPhone(acc.storeId)
            else if (acc.role == UserRole.OWNER) normalizeStoreIdFromPhone(acc.phone)
            else ""

            accStoreId == storeId || accStoreId == normStoreId || (cleanOwnerPhone.isNotBlank() && cleanAccPhone == cleanOwnerPhone)
        }
        saveAccounts(context, accounts)

        val currentSession = getCurrentSession(context)
        if (currentSession != null) {
            val sessionStoreId = if (currentSession.storeId.isNotBlank()) normalizeStoreIdFromPhone(currentSession.storeId)
            else if (currentSession.role == UserRole.OWNER) normalizeStoreIdFromPhone(currentSession.phone) else ""
            if (sessionStoreId == storeId || sessionStoreId == normStoreId || (cleanOwnerPhone.isNotBlank() && currentSession.phone.replace(" ", "") == cleanOwnerPhone)) {
                setCurrentSession(context, null)
            }
        }
        Log.d("UserAccountStore", "purgeLocalStoreData: purged local store data for storeId=$storeId, ownerPhone=$ownerPhone")
    }
}
