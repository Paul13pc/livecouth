package com.example.livecouth.ui.history

import com.example.livecouth.domain.model.DailyWaterSummary

data class HistoryUiState(
    val last7DaysSummaries: List<DailyWaterSummary> = emptyList(),
    val selectedDaySummary: DailyWaterSummary? = null,
    val weeklyAverageMl: Int = 0,
    val currentStreakDays: Int = 0,
    val dailyGoalMl: Int = 2000,
    val isLoading: Boolean = true,
)
