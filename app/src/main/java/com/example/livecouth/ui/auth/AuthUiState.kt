package com.example.livecouth.ui.auth

import com.example.livecouth.domain.model.AuthUser
import com.example.livecouth.domain.model.SyncStatus

data class AuthUiState(
    val user: AuthUser? = null,
    val isSigningIn: Boolean = false,
    val isSigningOut: Boolean = false,
    val error: AuthUiError? = null,
    val syncActionError: AuthUiError? = null,
    val syncStatus: SyncStatus = SyncStatus(),
)

enum class AuthUiError {
    CONFIGURATION,
    CONFIGURATION_ERROR,
    FAST_CANCELLATION_CONFIGURATION,
    NO_GOOGLE_ACCOUNT,
    USER_CANCELLED,
    CREDENTIAL,
    NETWORK,
    FIREBASE,
    UNKNOWN,
    LOCAL_DATA,
    SYNC_NETWORK,
    SYNC_PERMISSION,
    SYNC_FAILED,
    SYNC_IN_PROGRESS,
}
