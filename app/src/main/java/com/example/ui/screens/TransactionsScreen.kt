package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TimeFilter
import com.example.data.model.TransactionSortOrder
import com.example.data.model.TransactionType
import com.example.ui.components.MemberAvatar
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.IncomeGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    members: List<MemberEntity>,
    searchQuery: String,
    selectedMemberFilter: String?,
    selectedCategoryFilter: String? = null,
    selectedTimeFilter: TimeFilter = TimeFilter.ALL,
    selectedTypeFilter: TransactionType? = null,
    selectedSortOrder: TransactionSortOrder = TransactionSortOrder.NEWEST_FIRST,
    customDateRange: Pair<Long?, Long?>? = null,
    isFilterActive: Boolean = false,
    currencySymbol: String = "৳",
    onSearchChange: (String) -> Unit,
    onMemberFilterSelect: (String?) -> Unit,
    onCategoryFilterSelect: (String?) -> Unit = {},
    onTimeFilterSelect: (TimeFilter) -> Unit,
    onTypeFilterSelect: (TransactionType?) -> Unit,
    onSortOrderSelect: (TransactionSortOrder) -> Unit = {},
    onCustomDateRangeSelect: (Long?, Long?) -> Unit = { _, _ -> },
    onClearFilters: () -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    // Summary calculations from filtered transactions
    val count = transactions.size
    val totalAmount = transactions.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }

    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    // Date grouping when in standard NEWEST_FIRST order
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startOfYesterday = remember(startOfToday) { startOfToday - 86400000L }

    val todayList = remember(transactions, selectedSortOrder) {
        if (selectedSortOrder == TransactionSortOrder.NEWEST_FIRST) {
            transactions.filter { it.createdAt >= startOfToday }
        } else emptyList()
    }
    val yesterdayList = remember(transactions, selectedSortOrder) {
        if (selectedSortOrder == TransactionSortOrder.NEWEST_FIRST) {
            transactions.filter { it.createdAt in startOfYesterday until startOfToday }
        } else emptyList()
    }
    val earlierList = remember(transactions, selectedSortOrder) {
        if (selectedSortOrder == TransactionSortOrder.NEWEST_FIRST) {
            transactions.filter { it.createdAt < startOfYesterday }
        } else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen")
    ) {
        // --- Header & Search Bar ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Shared Transactions",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track, search and review money-box entries",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isFilterActive) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ExpenseRose.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ExpenseRose.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clickable { onClearFilters() }
                            .testTag("clear_filters_header_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = "Clear Filters",
                                tint = ExpenseRose,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRose
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar (Case-insensitive across description, category, member, and type)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input"),
                placeholder = {
                    Text(
                        text = "Search by note, 'food', 'Rahim', 'expense'...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = EmeraldPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: Transaction Type Chips + Sort Order Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Filter Chips: All, Money Added, Expense
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All Type
                    val isAllType = selectedTypeFilter == null
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllType) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onTypeFilterSelect(null) }
                            .testTag("filter_type_all")
                    ) {
                        Text(
                            text = "All Types",
                            fontSize = 12.sp,
                            fontWeight = if (isAllType) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllType) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }

                    // Money Added (Income)
                    val isIncomeType = selectedTypeFilter == TransactionType.INCOME
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isIncomeType) IncomeGreen else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onTypeFilterSelect(TransactionType.INCOME) }
                            .testTag("filter_type_income")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "💵 Added",
                                fontSize = 12.sp,
                                fontWeight = if (isIncomeType) FontWeight.Bold else FontWeight.Medium,
                                color = if (isIncomeType) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Expense
                    val isExpenseType = selectedTypeFilter == TransactionType.EXPENSE
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isExpenseType) ExpenseRose else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onTypeFilterSelect(TransactionType.EXPENSE) }
                            .testTag("filter_type_expense")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🛒 Expense",
                                fontSize = 12.sp,
                                fontWeight = if (isExpenseType) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExpenseType) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Sort Dropdown Menu
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = if (selectedSortOrder != TransactionSortOrder.NEWEST_FIRST) {
                            BorderStroke(1.dp, EmeraldPrimary)
                        } else null,
                        modifier = Modifier
                            .clickable { showSortMenu = true }
                            .testTag("sort_order_selector")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort order",
                                tint = if (selectedSortOrder != TransactionSortOrder.NEWEST_FIRST) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedSortOrder.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedSortOrder != TransactionSortOrder.NEWEST_FIRST) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        TransactionSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = order.label,
                                            fontWeight = if (selectedSortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedSortOrder == order) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (selectedSortOrder == order) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSortOrderSelect(order)
                                    showSortMenu = false
                                },
                                modifier = Modifier.testTag("sort_item_${order.name}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Date Filters (All Time, This Month, Last Month, Today, This Week, Custom Range)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(TimeFilter.entries) { filter ->
                    val isSelected = filter == selectedTimeFilter
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable {
                                if (filter == TimeFilter.CUSTOM_RANGE) {
                                    showCustomDateDialog = true
                                }
                                onTimeFilterSelect(filter)
                            }
                            .testTag("time_filter_${filter.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            if (filter == TimeFilter.CUSTOM_RANGE) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = filter.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Category Filter Row (All, plus each category)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val isAllCat = selectedCategoryFilter == null
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllCat) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onCategoryFilterSelect(null) }
                            .testTag("category_filter_all")
                    ) {
                        Text(
                            text = "All Categories",
                            fontSize = 11.sp,
                            fontWeight = if (isAllCat) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllCat) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }

                items(ExpenseCategory.entries) { cat ->
                    val isSelected = selectedCategoryFilter.equals(cat.id, ignoreCase = true) ||
                            selectedCategoryFilter.equals(cat.displayName, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable {
                                onCategoryFilterSelect(if (isSelected) null else cat.id)
                            }
                            .testTag("category_filter_${cat.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(text = cat.emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cat.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Member Filter Row (All Members, plus each member)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val isSelected = selectedMemberFilter == null
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onMemberFilterSelect(null) }
                            .testTag("member_filter_all")
                    ) {
                        Text(
                            text = "All Members",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }

                items(members) { member ->
                    val isSelected = selectedMemberFilter == member.memberId
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable {
                                onMemberFilterSelect(if (isSelected) null else member.memberId)
                            }
                            .testTag("member_filter_${member.memberId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(name = member.name, size = 16.dp, colorIndex = member.colorIndex)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = member.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // --- Summary Banner for Displayed / Filtered Transactions ---
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("transactions_summary_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Count
                Text(
                    text = "$count ${if (count == 1) "transaction" else "transactions"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Filtered Total
                val formattedTotal = "$currencySymbol ${numberFormatter.format(totalAmount.toLong())}"
                val formattedExpense = "$currencySymbol ${numberFormatter.format(totalExpense.toLong())}"
                val formattedIncome = "$currencySymbol ${numberFormatter.format(totalIncome.toLong())}"

                when {
                    selectedTypeFilter == TransactionType.EXPENSE -> {
                        Text(
                            text = "Total Spent: $formattedExpense",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ExpenseRose
                        )
                    }
                    selectedTypeFilter == TransactionType.INCOME -> {
                        Text(
                            text = "Total Added: $formattedIncome",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IncomeGreen
                        )
                    }
                    totalIncome > 0 && totalExpense > 0 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Spent: $formattedExpense",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRose
                            )
                            Text(
                                text = " • ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Added: $formattedIncome",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "Total: $formattedTotal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // --- Empty State vs Transactions List ---
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag("empty_transactions_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No transactions found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isFilterActive) "No results matched your search or filters." else "No transactions recorded yet in this hostel.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    if (isFilterActive) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onClearFilters,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("clear_filters_empty_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Clear Filters", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // LazyColumn of Transactions
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // If sort order is NEWEST_FIRST, display with convenient date groupings
                if (selectedSortOrder == TransactionSortOrder.NEWEST_FIRST) {
                    // Section TODAY
                    if (todayList.isNotEmpty()) {
                        item {
                            Text(
                                text = "TODAY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(todayList, key = { it.transactionId }) { tx ->
                            TransactionItemCard(
                                transaction = tx,
                                onClick = { onTransactionClick(tx) },
                                currencySymbol = currencySymbol
                            )
                        }
                    }

                    // Section YESTERDAY
                    if (yesterdayList.isNotEmpty()) {
                        item {
                            Text(
                                text = "YESTERDAY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        items(yesterdayList, key = { it.transactionId }) { tx ->
                            TransactionItemCard(
                                transaction = tx,
                                onClick = { onTransactionClick(tx) },
                                currencySymbol = currencySymbol
                            )
                        }
                    }

                    // Section EARLIER
                    if (earlierList.isNotEmpty()) {
                        item {
                            Text(
                                text = "EARLIER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        items(earlierList, key = { it.transactionId }) { tx ->
                            TransactionItemCard(
                                transaction = tx,
                                onClick = { onTransactionClick(tx) },
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                } else {
                    // For other sorting (HIGHEST_AMOUNT, LOWEST_AMOUNT, OLDEST_FIRST), show sorted sequence
                    item {
                        Text(
                            text = "ORDER: ${selectedSortOrder.label.uppercase(Locale.getDefault())}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            letterSpacing = 1.1.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    items(transactions, key = { it.transactionId }) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            onClick = { onTransactionClick(tx) },
                            currencySymbol = currencySymbol
                        )
                    }
                }
            }
        }
    }

    // --- Custom Date Range Dialog ---
    if (showCustomDateDialog) {
        CustomDateRangePickerSheet(
            currentRange = customDateRange,
            onDismiss = { showCustomDateDialog = false },
            onApply = { start, end ->
                onCustomDateRangeSelect(start, end)
                onTimeFilterSelect(TimeFilter.CUSTOM_RANGE)
                showCustomDateDialog = false
            }
        )
    }
}

/**
 * Custom Date Range Dialog offering quick presets and specific date selection
 */
@Composable
private fun CustomDateRangePickerSheet(
    currentRange: Pair<Long?, Long?>?,
    onDismiss: () -> Unit,
    onApply: (Long?, Long?) -> Unit
) {
    var selectedPreset by remember { mutableStateOf<String?>("Last 30 Days") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_date_range_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Date Filter",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select a quick period or custom range:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val presets = listOf(
                    "Last 7 Days" to 7,
                    "Last 30 Days" to 30,
                    "Last 60 Days" to 60,
                    "Last 90 Days" to 90,
                    "This Year" to 365
                )

                presets.forEach { (label, days) ->
                    val isSelected = selectedPreset == label
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) BorderStroke(1.5.dp, EmeraldPrimary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedPreset = label
                                val endMillis = System.currentTimeMillis()
                                val startMillis = endMillis - (days.toLong() * 86400000L)
                                onApply(startMillis, endMillis)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
