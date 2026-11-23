package com.example.livecouth.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.livecouth.data.local.AppDatabase
import com.example.livecouth.data.local.dao.QuickAddButtonDao
import com.example.livecouth.data.local.dao.WaterLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "livecouth_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        AppDatabase.insertDefaultButtons(db)
                    }
                },
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideWaterLogDao(database: AppDatabase): WaterLogDao {
        return database.waterLogDao()
    }

    @Provides
    @Singleton
    fun provideQuickAddButtonDao(database: AppDatabase): QuickAddButtonDao {
        return database.quickAddButtonDao()
    }
}
