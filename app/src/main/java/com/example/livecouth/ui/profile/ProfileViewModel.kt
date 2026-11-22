package com.example.livecouth.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livecouth.domain.model.ActivityLevel
import com.example.livecouth.domain.model.UserProfile
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.notification.WaterReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val profile = userPreferencesRepository.userProfile.first()
            _uiState.update {
                it.copy(
                    name = profile.name,
                    weightText = if (profile.weightKg > 0) profile.weightKg.toString() else "70",
                    ageText = if (profile.age > 0) profile.age.toString() else "25",
                    activityLevel = profile.activityLevel,
                    reminderEnabled = profile.reminderEnabled,
                    reminderStartHour = profile.reminderStartHour,
                    reminderEndHour = profile.reminderEndHour,
                    isLoading = false,
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onWeightChange(weight: String) {
        val filtered = weight.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(weightText = filtered) }
    }

    fun onAgeChange(age: String) {
        val filtered = age.filter { it.isDigit() }
        _uiState.update { it.copy(ageText = filtered) }
    }

    fun onActivityLevelChange(level: ActivityLevel) {
        _uiState.update { it.copy(activityLevel = level) }
    }

    fun onReminderEnabledChange(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
    }

    fun onReminderStartHourChange(startHour: Int) {
        _uiState.update { it.copy(reminderStartHour = startHour.coerceIn(0, 23)) }
    }

    fun onReminderEndHourChange(endHour: Int) {
        _uiState.update { it.copy(reminderEndHour = endHour.coerceIn(0, 23)) }
    }

    fun saveProfile(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val profile = UserProfile(
                name = state.name.trim(),
                weightKg = state.weightKg.coerceAtLeast(20f),
                age = state.age.coerceAtLeast(10),
                activityLevel = state.activityLevel,
                isOnboardingCompleted = true,
                reminderEnabled = state.reminderEnabled,
                reminderStartHour = state.reminderStartHour,
                reminderEndHour = state.reminderEndHour,
            )
            userPreferencesRepository.saveUserProfile(profile)

            if (state.reminderEnabled) {
                WaterReminderScheduler.schedulePeriodicReminders(context)
            } else {
                WaterReminderScheduler.cancelReminders(context)
            }

            _uiState.update { it.copy(isSaved = true) }
            onSuccess()
        }
    }

    fun triggerTestNotification(context: Context) {
        WaterReminderScheduler.triggerImmediateTestReminder(context)
    }
}
