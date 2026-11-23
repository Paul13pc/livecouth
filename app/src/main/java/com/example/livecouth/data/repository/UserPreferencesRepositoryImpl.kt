package com.example.livecouth.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.livecouth.domain.model.ReminderFrequency
import com.example.livecouth.domain.model.ReminderSettings
import com.example.livecouth.domain.model.ReminderSettingsError
import com.example.livecouth.domain.model.ReminderSettingsValidator
import com.example.livecouth.domain.model.ActivityLevel
import com.example.livecouth.domain.model.CityLocation
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.model.LocationCoordinates
import com.example.livecouth.domain.model.ManualClimate
import com.example.livecouth.domain.model.UserProfile
import com.example.livecouth.domain.model.SyncableUserProfile
import com.example.livecouth.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
        val LAST_SHARK_CELEBRATION_DATE = stringPreferencesKey("last_shark_celebration_date")
        val LAST_GOAL_NOTIFICATION_DATE = stringPreferencesKey("last_goal_notification_date")
        val CITY_NAME = stringPreferencesKey("city_name")
        val CITY_LATITUDE = floatPreferencesKey("city_latitude")
        val CITY_LONGITUDE = floatPreferencesKey("city_longitude")
        val USES_DEVICE_LOCATION_FOR_CLIMATE = booleanPreferencesKey("uses_device_location_for_climate")
        val LAST_DEVICE_LATITUDE = floatPreferencesKey("last_device_latitude")
        val LAST_DEVICE_LONGITUDE = floatPreferencesKey("last_device_longitude")
        val MANUAL_CLIMATE_DATE = stringPreferencesKey("manual_climate_date")
        val MANUAL_CLIMATE_LEVEL = stringPreferencesKey("manual_climate_level")
        val PROFILE_UPDATED_AT = longPreferencesKey("profile_updated_at")
        val PROFILE_SYNCED = booleanPreferencesKey("profile_synchronized")
        val PROFILE_DELETED = booleanPreferencesKey("profile_deleted")
        val REMINDER_START_MINUTE = intPreferencesKey("reminder_start_minute")
        val REMINDER_END_MINUTE = intPreferencesKey("reminder_end_minute")
        val REMINDER_FREQUENCY = stringPreferencesKey("reminder_frequency")
        val REMINDER_CUSTOM_INTERVAL = intPreferencesKey("reminder_custom_interval_minutes")
        val REMINDER_ACTIVE_DAYS = intPreferencesKey("reminder_active_days_mask")
        val REMINDER_QUIET_ENABLED = booleanPreferencesKey("reminder_quiet_hours_enabled")
        val REMINDER_QUIET_START = intPreferencesKey("reminder_quiet_start_minute")
        val REMINDER_QUIET_END = intPreferencesKey("reminder_quiet_end_minute")
    }

    override val userProfile: Flow<UserProfile> = dataStore.data.map { prefs ->
        prefs.toUserProfile()
    }.distinctUntilChanged()

    override val reminderSettings: Flow<ReminderSettings> = dataStore.data.map { prefs ->
        prefs.toReminderSettings()
    }.distinctUntilChanged()

    override val usesDeviceLocationForClimate: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.USES_DEVICE_LOCATION_FOR_CLIMATE] ?: false
    }.distinctUntilChanged()

    override suspend fun getSyncableProfile(): SyncableUserProfile {
        var prefs = dataStore.data.first()
        if (
            (prefs[PreferencesKeys.PROFILE_UPDATED_AT] ?: 0L) == 0L &&
            prefs.toUserProfile().isOnboardingCompleted
        ) {
            val updatedAt = System.currentTimeMillis()
            dataStore.edit { current ->
                if ((current[PreferencesKeys.PROFILE_UPDATED_AT] ?: 0L) == 0L) {
                    current[PreferencesKeys.PROFILE_UPDATED_AT] = updatedAt
                    current[PreferencesKeys.PROFILE_SYNCED] = false
                    current[PreferencesKeys.PROFILE_DELETED] = false
                }
            }
            prefs = dataStore.data.first()
        }
        return SyncableUserProfile(
            profile = prefs.toUserProfile(),
            updatedAt = prefs[PreferencesKeys.PROFILE_UPDATED_AT] ?: 0L,
            sincronizado = prefs[PreferencesKeys.PROFILE_SYNCED] ?: true,
            eliminado = prefs[PreferencesKeys.PROFILE_DELETED] ?: false,
        )
    }

    private fun Preferences.toUserProfile(): UserProfile {
        val name = InputValidation.normalizeName(this[PreferencesKeys.NAME] ?: "")
            .take(InputValidation.MAX_NAME_LENGTH)
        val storedWeight = this[PreferencesKeys.WEIGHT] ?: 70f
        val weight = storedWeight.takeIf {
            it.isFinite() && it in InputValidation.MIN_WEIGHT_KG..InputValidation.MAX_WEIGHT_KG
        } ?: 70f
        val storedAge = this[PreferencesKeys.AGE] ?: 25
        val age = storedAge.takeIf { it in InputValidation.MIN_AGE..InputValidation.MAX_AGE } ?: 25
        val activityStr = this[PreferencesKeys.ACTIVITY_LEVEL] ?: ActivityLevel.MEDIO.name
        val activityLevel = try {
            ActivityLevel.valueOf(activityStr)
        } catch (_: Exception) {
            ActivityLevel.MEDIO
        }
        val isCompleted = this[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        val reminderEnabled = this[PreferencesKeys.REMINDER_ENABLED] ?: true
        val storedStartHour = this[PreferencesKeys.REMINDER_START_HOUR] ?: 8
        val storedEndHour = this[PreferencesKeys.REMINDER_END_HOUR] ?: 21
        val (startHour, endHour) = if (
            storedStartHour in 0..23 &&
            storedEndHour in 0..23 &&
            storedEndHour > storedStartHour
        ) {
            storedStartHour to storedEndHour
        } else {
            8 to 21
        }

        return UserProfile(
            name = name,
            weightKg = weight,
            age = age,
            activityLevel = activityLevel,
            isOnboardingCompleted = isCompleted,
            reminderEnabled = reminderEnabled,
            reminderStartHour = startHour,
            reminderEndHour = endHour,
            city = CityLocation(
                name = this[PreferencesKeys.CITY_NAME] ?: CityLocation.MANTA.name,
                latitude = this[PreferencesKeys.CITY_LATITUDE] ?: CityLocation.MANTA.latitude,
                longitude = this[PreferencesKeys.CITY_LONGITUDE] ?: CityLocation.MANTA.longitude,
            ),
        )
    }

    private fun Preferences.toReminderSettings(): ReminderSettings {
        val quietStart = this[PreferencesKeys.REMINDER_QUIET_START] ?: 22 * 60
        val quietEnd = this[PreferencesKeys.REMINDER_QUIET_END] ?: 7 * 60
        val quietHoursAreValid = quietStart in 0 until 24 * 60 &&
            quietEnd in 0 until 24 * 60 &&
            quietStart != quietEnd
        val settings = ReminderSettings(
            enabled = this[PreferencesKeys.REMINDER_ENABLED] ?: true,
            startMinute = this[PreferencesKeys.REMINDER_START_MINUTE]
                ?: ((this[PreferencesKeys.REMINDER_START_HOUR] ?: 8).coerceIn(0, 23) * 60),
            endMinute = this[PreferencesKeys.REMINDER_END_MINUTE]
                ?: ((this[PreferencesKeys.REMINDER_END_HOUR] ?: 21).coerceIn(0, 23) * 60),
            frequency = this[PreferencesKeys.REMINDER_FREQUENCY]
                ?.let { name -> ReminderFrequency.entries.firstOrNull { it.name == name } }
                ?: ReminderFrequency.EVERY_2_HOURS,
            customIntervalMinutes = (this[PreferencesKeys.REMINDER_CUSTOM_INTERVAL] ?: 60)
                .coerceAtLeast(ReminderSettingsValidator.MIN_REMINDER_INTERVAL_MINUTES),
            activeDaysMask = (this[PreferencesKeys.REMINDER_ACTIVE_DAYS] ?: 0b1111111) and 0b1111111,
            quietHoursEnabled = (this[PreferencesKeys.REMINDER_QUIET_ENABLED] ?: false) &&
                quietHoursAreValid,
            quietStartMinute = quietStart.takeIf { quietHoursAreValid } ?: 22 * 60,
            quietEndMinute = quietEnd.takeIf { quietHoursAreValid } ?: 7 * 60,
        )
        return if (ReminderSettingsValidator.validate(settings) ==
            ReminderSettingsError.INVALID_ACTIVE_WINDOW
        ) {
            settings.copy(startMinute = 8 * 60, endMinute = 21 * 60)
        } else {
            settings
        }
    }

    override suspend fun saveUserProfile(userProfile: UserProfile) {
        dataStore.edit { prefs ->
            writeProfile(prefs, userProfile)
            prefs[PreferencesKeys.PROFILE_UPDATED_AT] = maxOf(
                System.currentTimeMillis(),
                (prefs[PreferencesKeys.PROFILE_UPDATED_AT] ?: 0L) + 1,
            )
            prefs[PreferencesKeys.PROFILE_SYNCED] = false
            prefs[PreferencesKeys.PROFILE_DELETED] = false
        }
    }

    override suspend fun saveReminderSettings(settings: ReminderSettings) {
        require(ReminderSettingsValidator.validate(settings) == null) {
            "La configuración de recordatorios no es válida."
        }
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.REMINDER_START_MINUTE] = settings.startMinute
            prefs[PreferencesKeys.REMINDER_END_MINUTE] = settings.endMinute
            prefs[PreferencesKeys.REMINDER_FREQUENCY] = settings.frequency.name
            prefs[PreferencesKeys.REMINDER_CUSTOM_INTERVAL] = settings.customIntervalMinutes
            prefs[PreferencesKeys.REMINDER_ACTIVE_DAYS] = settings.activeDaysMask and 0b1111111
            prefs[PreferencesKeys.REMINDER_QUIET_ENABLED] = settings.quietHoursEnabled
            prefs[PreferencesKeys.REMINDER_QUIET_START] = settings.quietStartMinute
            prefs[PreferencesKeys.REMINDER_QUIET_END] = settings.quietEndMinute
            prefs[PreferencesKeys.REMINDER_ENABLED] = settings.enabled
        }
    }

    override suspend fun setUsesDeviceLocationForClimate(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.USES_DEVICE_LOCATION_FOR_CLIMATE] = enabled
        }
    }

    override suspend fun saveLastDeviceLocation(location: LocationCoordinates) {
        require(location.areValid()) { "Las coordenadas de ubicación no son válidas." }
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_DEVICE_LATITUDE] = location.latitude
            prefs[PreferencesKeys.LAST_DEVICE_LONGITUDE] = location.longitude
        }
    }

    override suspend fun getLastDeviceLocation(): LocationCoordinates? {
        val prefs = dataStore.data.first()
        val latitude = prefs[PreferencesKeys.LAST_DEVICE_LATITUDE] ?: return null
        val longitude = prefs[PreferencesKeys.LAST_DEVICE_LONGITUDE] ?: return null
        return LocationCoordinates(latitude, longitude).takeIf { it.areValid() }
    }

    override suspend fun saveSyncedProfile(
        profile: UserProfile,
        updatedAt: Long,
        eliminado: Boolean,
    ) {
        dataStore.edit { prefs ->
            writeProfile(prefs, profile)
            prefs[PreferencesKeys.PROFILE_UPDATED_AT] = updatedAt
            prefs[PreferencesKeys.PROFILE_SYNCED] = true
            prefs[PreferencesKeys.PROFILE_DELETED] = eliminado
        }
    }

    override suspend fun markProfileSynced(updatedAt: Long) {
        dataStore.edit { prefs ->
            if (prefs[PreferencesKeys.PROFILE_UPDATED_AT] == updatedAt) {
                prefs[PreferencesKeys.PROFILE_SYNCED] = true
            }
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    private fun writeProfile(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        userProfile: UserProfile,
    ) {
        prefs[PreferencesKeys.NAME] = InputValidation.normalizeName(userProfile.name)
            .take(InputValidation.MAX_NAME_LENGTH)
        prefs[PreferencesKeys.WEIGHT] = userProfile.weightKg.takeIf {
            it.isFinite() && it in InputValidation.MIN_WEIGHT_KG..InputValidation.MAX_WEIGHT_KG
        } ?: 70f
        prefs[PreferencesKeys.AGE] = userProfile.age.takeIf {
            it in InputValidation.MIN_AGE..InputValidation.MAX_AGE
        } ?: 25
        prefs[PreferencesKeys.ACTIVITY_LEVEL] = userProfile.activityLevel.name
        prefs[PreferencesKeys.ONBOARDING_COMPLETED] = userProfile.isOnboardingCompleted
        prefs[PreferencesKeys.REMINDER_ENABLED] = userProfile.reminderEnabled
        val validReminderHours = userProfile.reminderStartHour in 0..23 &&
            userProfile.reminderEndHour in 0..23 &&
            userProfile.reminderEndHour > userProfile.reminderStartHour
        prefs[PreferencesKeys.REMINDER_START_HOUR] =
            if (validReminderHours) userProfile.reminderStartHour else 8
        prefs[PreferencesKeys.REMINDER_END_HOUR] =
            if (validReminderHours) userProfile.reminderEndHour else 21
        prefs[PreferencesKeys.CITY_NAME] = userProfile.city.name
        prefs[PreferencesKeys.CITY_LATITUDE] = userProfile.city.latitude
        prefs[PreferencesKeys.CITY_LONGITUDE] = userProfile.city.longitude
    }

    override suspend fun claimDailyCelebration(date: String): Boolean {
        var claimed = false
        dataStore.edit { prefs ->
            if (prefs[PreferencesKeys.LAST_SHARK_CELEBRATION_DATE] != date) {
                prefs[PreferencesKeys.LAST_SHARK_CELEBRATION_DATE] = date
                claimed = true
            }
        }
        return claimed
    }

    override suspend fun claimDailyGoalNotification(date: String): Boolean {
        var claimed = false
        dataStore.edit { prefs ->
            if (prefs[PreferencesKeys.LAST_GOAL_NOTIFICATION_DATE] != date) {
                prefs[PreferencesKeys.LAST_GOAL_NOTIFICATION_DATE] = date
                claimed = true
            }
        }
        return claimed
    }

    override suspend fun saveManualClimate(date: String, climate: ManualClimate?) {
        dataStore.edit { prefs ->
            if (climate == null) {
                prefs.remove(PreferencesKeys.MANUAL_CLIMATE_DATE)
                prefs.remove(PreferencesKeys.MANUAL_CLIMATE_LEVEL)
            } else {
                prefs[PreferencesKeys.MANUAL_CLIMATE_DATE] = date
                prefs[PreferencesKeys.MANUAL_CLIMATE_LEVEL] = climate.name
            }
        }
    }

    override suspend fun getManualClimate(date: String): ManualClimate? {
        val prefs = dataStore.data.first()
        if (prefs[PreferencesKeys.MANUAL_CLIMATE_DATE] != date) return null
        val name = prefs[PreferencesKeys.MANUAL_CLIMATE_LEVEL] ?: return null
        return ManualClimate.entries.firstOrNull { it.name == name }
    }
}
