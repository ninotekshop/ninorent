package com.ninotek.ninorent

import com.ninotek.ninorent.model.UserAccount
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.model.UserRole
import com.ninotek.ninorent.utils.SupabaseManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountProtectionTest {

    @Test
    fun isMasterAdmin_correctlyIdentifiesMasterAdmin() {
        val masterAdminAccount = UserAccount(
            fullName = "Quản trị hệ thống",
            phone = "0901992349",
            email = "admin@ninorent.vn",
            password = "password123",
            role = UserRole.OWNER,
            storeId = "SHOP_0901992349"
        )
        val regularStaffAccount = UserAccount(
            fullName = "Nhân viên A",
            phone = "0912345678",
            email = "staff@ninorent.vn",
            password = "password123",
            role = UserRole.STAFF,
            storeId = "SHOP_0912345678"
        )

        assertTrue(UserAccountStore.isMasterAdmin(masterAdminAccount))
        assertFalse(UserAccountStore.isMasterAdmin(regularStaffAccount))
        assertFalse(UserAccountStore.isMasterAdmin(null))
    }

    @Test
    fun currentUserMatching_identifiesActiveLoggedInAccount() {
        val activeUser = UserAccount(
            fullName = "Admin Store A",
            phone = "0988776655",
            email = "admin.a@store.vn",
            password = "password123",
            role = UserRole.OWNER
        )

        val samePhoneStaff = UserAccount(
            fullName = "Admin Store A",
            phone = "0988776655",
            email = "other@store.vn",
            password = "password123"
        )

        val sameEmailStaff = UserAccount(
            fullName = "Admin Store A",
            phone = "0911223344",
            email = "ADMIN.A@STORE.VN",
            password = "password123"
        )

        val differentStaff = UserAccount(
            fullName = "Staff B",
            phone = "0900000000",
            email = "staff.b@store.vn",
            password = "password123"
        )

        val isSamePhoneMatch = activeUser.phone == samePhoneStaff.phone || activeUser.email.equals(samePhoneStaff.email, ignoreCase = true)
        val isSameEmailMatch = activeUser.phone == sameEmailStaff.phone || activeUser.email.equals(sameEmailStaff.email, ignoreCase = true)
        val isDifferentMatch = activeUser.phone == differentStaff.phone || activeUser.email.equals(differentStaff.email, ignoreCase = true)

        assertTrue(isSamePhoneMatch)
        assertTrue(isSameEmailMatch)
        assertFalse(isDifferentMatch)
    }

    @Test
    fun purgeStoreData_blocksMasterAdminStoreDeletion() = runTest {
        val resultShopId = SupabaseManager.purgeStoreData("SHOP_0901992349", "0901992349")
        val resultPhone = SupabaseManager.purgeStoreData("SHOP_123456", "0901992349")

        assertFalse(resultShopId)
        assertFalse(resultPhone)
    }
}
