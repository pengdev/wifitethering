package com.geminiapps.wifitethering.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.geminiapps.wifitethering.data.model.Schedule

@Database(entities = [Schedule::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
}
