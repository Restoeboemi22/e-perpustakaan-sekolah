package com.sekolah.aplikasismpn3pacet.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "school_schedules")
data class SchoolSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // Calendar.MONDAY etc.
    val dayName: String, // "Senin", "Selasa", etc.
    val startTime: String, // "06:30"
    val endTime: String, // "13:00"
    val isHoliday: Boolean = false
)
