package com.example.livecouth.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryCalculatorTest {

    @Test
    fun `calculateWeeklyAverage returns correct average`() {
        val dailyTotals = listOf(2000, 1500, 2500, 2000, 1800, 2200, 2100)
        val avg = HistoryCalculator.calculateWeeklyAverage(dailyTotals)
        assertEquals(2014, avg)
    }

    @Test
    fun `calculateWeeklyAverage with empty list returns zero`() {
        val avg = HistoryCalculator.calculateWeeklyAverage(emptyList())
        assertEquals(0, avg)
    }

    @Test
    fun `calculateCurrentStreak counts consecutive met goals`() {
        val summaries = listOf(
            DailyWaterSummary(1000, "Lun", 2000, 2000, emptyList()),
            DailyWaterSummary(2000, "Mar", 2200, 2000, emptyList()),
            DailyWaterSummary(3000, "Mié", 2100, 2000, emptyList()),
        )
        val streak = HistoryCalculator.calculateCurrentStreak(summaries)
        assertEquals(3, streak)
    }

    @Test
    fun `calculateCurrentStreak breaks on missed day`() {
        val summaries = listOf(
            DailyWaterSummary(1000, "Lun", 2000, 2000, emptyList()),
            DailyWaterSummary(2000, "Mar", 1000, 2000, emptyList()), // Missed
            DailyWaterSummary(3000, "Mié", 2100, 2000, emptyList()),
        )
        val streak = HistoryCalculator.calculateCurrentStreak(summaries)
        assertEquals(1, streak)
    }
}
