package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.CashCheckEntity
import com.example.data.local.entities.HostelGroupEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommonBoxDao {

    // --- Groups ---
    @Query("SELECT * FROM hostel_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<HostelGroupEntity>>

    @Query("SELECT * FROM hostel_groups WHERE groupId = :groupId LIMIT 1")
    fun getGroupById(groupId: String): Flow<HostelGroupEntity?>

    @Query("SELECT * FROM hostel_groups WHERE UPPER(groupCode) = UPPER(:groupCode) LIMIT 1")
    suspend fun findGroupByCode(groupCode: String): HostelGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: HostelGroupEntity)

    @Query("DELETE FROM hostel_groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    // --- Members ---
    @Query("SELECT * FROM hostel_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    fun getMembersForGroup(groupId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM hostel_members WHERE memberId = :memberId LIMIT 1")
    suspend fun getMemberById(memberId: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Query("DELETE FROM hostel_members WHERE memberId = :memberId")
    suspend fun deleteMember(memberId: String)

    // --- Transactions ---
    @Query("SELECT * FROM box_transactions WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getTransactionsForGroup(groupId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM box_transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(tx: TransactionEntity)

    @Query("DELETE FROM box_transactions WHERE transactionId = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM box_transactions WHERE groupId = :groupId")
    suspend fun deleteAllTransactionsForGroup(groupId: String)

    @Query("DELETE FROM hostel_members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: String)

    @Query("DELETE FROM cash_checks WHERE groupId = :groupId")
    suspend fun deleteCashChecksForGroup(groupId: String)

    @androidx.room.Transaction
    suspend fun deleteEntireGroupCascade(groupId: String) {
        deleteAllTransactionsForGroup(groupId)
        deleteMembersForGroup(groupId)
        deleteCashChecksForGroup(groupId)
        deleteGroup(groupId)
    }

    @Query("SELECT * FROM box_transactions WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPendingSyncTransactions(): List<TransactionEntity>

    @Query("UPDATE box_transactions SET syncStatus = :status WHERE transactionId = :txId")
    suspend fun updateTransactionSyncStatus(txId: String, status: String)

    @Query("SELECT SUM(amount) FROM box_transactions WHERE groupId = :groupId AND type = 'INCOME'")
    fun getTotalIncome(groupId: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM box_transactions WHERE groupId = :groupId AND type = 'EXPENSE'")
    fun getTotalExpense(groupId: String): Flow<Double?>

    // --- Cash Checks ---
    @Query("SELECT * FROM cash_checks WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getCashChecksForGroup(groupId: String): Flow<List<CashCheckEntity>>

    @Query("SELECT * FROM cash_checks WHERE groupId = :groupId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestCashCheck(groupId: String): Flow<CashCheckEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashCheck(check: CashCheckEntity)
}
