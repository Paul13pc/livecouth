package com.example.livecouth.di

import com.example.livecouth.data.repository.WaterRepositoryImpl
import com.example.livecouth.domain.repository.WaterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWaterRepository(
        waterRepositoryImpl: WaterRepositoryImpl,
    ): WaterRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: com.example.livecouth.data.repository.UserPreferencesRepositoryImpl,
    ): com.example.livecouth.domain.repository.UserPreferencesRepository
}
