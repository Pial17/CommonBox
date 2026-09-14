package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "box_transactions",
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["createdAt"]),
        Index(value = ["groupId", "type"]),
        Index(value = ["groupId", "createdAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val transactionId: String,
    val groupId: String,
    val memberId: String,
    val memberName: String,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val category: String, // e.g. "grocery", "fish", "deposit"
    val description: String,
    val note: String? = null,
    val receiptUri: String? = null,
    val syncStatus: String = "SYNCED", // "SYNCED", "PENDING", "FAILED"
    val createdByUserId: String = "",
    val updatedByUserId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val editedHistory: String? = null
)

