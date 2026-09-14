package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CashCheckEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BalanceCard(
    commonBalance: Double,
    totalAdded: Double,
    totalSpent: Double,
    memberCount: Int,
    currencySymbol: String = "৳",
    latestCashCheck: CashCheckEntity? = null,
    onCashCheckClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    val formattedBalance = "$currencySymbol ${formatter.format(commonBalance.toLong())}"
    val formattedAdded = "$currencySymbol ${formatter.format(totalAdded.toLong())}"
    val formattedSpent = "$currencySymbol ${formatter.format(totalSpent.toLong())}"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("balance_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldDark,
                            Color(0xFF094333),
                            Color(0xFF062D22)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header row: Label + Member Count Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COMMON BALANCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFFA7D7C5)
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x33FFFFFF)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFFD1FAE5),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$memberCount Members",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD1FAE5)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Balance Display with Animated Content
                AnimatedContent(
                    targetState = formattedBalance,
                    transitionSpec = {
                        slideInVertically { height -> height } togetherWith
                                slideOutVertically { height -> -height }
                    },
                    label = "BalanceAnimation"
                ) { targetText ->
                    Text(
                        text = targetText,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.testTag("common_balance_text")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0x26FFFFFF), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Metrics Row: Total Added vs Total Spent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Total Added
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x3310B981),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = EmeraldPrimaryLight,
                                    modifier = Modifier.padding(3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Total Added",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedAdded,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD1FAE5),
                            modifier = Modifier.testTag("total_added_text")
                        )
                    }

                    // Total Spent
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total Spent",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x33F43F5E),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFFFDA4AF),
                                    modifier = Modifier.padding(3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedSpent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFE4E6),
                            modifier = Modifier.testTag("total_spent_text")
                        )
                    }
                }

                // Cash Box Physical Audit Status Pill (Section 14)
                if (latestCashCheck != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val diff = latestCashCheck.difference
                    val isBalanced = diff == 0.0
                    val isShort = diff < 0.0

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCashCheckClick() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x26000000),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (isBalanced) EmeraldPrimaryLight else WarningAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Physical Box Check",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }

                            val diffTag = when {
                                isBalanced -> "✓ Cash Matched"
                                isShort -> "-$currencySymbol${formatter.format((-diff).toLong())} Diff"
                                else -> "+$currencySymbol${formatter.format(diff.toLong())} Diff"
                            }

                            val tagBg = when {
                                isBalanced -> Color(0x3310B981)
                                isShort -> Color(0x33F43F5E)
                                else -> Color(0x33F59E0B)
                            }
                            val tagColor = when {
                                isBalanced -> Color(0xFFA7F3D0)
                                isShort -> Color(0xFFFECDD3)
                                else -> Color(0xFFFDE68A)
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tagBg
                            ) {
                                Text(
                                    text = diffTag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tagColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
