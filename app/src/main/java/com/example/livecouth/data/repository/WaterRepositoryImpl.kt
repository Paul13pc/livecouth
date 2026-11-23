package com.example.livecouth.data.repository

import com.example.livecouth.data.local.dao.WaterLogDao
import com.example.livecouth.data.local.dao.QuickAddButtonDao
import com.example.livecouth.data.local.entity.QuickAddButton
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class WaterRepositoryImpl @Inject constructor(
    private val waterLogDao: WaterLogDao,
    private val quickAddButtonDao: QuickAddButtonDao,
) : WaterRepository {

    override suspend fun insertWaterLog(waterLog: WaterLog): Long {
        require(waterLog.isValidInput()) { "El registro de agua no es válido." }
        return waterLogDao.insertWaterLog(waterLog)
    }

    override suspend fun deleteWaterLog(waterLog: WaterLog) {
        waterLogDao.updateWaterLog(
            waterLog.copy(
                updatedAt = maxOf(System.currentTimeMillis(), waterLog.updatedAt + 1),
                sincronizado = false,
                eliminado = true,
            ),
        )
    }

    override fun getTodayLogs(): Flow<List<WaterLog>> {
        val (start, end) = getTodayTimeRange()
        return waterLogDao.getTodayLogs(start, end).map { logs ->
            logs.filter { it.isValidInput() }
        }
    }

    override fun getTodayTotalMl(): Flow<Int> {
        return getTodayLogs().map { logs ->
            logs.sumOf(WaterLog::hydratedMl)
        }
    }

    override fun getLogsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<WaterLog>> {
        return waterLogDao.getLogsBetween(startTimestamp, endTimestamp).map { logs ->
            logs.filter { it.isValidInput() }
        }
    }

    override fun observeQuickAddButtons(): Flow<List<QuickAddButton>> =
        quickAddButtonDao.observeButtons().map { buttons ->
            buttons.filter { button ->
                InputValidation.validateName(button.name) == null &&
                    InputValidation.isValidWaterAmount(button.amountMl)
            }
        }

    override suspend fun saveQuickAddButton(button: QuickAddButton) {
        require(
            InputValidation.validateName(button.name) == null &&
                InputValidation.isValidWaterAmount(button.amountMl),
        ) { "El botón rápido no es válido." }
        if (button.id == 0L) {
            quickAddButtonDao.insertAtEnd(
                name = InputValidation.normalizeName(button.name),
                amountMl = button.amountMl,
            )
        } else {
            quickAddButtonDao.update(
                button.copy(
                    name = InputValidation.normalizeName(button.name),
                    updatedAt = maxOf(System.currentTimeMillis(), button.updatedAt + 1),
                    sincronizado = false,
                    eliminado = false,
                ),
            )
        }
    }

    override suspend fun deleteQuickAddButton(buttonId: Long) {
        quickAddButtonDao.deleteAndCompact(buttonId)
    }

    override suspend fun moveQuickAddButton(buttonId: Long, direction: Int) {
        val normalizedDirection = direction.compareTo(0)
        if (normalizedDirection != 0) {
            quickAddButtonDao.move(buttonId, normalizedDirection)
        }
    }

    private fun getTodayTimeRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    private fun WaterLog.isValidInput(): Boolean =
        InputValidation.isValidWaterAmount(cantidadMl) &&
            hydrationFactor.isFinite() &&
            hydrationFactor in 0f..2f
}
