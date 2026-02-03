package com.sekolah.aplikasismpn3pacet.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "literacy_tasks")
data class LiteracyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val points: Int,
    val durationMinutes: Int = 45, // Default static value as per requirement
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
