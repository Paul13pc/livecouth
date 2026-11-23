package com.example.livecouth.di

import com.example.livecouth.data.repository.WaterRepositoryImpl
import com.example.livecouth.data.repository.WeatherRepositoryImpl
import com.example.livecouth.data.repository.FirebaseAuthRepository
import com.example.livecouth.data.repository.FirestoreSyncRepository
import com.example.livecouth.domain.repository.WaterRepository
import com.example.livecouth.domain.repository.WeatherRepository
import com.example.livecouth.domain.repository.AuthRepository
import com.example.livecouth.domain.repository.SyncRepository
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

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        weatherRepositoryImpl: WeatherRepositoryImpl,
    ): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        firebaseAuthRepository: FirebaseAuthRepository,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        firestoreSyncRepository: FirestoreSyncRepository,
    ): SyncRepository
}
