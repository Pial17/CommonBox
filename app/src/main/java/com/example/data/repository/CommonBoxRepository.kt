package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.CommonBoxDatabase
import com.example.data.local.dao.CommonBoxDao
import com.example.data.local.entities.CashCheckEntity
import com.example.data.local.entities.HostelGroupEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.data.remote.FirebaseAuthManager
import com.example.data.remote.FirestoreHostelService
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CommonBoxRepository(
    private val dao: CommonBoxDao,
    context: Context
) {
    constructor(context: Context) : this(
        CommonBoxDatabase.getDatabase(context).commonBoxDao(),
        context
    )

    companion object {
        private const val TAG = "CommonBoxRepository"
        private const val PREFS_NAME = "common_box_prefs"
        private const val KEY_SAVED_GROUP_ID = "saved_hostel_group_id"
        private const val KEY_SAVED_MEMBER_ID = "saved_hostel_member_id"
        private const val LEGACY_DEMO_GROUP_ID = "group_hostel_default"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val authManager = FirebaseAuthManager(context)
    val firestoreService = FirestoreHostelService(context)
    val syncManager = CloudSyncManager(context, dao, firestoreService)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var realtimeListenerCleanup: (() -> Unit)? = null

    private val _currentGroupId = MutableStateFlow<String?>(null)
    val currentGroupId: StateFlow<String?> = _currentGroupId.asStateFlow()

    private val _activeMemberId = MutableStateFlow<String?>(null)
    val activeMemberId: StateFlow<String?> = _activeMemberId.asStateFlow()

    private val _isStartupChecked = MutableStateFlow(false)
    val isStartupChecked: StateFlow<Boolean> = _isStartupChecked.asStateFlow()

    val allGroups: Flow<List<HostelGroupEntity>> = dao.getAllGroups()

    init {
        coroutineScope.launch {
            checkStartupSession()
        }
    }

    private fun attachCloudRealtimeSync(groupId: String) {
        realtimeListenerCleanup?.invoke()
        realtimeListenerCleanup = firestoreService.attachRealtimeListeners(
            groupId = groupId,
            onTransactionsChanged = { remoteTxs ->
                coroutineScope.launch {
                    try {
                        for (remoteTx in remoteTxs) {
                            val local = dao.getTransactionById(remoteTx.transactionId)
                            if (local == null) {
                                dao.insertTransaction(remoteTx.copy(syncStatus = "SYNCED"))
                            } else if (remoteTx.updatedAt >= local.updatedAt) {
                                dao.updateTransaction(remoteTx.copy(syncStatus = "SYNCED"))
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error applying remote transactions: ${e.message}")
                    }
                }
            },
            onMembersChanged = { remoteMembers ->
                coroutineScope.launch {
                    try {
                        for (remoteMem in remoteMembers) {
                            val local = dao.getMemberById(remoteMem.memberId)
                            if (local == null) {
                                dao.insertMember(remoteMem)
                            } else {
                                dao.updateMember(remoteMem)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error applying remote members: ${e.message}")
                    }
                }
            }
        )
    }

    private suspend fun checkStartupSession() {
        try {
            // 1. Completely remove legacy demo hostel if present from previous builds
            val legacyGroup = dao.getGroupById(LEGACY_DEMO_GROUP_ID).firstOrNull()
            if (legacyGroup != null) {
                dao.deleteEntireGroupCascade(LEGACY_DEMO_GROUP_ID)
                val currentSaved = prefs.getString(KEY_SAVED_GROUP_ID, null)
                if (currentSaved == LEGACY_DEMO_GROUP_ID) {
                    prefs.edit().remove(KEY_SAVED_GROUP_ID).remove(KEY_SAVED_MEMBER_ID).apply()
                }
            }

            // 2. Check saved persistent membership
            val savedGroupId = prefs.getString(KEY_SAVED_GROUP_ID, null)
            val savedMemberId = prefs.getString(KEY_SAVED_MEMBER_ID, null)

            if (!savedGroupId.isNullOrBlank() && savedGroupId != LEGACY_DEMO_GROUP_ID) {
                val existingGroup = dao.getGroupById(savedGroupId).firstOrNull()
                if (existingGroup != null) {
                    _currentGroupId.value = savedGroupId
                    if (!savedMemberId.isNullOrBlank()) {
                        val member = dao.getMemberById(savedMemberId)
                        if (member != null && member.groupId == savedGroupId) {
                            _activeMemberId.value = savedMemberId
                        } else {
                            val groupMembers = dao.getMembersForGroup(savedGroupId).firstOrNull() ?: emptyList()
                            _activeMemberId.value = groupMembers.firstOrNull()?.memberId
                        }
                    } else {
                        val groupMembers = dao.getMembersForGroup(savedGroupId).firstOrNull() ?: emptyList()
                        _activeMemberId.value = groupMembers.firstOrNull()?.memberId
                    }
                    attachCloudRealtimeSync(savedGroupId)

                    // Background refresh from Firestore
                    coroutineScope.launch {
                        try {
                            val cloudMembers = firestoreService.fetchCloudMembers(savedGroupId).getOrNull()
                            if (cloudMembers != null) {
                                for (m in cloudMembers) {
                                    val existing = dao.getMemberById(m.memberId)
                                    if (existing == null) dao.insertMember(m) else dao.updateMember(m)
                                }
                            }
                            val cloudTxs = firestoreService.fetchCloudTransactions(savedGroupId).getOrNull()
                            if (cloudTxs != null) {
                                for (tx in cloudTxs) {
                                    val existing = dao.getTransactionById(tx.transactionId)
                                    if (existing == null) dao.insertTransaction(tx.copy(syncStatus = "SYNCED"))
                                    else if (tx.updatedAt >= existing.updatedAt) dao.updateTransaction(tx.copy(syncStatus = "SYNCED"))
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Background cloud refresh: ${e.message}")
                        }
                    }
                } else {
                    // Stored group was deleted. Check if user belongs to any other group
                    val allLocalGroups = dao.getAllGroups().firstOrNull() ?: emptyList()
                    val validGroup = allLocalGroups.firstOrNull { it.groupId != LEGACY_DEMO_GROUP_ID }
                    if (validGroup != null) {
                        _currentGroupId.value = validGroup.groupId
                        prefs.edit().putString(KEY_SAVED_GROUP_ID, validGroup.groupId).apply()
                        val groupMembers = dao.getMembersForGroup(validGroup.groupId).firstOrNull() ?: emptyList()
                        val firstMember = groupMembers.firstOrNull()
                        _activeMemberId.value = firstMember?.memberId
                        if (firstMember != null) {
                            prefs.edit().putString(KEY_SAVED_MEMBER_ID, firstMember.memberId).apply()
                        }
                        attachCloudRealtimeSync(validGroup.groupId)
                    } else {
                        _currentGroupId.value = null
                        _activeMemberId.value = null
                        prefs.edit().remove(KEY_SAVED_GROUP_ID).remove(KEY_SAVED_MEMBER_ID).apply()
                    }
                }
            } else {
                // No saved group in preferences. Check if any real groups exist in Room database
                val allLocalGroups = dao.getAllGroups().firstOrNull() ?: emptyList()
                val validGroup = allLocalGroups.firstOrNull { it.groupId != LEGACY_DEMO_GROUP_ID }
                if (validGroup != null) {
                    _currentGroupId.value = validGroup.groupId
                    prefs.edit().putString(KEY_SAVED_GROUP_ID, validGroup.groupId).apply()
                    val groupMembers = dao.getMembersForGroup(validGroup.groupId).firstOrNull() ?: emptyList()
                    val firstMember = groupMembers.firstOrNull()
                    _activeMemberId.value = firstMember?.memberId
                    if (firstMember != null) {
                        prefs.edit().putString(KEY_SAVED_MEMBER_ID, firstMember.memberId).apply()
                    }
                    attachCloudRealtimeSync(validGroup.groupId)
                } else {
                    _currentGroupId.value = null
                    _activeMemberId.value = null
                    prefs.edit().remove(KEY_SAVED_GROUP_ID).remove(KEY_SAVED_MEMBER_ID).apply()
                }
            }
        } catch (e: Exception) {
            _currentGroupId.value = null
            _activeMemberId.value = null
        } finally {
            // Brief settling delay to ensure a polished transition from "Loading CommonBox..."
            delay(350)
            _isStartupChecked.value = true
        }
    }

    fun setCurrentGroupId(groupId: String) {
        _currentGroupId.value = groupId
        prefs.edit().putString(KEY_SAVED_GROUP_ID, groupId).apply()
        attachCloudRealtimeSync(groupId)
    }

    fun setActiveMemberId(memberId: String) {
        _activeMemberId.value = memberId
        prefs.edit().putString(KEY_SAVED_MEMBER_ID, memberId).apply()
    }

    fun getGroup(groupId: String): Flow<HostelGroupEntity?> = dao.getGroupById(groupId)

    fun getMembers(groupId: String): Flow<List<MemberEntity>> = dao.getMembersForGroup(groupId)

    fun getTransactions(groupId: String): Flow<List<TransactionEntity>> = dao.getTransactionsForGroup(groupId)

    fun getCashChecks(groupId: String): Flow<List<CashCheckEntity>> = dao.getCashChecksForGroup(groupId)

    fun getLatestCashCheck(groupId: String): Flow<CashCheckEntity?> = dao.getLatestCashCheck(groupId)

    suspend fun createHostel(name: String, creatorName: String): Result<HostelGroupEntity> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedCreator = creatorName.trim()
        if (trimmedName.isBlank() || trimmedCreator.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Hostel and creator names cannot be empty"))
        }

        if (!syncManager.isEffectiveOnline()) {
            return@withContext Result.failure(
                IllegalStateException("Internet connection required to create or join a hostel.")
            )
        }

        // 1. Provision stable authenticated user ID
        val userId = authManager.getOrProvisionUserId(trimmedCreator)

        // 2. Create hostel in Firestore cloud
        val cloudResult = firestoreService.createHostelInCloud(trimmedName, trimmedCreator, userId)
        if (cloudResult.isFailure) {
            return@withContext cloudResult
        }
        val group = cloudResult.getOrThrow()

        // 3. Register creator member entity
        val creator = MemberEntity(
            memberId = userId,
            groupId = group.groupId,
            name = trimmedCreator,
            role = "Admin",
            colorIndex = 0,
            joinedAt = System.currentTimeMillis()
        )

        // 4. Save into local Room database cache
        dao.insertGroup(group)
        dao.insertMember(creator)

        _currentGroupId.value = group.groupId
        _activeMemberId.value = creator.memberId

        prefs.edit()
            .putString(KEY_SAVED_GROUP_ID, group.groupId)
            .putString(KEY_SAVED_MEMBER_ID, creator.memberId)
            .apply()

        attachCloudRealtimeSync(group.groupId)

        Result.success(group)
    }

    suspend fun joinHostel(code: String, userName: String): Result<HostelGroupEntity> = withContext(Dispatchers.IO) {
        val trimmedCode = code.trim().uppercase(Locale.getDefault())
        val trimmedName = userName.trim()
        if (trimmedCode.isBlank() || trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code and member name are required"))
        }

        if (!syncManager.isEffectiveOnline()) {
            return@withContext Result.failure(
                IllegalStateException("Internet connection required to create or join a hostel.")
            )
        }

        // 1. Validate join code against Firestore backend
        val hostelResult = firestoreService.lookupHostelByJoinCode(trimmedCode)
        if (hostelResult.isFailure) {
            return@withContext hostelResult
        }
        val group = hostelResult.getOrThrow()

        // 2. Provision stable authenticated user ID
        val userId = authManager.getOrProvisionUserId(trimmedName)

        // 3. Register user as a member in Firestore
        val memberResult = firestoreService.registerMemberInCloud(group.groupId, trimmedName, userId)
        if (memberResult.isFailure) {
            return@withContext Result.failure(
                memberResult.exceptionOrNull() ?: Exception("Something went wrong while joining the hostel.")
            )
        }
        val activeMember = memberResult.getOrThrow()

        // 4. Fetch all existing shared members and transactions from Firestore
        val remoteMembers = firestoreService.fetchCloudMembers(group.groupId).getOrNull() ?: listOf(activeMember)
        val remoteTransactions = firestoreService.fetchCloudTransactions(group.groupId).getOrNull() ?: emptyList()

        // 5. Populate local Room database (cache)
        dao.insertGroup(group)
        for (m in remoteMembers) {
            val existing = dao.getMemberById(m.memberId)
            if (existing == null) {
                dao.insertMember(m)
            } else {
                dao.updateMember(m)
            }
        }
        for (tx in remoteTransactions) {
            val existing = dao.getTransactionById(tx.transactionId)
            if (existing == null) {
                dao.insertTransaction(tx.copy(syncStatus = "SYNCED"))
            } else {
                dao.updateTransaction(tx.copy(syncStatus = "SYNCED"))
            }
        }

        _currentGroupId.value = group.groupId
        _activeMemberId.value = activeMember.memberId

        prefs.edit()
            .putString(KEY_SAVED_GROUP_ID, group.groupId)
            .putString(KEY_SAVED_MEMBER_ID, activeMember.memberId)
            .apply()

        attachCloudRealtimeSync(group.groupId)

        Result.success(group)
    }

    suspend fun addMember(groupId: String, name: String, role: String = "Member"): Result<MemberEntity> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Member name cannot be blank"))
        }
        val existingMembers = dao.getMembersForGroup(groupId).firstOrNull() ?: emptyList()
        if (existingMembers.any { it.name.equals(trimmedName, ignoreCase = true) }) {
            return@withContext Result.failure(IllegalArgumentException("A member named '$trimmedName' already exists"))
        }
        val newMember = MemberEntity(
            memberId = "mem_" + UUID.randomUUID().toString().take(8),
            groupId = groupId,
            name = trimmedName,
            role = role,
            colorIndex = existingMembers.size % 8,
            joinedAt = System.currentTimeMillis()
        )
        dao.insertMember(newMember)
        Result.success(newMember)
    }

    suspend fun addMoney(
        groupId: String,
        memberId: String,
        memberName: String,
        amount: Double,
        note: String
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        val cleanAmount = kotlin.math.round(amount * 100.0) / 100.0
        if (cleanAmount <= 0.0) {
            return@withContext Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        val isOnline = syncManager.isEffectiveOnline()
        val tx = TransactionEntity(
            transactionId = "tx_" + UUID.randomUUID().toString(),
            groupId = groupId,
            memberId = memberId,
            memberName = memberName,
            type = TransactionType.INCOME.name,
            amount = cleanAmount,
            category = "CONTRIBUTION",
            description = "Cash Added to Box",
            note = note.trim().ifBlank { "Added by $memberName" },
            receiptUri = null,
            syncStatus = if (isOnline) "SYNCED" else "PENDING",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertTransaction(tx)
        syncManager.onTransactionCreated(tx)
        Result.success(tx)
    }

    suspend fun addExpense(
        groupId: String,
        memberId: String,
        memberName: String,
        amount: Double,
        category: ExpenseCategory,
        description: String,
        note: String?,
        receiptUri: String?
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        val cleanAmount = kotlin.math.round(amount * 100.0) / 100.0
        if (cleanAmount <= 0.0) {
            return@withContext Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        val desc = description.trim().ifBlank { "${category.emoji} ${category.displayName}" }
        val isOnline = syncManager.isEffectiveOnline()

        val tx = TransactionEntity(
            transactionId = "tx_" + UUID.randomUUID().toString(),
            groupId = groupId,
            memberId = memberId,
            memberName = memberName,
            type = TransactionType.EXPENSE.name,
            amount = cleanAmount,
            category = category.id,
            description = desc,
            note = note?.trim()?.takeIf { it.isNotBlank() },
            receiptUri = receiptUri,
            syncStatus = if (isOnline) "SYNCED" else "PENDING",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertTransaction(tx)
        syncManager.onTransactionCreated(tx)
        Result.success(tx)
    }

    suspend fun editTransaction(
        transactionId: String,
        newAmount: Double,
        newCategory: ExpenseCategory?,
        newDescription: String,
        newNote: String?,
        editorName: String
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getTransactionById(transactionId)
            ?: return@withContext Result.failure(IllegalArgumentException("Transaction not found"))

        val cleanAmount = kotlin.math.round(newAmount * 100.0) / 100.0
        if (cleanAmount <= 0.0) {
            return@withContext Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val editRecord = "$editorName changed amount ৳${existing.amount.toInt()} → ৳${cleanAmount.toInt()} • Edited at ${timeFormat.format(Date())}"
        val updatedHistory = if (existing.editedHistory.isNullOrBlank()) {
            editRecord
        } else {
            "${existing.editedHistory}\n$editRecord"
        }

        val updated = existing.copy(
            amount = cleanAmount,
            category = newCategory?.id ?: existing.category,
            description = newDescription.trim().ifBlank { existing.description },
            note = newNote?.trim()?.ifBlank { null } ?: existing.note,
            updatedAt = System.currentTimeMillis(),
            editedHistory = updatedHistory,
            syncStatus = if (syncManager.isEffectiveOnline()) "SYNCED" else "PENDING"
        )
        dao.updateTransaction(updated)
        syncManager.onTransactionCreated(updated)
        Result.success(updated)
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val groupId = _currentGroupId.value
        dao.deleteTransaction(transactionId)
        if (!groupId.isNullOrBlank()) {
            try {
                firestoreService.deleteCloudTransaction(groupId, transactionId)
            } catch (e: Exception) {
                Log.w(TAG, "Cloud transaction deletion warning: ${e.message}")
            }
        }
        Result.success(Unit)
    }

    suspend fun removeMember(memberId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentActive = _activeMemberId.value
        dao.deleteMember(memberId)
        // If active member was removed, fallback to next available member in current group
        if (currentActive == memberId) {
            val groupId = _currentGroupId.value
            if (groupId != null) {
                val remaining = dao.getMembersForGroup(groupId).firstOrNull() ?: emptyList()
                _activeMemberId.value = remaining.firstOrNull()?.memberId
            }
        }
        Result.success(Unit)
    }

    suspend fun deleteGroup(groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        dao.deleteEntireGroupCascade(groupId)
        val remaining = dao.getAllGroups().firstOrNull() ?: emptyList()
        val validRemaining = remaining.filter { it.groupId != LEGACY_DEMO_GROUP_ID }
        if (validRemaining.isNotEmpty()) {
            val nextGroup = validRemaining.first()
            _currentGroupId.value = nextGroup.groupId
            val nextMembers = dao.getMembersForGroup(nextGroup.groupId).firstOrNull() ?: emptyList()
            val nextMemberId = nextMembers.firstOrNull()?.memberId
            _activeMemberId.value = nextMemberId
            prefs.edit().putString(KEY_SAVED_GROUP_ID, nextGroup.groupId).apply()
            if (nextMemberId != null) {
                prefs.edit().putString(KEY_SAVED_MEMBER_ID, nextMemberId).apply()
            } else {
                prefs.edit().remove(KEY_SAVED_MEMBER_ID).apply()
            }
        } else {
            // Clean state - no fallback demo hostel
            _currentGroupId.value = null
            _activeMemberId.value = null
            prefs.edit().remove(KEY_SAVED_GROUP_ID).remove(KEY_SAVED_MEMBER_ID).apply()
        }
        Result.success(Unit)
    }

    /**
     * Simulates Device B (Roommate on another device) creating a transaction and broadcasting to cloud.
     * Section 30 requirement: "Device A Pial adds Expense. Device B Rahim adds Money. Verify real-time update."
     */
    suspend fun simulateDeviceBAction(
        groupId: String,
        memberName: String,
        type: TransactionType,
        amount: Double,
        category: String,
        description: String
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        val member = dao.getMembersForGroup(groupId).firstOrNull()?.find { it.name.equals(memberName, ignoreCase = true) }
        val memberId = member?.memberId ?: "mem_remote_${UUID.randomUUID().toString().take(6)}"
        val cleanAmount = kotlin.math.round(amount * 100.0) / 100.0
        val tx = TransactionEntity(
            transactionId = "tx_remote_" + UUID.randomUUID().toString().take(8),
            groupId = groupId,
            memberId = memberId,
            memberName = memberName,
            type = type.name,
            amount = cleanAmount,
            category = category,
            description = description,
            note = "Synced from Device B ($memberName)",
            syncStatus = "SYNCED",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertTransaction(tx)
        try {
            firestoreService.saveCloudTransaction(tx)
        } catch (e: Exception) {
            Log.w(TAG, "Device B action cloud save warning: ${e.message}")
        }
        syncManager.simulateRemoteTransactionReceived(tx)
        Result.success(tx)
    }

    suspend fun recordCashCheck(
        groupId: String,
        memberId: String,
        memberName: String,
        appBalance: Double,
        actualCash: Double,
        note: String?
    ): Result<CashCheckEntity> = withContext(Dispatchers.IO) {
        val diff = actualCash - appBalance
        val check = CashCheckEntity(
            checkId = "chk_" + UUID.randomUUID().toString().take(8),
            groupId = groupId,
            memberId = memberId,
            memberName = memberName,
            appBalance = appBalance,
            actualCash = actualCash,
            difference = diff,
            note = note?.trim()?.takeIf { it.isNotBlank() },
            createdAt = System.currentTimeMillis()
        )
        dao.insertCashCheck(check)
        Result.success(check)
    }

    suspend fun triggerManualSync(): Result<Int> = syncManager.syncPendingTransactions()

    suspend fun exportTransactionsCsv(groupId: String): String = withContext(Dispatchers.IO) {
        val txList = dao.getTransactionsForGroup(groupId).firstOrNull() ?: emptyList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()

        // Section 18 exact required columns: Date, Time, Type, Amount, Category, Description, Member, Transaction ID
        sb.append("Date,Time,Type,Amount,Category,Description,Member,Transaction ID,Note\n")

        var totalIncome = 0.0
        var totalExpense = 0.0

        for (tx in txList) {
            val dateStr = dateFormat.format(Date(tx.createdAt))
            val timeStr = timeFormat.format(Date(tx.createdAt))
            val cleanDesc = tx.description.replace(",", ";").replace("\"", "'")
            val cleanNote = (tx.note ?: "").replace(",", ";").replace("\"", "'")
            sb.append("\"$dateStr\",\"$timeStr\",\"${tx.type}\",${tx.amount},\"${tx.category}\",\"$cleanDesc\",\"${tx.memberName}\",\"${tx.transactionId}\",\"$cleanNote\"\n")

            if (tx.type == TransactionType.INCOME.name) {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
            }
        }

        // Summary reconciliation rows (Section 18 & 19)
        val closingBalance = totalIncome - totalExpense
        sb.append("\n\"--- AUDIT SUMMARY ---\"\n")
        sb.append("\"Total Money Added\",$totalIncome\n")
        sb.append("\"Total Expenses\",$totalExpense\n")
        sb.append("\"Closing Cash Box Balance\",$closingBalance\n")
        sb.append("\"Total Records\",${txList.size}\n")

        sb.toString()
    }
}
