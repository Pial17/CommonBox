package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CommonBoxDatabase
import com.example.data.local.entities.HostelGroupEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.CommonBoxRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CommonBoxPhase2Test {

    private lateinit var database: CommonBoxDatabase
    private lateinit var repository: CommonBoxRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, CommonBoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CommonBoxRepository(database.commonBoxDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testStartupSafety_zeroStateDoesNotCrash() = runBlocking {
        // Startup safety: 0 groups, 0 members, 0 transactions
        val groups = database.commonBoxDao().getAllGroups().first()
        val txs = database.commonBoxDao().getTransactionsForGroup("non_existent").first()
        val members = database.commonBoxDao().getMembersForGroup("non_existent").first()

        assertEquals(0, groups.size)
        assertEquals(0, txs.size)
        assertEquals(0, members.size)
    }

    @Test
    fun testMathematicalBalanceIntegrity() = runBlocking {
        val groupId = "group_test_1"
        val group = HostelGroupEntity(
            groupId = groupId,
            groupName = "Room 402",
            groupCode = "HST-402",
            createdBy = "Sakib"
        )
        database.commonBoxDao().insertGroup(group)

        val member = MemberEntity(
            memberId = "mem_sakib",
            groupId = groupId,
            name = "Sakib",
            role = "Admin"
        )
        database.commonBoxDao().insertMember(member)

        // Add ৳5,000 Income
        val incomeTx = TransactionEntity(
            transactionId = "tx_1",
            groupId = groupId,
            memberId = member.memberId,
            memberName = member.name,
            type = TransactionType.INCOME.name,
            amount = 5000.0,
            category = "CONTRIBUTION",
            description = "Initial common cash"
        )
        database.commonBoxDao().insertTransaction(incomeTx)

        // Add ৳1,200 Expense
        val expenseTx = TransactionEntity(
            transactionId = "tx_2",
            groupId = groupId,
            memberId = member.memberId,
            memberName = member.name,
            type = TransactionType.EXPENSE.name,
            amount = 1200.0,
            category = "Groceries",
            description = "Rice and oil"
        )
        database.commonBoxDao().insertTransaction(expenseTx)

        val income = database.commonBoxDao().getTotalIncome(groupId).first() ?: 0.0
        val expense = database.commonBoxDao().getTotalExpense(groupId).first() ?: 0.0
        val balance = income - expense

        assertEquals(5000.0, income, 0.001)
        assertEquals(1200.0, expense, 0.001)
        assertEquals(3800.0, balance, 0.001)
    }

    @Test
    fun testGroupIsolation_transactionsDoNotLeakAcrossHostels() = runBlocking {
        val groupA = "group_A"
        val groupB = "group_B"

        database.commonBoxDao().insertGroup(HostelGroupEntity(groupA, "Hostel Alpha", "HST-AAA", createdBy = "A"))
        database.commonBoxDao().insertGroup(HostelGroupEntity(groupB, "Hostel Beta", "HST-BBB", createdBy = "B"))

        val txA = TransactionEntity(
            transactionId = "tx_a1",
            groupId = groupA,
            memberId = "m1",
            memberName = "Rahim",
            type = TransactionType.INCOME.name,
            amount = 10000.0,
            category = "CONTRIBUTION",
            description = "Alpha deposit"
        )
        database.commonBoxDao().insertTransaction(txA)

        val txListA = database.commonBoxDao().getTransactionsForGroup(groupA).first()
        val txListB = database.commonBoxDao().getTransactionsForGroup(groupB).first()

        assertEquals(1, txListA.size)
        assertEquals(0, txListB.size)

        val incomeB = database.commonBoxDao().getTotalIncome(groupB).first() ?: 0.0
        assertEquals(0.0, incomeB, 0.001)
    }

    @Test
    fun testDeleteTransaction_restoresBalanceCorrectly() = runBlocking {
        val groupId = "group_del_test"
        database.commonBoxDao().insertGroup(HostelGroupEntity(groupId, "Del Hostel", "HST-DEL", createdBy = "Admin"))

        // Add ৳2,000 Income
        val txIncome = TransactionEntity("tx_inc", groupId, "m1", "Admin", TransactionType.INCOME.name, 2000.0, "CASH", "Deposit")
        database.commonBoxDao().insertTransaction(txIncome)

        // Add ৳500 Expense
        val txExp = TransactionEntity("tx_exp", groupId, "m1", "Admin", TransactionType.EXPENSE.name, 500.0, "FOOD", "Dinner")
        database.commonBoxDao().insertTransaction(txExp)

        var income = database.commonBoxDao().getTotalIncome(groupId).first() ?: 0.0
        var expense = database.commonBoxDao().getTotalExpense(groupId).first() ?: 0.0
        assertEquals(1500.0, income - expense, 0.001)

        // Delete expense of ৳500 -> balance should increase back to ৳2000
        database.commonBoxDao().deleteTransaction("tx_exp")
        expense = database.commonBoxDao().getTotalExpense(groupId).first() ?: 0.0
        assertEquals(0.0, expense, 0.001)
        assertEquals(2000.0, income - expense, 0.001)
    }

    @Test
    fun testSimulateDeviceBAction_multiUserSync() = runBlocking {
        val group = repository.createHostel("Room 204", "Tanvir").getOrThrow()

        // Device B performs action
        val result = repository.simulateDeviceBAction(
            groupId = group.groupId,
            memberName = "Roommate B",
            type = TransactionType.INCOME,
            amount = 3000.0,
            category = "CONTRIBUTION",
            description = "Cash from Device B"
        )

        assertTrue(result.isSuccess)

        val txs = repository.getTransactions(group.groupId).first()
        val simulatedTx = txs.find { it.memberName == "Roommate B" }

        assertNotNull(simulatedTx)
        assertEquals(3000.0, simulatedTx?.amount ?: 0.0, 0.001)
    }
}
