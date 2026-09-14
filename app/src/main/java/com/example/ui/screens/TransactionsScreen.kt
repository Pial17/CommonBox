package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.model.TimeFilter
import com.example.data.model.TransactionType
import com.example.ui.components.MemberAvatar
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.IncomeGreen
import java.util.Calendar

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    members: List<MemberEntity>,
    searchQuery: String,
    selectedMemberFilter: String?,
    selectedTimeFilter: TimeFilter,
    selectedTypeFilter: TransactionType?,
    currencySymbol: String = "৳",
    onSearchChange: (String) -> Unit,
    onMemberFilterSelect: (String?) -> Unit,
    onTimeFilterSelect: (TimeFilter) -> Unit,
    onTypeFilterSelect: (TransactionType?) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group transactions into TODAY, YESTERDAY, EARLIER
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val startOfYesterday = startOfToday - 86400000L

    val todayList = transactions.filter { it.createdAt >= startOfToday }
    val yesterdayList = transactions.filter { it.createdAt in startOfYesterday until startOfToday }
    val earlierList = transactions.filter { it.createdAt < startOfYesterday }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen")
    ) {
        // Search & Filter Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Shared History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input"),
                placeholder = { Text("Search by description, bazaar items, member...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Time Filter Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimeFilter.entries) { filter ->
                    val isSelected = filter == selectedTimeFilter
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onTimeFilterSelect(filter) }
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Member Filter Row (All, Pial, Rafi, Sakib, etc.)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All option
                item {
                    val isSelected = selectedMemberFilter == null
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onMemberFilterSelect(null) }
                    ) {
                        Text(
                            text = "All Members",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                items(members) { member ->
                    val isSelected = selectedMemberFilter == member.memberId
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onMemberFilterSelect(member.memberId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(name = member.name, size = 18.dp, colorIndex = member.colorIndex)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = member.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Transactions List grouped by TODAY, YESTERDAY, EARLIER
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No matching transactions",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try clearing search or filter parameters.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section TODAY
                if (todayList.isNotEmpty()) {
                    item {
                        Text(
                            text = "TODAY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(todayList) { tx ->
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                        )
                    }
                    items(yesterdayList) { tx ->
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
                            text = "EARLIER THIS MONTH",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                        )
                    }
                    items(earlierList) { tx ->
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
}
