package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hostel_members",
    indices = [Index(value = ["groupId"])]
)
data class MemberEntity(
    @PrimaryKey val memberId: String,
    val groupId: String,
    val name: String,
    val role: String = "Member",
    val colorIndex: Int = 0,
    val userId: String = "",
    val phone: String? = null,
    val joinedAt: Long = System.currentTimeMillis()
)

