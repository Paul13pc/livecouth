package com.example.livecouth.ui.profile

import com.example.livecouth.domain.model.ActivityLevel
import com.example.livecouth.domain.model.CityLocation
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.model.ReminderSettings
import com.example.livecouth.domain.model.ReminderSettingsError
import com.example.livecouth.domain.model.ReminderSettingsValidator
import com.example.livecouth.domain.model.ValidationError
import com.example.livecouth.domain.model.WaterGoalCalculator

enum class DeviceLocationNotice {
    PERMISSION_DENIED,
    PERMISSION_PERMANENTLY_DENIED,
    LOCATION_DISABLED_LAST_USED,
    LOCATION_DISABLED_MANUAL,
    LAST_KNOWN_LOCATION_USED,
    LOCATION_UNAVAILABLE,
    LOCATION_STORAGE_ERROR,
}

data class ProfileUiState(
    val name: String = "",
    val weightText: String = "70",
    val ageText: String = "25",
    val activityLevel: ActivityLevel = ActivityLevel.MEDIO,
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val customReminderIntervalText: String = "60",
    val isOnboardingCompleted: Boolean = false,
    val cityName: String = CityLocation.MANTA.name,
    val latitudeText: String = CityLocation.MANTA.latitude.toString(),
    val longitudeText: String = CityLocation.MANTA.longitude.toString(),
    val isCityError: Boolean = false,
    val usesDeviceLocationForClimate: Boolean = false,
    val isResolvingDeviceLocation: Boolean = false,
    val deviceLocationNotice: DeviceLocationNotice? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
) {
    val weightKg: Float
        get() = weightText.toFloatOrNull() ?: 0f

    val age: Int
        get() = ageText.toIntOrNull() ?: 0

    val nameError: ValidationError?
        get() = InputValidation.validateName(name)

    val weightError: ValidationError?
        get() = InputValidation.validateWeight(weightText)

    val ageError: ValidationError?
        get() = InputValidation.validateAge(ageText)

    val reminderSettingsError: ReminderSettingsError?
        get() = ReminderSettingsValidator.validate(reminderSettings)

    val canSave: Boolean
        get() = !isLoading &&
            nameError == null &&
            weightError == null &&
            ageError == null &&
            reminderSettingsError == null &&
            cityLocation != null

    val calculatedGoalMl: Int
        get() = WaterGoalCalculator.calculateGoalMl(weightKg, activityLevel)

    val cityLocation: CityLocation?
        get() {
            val latitude = latitudeText.toFloatOrNull() ?: return null
            val longitude = longitudeText.toFloatOrNull() ?: return null
            return CityLocation(cityName.trim(), latitude, longitude)
                .takeIf { it.name.isNotEmpty() && it.hasValidCoordinates() }
        }
}

internal fun ProfileUiState.withManualClimateLocation(
        notice: DeviceLocationNotice,
): ProfileUiState = copy(
        usesDeviceLocationForClimate = false,
        isResolvingDeviceLocation = false,
        deviceLocationNotice = notice,
)
