package com.geminiapps.wifitethering.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScheduleAction { REMIND_ENABLE, REMIND_DISABLE }

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val hourOfDay: Int,
    val minute: Int,
    val daysOfWeek: Int, // bitmask: bit 0 = Mon, bit 6 = Sun
    val action: ScheduleAction,
    val enabled: Boolean = true,
)
