package com.example.livecouth.domain.repository

import com.example.livecouth.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    val status: Flow<SyncStatus>

    suspend fun synchronize(uid: String)
    suspend fun clearLocalData()
    suspend fun resetSyncStatus()
}
