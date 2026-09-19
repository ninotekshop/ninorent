package com.ninotek.ninorent

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ninotek.ninorent.utils.StaffAccountDto
import com.ninotek.ninorent.utils.SupabaseManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupabaseTest {

    @Test
    fun testSupabaseConnectivityAndStaffAccountRoundtrip() = runTest {
        // 1. Test connectivity by fetching staff accounts
        val initialAccounts = SupabaseManager.fetchStaffAccounts()
        assertNotNull("Supabase client and postgrest should be initialized and return a list (or empty list)", initialAccounts)
        println("Fetched initial staff accounts count: ${initialAccounts?.size}")

        // 2. Prepare test staff account
        val uniquePhone = "0998887766"
        val testDto = StaffAccountDto(
            full_name = "Unit Test Staff",
            phone = uniquePhone,
            email = "unittest.staff@ninorent.com",
            password = "securepassword123",
            role = "STAFF",
            staff_role_detail = "Nhân viên kiểm thử tự động"
        )

        // 3. Clean up any existing test account with this phone first
        SupabaseManager.deleteStaffAccountByPhone(uniquePhone)

        // 4. Insert test staff account
        val insertSuccess = SupabaseManager.insertStaffAccount(testDto)
        assertTrue("Inserting test staff account into Supabase should succeed", insertSuccess)
        println("Inserted test staff account successfully with phone: $uniquePhone")

        // 5. Query back and verify insertion
        val updatedAccounts = SupabaseManager.fetchStaffAccounts()
        assertNotNull("Updated staff accounts list should not be null", updatedAccounts)
        val found = updatedAccounts?.any { it.phone == uniquePhone } == true
        assertTrue("Newly inserted staff account should be queried back from Supabase", found)
        println("Verified test staff account exists in Supabase query results.")

        // 6. Clean up test staff account
        val deleteSuccess = SupabaseManager.deleteStaffAccountByPhone(uniquePhone)
        assertTrue("Deleting test staff account should succeed", deleteSuccess)
        println("Cleaned up test staff account successfully.")
    }
}
