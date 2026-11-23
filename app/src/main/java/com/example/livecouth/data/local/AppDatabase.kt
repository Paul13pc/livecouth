package com.example.livecouth.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.livecouth.data.local.dao.QuickAddButtonDao
import com.example.livecouth.data.local.dao.WaterLogDao
import com.example.livecouth.data.local.entity.QuickAddButton
import com.example.livecouth.data.local.entity.WaterLog

@Database(
    entities = [WaterLog::class, QuickAddButton::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(DrinkTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterLogDao(): WaterLogDao
    abstract fun quickAddButtonDao(): QuickAddButtonDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE water_logs ADD COLUMN drinkType TEXT NOT NULL DEFAULT 'WATER'",
                )
                db.execSQL(
                    "ALTER TABLE water_logs ADD COLUMN hydrationFactor REAL NOT NULL DEFAULT 1.0",
                )
                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS quick_add_buttons (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            amountMl INTEGER NOT NULL,
                            sortOrder INTEGER NOT NULL
                        )
                    """.trimIndent(),
                )
                insertDefaultButtonsV2(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE water_logs ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE water_logs ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE water_logs ADD COLUMN sincronizado INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE water_logs ADD COLUMN eliminado INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE water_logs SET syncId = 'legacy-' || lower(hex(randomblob(16))), updatedAt = fechaHora",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_water_logs_syncId ON water_logs(syncId)",
                )

                db.execSQL("ALTER TABLE quick_add_buttons ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quick_add_buttons ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE quick_add_buttons ADD COLUMN sincronizado INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE quick_add_buttons ADD COLUMN eliminado INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                        UPDATE quick_add_buttons
                        SET syncId = 'legacy-' || lower(hex(randomblob(16))),
                            updatedAt = strftime('%s', 'now') * 1000
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                        UPDATE quick_add_buttons
                        SET sincronizado = 1, updatedAt = 0
                        WHERE (name = '+150 ml' AND amountMl = 150 AND sortOrder = 0)
                           OR (name = '+250 ml' AND amountMl = 250 AND sortOrder = 1)
                           OR (name = '+500 ml' AND amountMl = 500 AND sortOrder = 2)
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_quick_add_buttons_syncId ON quick_add_buttons(syncId)",
                )
            }
        }

        private fun insertDefaultButtonsV2(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                    INSERT INTO quick_add_buttons (name, amountMl, sortOrder)
                    VALUES ('+150 ml', 150, 0), ('+250 ml', 250, 1), ('+500 ml', 500, 2)
                """.trimIndent(),
            )
        }

        fun insertDefaultButtons(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                    INSERT INTO quick_add_buttons
                        (name, amountMl, sortOrder, syncId, updatedAt, sincronizado, eliminado)
                    VALUES
                        ('+150 ml', 150, 0, lower(hex(randomblob(16))), 0, 1, 0),
                        ('+250 ml', 250, 1, lower(hex(randomblob(16))), 0, 1, 0),
                        ('+500 ml', 500, 2, lower(hex(randomblob(16))), 0, 1, 0)
                """.trimIndent(),
            )
        }
    }
}
