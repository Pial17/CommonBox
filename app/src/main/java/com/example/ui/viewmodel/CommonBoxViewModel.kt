package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.CashCheckEntity
import com.example.data.local.entities.HostelGroupEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TimeFilter
import com.example.data.model.TransactionType
import com.example.data.repository.CommonBoxRepository
import com.example.data.sync.SyncReport
import com.example.data.sync.SyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class CommonBoxViewModel(application: Application) : AndroidViewModel(application) {

    val repository = CommonBoxRepository(application)

    // Current Group & Active Member
    val allGroups: StateFlow<List<HostelGroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentGroupId: StateFlow<String?> = repository.currentGroupId
    val activeMemberId: StateFlow<String?> = repository.activeMemberId

    val currentGroup: StateFlow<HostelGroupEntity?> = currentGroupId.flatMapLatest { id ->
        if (id != null) repository.getGroup(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val members: StateFlow<List<MemberEntity>> = currentGroupId.flatMapLatest { id ->
        if (id != null) repository.getMembers(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMember: StateFlow<MemberEntity?> = combine(members, activeMemberId) { memberList, activeId ->
        memberList.find { it.memberId == activeId } ?: memberList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val rawTransactions: StateFlow<List<TransactionEntity>> = currentGroupId.flatMapLatest { id ->
        if (id != null) repository.getTransactions(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashChecks: StateFlow<List<CashCheckEntity>> = currentGroupId.flatMapLatest { id ->
        if (id != null) repository.getCashChecks(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestCashCheck: StateFlow<CashCheckEntity?> = currentGroupId.flatMapLatest { id ->
        if (id != null) repository.getLatestCashCheck(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Financial balance calculations
    val totalIncome: StateFlow<Double> = rawTransactions.flatMapLatest { list ->
        val sum = list.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        flowOf(sum)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = rawTransactions.flatMapLatest { list ->
        val sum = list.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        flowOf(sum)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val commonBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Filter & Search states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMemberFilter = MutableStateFlow<String?>(null) // null = all
    val selectedMemberFilter: StateFlow<String?> = _selectedMemberFilter.asStateFlow()

    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.ALL)
    val selectedTimeFilter: StateFlow<TimeFilter> = _selectedTimeFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null) // null = all
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    // Sync report from CloudSyncManager
    val syncReport: StateFlow<SyncReport> = repository.syncManager.syncReport
    val isNetworkOnline: StateFlow<Boolean> = repository.syncManager.isDeviceOnline

    // Dialog & Navigation states
    private val _isAddMoneyOpen = MutableStateFlow(false)
    val isAddMoneyOpen: StateFlow<Boolean> = _isAddMoneyOpen.asStateFlow()

    private val _isAddExpenseOpen = MutableStateFlow(false)
    val isAddExpenseOpen: StateFlow<Boolean> = _isAddExpenseOpen.asStateFlow()

    private val _isCashCheckOpen = MutableStateFlow(false)
    val isCashCheckOpen: StateFlow<Boolean> = _isCashCheckOpen.asStateFlow()

    private val _isGroupManagementOpen = MutableStateFlow(false)
    val isGroupManagementOpen: StateFlow<Boolean> = _isGroupManagementOpen.asStateFlow()

    private val _isAddMemberOpen = MutableStateFlow(false)
    val isAddMemberOpen: StateFlow<Boolean> = _isAddMemberOpen.asStateFlow()

    private val _selectedTransactionForDetail = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransactionForDetail: StateFlow<TransactionEntity?> = _selectedTransactionForDetail.asStateFlow()

    private val _transactionToEdit = MutableStateFlow<TransactionEntity?>(null)
    val transactionToEdit: StateFlow<TransactionEntity?> = _transactionToEdit.asStateFlow()

    private val _transactionToDelete = MutableStateFlow<TransactionEntity?>(null)
    val transactionToDelete: StateFlow<TransactionEntity?> = _transactionToDelete.asStateFlow()

    private val _selectedMemberForDetail = MutableStateFlow<MemberEntity?>(null)
    val selectedMemberForDetail: StateFlow<MemberEntity?> = _selectedMemberForDetail.asStateFlow()

    private val _groupToDelete = MutableStateFlow<HostelGroupEntity?>(null)
    val groupToDelete: StateFlow<HostelGroupEntity?> = _groupToDelete.asStateFlow()

    private val _memberToRemove = MutableStateFlow<MemberEntity?>(null)
    val memberToRemove: StateFlow<MemberEntity?> = _memberToRemove.asStateFlow()

    // Animated Success Feedback: (Title, Subtitle)
    private val _successFeedback = MutableStateFlow<Pair<String, String>?>(null)
    val successFeedback: StateFlow<Pair<String, String>?> = _successFeedback.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // Filtered transactions
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        rawTransactions,
        searchQuery,
        selectedMemberFilter,
        selectedTimeFilter,
        selectedTypeFilter
    ) { txList, query, memberFilter, timeFilter, typeFilter ->
        val nowCal = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        txList.filter { tx ->
            // Search query filter
            val matchesQuery = query.isBlank() ||
                    tx.description.contains(query, ignoreCase = true) ||
                    tx.memberName.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    (tx.note?.contains(query, ignoreCase = true) == true)

            // Member filter
            val matchesMember = memberFilter == null || tx.memberId == memberFilter

            // Type filter
            val matchesType = typeFilter == null || tx.type == typeFilter.name

            // Time filter
            val matchesTime = when (timeFilter) {
                TimeFilter.ALL -> true
                TimeFilter.TODAY -> tx.createdAt >= startOfToday
                TimeFilter.THIS_WEEK -> tx.createdAt >= startOfWeek
                TimeFilter.THIS_MONTH -> tx.createdAt >= startOfMonth
            }

            matchesQuery && matchesMember && matchesType && matchesTime
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filter setters ---
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedMemberFilter(memberId: String?) { _selectedMemberFilter.value = memberId }
    fun setSelectedTimeFilter(filter: TimeFilter) { _selectedTimeFilter.value = filter }
    fun setSelectedTypeFilter(type: TransactionType?) { _selectedTypeFilter.value = type }
    fun toggleNetworkOnline() {
        repository.syncManager.toggleOfflineMode()
    }

    fun syncNow() {
        viewModelScope.launch {
            repository.syncManager.syncPendingTransactions()
        }
    }

    // --- Dialog controls ---
    fun setAddMoneyOpen(open: Boolean) { _isAddMoneyOpen.value = open }
    fun setAddExpenseOpen(open: Boolean) { _isAddExpenseOpen.value = open }
    fun setCashCheckOpen(open: Boolean) { _isCashCheckOpen.value = open }
    fun setGroupManagementOpen(open: Boolean) { _isGroupManagementOpen.value = open }
    fun setAddMemberOpen(open: Boolean) { _isAddMemberOpen.value = open }
    fun setSelectedTransactionForDetail(tx: TransactionEntity?) { _selectedTransactionForDetail.value = tx }
    fun setTransactionToEdit(tx: TransactionEntity?) { _transactionToEdit.value = tx }
    fun requestDeleteTransaction(tx: TransactionEntity) { _transactionToDelete.value = tx }
    fun cancelDeleteTransaction() { _transactionToDelete.value = null }
    fun setSelectedMemberForDetail(member: MemberEntity?) { _selectedMemberForDetail.value = member }
    fun clearFeedback() { _successFeedback.value = null; _errorMessage.value = null }

    fun switchActiveMember(memberId: String) {
        repository.setActiveMemberId(memberId)
    }

    fun switchGroup(groupId: String) {
        repository.setCurrentGroupId(groupId)
    }

    // --- Core Actions ---
    fun submitAddMoney(amount: Double, memberId: String, note: String) {
        val groupId = currentGroupId.value ?: return
        if (amount <= 0.0) {
            _errorMessage.value = "Please enter a valid amount greater than 0"
            return
        }
        val member = members.value.find { it.memberId == memberId } ?: activeMember.value
        if (member == null) {
            _errorMessage.value = "Please select who added the money"
            return
        }

        if (_isSubmitting.value) return // Prevent accidental double submit
        _isSubmitting.value = true

        viewModelScope.launch {
            try {
                val result = repository.addMoney(
                    groupId = groupId,
                    memberId = member.memberId,
                    memberName = member.name,
                    amount = amount,
                    note = note
                )
                if (result.isSuccess) {
                    _isAddMoneyOpen.value = false
                    _successFeedback.value = Pair(
                        "Money Added ✓",
                        "৳${amount.toInt()} added by ${member.name}"
                    )
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to add money"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to add money"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun submitAddExpense(
        amount: Double,
        memberId: String,
        category: ExpenseCategory,
        description: String,
        note: String?,
        receiptUri: String?,
        allowNegative: Boolean = false
    ) {
        val groupId = currentGroupId.value ?: return
        if (amount <= 0.0) {
            _errorMessage.value = "Please enter a valid expense amount"
            return
        }
        val member = members.value.find { it.memberId == memberId } ?: activeMember.value
        if (member == null) {
            _errorMessage.value = "Please select who spent the money"
            return
        }

        val balanceAfterExpense = commonBalance.value - amount
        if (balanceAfterExpense < 0 && !allowNegative) {
            _errorMessage.value = "Insufficient balance! The cash box only has ৳${commonBalance.value.toInt()}."
            return
        }

        if (_isSubmitting.value) return
        _isSubmitting.value = true

        viewModelScope.launch {
            try {
                val result = repository.addExpense(
                    groupId = groupId,
                    memberId = member.memberId,
                    memberName = member.name,
                    amount = amount,
                    category = category,
                    description = description,
                    note = note,
                    receiptUri = receiptUri
                )
                if (result.isSuccess) {
                    _isAddExpenseOpen.value = false
                    _successFeedback.value = Pair(
                        "Expense Added ✓",
                        "৳${amount.toInt()} spent by ${member.name}"
                    )
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to submit expense"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to record expense"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun submitEditTransaction(
        txId: String,
        amount: Double,
        category: ExpenseCategory?,
        description: String,
        note: String?
    ) {
        val editorName = activeMember.value?.name ?: "Hostel Member"
        if (amount <= 0.0) {
            _errorMessage.value = "Amount must be greater than zero"
            return
        }

        viewModelScope.launch {
            try {
                val result = repository.editTransaction(
                    transactionId = txId,
                    newAmount = amount,
                    newCategory = category,
                    newDescription = description,
                    newNote = note,
                    editorName = editorName
                )
                if (result.isSuccess) {
                    _transactionToEdit.value = null
                    _selectedTransactionForDetail.value = result.getOrNull()
                    _successFeedback.value = Pair(
                        "Updated Successfully ✓",
                        "Transaction adjusted to ৳${amount.toInt()}"
                    )
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Edit failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not update transaction"
            }
        }
    }

    fun confirmDeleteTransaction() {
        val tx = _transactionToDelete.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteTransaction(tx.transactionId)
                _transactionToDelete.value = null
                _selectedTransactionForDetail.value = null
                val effectMsg = if (tx.type == TransactionType.EXPENSE.name) {
                    "৳${tx.amount.toInt()} returned to common cash balance"
                } else {
                    "৳${tx.amount.toInt()} removed from common cash balance"
                }
                _successFeedback.value = Pair("Transaction Deleted", effectMsg)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete transaction"
            }
        }
    }

    fun requestRemoveMember(member: MemberEntity) {
        _memberToRemove.value = member
    }

    fun cancelRemoveMember() {
        _memberToRemove.value = null
    }

    fun confirmRemoveMember() {
        val member = _memberToRemove.value ?: return
        viewModelScope.launch {
            try {
                repository.removeMember(member.memberId)
                _memberToRemove.value = null
                _selectedMemberForDetail.value = null
                _successFeedback.value = Pair("Member Removed", "${member.name} removed from the hostel")
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to remove member"
            }
        }
    }

    fun requestDeleteGroup(group: HostelGroupEntity) {
        _groupToDelete.value = group
    }

    fun cancelDeleteGroup() {
        _groupToDelete.value = null
    }

    fun confirmDeleteGroup() {
        val group = _groupToDelete.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteGroup(group.groupId)
                _groupToDelete.value = null
                _isGroupManagementOpen.value = false
                _successFeedback.value = Pair("Group Deleted", "${group.groupName} was deleted")
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete group"
            }
        }
    }

    fun simulateDeviceBAction(
        memberName: String,
        type: TransactionType,
        amount: Double,
        category: String,
        description: String
    ) {
        val groupId = currentGroupId.value ?: return
        viewModelScope.launch {
            try {
                val result = repository.simulateDeviceBAction(
                    groupId = groupId,
                    memberName = memberName,
                    type = type,
                    amount = amount,
                    category = category,
                    description = description
                )
                if (result.isSuccess) {
                    _successFeedback.value = Pair(
                        "Remote Update Synced 📲",
                        "$memberName recorded ${if (type == TransactionType.INCOME) "+৳" else "-৳"}${amount.toInt()} ($description)"
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Simulation failed: ${e.message}"
            }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            try {
                val result = repository.triggerManualSync()
                if (result.isSuccess) {
                    _successFeedback.value = Pair("Sync Complete 🟢", "All local cash records synchronized")
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Sync encountered an error"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sync failed: ${e.message}"
            }
        }
    }

    fun submitCashCheck(actualCash: Double, note: String?) {
        val groupId = currentGroupId.value ?: return
        val member = activeMember.value ?: members.value.firstOrNull() ?: return
        if (actualCash < 0.0) {
            _errorMessage.value = "Actual cash cannot be negative"
            return
        }

        viewModelScope.launch {
            try {
                val currentBal = commonBalance.value
                val result = repository.recordCashCheck(
                    groupId = groupId,
                    memberId = member.memberId,
                    memberName = member.name,
                    appBalance = currentBal,
                    actualCash = actualCash,
                    note = note
                )
                if (result.isSuccess) {
                    _isCashCheckOpen.value = false
                    val diff = actualCash - currentBal
                    val diffStr = when {
                        diff == 0.0 -> "Matched exact balance!"
                        diff > 0 -> "Extra +৳${diff.toInt()} in box"
                        else -> "Short by -৳${(-diff).toInt()} in box"
                    }
                    _successFeedback.value = Pair("Cash Box Audited ✓", diffStr)
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Cash check failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not record cash check"
            }
        }
    }

    fun createHostel(name: String, creatorName: String) {
        if (name.isBlank() || creatorName.isBlank()) {
            _errorMessage.value = "Please fill in both hostel and your name"
            return
        }
        viewModelScope.launch {
            try {
                val result = repository.createHostel(name, creatorName)
                if (result.isSuccess) {
                    val group = result.getOrNull()
                    _isGroupManagementOpen.value = false
                    _successFeedback.value = Pair("Hostel Created! 🎉", "Code: ${group?.groupCode}")
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to create hostel"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to create hostel"
            }
        }
    }

    fun joinHostel(code: String, userName: String) {
        if (code.isBlank() || userName.isBlank()) {
            _errorMessage.value = "Please enter both hostel code and your name"
            return
        }
        viewModelScope.launch {
            try {
                val result = repository.joinHostel(code, userName)
                if (result.isSuccess) {
                    val group = result.getOrNull()
                    _isGroupManagementOpen.value = false
                    _successFeedback.value = Pair("Joined Hostel! 🤝", "Welcome to ${group?.groupName}")
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to join hostel"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to join hostel"
            }
        }
    }

    fun addMemberToCurrentGroup(name: String, role: String) {
        val groupId = currentGroupId.value ?: return
        if (name.isBlank()) {
            _errorMessage.value = "Please enter the member's name"
            return
        }
        viewModelScope.launch {
            try {
                val result = repository.addMember(groupId, name, role)
                if (result.isSuccess) {
                    _isAddMemberOpen.value = false
                    _successFeedback.value = Pair("Member Added ✓", "$name is now part of the hostel")
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to add member"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to add member"
            }
        }
    }

    fun exportTransactionsCsv(context: Context) {
        val groupId = currentGroupId.value ?: return
        val group = currentGroup.value ?: return
        viewModelScope.launch {
            try {
                val csvContent = repository.exportTransactionsCsv(groupId)
                val fileName = "CommonBox_${group.groupName.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
                val file = File(context.cacheDir, fileName)
                file.writeText(csvContent)

                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "${group.groupName} - CommonBox Transactions")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(sendIntent, "Share CommonBox CSV")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                // Fallback: simple text share if file provider is not declared
                try {
                    val csvContent = repository.exportTransactionsCsv(groupId)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, csvContent)
                        putExtra(Intent.EXTRA_SUBJECT, "${group.groupName} - CommonBox Transactions CSV")
                    }
                    val chooser = Intent.createChooser(sendIntent, "Share CommonBox CSV")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (ex: Exception) {
                    _errorMessage.value = "Export failed: ${ex.message}"
                }
            }
        }
    }
}
