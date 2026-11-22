package com.example.livecouth.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.domain.model.DailyWaterSummary
import com.example.livecouth.domain.model.HistoryCalculator
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.domain.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistoryData()
    }

    private fun loadHistoryData() {
        val (start7Days, endToday) = getLast7DaysRange()

        viewModelScope.launch {
            combine(
                waterRepository.getLogsBetween(start7Days, endToday),
                userPreferencesRepository.userProfile,
            ) { logs, profile ->
                val goalMl = profile.dailyGoalMl
                val summaries = build7DaysSummaries(logs, goalMl)
                val average = HistoryCalculator.calculateWeeklyAverage(summaries.map { it.totalMl })
                val streak = HistoryCalculator.calculateCurrentStreak(summaries)

                val selected = _uiState.value.selectedDaySummary
                    ?: summaries.lastOrNull()

                _uiState.value.copy(
                    last7DaysSummaries = summaries,
                    selectedDaySummary = selected,
                    weeklyAverageMl = average,
                    currentStreakDays = streak,
                    dailyGoalMl = goalMl,
                    isLoading = false,
                )
            }.collect { updatedState ->
                _uiState.value = updatedState
            }
        }
    }

    fun selectDaySummary(summary: DailyWaterSummary) {
        _uiState.update { it.copy(selectedDaySummary = summary) }
    }

    private fun build7DaysSummaries(
        logs: List<WaterLog>,
        goalMl: Int,
    ): List<DailyWaterSummary> {
        val calendar = Calendar.getInstance()
        val summaries = mutableListOf<DailyWaterSummary>()
        val dayFormat = SimpleDateFormat("E", Locale.forLanguageTag("es-ES"))

        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStart = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val dayEnd = calendar.timeInMillis

            val dayLogs = logs.filter { it.fechaHora in dayStart..dayEnd }
            val dayTotal = dayLogs.sumOf { it.cantidadMl }
            val dayName = dayFormat.format(Date(dayStart)).replaceFirstChar { it.uppercase() }

            summaries.add(
                DailyWaterSummary(
                    dateTimestamp = dayStart,
                    dayLabel = dayName,
                    totalMl = dayTotal,
                    goalMl = goalMl,
                    logs = dayLogs,
                ),
            )
        }
        return summaries
    }

    private fun getLast7DaysRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start7DaysAgo = calendar.timeInMillis

        val nowCalendar = Calendar.getInstance()
        nowCalendar.set(Calendar.HOUR_OF_DAY, 23)
        nowCalendar.set(Calendar.MINUTE, 59)
        nowCalendar.set(Calendar.SECOND, 59)
        nowCalendar.set(Calendar.MILLISECOND, 999)
        val endToday = nowCalendar.timeInMillis

        return Pair(start7DaysAgo, endToday)
    }
}
