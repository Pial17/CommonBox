package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SuccessConfirmationDialog
import com.example.ui.screens.AddExpenseSheet
import com.example.ui.screens.AddMemberDialog
import com.example.ui.screens.AddMoneyDialog
import com.example.ui.screens.CashCheckDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeleteConfirmationDialog
import com.example.ui.screens.EditTransactionDialog
import com.example.ui.screens.GroupManagementDialog
import com.example.ui.screens.LoadingSplashScreen
import com.example.ui.screens.MemberDetailDialog
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.OnboardingHostelScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.screens.TransactionDetailSheet
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.UserSwitcherDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.CommonBoxViewModel

enum class NavigationTab(val label: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.AccountBalanceWallet, "tab_dashboard"),
    TRANSACTIONS("Transactions", Icons.Default.ReceiptLong, "tab_transactions"),
    MEMBERS("Friends", Icons.Default.Group, "tab_members"),
    SUMMARY("Summary", Icons.Default.BarChart, "tab_summary"),
    SETTINGS("Settings", Icons.Default.Settings, "tab_settings")
}

@Composable
fun CommonBoxApp(
    viewModel: CommonBoxViewModel = viewModel()
) {
    val currentGroup by viewModel.currentGroup.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val members by viewModel.members.collectAsState()
    val activeMember by viewModel.activeMember.collectAsState()
    val rawTransactions by viewModel.rawTransactions.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val cashChecks by viewModel.cashChecks.collectAsState()
    val latestCashCheck by viewModel.latestCashCheck.collectAsState()

    val commonBalance by viewModel.commonBalance.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMemberFilter by viewModel.selectedMemberFilter.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedTimeFilter by viewModel.selectedTimeFilter.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsState()
    val customDateRange by viewModel.customDateRange.collectAsState()
    val isFilterActive by viewModel.isFilterActive.collectAsState()
    val isNetworkOnline by viewModel.isNetworkOnline.collectAsState()
    val syncReport by viewModel.syncReport.collectAsState()

    val isAddMoneyOpen by viewModel.isAddMoneyOpen.collectAsState()
    val isAddExpenseOpen by viewModel.isAddExpenseOpen.collectAsState()
    val isCashCheckOpen by viewModel.isCashCheckOpen.collectAsState()
    val isGroupManagementOpen by viewModel.isGroupManagementOpen.collectAsState()
    val isAddMemberOpen by viewModel.isAddMemberOpen.collectAsState()
    val selectedTransactionForDetail by viewModel.selectedTransactionForDetail.collectAsState()
    val transactionToEdit by viewModel.transactionToEdit.collectAsState()
    val transactionToDelete by viewModel.transactionToDelete.collectAsState()
    val selectedMemberForDetail by viewModel.selectedMemberForDetail.collectAsState()

    val successFeedback by viewModel.successFeedback.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isStartupChecked by viewModel.isStartupChecked.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var isUserSwitcherOpen by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val currency = currentGroup?.currencySymbol ?: "৳"

    if (!isStartupChecked) {
        LoadingSplashScreen()
    } else if (currentGroup == null) {
        OnboardingHostelScreen(
            onCreateHostel = { name, creator -> viewModel.createHostel(name, creator) },
            onJoinHostel = { code, userName -> viewModel.joinHostel(code, userName) },
            errorMessage = errorMessage,
            onClearError = { viewModel.clearFeedback() },
            isSubmitting = isSubmitting
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationTab.entries.forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = EmeraldPrimary.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            when (NavigationTab.entries[selectedTabIndex]) {
                NavigationTab.DASHBOARD -> {
                    DashboardScreen(
                        currentGroup = currentGroup,
                        members = members,
                        activeMember = activeMember,
                        commonBalance = commonBalance,
                        totalAdded = totalIncome,
                        totalSpent = totalExpense,
                        recentTransactions = rawTransactions,
                        latestCashCheck = latestCashCheck,
                        isNetworkOnline = isNetworkOnline,
                        syncReport = syncReport,
                        onSyncClick = { viewModel.triggerManualSync() },
                        onAddMoneyClick = { viewModel.setAddMoneyOpen(true) },
                        onAddExpenseClick = { viewModel.setAddExpenseOpen(true) },
                        onCashCheckClick = { viewModel.setCashCheckOpen(true) },
                        onTransactionClick = { tx -> viewModel.setSelectedTransactionForDetail(tx) },
                        onViewAllTransactionsClick = { selectedTabIndex = NavigationTab.TRANSACTIONS.ordinal },
                        onSwitchUserClick = { isUserSwitcherOpen = true },
                        onGroupClick = { viewModel.setGroupManagementOpen(true) }
                    )
                }

                NavigationTab.TRANSACTIONS -> {
                    TransactionsScreen(
                        transactions = filteredTransactions,
                        members = members,
                        searchQuery = searchQuery,
                        selectedMemberFilter = selectedMemberFilter,
                        selectedCategoryFilter = selectedCategoryFilter,
                        selectedTimeFilter = selectedTimeFilter,
                        selectedTypeFilter = selectedTypeFilter,
                        selectedSortOrder = selectedSortOrder,
                        customDateRange = customDateRange,
                        isFilterActive = isFilterActive,
                        currencySymbol = currency,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onMemberFilterSelect = { viewModel.setSelectedMemberFilter(it) },
                        onCategoryFilterSelect = { viewModel.setSelectedCategoryFilter(it) },
                        onTimeFilterSelect = { viewModel.setSelectedTimeFilter(it) },
                        onTypeFilterSelect = { viewModel.setSelectedTypeFilter(it) },
                        onSortOrderSelect = { viewModel.setSelectedSortOrder(it) },
                        onCustomDateRangeSelect = { start, end -> viewModel.setCustomDateRange(start, end) },
                        onClearFilters = { viewModel.clearAllFilters() },
                        onTransactionClick = { tx -> viewModel.setSelectedTransactionForDetail(tx) }
                    )
                }

                NavigationTab.MEMBERS -> {
                    MembersScreen(
                        members = members,
                        activeMember = activeMember,
                        transactions = rawTransactions,
                        currencySymbol = currency,
                        onMemberClick = { member -> viewModel.setSelectedMemberForDetail(member) },
                        onAddMemberClick = { viewModel.setAddMemberOpen(true) },
                        onSetActiveMember = { id -> viewModel.switchActiveMember(id) }
                    )
                }

                NavigationTab.SUMMARY -> {
                    SummaryScreen(
                        members = members,
                        transactions = rawTransactions,
                        cashChecks = cashChecks,
                        groupName = currentGroup?.groupName ?: "Hostel Box",
                        currencySymbol = currency
                    )
                }

                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        currentGroup = currentGroup,
                        activeMember = activeMember,
                        members = members,
                        allGroups = allGroups,
                        isNetworkOnline = isNetworkOnline,
                        syncReport = syncReport,
                        onSyncClick = { viewModel.triggerManualSync() },
                        onSimulateDeviceBAction = { name, type, amount, cat, desc ->
                            viewModel.simulateDeviceBAction(name, type, amount, cat, desc)
                        },
                        onToggleNetwork = { viewModel.toggleNetworkOnline() },
                        onOpenGroupManagement = { viewModel.setGroupManagementOpen(true) },
                        onOpenAddMember = { viewModel.setAddMemberOpen(true) },
                        onSwitchUser = { isUserSwitcherOpen = true },
                        onExportCsv = { context -> viewModel.exportTransactionsCsv(context) }
                    )
                }
            }
        }
    }
}

    // --- Interactive Dialogs & Overlays ---

    // Add Money Dialog
    if (isAddMoneyOpen) {
        AddMoneyDialog(
            members = members,
            activeMember = activeMember,
            currencySymbol = currency,
            isSubmitting = isSubmitting,
            onDismiss = { viewModel.setAddMoneyOpen(false) },
            onSubmit = { amount, memberId, note ->
                viewModel.submitAddMoney(amount, memberId, note)
            }
        )
    }

    // Add Expense Sheet
    if (isAddExpenseOpen) {
        AddExpenseSheet(
            members = members,
            activeMember = activeMember,
            currentBalance = commonBalance,
            currencySymbol = currency,
            isSubmitting = isSubmitting,
            onDismiss = { viewModel.setAddExpenseOpen(false) },
            onSubmit = { amount, memberId, category, description, note, receiptUri, allowNegative ->
                viewModel.submitAddExpense(
                    amount = amount,
                    memberId = memberId,
                    category = category,
                    description = description,
                    note = note,
                    receiptUri = receiptUri,
                    allowNegative = allowNegative
                )
            }
        )
    }

    // Cash Check Audit Dialog
    if (isCashCheckOpen) {
        CashCheckDialog(
            appBalance = commonBalance,
            activeMember = activeMember,
            cashCheckHistory = cashChecks,
            currencySymbol = currency,
            onDismiss = { viewModel.setCashCheckOpen(false) },
            onSubmitCheck = { actualCash, note ->
                viewModel.submitCashCheck(actualCash, note)
            }
        )
    }

    // Transaction Details Sheet
    selectedTransactionForDetail?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            currencySymbol = currency,
            onDismiss = { viewModel.setSelectedTransactionForDetail(null) },
            onEditClick = { editTx ->
                viewModel.setTransactionToEdit(editTx)
            },
            onDeleteClick = { _ ->
                viewModel.requestDeleteTransaction(tx)
            }
        )
    }

    // Confirmation Dialog for Deletion (Section 16)
    transactionToDelete?.let { tx ->
        DeleteConfirmationDialog(
            transaction = tx,
            currencySymbol = currency,
            onDismiss = { viewModel.cancelDeleteTransaction() },
            onConfirm = { viewModel.confirmDeleteTransaction() }
        )
    }

    // Edit Transaction Dialog
    transactionToEdit?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            currencySymbol = currency,
            onDismiss = { viewModel.setTransactionToEdit(null) },
            onConfirmEdit = { txId, amount, category, description, note ->
                viewModel.submitEditTransaction(txId, amount, category, description, note)
            }
        )
    }

    // Add Member Dialog
    if (isAddMemberOpen) {
        AddMemberDialog(
            onDismiss = { viewModel.setAddMemberOpen(false) },
            onAddMember = { name, role ->
                viewModel.addMemberToCurrentGroup(name, role)
            }
        )
    }

    // Member Detail Dialog
    selectedMemberForDetail?.let { member ->
        MemberDetailDialog(
            member = member,
            transactions = rawTransactions,
            currencySymbol = currency,
            onDismiss = { viewModel.setSelectedMemberForDetail(null) }
        )
    }

    // Group Management Dialog (Switch, Create, Join)
    if (isGroupManagementOpen) {
        GroupManagementDialog(
            currentGroup = currentGroup,
            allGroups = allGroups,
            onDismiss = { viewModel.setGroupManagementOpen(false) },
            onSwitchGroup = { groupId -> viewModel.switchGroup(groupId) },
            onCreateGroup = { name, creator -> viewModel.createHostel(name, creator) },
            onJoinGroup = { code, user -> viewModel.joinHostel(code, user) }
        )
    }

    // User Profile Switcher Dialog
    if (isUserSwitcherOpen) {
        UserSwitcherDialog(
            members = members,
            activeMember = activeMember,
            onDismiss = { isUserSwitcherOpen = false },
            onSelectMember = { memberId -> viewModel.switchActiveMember(memberId) }
        )
    }

    // Success Animated Feedback Overlay
    successFeedback?.let { (title, subtitle) ->
        SuccessConfirmationDialog(
            title = title,
            subtitle = subtitle,
            onDismiss = { viewModel.clearFeedback() }
        )
    }
}
