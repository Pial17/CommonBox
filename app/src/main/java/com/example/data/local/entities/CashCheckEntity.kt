package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cash_checks",
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["createdAt"])
    ]
)
data class CashCheckEntity(
    @PrimaryKey val checkId: String,
    val groupId: String,
    val memberId: String,
    val memberName: String,
    val appBalance: Double,
    val actualCash: Double,
    val difference: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
