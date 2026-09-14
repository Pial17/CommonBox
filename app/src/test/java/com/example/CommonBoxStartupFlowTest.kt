package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CommonBoxDatabase
import com.example.data.repository.CommonBoxRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CommonBoxStartupFlowTest {

    private lateinit var database: CommonBoxDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear preferences before each test
        context.getSharedPreferences("common_box_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        database = Room.inMemoryDatabaseBuilder(context, CommonBoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testFreshInstall_hasNoDemoHostelAndNoDummyData() = runBlocking {
        val repository = CommonBoxRepository(database.commonBoxDao(), context)

        // Verify no demo hostel exists
        val groups = database.commonBoxDao().getAllGroups().first()
        assertEquals(0, groups.size)

        val allTxs = database.commonBoxDao().getTransactionsForGroup("group_hostel_default").first()
        assertEquals(0, allTxs.size)

        // Wait a moment for startup check to complete
        val isChecked = repository.isStartupChecked.first { it }
        assertTrue(isChecked)

        // Both group ID and active member ID must be null
        assertNull(repository.currentGroupId.value)
        assertNull(repository.activeMemberId.value)
    }

    @Test
    fun testCreateHostel_persistsAndRemembersOnRestart() = runBlocking {
        val repoLaunch1 = CommonBoxRepository(database.commonBoxDao(), context)

        // User creates a hostel
        val result = repoLaunch1.createHostel("Sunrise Mess", "Sakib")
        assertTrue(result.isSuccess)
        val group = result.getOrNull()
        assertNotNull(group)
        assertEquals("Sunrise Mess", group?.groupName)

        val groupId = repoLaunch1.currentGroupId.value
        assertEquals(group?.groupId, groupId)

        val activeMemberId = repoLaunch1.activeMemberId.value
        assertNotNull(activeMemberId)

        // Simulate app restart by instantiating a new repository with the same DB and context
        val repoLaunch2 = CommonBoxRepository(database.commonBoxDao(), context)
        val isChecked = repoLaunch2.isStartupChecked.first { it }
        assertTrue(isChecked)

        // Returning user must have hostel already loaded!
        assertEquals(group?.groupId, repoLaunch2.currentGroupId.value)
        assertEquals(activeMemberId, repoLaunch2.activeMemberId.value)

        // Verify group data intact
        val loadedGroup = repoLaunch2.getGroup(repoLaunch2.currentGroupId.value!!).first()
        assertNotNull(loadedGroup)
        assertEquals("Sunrise Mess", loadedGroup?.groupName)
    }

    @Test
    fun testJoinHostel_persistsAndLoadsHostel() = runBlocking {
        val repo1 = CommonBoxRepository(database.commonBoxDao(), context)
        val created = repo1.createHostel("Flat 4B", "AdminUser").getOrThrow()

        // Roommate joins hostel
        val joined = repo1.joinHostel(created.groupCode, "RoommateUser").getOrThrow()
        assertEquals(created.groupId, joined.groupId)

        val members = database.commonBoxDao().getMembersForGroup(created.groupId).first()
        assertEquals(2, members.size)

        // App restart simulation
        val repo2 = CommonBoxRepository(database.commonBoxDao(), context)
        repo2.isStartupChecked.first { it }
        assertEquals(created.groupId, repo2.currentGroupId.value)
    }

    @Test
    fun testDeleteHostel_cleansUpAndDoesNotSeedDemoFallback() = runBlocking {
        val repo = CommonBoxRepository(database.commonBoxDao(), context)
        val created = repo.createHostel("Temporary Mess", "Sakib").getOrThrow()

        assertEquals(created.groupId, repo.currentGroupId.value)

        // Delete the hostel
        val deleteResult = repo.deleteGroup(created.groupId)
        assertTrue(deleteResult.isSuccess)

        // Verify clean zero state, NO demo hostel fallback
        val groups = database.commonBoxDao().getAllGroups().first()
        assertEquals(0, groups.size)
        assertNull(repo.currentGroupId.value)
        assertNull(repo.activeMemberId.value)
    }
}
