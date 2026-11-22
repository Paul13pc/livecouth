package com.example.livecouth.domain.model

import com.example.livecouth.data.local.entity.WaterLog

data class DailyWaterSummary(
    val dateTimestamp: Long,
    val dayLabel: String,
    val totalMl: Int,
    val goalMl: Int,
    val logs: List<WaterLog>,
) {
    val isGoalReached: Boolean get() = totalMl >= goalMl
}

object HistoryCalculator {

    /**
     * Pure function to calculate weekly average in ml.
     */
    fun calculateWeeklyAverage(dailyTotals: List<Int>): Int {
        if (dailyTotals.isEmpty()) return 0
        val sum = dailyTotals.sum()
        return sum / dailyTotals.size
    }

    /**
     * Pure function to calculate current streak in days.
     */
    fun calculateCurrentStreak(summaries: List<DailyWaterSummary>): Int {
        if (summaries.isEmpty()) return 0

        val sorted = summaries.sortedByDescending { it.dateTimestamp }
        var streak = 0

        for ((index, summary) in sorted.withIndex()) {
            if (summary.isGoalReached) {
                streak++
            } else {
                // If today (most recent index 0) is not reached yet, don't break previous streak
                if (index == 0 && summary.totalMl < summary.goalMl) {
                    continue
                }
                break
            }
        }
        return streak
    }
}
