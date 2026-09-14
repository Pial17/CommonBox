package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.MemberEntity
import com.example.data.model.ExpenseCategory
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.ExpenseRose

@Composable
fun AddExpenseSheet(
    members: List<MemberEntity>,
    activeMember: MemberEntity?,
    currentBalance: Double,
    currencySymbol: String = "৳",
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (
        amount: Double,
        memberId: String,
        category: ExpenseCategory,
        description: String,
        note: String?,
        receiptUri: String?,
        allowNegative: Boolean
    ) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMemberId by remember {
        mutableStateOf(activeMember?.memberId ?: members.firstOrNull()?.memberId ?: "")
    }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.GROCERY) }
    var descriptionText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var receiptAttached by remember { mutableStateOf(false) }
    var allowNegativeBalance by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Fast quick Bazaar tag presets
    val quickPresets = listOf(
        "বাজার (Daily Bazaar)",
        "মাছ (Fish)",
        "মুরগি ও মাংস (Meat)",
        "চাল (Rice)",
        "ডিম (Eggs)",
        "মসলা ও তেল (Cooking)",
        "বিদ্যুৎ বিল (Electricity)",
        "ওয়াইফাই (WiFi Bill)",
        "মেস সামগ্রী (Mess Supplies)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_expense_sheet"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFE4E6),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = ExpenseRose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add Expense",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Who Spent?
                Text(
                    text = "Who spent?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        val isSelected = member.memberId == selectedMemberId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFFFE4E6) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, ExpenseRose) else null,
                            modifier = Modifier
                                .clickable { selectedMemberId = member.memberId }
                                .testTag("expense_member_${member.memberId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MemberAvatar(name = member.name, size = 24.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = member.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ExpenseRose else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount
                Text(
                    text = "Amount ($currencySymbol)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                            amountText = it
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_expense_amount_input"),
                    placeholder = { Text("e.g. 750", fontSize = 18.sp) },
                    prefix = {
                        Text(
                            text = "$currencySymbol ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRose
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExpenseRose,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Quick amount chips
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(100, 250, 500, 750, 1000, 1500)) { amt ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (amountText == amt.toString()) ExpenseRose else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                amountText = amt.toString()
                                errorMessage = null
                            }
                        ) {
                            Text(
                                text = "$currencySymbol$amt",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (amountText == amt.toString()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Selection
                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ExpenseCategory.entries) { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, ExpenseRose) else null,
                            modifier = Modifier
                                .clickable {
                                    selectedCategory = cat
                                    if (descriptionText.isBlank()) {
                                        descriptionText = "${cat.emoji} ${cat.displayName}"
                                    }
                                }
                                .testTag("category_select_${cat.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cat.emoji, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = cat.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ExpenseRose else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                Text(
                    text = "Description / বাজার",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_expense_desc_input"),
                    placeholder = { Text("e.g. বাজার, Fish, Rice, Electricity") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExpenseRose,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Quick preset pills
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickPresets) { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                descriptionText = preset
                            }
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Note (Optional)
                Text(
                    text = "Note / Items (Optional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_expense_note_input"),
                    placeholder = { Text("e.g. Fish, vegetables and eggs") },
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExpenseRose,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Receipt (Optional)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { receiptAttached = !receiptAttached }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = if (receiptAttached) ExpenseRose else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (receiptAttached) "Receipt Attached ✓" else "Attach Receipt Photo (Optional)",
                            fontSize = 13.sp,
                            fontWeight = if (receiptAttached) FontWeight.Bold else FontWeight.Normal,
                            color = if (receiptAttached) ExpenseRose else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (receiptAttached) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFE4E6)
                        ) {
                            Text(
                                text = "Remove",
                                fontSize = 11.sp,
                                color = ExpenseRose,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Balance warning if amount > available cash (Section 16)
                val enteredAmt = amountText.toDoubleOrNull() ?: 0.0
                if (enteredAmt > currentBalance && enteredAmt > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ExpenseRose,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Low Balance Warning",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRose
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Current cash box has $currencySymbol${currentBalance.toInt()}. This expense ($currencySymbol${enteredAmt.toInt()}) will make balance negative.",
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { allowNegativeBalance = !allowNegativeBalance }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (allowNegativeBalance) ExpenseRose else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRose),
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    if (allowNegativeBalance) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Allow temporary negative balance (Friend paid extra)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary SUBMIT EXPENSE button
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            errorMessage = "Please enter an amount greater than 0"
                            return@Button
                        }
                        if (selectedMemberId.isBlank()) {
                            errorMessage = "Please select who spent the money"
                            return@Button
                        }
                        if (amt > currentBalance && !allowNegativeBalance) {
                            errorMessage = "Please confirm the negative balance checkbox above"
                            return@Button
                        }

                        val desc = descriptionText.ifBlank { "${selectedCategory.emoji} ${selectedCategory.displayName}" }
                        val mockReceipt = if (receiptAttached) "receipt_bazaar_photo_${System.currentTimeMillis()}.jpg" else null

                        onSubmit(
                            amt,
                            selectedMemberId,
                            selectedCategory,
                            desc,
                            noteText.ifBlank { null },
                            mockReceipt,
                            allowNegativeBalance
                        )
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_expense_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRose)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "SUBMIT EXPENSE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
