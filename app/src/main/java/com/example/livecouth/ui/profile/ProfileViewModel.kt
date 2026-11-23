package com.example.livecouth.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.livecouth.domain.model.ActivityLevel
import com.example.livecouth.domain.model.CityLocation
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.model.ReminderFrequency
import com.example.livecouth.domain.model.ReminderSettings
import com.example.livecouth.domain.model.ReminderSettingsValidator
import com.example.livecouth.domain.model.UserProfile
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.data.location.FusedDeviceLocationProvider
import com.example.livecouth.notification.SyncScheduler
import com.example.livecouth.notification.WaterReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deviceLocationProvider: FusedDeviceLocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val profile = userPreferencesRepository.userProfile.first()
            val reminderSettings = userPreferencesRepository.reminderSettings.first()
            val usesDeviceLocation = userPreferencesRepository.usesDeviceLocationForClimate.first()
            _uiState.update {
                it.copy(
                    name = profile.name,
                    weightText = if (profile.weightKg > 0) profile.weightKg.toString() else "70",
                    ageText = if (profile.age > 0) profile.age.toString() else "25",
                    activityLevel = profile.activityLevel,
                    reminderSettings = reminderSettings,
                    customReminderIntervalText = reminderSettings.customIntervalMinutes.toString(),
                    isOnboardingCompleted = profile.isOnboardingCompleted,
                    cityName = profile.city.name,
                    latitudeText = profile.city.latitude.toString(),
                    longitudeText = profile.city.longitude.toString(),
                    usesDeviceLocationForClimate = usesDeviceLocation,
                    isLoading = false,
                )
            }
            if (profile.isOnboardingCompleted && reminderSettings.enabled) {
                WaterReminderScheduler.schedulePeriodicReminders(appContext, reminderSettings)
            } else {
                WaterReminderScheduler.cancelReminders(appContext)
            }
        }
    }

    fun onManualClimateLocationSelected() {
        _uiState.update {
            it.copy(
                usesDeviceLocationForClimate = false,
                isResolvingDeviceLocation = false,
                deviceLocationNotice = null,
            )
        }
        viewModelScope.launch {
            try {
                userPreferencesRepository.setUsesDeviceLocationForClimate(false)
            } catch (_: IOException) {
                _uiState.update {
                    it.copy(deviceLocationNotice = DeviceLocationNotice.LOCATION_STORAGE_ERROR)
                }
            }
        }
    }

    fun clearDeviceLocationNotice() {
        _uiState.update { it.copy(deviceLocationNotice = null) }
    }

    fun onDeviceLocationPermissionDenied(permanentlyDenied: Boolean) {
        val notice = if (permanentlyDenied) {
            DeviceLocationNotice.PERMISSION_PERMANENTLY_DENIED
        } else {
            DeviceLocationNotice.PERMISSION_DENIED
        }
        selectManualClimateLocation(notice)
    }

    fun onDeviceLocationPermissionGranted() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isResolvingDeviceLocation = true,
                    deviceLocationNotice = null,
                )
            }

            val savedLocation = try {
                userPreferencesRepository.getLastDeviceLocation()
            } catch (_: IOException) {
                selectManualClimateLocation(DeviceLocationNotice.LOCATION_STORAGE_ERROR)
                return@launch
            }

            if (!deviceLocationProvider.isEnabled()) {
                if (savedLocation != null) {
                    enableDeviceLocationClimate(
                        DeviceLocationNotice.LOCATION_DISABLED_LAST_USED,
                    )
                } else {
                    selectManualClimateLocation(DeviceLocationNotice.LOCATION_DISABLED_MANUAL)
                }
                return@launch
            }

            if (!deviceLocationProvider.hasPermission()) {
                selectManualClimateLocation(DeviceLocationNotice.PERMISSION_DENIED)
                return@launch
            }

            val currentLocation = deviceLocationProvider.getCurrentLocation()
            val selectedLocation = currentLocation ?: savedLocation
            if (selectedLocation == null) {
                selectManualClimateLocation(DeviceLocationNotice.LOCATION_UNAVAILABLE)
                return@launch
            }

            try {
                if (currentLocation != null) {
                    userPreferencesRepository.saveLastDeviceLocation(currentLocation)
                }
                userPreferencesRepository.setUsesDeviceLocationForClimate(true)
                _uiState.update {
                    it.copy(
                        usesDeviceLocationForClimate = true,
                        isResolvingDeviceLocation = false,
                        deviceLocationNotice = if (currentLocation == null) {
                            DeviceLocationNotice.LAST_KNOWN_LOCATION_USED
                        } else {
                            null
                        },
                    )
                }
            } catch (_: IOException) {
                selectManualClimateLocation(DeviceLocationNotice.LOCATION_STORAGE_ERROR)
            }
        }
    }

    private suspend fun enableDeviceLocationClimate(notice: DeviceLocationNotice?) {
        try {
            userPreferencesRepository.setUsesDeviceLocationForClimate(true)
            _uiState.update {
                it.copy(
                    usesDeviceLocationForClimate = true,
                    isResolvingDeviceLocation = false,
                    deviceLocationNotice = notice,
                )
            }
        } catch (_: IOException) {
            selectManualClimateLocation(DeviceLocationNotice.LOCATION_STORAGE_ERROR)
        }
    }

    private fun selectManualClimateLocation(notice: DeviceLocationNotice) {
        _uiState.update { it.withManualClimateLocation(notice) }
        viewModelScope.launch {
            try {
                userPreferencesRepository.setUsesDeviceLocationForClimate(false)
            } catch (_: IOException) {
                _uiState.update {
                    it.copy(deviceLocationNotice = DeviceLocationNotice.LOCATION_STORAGE_ERROR)
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onWeightChange(weight: String) {
        val filtered = InputValidation.filterWeightInput(weight)
        _uiState.update { it.copy(weightText = filtered) }
    }

    fun onAgeChange(age: String) {
        val filtered = InputValidation.filterIntegerInput(age)
        _uiState.update { it.copy(ageText = filtered) }
    }

    fun onActivityLevelChange(level: ActivityLevel) {
        _uiState.update { it.copy(activityLevel = level) }
    }

    fun onCityNameChange(name: String) {
        _uiState.update { it.copy(cityName = name, isCityError = false) }
    }

    fun onCityCoordinatesChange(latitude: String, longitude: String) {
        _uiState.update {
            it.copy(
                latitudeText = latitude.filter { character -> character.isDigit() || character == '.' || character == '-' },
                longitudeText = longitude.filter { character -> character.isDigit() || character == '.' || character == '-' },
                isCityError = false,
            )
        }
    }

    fun onCitySelected(city: CityLocation) {
        _uiState.update {
            it.copy(
                cityName = city.name,
                latitudeText = city.latitude.toString(),
                longitudeText = city.longitude.toString(),
                isCityError = false,
            )
        }
    }

    fun onReminderEnabledChange(enabled: Boolean) {
        updateReminderSettings { it.copy(enabled = enabled) }
    }

    fun onReminderStartMinuteChange(startMinute: Int) {
        updateReminderSettings { it.copy(startMinute = startMinute.coerceIn(0, 1439)) }
    }

    fun onReminderEndMinuteChange(endMinute: Int) {
        updateReminderSettings { it.copy(endMinute = endMinute.coerceIn(0, 1439)) }
    }

    fun onReminderFrequencyChange(frequency: ReminderFrequency) {
        updateReminderSettings { it.copy(frequency = frequency) }
    }

    fun onCustomReminderIntervalChange(value: String) {
        val filtered = InputValidation.filterIntegerInput(value)
        val interval = filtered.toIntOrNull() ?: 0
        val settings = _uiState.value.reminderSettings.copy(customIntervalMinutes = interval)
        _uiState.update {
            it.copy(
                reminderSettings = settings,
                customReminderIntervalText = filtered,
            )
        }
        persistReminderSettings(settings)
    }

    fun onReminderDayToggle(mondayFirstDayIndex: Int) {
        require(mondayFirstDayIndex in 0..6) { "El día de recordatorio no es válido." }
        updateReminderSettings { settings ->
            settings.copy(activeDaysMask = settings.activeDaysMask xor (1 shl mondayFirstDayIndex))
        }
    }

    fun onQuietHoursEnabledChange(enabled: Boolean) {
        updateReminderSettings { it.copy(quietHoursEnabled = enabled) }
    }

    fun onQuietStartMinuteChange(minute: Int) {
        updateReminderSettings { it.copy(quietStartMinute = minute.coerceIn(0, 1439)) }
    }

    fun onQuietEndMinuteChange(minute: Int) {
        updateReminderSettings { it.copy(quietEndMinute = minute.coerceIn(0, 1439)) }
    }

    private fun updateReminderSettings(update: (ReminderSettings) -> ReminderSettings) {
        val settings = update(_uiState.value.reminderSettings)
        _uiState.update {
            it.copy(
                reminderSettings = settings,
                customReminderIntervalText = settings.customIntervalMinutes.toString(),
            )
        }
        persistReminderSettings(settings)
    }

    private fun persistReminderSettings(settings: ReminderSettings) {
        if (ReminderSettingsValidator.validate(settings) != null) return
        viewModelScope.launch {
            userPreferencesRepository.saveReminderSettings(settings)
            if (settings.enabled && _uiState.value.isOnboardingCompleted) {
                WaterReminderScheduler.schedulePeriodicReminders(appContext, settings)
            } else {
                WaterReminderScheduler.cancelReminders(appContext)
            }
        }
    }

    fun saveProfile(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.canSave) return@launch
            val city = state.cityLocation
                ?: return@launch
            val profile = UserProfile(
                name = InputValidation.normalizeName(state.name),
                weightKg = state.weightKg,
                age = state.age,
                activityLevel = state.activityLevel,
                isOnboardingCompleted = true,
                reminderEnabled = state.reminderSettings.enabled,
                reminderStartHour = state.reminderSettings.startMinute / 60,
                reminderEndHour = state.reminderSettings.endMinute / 60,
                city = city,
            )
            userPreferencesRepository.saveReminderSettings(state.reminderSettings)
            userPreferencesRepository.saveUserProfile(profile)
            SyncScheduler.enqueueImmediate(context)

            if (state.reminderSettings.enabled) {
                WaterReminderScheduler.schedulePeriodicReminders(context, state.reminderSettings)
            } else {
                WaterReminderScheduler.cancelReminders(context)
            }

            _uiState.update {
                it.copy(isSaved = true, isOnboardingCompleted = true)
            }
            onSuccess()
        }
    }

    fun triggerTestNotification(context: Context) {
        WaterReminderScheduler.triggerImmediateTestReminder(context)
    }
}
