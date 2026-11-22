package com.example.livecouth.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.livecouth.data.local.dao.WaterLogDao
import com.example.livecouth.data.local.entity.WaterLog

@Database(
    entities = [WaterLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterLogDao(): WaterLogDao
}
