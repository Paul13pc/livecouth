package com.example.livecouth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.livecouth.data.local.entity.QuickAddButton
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickAddButtonDao {

    @Query("SELECT * FROM quick_add_buttons WHERE eliminado = 0 ORDER BY sortOrder, id")
    fun observeButtons(): Flow<List<QuickAddButton>>

    @Query("SELECT * FROM quick_add_buttons WHERE eliminado = 0 ORDER BY sortOrder, id")
    suspend fun getButtons(): List<QuickAddButton>

    @Query("SELECT * FROM quick_add_buttons")
    suspend fun getAllForSync(): List<QuickAddButton>

    @Query("SELECT COUNT(*) FROM quick_add_buttons")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(button: QuickAddButton): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(buttons: List<QuickAddButton>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRemote(button: QuickAddButton)

    @Update
    suspend fun update(button: QuickAddButton)

    @Query("UPDATE quick_add_buttons SET eliminado = 1, updatedAt = :updatedAt, sincronizado = 0 WHERE id = :buttonId")
    suspend fun softDeleteById(buttonId: Long, updatedAt: Long)

    @Query("UPDATE quick_add_buttons SET sortOrder = :sortOrder, updatedAt = :updatedAt, sincronizado = 0 WHERE id = :buttonId")
    suspend fun updateSortOrder(buttonId: Long, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE quick_add_buttons SET sincronizado = 1 WHERE syncId = :syncId AND updatedAt = :updatedAt")
    suspend fun markSynced(syncId: String, updatedAt: Long)

    @Query("DELETE FROM quick_add_buttons")
    suspend fun deleteAll()

    @Transaction
    suspend fun insertAtEnd(name: String, amountMl: Int) {
        insert(
            QuickAddButton(
                name = name,
                amountMl = amountMl,
                sortOrder = getButtons().size,
                updatedAt = System.currentTimeMillis(),
                sincronizado = false,
            ),
        )
    }

    @Transaction
    suspend fun deleteAndCompact(buttonId: Long) {
        val updatedAt = maxOf(
            System.currentTimeMillis(),
            getButtons().maxOfOrNull { it.updatedAt + 1 } ?: 0L,
        )
        softDeleteById(buttonId, updatedAt)
        getButtons().forEachIndexed { index, button ->
            updateSortOrder(button.id, index, updatedAt)
        }
    }

    @Transaction
    suspend fun move(buttonId: Long, direction: Int) {
        val buttons = getButtons().toMutableList()
        val currentIndex = buttons.indexOfFirst { it.id == buttonId }
        val targetIndex = currentIndex + direction
        if (currentIndex !in buttons.indices || targetIndex !in buttons.indices) return

        val current = buttons[currentIndex]
        buttons[currentIndex] = buttons[targetIndex]
        buttons[targetIndex] = current
        val updatedAt = maxOf(
            System.currentTimeMillis(),
            buttons.maxOfOrNull { it.updatedAt + 1 } ?: 0L,
        )
        buttons.forEachIndexed { index, button ->
            updateSortOrder(button.id, index, updatedAt)
        }
    }

    suspend fun restoreDefaultButtons() {
        deleteAll()
        insertAll(defaultButtons())
    }

    fun defaultButtons(): List<QuickAddButton> = listOf(
        QuickAddButton(
            name = "+150 ml",
            amountMl = 150,
            sortOrder = 0,
            updatedAt = 0,
            sincronizado = true,
        ),
        QuickAddButton(
            name = "+250 ml",
            amountMl = 250,
            sortOrder = 1,
            updatedAt = 0,
            sincronizado = true,
        ),
        QuickAddButton(
            name = "+500 ml",
            amountMl = 500,
            sortOrder = 2,
            updatedAt = 0,
            sincronizado = true,
        ),
    )
}
