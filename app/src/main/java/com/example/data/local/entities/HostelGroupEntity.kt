package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hostel_groups",
    indices = [
        Index(value = ["groupCode"], unique = true)
    ]
)
data class HostelGroupEntity(
    @PrimaryKey val groupId: String,
    val groupName: String,
    val groupCode: String,
    val currencySymbol: String = "৳",
    val currencyCode: String = "BDT",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)

