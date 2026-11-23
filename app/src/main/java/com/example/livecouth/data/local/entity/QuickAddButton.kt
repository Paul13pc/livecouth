package com.example.livecouth.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "quick_add_buttons",
    indices = [Index(value = ["syncId"], unique = true)],
)
data class QuickAddButton(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amountMl: Int,
    val sortOrder: Int,
    @androidx.room.ColumnInfo(defaultValue = "''")
    val syncId: String = UUID.randomUUID().toString(),
    @androidx.room.ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    @androidx.room.ColumnInfo(defaultValue = "0")
    val sincronizado: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "0")
    val eliminado: Boolean = false,
)
