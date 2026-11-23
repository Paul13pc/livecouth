package com.example.livecouth.domain.model

data class SyncStatus(
    val isSyncing: Boolean = false,
    val isRestoring: Boolean = false,
    val lastSuccessfulSyncAt: Long? = null,
    val error: SyncError? = null,
)

enum class SyncError {
    NETWORK,
    PERMISSION_DENIED,
    AUTH_REQUIRED,
    UNKNOWN,
}
