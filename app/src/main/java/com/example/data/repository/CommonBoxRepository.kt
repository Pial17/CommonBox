package com.example.data.repository

import android.content.Context
import com.example.data.local.CommonBoxDatabase
import com.example.data.local.dao.CommonBoxDao
import com.example.data.local.entities.CashCheckEntity
import com.example.data.local.entities.HostelGroupEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    val syncManager = CloudSyncManager(context, dao)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _currentGroupId = MutableStateFlow<String?>(null)
    val currentGroupId: StateFlow<String?> = _currentGroupId.asStateFlow()

    private val _activeMemberId = MutableStateFlow<String?>(null)
    val activeMemberId: StateFlow<String?> = _activeMemberId.asStateFlow()

    val allGroups: Flow<List<HostelGroupEntity>> = dao.getAllGroups()

    init {
        coroutineScope.launch {
            seedInitialDataIfNeeded()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val groups = dao.getAllGroups().firstOrNull() ?: emptyList()
        if (groups.isEmpty()) {
            val defaultGroupId = "group_hostel_default"
            val defaultGroup = HostelGroupEntity(
                groupId = defaultGroupId,
                groupName = "Green Horizon Mess",
                groupCode = "HST-7K92P",
                currencySymbol = "৳",
                currencyCode = "BDT",
                createdBy = "Pial",
                createdAt = System.currentTimeMillis() - (14L * 86400000L)
            )
            dao.insertGroup(defaultGroup)

            val pial = MemberEntity(
                memberId = "mem_pial",
                groupId = defaultGroupId,
                name = "Pial",
                role = "Admin",
                colorIndex = 0,
                joinedAt = System.currentTimeMillis() - (14L * 86400000L)
            )
            val rafi = MemberEntity(
                memberId = "mem_rafi",
                groupId = defaultGroupId,
                name = "Rafi",
                role = "Member",
                colorIndex = 1,
                joinedAt = System.currentTimeMillis() - (14L * 86400000L)
            )
            val sakib = MemberEntity(
                memberId = "mem_sakib",
                groupId = defaultGroupId,
                name = "Sakib",
                role = "Member",
                colorIndex = 2,
                joinedAt = System.currentTimeMillis() - (12L * 86400000L)
            )
            val tanvir = MemberEntity(
                memberId = "mem_tanvir",
                groupId = defaultGroupId,
                name = "Tanvir",
                role = "Member",
                colorIndex = 3,
                joinedAt = System.currentTimeMillis() - (10L * 86400000L)
            )
            dao.insertMembers(listOf(pial, rafi, sakib, tanvir))

            // Seed initial transactions to match prompt's figures:
            // Total Added = ৳20,000 | Total Spent = ৳16,650 | Balance = ৳3,350
            val now = System.currentTimeMillis()
            val hourMs = 3600000L

            val tx1 = TransactionEntity(
                transactionId = "tx_01",
                groupId = defaultGroupId,
                memberId = pial.memberId,
                memberName = pial.name,
                type = TransactionType.INCOME.name,
                amount = 10000.0,
                category = "CONTRIBUTION",
                description = "Monthly Cash Contribution",
                note = "Hostel shared fund deposit for September",
                createdAt = now - (10L * 86400000L)
            )
            val tx2 = TransactionEntity(
                transactionId = "tx_02",
                groupId = defaultGroupId,
                memberId = rafi.memberId,
                memberName = rafi.name,
                type = TransactionType.INCOME.name,
                amount = 10000.0,
                category = "CONTRIBUTION",
                description = "Monthly Cash Contribution",
                note = "Cash put directly in the hostel box",
                createdAt = now - (9L * 86400000L)
            )
            val tx3 = TransactionEntity(
                transactionId = "tx_03",
                groupId = defaultGroupId,
                memberId = pial.memberId,
                memberName = pial.name,
                type = TransactionType.EXPENSE.name,
                amount = 9550.0,
                category = ExpenseCategory.COOKING.id,
                description = "Monthly Mess Staples & Oil",
                note = "Cooking oil 5L, spices, salt, lentils, onion",
                createdAt = now - (5L * 86400000L)
            )
            val tx4 = TransactionEntity(
                transactionId = "tx_04",
                groupId = defaultGroupId,
                memberId = tanvir.memberId,
                memberName = tanvir.name,
                type = TransactionType.EXPENSE.name,
                amount = 4200.0,
                category = ExpenseCategory.UTILITIES.id,
                description = "Electricity & WiFi Bill",
                note = "Paid electricity prepaid token & broadband",
                createdAt = now - (3L * 86400000L)
            )
            val tx5 = TransactionEntity(
                transactionId = "tx_05",
                groupId = defaultGroupId,
                memberId = pial.memberId,
                memberName = pial.name,
                type = TransactionType.EXPENSE.name,
                amount = 1250.0,
                category = ExpenseCategory.RICE.id,
                description = "Miniket Rice 25kg sack",
                note = "From Karwan Bazar wholesaler",
                createdAt = now - (2L * 86400000L)
            )
            val tx6 = TransactionEntity(
                transactionId = "tx_06",
                groupId = defaultGroupId,
                memberId = sakib.memberId,
                memberName = sakib.name,
                type = TransactionType.EXPENSE.name,
                amount = 580.0,
                category = ExpenseCategory.FISH.id,
                description = "Rui Fish 1.5kg",
                note = "Fresh fish from evening market",
                createdAt = now - (1L * 86400000L) - (3 * hourMs)
            )
            val tx7 = TransactionEntity(
                transactionId = "tx_07",
                groupId = defaultGroupId,
                memberId = rafi.memberId,
                memberName = rafi.name,
                type = TransactionType.EXPENSE.name,
                amount = 320.0,
                category = ExpenseCategory.VEGETABLES.id,
                description = "Vegetables & Green Chilies",
                note = "Potato 3kg, tomatoes, coriander, green chili",
                createdAt = now - (4 * hourMs)
            )
            val tx8 = TransactionEntity(
                transactionId = "tx_08",
                groupId = defaultGroupId,
                memberId = pial.memberId,
                memberName = pial.name,
                type = TransactionType.EXPENSE.name,
                amount = 750.0,
                category = ExpenseCategory.GROCERY.id,
                description = "বাজার (Daily Bazaar)",
                note = "Fish, vegetables and eggs",
                createdAt = now - (45 * 60000L)
            )

            dao.insertTransaction(tx1)
            dao.insertTransaction(tx2)
            dao.insertTransaction(tx3)
            dao.insertTransaction(tx4)
            dao.insertTransaction(tx5)
            dao.insertTransaction(tx6)
            dao.insertTransaction(tx7)
            dao.insertTransaction(tx8)

            // Seed cash check audit
            val cashCheck = CashCheckEntity(
                checkId = "chk_01",
                groupId = defaultGroupId,
                memberId = rafi.memberId,
                memberName = rafi.name,
                appBalance = 3350.0,
                actualCash = 3300.0,
                difference = -50.0,
                note = "Counted notes and coins. Short by 50 taka (likely tea vendor small change).",
                createdAt = now - (2 * hourMs)
            )
            dao.insertCashCheck(cashCheck)

            _currentGroupId.value = defaultGroupId
            _activeMemberId.value = pial.memberId
        } else {
            if (_currentGroupId.value == null) {
                _currentGroupId.value = groups.first().groupId
            }
        }
    }

    fun setCurrentGroupId(groupId: String) {
        _currentGroupId.value = groupId
    }

    fun setActiveMemberId(memberId: String) {
        _activeMemberId.value = memberId
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
        val randomChars = (1..5)
            .map { "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".random() }
            .joinToString("")
        val code = "HST-$randomChars"
        val groupId = "group_" + UUID.randomUUID().toString().take(8)

        val group = HostelGroupEntity(
            groupId = groupId,
            groupName = trimmedName,
            groupCode = code,
            currencySymbol = "৳",
            currencyCode = "BDT",
            createdBy = trimmedCreator,
            createdAt = System.currentTimeMillis()
        )
        dao.insertGroup(group)

        val creator = MemberEntity(
            memberId = "mem_" + UUID.randomUUID().toString().take(8),
            groupId = groupId,
            name = trimmedCreator,
            role = "Admin",
            colorIndex = 0,
            joinedAt = System.currentTimeMillis()
        )
        dao.insertMember(creator)

        _currentGroupId.value = groupId
        _activeMemberId.value = creator.memberId

        Result.success(group)
    }

    suspend fun joinHostel(code: String, userName: String): Result<HostelGroupEntity> = withContext(Dispatchers.IO) {
        val trimmedCode = code.trim().uppercase(Locale.getDefault())
        val trimmedName = userName.trim()
        if (trimmedCode.isBlank() || trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code and member name are required"))
        }
        val group = dao.findGroupByCode(trimmedCode)
            ?: return@withContext Result.failure(IllegalArgumentException("No hostel found with code '$trimmedCode'"))

        // Check if member with same name already exists in this group
        val existingMembers = dao.getMembersForGroup(group.groupId).firstOrNull() ?: emptyList()
        val existingMember = existingMembers.find { it.name.equals(trimmedName, ignoreCase = true) }

        val activeMember = if (existingMember != null) {
            existingMember
        } else {
            val newMember = MemberEntity(
                memberId = "mem_" + UUID.randomUUID().toString().take(8),
                groupId = group.groupId,
                name = trimmedName,
                role = "Member",
                colorIndex = existingMembers.size % 8,
                joinedAt = System.currentTimeMillis()
            )
            dao.insertMember(newMember)
            newMember
        }

        _currentGroupId.value = group.groupId
        _activeMemberId.value = activeMember.memberId

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
        dao.deleteTransaction(transactionId)
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
        if (remaining.isNotEmpty()) {
            val nextGroup = remaining.first()
            _currentGroupId.value = nextGroup.groupId
            val nextMembers = dao.getMembersForGroup(nextGroup.groupId).firstOrNull() ?: emptyList()
            _activeMemberId.value = nextMembers.firstOrNull()?.memberId
        } else {
            // Seed a fresh hostel if all were removed
            createHostel("Hostel 402", "Pial")
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
