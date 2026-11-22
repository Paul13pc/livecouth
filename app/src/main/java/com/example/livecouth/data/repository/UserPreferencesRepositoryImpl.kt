package com.example.livecouth.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.livecouth.domain.model.ActivityLevel
import com.example.livecouth.domain.model.UserProfile
import com.example.livecouth.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val NAME = stringPreferencesKey("user_name")
        val WEIGHT = floatPreferencesKey("user_weight")
        val AGE = intPreferencesKey("user_age")
        val ACTIVITY_LEVEL = stringPreferencesKey("user_activity_level")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_START_HOUR = intPreferencesKey("reminder_start_hour")
        val REMINDER_END_HOUR = intPreferencesKey("reminder_end_hour")
    }

    override val userProfile: Flow<UserProfile> = dataStore.data.map { prefs ->
        val name = prefs[PreferencesKeys.NAME] ?: ""
        val weight = prefs[PreferencesKeys.WEIGHT] ?: 70f
        val age = prefs[PreferencesKeys.AGE] ?: 25
        val activityStr = prefs[PreferencesKeys.ACTIVITY_LEVEL] ?: ActivityLevel.MEDIO.name
        val activityLevel = try {
            ActivityLevel.valueOf(activityStr)
        } catch (_: Exception) {
            ActivityLevel.MEDIO
        }
        val isCompleted = prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        val reminderEnabled = prefs[PreferencesKeys.REMINDER_ENABLED] ?: true
        val startHour = prefs[PreferencesKeys.REMINDER_START_HOUR] ?: 8
        val endHour = prefs[PreferencesKeys.REMINDER_END_HOUR] ?: 21

        UserProfile(
            name = name,
            weightKg = weight,
            age = age,
            activityLevel = activityLevel,
            isOnboardingCompleted = isCompleted,
            reminderEnabled = reminderEnabled,
            reminderStartHour = startHour,
            reminderEndHour = endHour,
        )
    }

    override suspend fun saveUserProfile(userProfile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.NAME] = userProfile.name
            prefs[PreferencesKeys.WEIGHT] = userProfile.weightKg
            prefs[PreferencesKeys.AGE] = userProfile.age
            prefs[PreferencesKeys.ACTIVITY_LEVEL] = userProfile.activityLevel.name
            prefs[PreferencesKeys.ONBOARDING_COMPLETED] = true
            prefs[PreferencesKeys.REMINDER_ENABLED] = userProfile.reminderEnabled
            prefs[PreferencesKeys.REMINDER_START_HOUR] = userProfile.reminderStartHour
            prefs[PreferencesKeys.REMINDER_END_HOUR] = userProfile.reminderEndHour
        }
    }
}
