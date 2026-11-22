package com.example.livecouth.di

import android.content.Context
import androidx.room.Room
import com.example.livecouth.data.local.AppDatabase
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
        ).build()
    }

    @Provides
    @Singleton
    fun provideWaterLogDao(database: AppDatabase): WaterLogDao {
        return database.waterLogDao()
    }
}
