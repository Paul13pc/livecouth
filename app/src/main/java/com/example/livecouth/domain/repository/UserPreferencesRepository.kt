package com.example.livecouth.domain.repository

import com.example.livecouth.domain.model.UserProfile
import com.example.livecouth.domain.model.LocationCoordinates
import com.example.livecouth.domain.model.ManualClimate
import com.example.livecouth.domain.model.ReminderSettings
import com.example.livecouth.domain.model.SyncableUserProfile
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userProfile: Flow<UserProfile>
    val reminderSettings: Flow<ReminderSettings>
    val usesDeviceLocationForClimate: Flow<Boolean>
    suspend fun saveUserProfile(userProfile: UserProfile)
    suspend fun saveReminderSettings(settings: ReminderSettings)
    suspend fun setUsesDeviceLocationForClimate(enabled: Boolean)
    suspend fun saveLastDeviceLocation(location: LocationCoordinates)
    suspend fun getLastDeviceLocation(): LocationCoordinates?
    suspend fun claimDailyCelebration(date: String): Boolean
    suspend fun claimDailyGoalNotification(date: String): Boolean
    suspend fun saveManualClimate(date: String, climate: ManualClimate?)
    suspend fun getManualClimate(date: String): ManualClimate?
    suspend fun getSyncableProfile(): SyncableUserProfile
    suspend fun saveSyncedProfile(profile: UserProfile, updatedAt: Long, eliminado: Boolean)
    suspend fun markProfileSynced(updatedAt: Long)
    suspend fun clearAll()
}
