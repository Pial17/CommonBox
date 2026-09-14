package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CashCheckEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.IncomeGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun SummaryScreen(
    members: List<MemberEntity>,
    transactions: List<TransactionEntity>,
    cashChecks: List<CashCheckEntity> = emptyList(),
    groupName: String = "Hostel Box",
    currencySymbol: String = "৳",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    val totalAdded = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    val totalSpent = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    val remainingBalance = totalAdded - totalSpent

    // Top spenders calculation
    val spenders = members.map { member ->
        val spent = transactions
            .filter { it.memberId == member.memberId && it.type == TransactionType.EXPENSE.name }
            .sumOf { it.amount }
        val added = transactions
            .filter { it.memberId == member.memberId && it.type == TransactionType.INCOME.name }
            .sumOf { it.amount }
        Triple(member, spent, added)
    }.sortedByDescending { it.second }

    val highestSpending = spenders.firstOrNull()?.second?.takeIf { it > 0 } ?: 1.0

    // Categories breakdown
    val categoryTotals = ExpenseCategory.entries.mapNotNull { cat ->
        val catSpent = transactions
            .filter { it.category == cat.id && it.type == TransactionType.EXPENSE.name }
            .sumOf { it.amount }
        if (catSpent > 0) Pair(cat, catSpent) else null
    }.sortedByDescending { it.second }

    val highestCategorySpent = categoryTotals.firstOrNull()?.second?.takeIf { it > 0 } ?: 1.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("summary_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Summary",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Mess cash flow and spending analysis",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentMonthYear,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3 Key Stat Cards (Total Added, Total Spent, Remaining)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Added
                        Column {
                            Text(text = "Total Added", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currencySymbol ${formatter.format(totalAdded.toLong())}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }

                        // Spent
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Spent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currencySymbol ${formatter.format(totalSpent.toLong())}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRose
                            )
                        }

                        // Remaining
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Remaining", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currencySymbol ${formatter.format(remainingBalance.toLong())}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    // Progress bar: Spent vs Total Added
                    if (totalAdded > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val spentFraction = (totalSpent / totalAdded).toFloat().coerceIn(0f, 1f)
                        val animatedFraction by animateFloatAsState(
                            targetValue = spentFraction,
                            animationSpec = tween(600),
                            label = "SpentRatio"
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(spentFraction * 100).toInt()}% of added money spent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${((1f - spentFraction) * 100).toInt()}% available in box",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (spentFraction > 0.9f) ExpenseRose else EmeraldPrimary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Spenders Section (Section 13)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Top Mess Spenders",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    spenders.forEachIndexed { index, (member, spent, _) ->
                        val barFraction = (spent / highestSpending).toFloat().coerceIn(0.05f, 1f)
                        val animatedBar by animateFloatAsState(
                            targetValue = barFraction,
                            animationSpec = tween(500),
                            label = "SpenderBar"
                        )

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    MemberAvatar(name = member.name, size = 26.dp, colorIndex = member.colorIndex)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = member.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "$currencySymbol ${formatter.format(spent.toLong())}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRose
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedBar)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ExpenseRose)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Most Used Categories Section (Section 13)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Spending by Category",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (categoryTotals.isEmpty()) {
                        Text(
                            text = "No category spending data yet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categoryTotals.forEach { (cat, catAmt) ->
                            val catFraction = (catAmt / highestCategorySpent).toFloat().coerceIn(0.05f, 1f)
                            val animatedCatBar by animateFloatAsState(
                                targetValue = catFraction,
                                animationSpec = tween(500),
                                label = "CategoryBar"
                            )

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = cat.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cat.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "$currencySymbol ${formatter.format(catAmt.toLong())}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(5.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFFF1F5F9))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedCatBar)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(EmeraldPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fair Share & Equal Split Settlement (Part 3)
        item {
            val activeMemberCount = members.size.coerceAtLeast(1)
            val fairShare = if (totalSpent > 0 && activeMemberCount > 0) totalSpent / activeMemberCount else 0.0

            val settlements = members.map { member ->
                val added = transactions
                    .filter { it.memberId == member.memberId && it.type == TransactionType.INCOME.name }
                    .sumOf { it.amount }
                val net = added - fairShare
                Triple(member, added, net)
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settlement_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Balance,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fair Share & Settlement",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = "$currencySymbol ${formatter.format(fairShare.toLong())} / person",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Splits total hostel mess expenses ($currencySymbol ${formatter.format(totalSpent.toLong())}) equally across $activeMemberCount roommates.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    settlements.forEach { (member, added, net) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MemberAvatar(name = member.name, size = 30.dp, colorIndex = member.colorIndex)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = member.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Deposited: $currencySymbol ${formatter.format(added.toLong())}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val (badgeText, badgeBg, badgeTextColor) = when {
                                net > 1.0 -> Triple(
                                    "To Receive: +$currencySymbol ${formatter.format(net.toLong())}",
                                    Color(0xFFDCFCE7),
                                    IncomeGreen
                                )
                                net < -1.0 -> Triple(
                                    "Owes Box: -$currencySymbol ${formatter.format(abs(net).toLong())}",
                                    Color(0xFFFFE4E6),
                                    ExpenseRose
                                )
                                else -> Triple(
                                    "Settled ✅",
                                    Color(0xFFF1F5F9),
                                    Color(0xFF64748B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // WhatsApp / Roommate chat share button
                    Button(
                        onClick = {
                            val reportText = buildString {
                                appendLine("📦 *CommonBox Mess Report*")
                                appendLine("🏠 Hostel: $groupName")
                                appendLine("📅 Period: $currentMonthYear")
                                appendLine("-----------------------------")
                                appendLine("💰 Total Added to Box: $currencySymbol ${formatter.format(totalAdded.toLong())}")
                                appendLine("🛒 Total Spent: $currencySymbol ${formatter.format(totalSpent.toLong())}")
                                appendLine("📦 Remaining in Box: $currencySymbol ${formatter.format(remainingBalance.toLong())}")
                                appendLine("")
                                appendLine("⚖️ *Fair Share per Roommate*: $currencySymbol ${formatter.format(fairShare.toLong())}")
                                appendLine("")
                                appendLine("👥 *Settlement Breakdown*:")
                                settlements.forEach { (mem, added, net) ->
                                    val status = when {
                                        net > 1.0 -> "To Receive +$currencySymbol ${formatter.format(net.toLong())} 🟢"
                                        net < -1.0 -> "Owes Box -$currencySymbol ${formatter.format(abs(net).toLong())} 🔴"
                                        else -> "Settled ✅"
                                    }
                                    appendLine("• ${mem.name}: Deposited $currencySymbol ${formatter.format(added.toLong())} ➔ $status")
                                }
                                appendLine("-----------------------------")
                                appendLine("Sent via CommonBox 📱")
                            }

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, reportText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Mess Settlement"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_whatsapp_report_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share Mess Report to Roommate Chat",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Physical Cash Audit History (Part 3)
        if (cashChecks.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audit_history_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Physical Cash Box Audits (${cashChecks.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val checkDateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

                        cashChecks.take(5).forEach { check ->
                            val isMatch = abs(check.difference) < 0.01
                            val isShortage = check.difference > 0.01
                            val statusLabel = when {
                                isMatch -> "MATCH"
                                isShortage -> "SHORTAGE"
                                else -> "SURPLUS"
                            }
                            val statusBg = when {
                                isMatch -> Color(0xFFDCFCE7)
                                isShortage -> Color(0xFFFFE4E6)
                                else -> Color(0xFFFEF3C7)
                            }
                            val statusText = when {
                                isMatch -> IncomeGreen
                                isShortage -> ExpenseRose
                                else -> Color(0xFFD97706)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Audited by ${check.memberName}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${checkDateFormat.format(Date(check.createdAt))} • Counted: $currencySymbol ${formatter.format(check.actualCash.toLong())}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!check.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "\"${check.note}\"",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusBg
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
