package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CashCheckEntity
import com.example.data.local.entities.HostelGroupEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.sync.SyncReport
import com.example.data.sync.SyncState
import com.example.ui.components.BalanceCard
import com.example.ui.components.MemberAvatar
import com.example.ui.components.QuickActionButtons
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    currentGroup: HostelGroupEntity?,
    members: List<MemberEntity>,
    activeMember: MemberEntity?,
    commonBalance: Double,
    totalAdded: Double,
    totalSpent: Double,
    recentTransactions: List<TransactionEntity>,
    latestCashCheck: CashCheckEntity?,
    isNetworkOnline: Boolean,
    syncReport: SyncReport? = null,
    onSyncClick: () -> Unit = {},
    onAddMoneyClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onCashCheckClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onSwitchUserClick: () -> Unit,
    onGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val groupName = currentGroup?.groupName ?: "Hostel Common Box"
    val groupCode = currentGroup?.groupCode ?: "HST-XXXXX"
    val currency = currentGroup?.currencySymbol ?: "৳"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar: Hostel Name, Code Chip, and Active User Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hostel name & Code
                Column(
                    modifier = Modifier.clickable { onGroupClick() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = groupName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch group",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Hostel Code badge with copy
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Hostel Code", groupCode))
                            Toast.makeText(context, "Code copied: $groupCode", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Code: $groupCode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Active Profile Switcher & Network status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync / Network indicator with clear status
                    val syncState = syncReport?.state ?: if (isNetworkOnline) SyncState.SYNCED else SyncState.OFFLINE
                    val (badgeBg, badgeFg) = when (syncState) {
                        SyncState.SYNCED -> Pair(Color(0xFFDCFCE7), Color(0xFF059669))
                        SyncState.OFFLINE -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                        SyncState.SYNCING -> Pair(Color(0xFFDBEAFE), Color(0xFF2563EB))
                        SyncState.SYNC_ERROR -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeBg,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onSyncClick() }
                            .testTag("sync_status_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (syncState) {
                                    SyncState.SYNCED -> "🟢"
                                    SyncState.OFFLINE -> "🟠"
                                    SyncState.SYNCING -> "🔵"
                                    SyncState.SYNC_ERROR -> "🔴"
                                },
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = syncState.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeFg
                            )
                        }
                    }

                    // Profile avatar badge (Acting as...)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onSwitchUserClick() }
                            .testTag("switch_user_profile_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(
                                name = activeMember?.name ?: "User",
                                size = 26.dp,
                                colorIndex = activeMember?.colorIndex ?: 0
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = activeMember?.name ?: "User",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 19: Offline / Sync Notice if offline or pending items exist
        if (syncReport != null && (syncReport.state == SyncState.OFFLINE || syncReport.pendingCount > 0)) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (syncReport.pendingCount > 0) "${syncReport.pendingCount} records queued locally" else "Offline Mode Active",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Your money records are safely stored locally on this device.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        TextButton(
                            onClick = onSyncClick,
                            modifier = Modifier.testTag("dashboard_sync_now_button")
                        ) {
                            Text("Sync Now", fontWeight = FontWeight.Bold, color = Color(0xFF92400E), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 5: Common Balance Card
        item {
            BalanceCard(
                commonBalance = commonBalance,
                totalAdded = totalAdded,
                totalSpent = totalSpent,
                memberCount = members.size,
                currencySymbol = currency,
                latestCashCheck = latestCashCheck,
                onCashCheckClick = onCashCheckClick
            )
        }

        // Section 18: Large Quick Action Buttons (+ Add Money, - Add Expense)
        item {
            QuickActionButtons(
                onAddMoneyClick = onAddMoneyClick,
                onAddExpenseClick = onAddExpenseClick
            )
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (recentTransactions.isNotEmpty()) {
                    TextButton(onClick = onViewAllTransactionsClick) {
                        Text(
                            text = "View All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }

        // Transactions List or Empty State
        if (recentTransactions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your shared hostel cash activity will appear here.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddExpenseClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add First Transaction")
                        }
                    }
                }
            }
        } else {
            items(recentTransactions.take(6)) { tx ->
                TransactionItemCard(
                    transaction = tx,
                    onClick = { onTransactionClick(tx) },
                    currencySymbol = currency
                )
            }
        }
    }
}
