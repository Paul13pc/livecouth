package com.example.livecouth.ui.water

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.data.local.entity.QuickAddButton
import com.example.livecouth.domain.model.DrinkType
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.model.ManualClimate
import com.example.livecouth.domain.model.SharkState
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.domain.repository.WaterRepository
import com.example.livecouth.domain.repository.WeatherRepository
import com.example.livecouth.domain.repository.WeatherResult
import com.example.livecouth.notification.SyncScheduler
import com.example.livecouth.notification.WaterReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class WaterViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val waterRepository: WaterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val weatherRepository: WeatherRepository,
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
            ) { logs, totalMl -> logs to totalMl }
                .collect { (logs, totalMl) ->
                    _uiState.update {
                        it.copy(todayLogs = logs, currentMl = totalMl).refreshSharkState()
                    }
                    maybeCelebrate()
                }
        }

        viewModelScope.launch {
            waterRepository.observeQuickAddButtons().collect { buttons ->
                _uiState.update { it.copy(quickAddButtons = buttons) }
            }
        }

        viewModelScope.launch {
            combine(
                userPreferencesRepository.userProfile,
                userPreferencesRepository.usesDeviceLocationForClimate,
            ) { profile, useDeviceLocation ->
                profile to useDeviceLocation
            }.collect { (profile, useDeviceLocation) ->
                _uiState.update {
                    it.copy(
                        baseGoalMl = profile.dailyGoalMl,
                        cityName = profile.city.name,
                        weather = null,
                        isWeatherLoading = true,
                        isWeatherUnavailable = false,
                        isGoalReady = false,
                    ).refreshSharkState()
                }

                val weatherResult = weatherRepository.getTodayWeather(
                    manualCity = profile.city,
                    useDeviceLocation = useDeviceLocation,
                )
                val savedManualClimate = if (weatherResult is WeatherResult.Offline) {
                    userPreferencesRepository.getManualClimate(todayDate())
                } else {
                    null
                }
                _uiState.update { state ->
                    when (weatherResult) {
                        is WeatherResult.Available -> state.copy(
                            weather = weatherResult.weather,
                            cityName = weatherResult.weather.cityName,
                            isWeatherLoading = false,
                            isWeatherUnavailable = false,
                            isGoalReady = true,
                            manualClimate = null,
                        )

                        is WeatherResult.Offline -> state.copy(
                            weather = weatherResult.cachedWeather,
                            cityName = weatherResult.cachedWeather?.cityName
                                ?: weatherResult.requestedCityName,
                            isWeatherLoading = false,
                            isWeatherUnavailable = true,
                            isGoalReady = true,
                            manualClimate = savedManualClimate.takeIf {
                                weatherResult.cachedWeather == null
                            },
                        )
                    }.refreshSharkState()
                }
                maybeCelebrate()
            }
        }
    }

    fun onManualClimateChange(climate: ManualClimate) {
        _uiState.update { it.copy(manualClimate = climate).refreshSharkState() }
        viewModelScope.launch {
            userPreferencesRepository.saveManualClimate(todayDate(), climate)
            maybeCelebrate()
        }
    }

    private suspend fun maybeCelebrate() {
        val state = _uiState.value
        if (state.isGoalReady && state.goalMl > 0 && state.currentMl >= state.goalMl &&
            userPreferencesRepository.claimDailyCelebration(todayDate())
        ) {
            WaterReminderScheduler.triggerGoalCheck(context)
            _uiState.update { it.copy(isCelebrationTriggered = true) }
            viewModelScope.launch {
                delay(1800)
                _uiState.update { it.copy(isCelebrationTriggered = false) }
            }
        }
    }

    fun addWater(amountMl: Int) {
        addDrink(amountMl, DrinkType.WATER, DrinkType.WATER.defaultHydrationFactor)
    }

    fun addDrink(amountMl: Int, drinkType: DrinkType, hydrationFactor: Float) {
        require(
            InputValidation.isValidWaterAmount(amountMl) &&
                hydrationFactor.isFinite() &&
                hydrationFactor in 0f..2f,
        ) { "El registro de agua no es válido." }
        viewModelScope.launch {
            waterRepository.insertWaterLog(
                WaterLog(
                    cantidadMl = amountMl,
                    drinkType = drinkType,
                    hydrationFactor = hydrationFactor,
                ),
            )
            SyncScheduler.enqueueImmediate(context)
            triggerSplashAnimation()
        }
    }

    fun addQuickAmount(buttonId: Long) {
        val button = _uiState.value.quickAddButtons.firstOrNull { it.id == buttonId } ?: return
        addWater(button.amountMl)
    }

    fun showQuickButtonsEditor(show: Boolean) {
        _uiState.update { it.copy(showQuickButtonsEditor = show) }
    }

    fun saveQuickAddButton(id: Long?, name: String, amountMl: Int) {
        require(
            InputValidation.validateName(name) == null &&
                InputValidation.isValidWaterAmount(amountMl),
        ) { "El botón rápido no es válido." }
        viewModelScope.launch {
            val existing = id?.let { buttonId ->
                _uiState.value.quickAddButtons.firstOrNull { it.id == buttonId }
            }
            waterRepository.saveQuickAddButton(
                QuickAddButton(
                    id = id ?: 0,
                    name = InputValidation.normalizeName(name),
                    amountMl = amountMl,
                    sortOrder = existing?.sortOrder ?: _uiState.value.quickAddButtons.size,
                    syncId = existing?.syncId ?: java.util.UUID.randomUUID().toString(),
                    updatedAt = System.currentTimeMillis(),
                    sincronizado = false,
                    eliminado = false,
                ),
            )
            SyncScheduler.enqueueImmediate(context)
        }
    }

    fun deleteQuickAddButton(buttonId: Long) {
        viewModelScope.launch {
            waterRepository.deleteQuickAddButton(buttonId)
            SyncScheduler.enqueueImmediate(context)
        }
    }

    fun moveQuickAddButton(buttonId: Long, direction: Int) {
        viewModelScope.launch {
            waterRepository.moveQuickAddButton(buttonId, direction)
            SyncScheduler.enqueueImmediate(context)
        }
    }

    fun deleteWaterLog(waterLog: WaterLog) {
        viewModelScope.launch {
            waterRepository.deleteWaterLog(waterLog)
            SyncScheduler.enqueueImmediate(context)
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

    private fun todayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

private fun WaterUiState.refreshSharkState(): WaterUiState =
    copy(sharkState = SharkState.calculateState(currentMl, goalMl))
