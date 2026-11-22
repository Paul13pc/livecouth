package com.example.livecouth.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.domain.model.SharkState
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.domain.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaterUiState())
    val uiState: StateFlow<WaterUiState> = _uiState.asStateFlow()

    init {
        observeWaterData()
    }

    private fun observeWaterData() {
        viewModelScope.launch {
            combine(
                waterRepository.getTodayLogs(),
                waterRepository.getTodayTotalMl(),
                userPreferencesRepository.userProfile,
            ) { logs, totalMl, profile ->
                val currentTotal = totalMl
                val goal = profile.dailyGoalMl
                val newState = SharkState.calculateState(currentTotal, goal)
                _uiState.value.copy(
                    todayLogs = logs,
                    currentMl = currentTotal,
                    goalMl = goal,
                    sharkState = newState,
                )
            }.collect { updatedState ->
                _uiState.value = updatedState
            }
        }
    }

    fun addWater(amountMl: Int) {
        if (amountMl <= 0) return
        viewModelScope.launch {
            waterRepository.insertWaterLog(
                WaterLog(cantidadMl = amountMl),
            )
            triggerSplashAnimation()
        }
    }

    fun deleteWaterLog(waterLog: WaterLog) {
        viewModelScope.launch {
            waterRepository.deleteWaterLog(waterLog)
        }
    }

    fun showCustomDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomAmountDialog = show) }
    }

    private fun triggerSplashAnimation() {
        _uiState.update { it.copy(isWaterAddedAnimationTriggered = true) }
    }

    fun onSplashAnimationFinished() {
        _uiState.update { it.copy(isWaterAddedAnimationTriggered = false) }
    }
}
