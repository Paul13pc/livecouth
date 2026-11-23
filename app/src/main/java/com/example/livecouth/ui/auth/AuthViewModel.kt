package com.example.livecouth.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteException
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livecouth.BuildConfig
import com.example.livecouth.data.remote.SyncDataException
import com.example.livecouth.data.repository.GoogleAuthConfigurationException
import com.example.livecouth.data.repository.GoogleSignInCancelledException
import com.example.livecouth.domain.repository.AuthRepository
import com.example.livecouth.domain.repository.SyncRepository
import com.example.livecouth.notification.SyncScheduler
import com.example.livecouth.notification.WaterReminderScheduler
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    @param:ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
                if (user != null) SyncScheduler.schedulePeriodic(applicationContext)
            }
        }
        viewModelScope.launch {
            syncRepository.status.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
    }

    fun signIn(context: Context) {
        if (_uiState.value.isSigningIn || _uiState.value.isSigningOut) return

        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        authDebug("step=configuration webClientIdConfigured=${webClientId.isNotEmpty()}")
        if (webClientId.isBlank()) {
            _uiState.update { it.copy(error = AuthUiError.CONFIGURATION) }
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            _uiState.update { it.copy(error = AuthUiError.CREDENTIAL) }
            return
        }

        _uiState.update { it.copy(isSigningIn = true, error = null) }
        viewModelScope.launch {
            var step = "credential_request"
            try {
                step = "google_credential_flow"
                authRepository.signInWithGoogle(activity, webClientId)
                SyncScheduler.scheduleAfterSignIn(activity)
                _uiState.update { it.copy(isSigningIn = false, error = null) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: GoogleSignInCancelledException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        error = if (exception.elapsedMs < 1_000) {
                            AuthUiError.FAST_CANCELLATION_CONFIGURATION
                        } else {
                            AuthUiError.USER_CANCELLED
                        },
                    )
                }
            } catch (exception: GetCredentialCancellationException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(isSigningIn = false, error = AuthUiError.USER_CANCELLED)
                }
            } catch (exception: NoCredentialException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(isSigningIn = false, error = AuthUiError.NO_GOOGLE_ACCOUNT)
                }
            } catch (exception: GetCredentialProviderConfigurationException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(isSigningIn = false, error = AuthUiError.CONFIGURATION_ERROR)
                }
            } catch (exception: GetCredentialException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        error = when {
                            exception.isGoogleConfigurationError() ->
                                AuthUiError.CONFIGURATION_ERROR
                            !hasNetworkConnection(context) -> AuthUiError.NETWORK
                            exception is androidx.credentials.exceptions.GetCredentialUnknownException ->
                                AuthUiError.UNKNOWN
                            else -> AuthUiError.CREDENTIAL
                        },
                    )
                }
            } catch (exception: GoogleIdTokenParsingException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(isSigningIn = false, error = AuthUiError.UNKNOWN)
                }
            } catch (exception: FirebaseNetworkException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(isSigningIn = false, error = AuthUiError.NETWORK)
                }
            } catch (exception: FirebaseAuthInvalidCredentialsException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(isSigningIn = false, error = AuthUiError.CONFIGURATION_ERROR)
                }
            } catch (exception: GoogleAuthConfigurationException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(isSigningIn = false, error = AuthUiError.CONFIGURATION)
                }
            } catch (exception: FirebaseException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(isSigningIn = false, error = AuthUiError.FIREBASE)
                }
            } catch (exception: IOException) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(isSigningIn = false, error = AuthUiError.NETWORK)
                }
            } catch (exception: Exception) {
                authDebugFailure(step, exception)
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        error = if (exception.isGoogleConfigurationError()) {
                            AuthUiError.CONFIGURATION_ERROR
                        } else {
                            AuthUiError.UNKNOWN
                        },
                    )
                }
            }
        }
    }

    fun synchronizeNow() {
        val user = _uiState.value.user ?: return
        if (_uiState.value.syncStatus.isSyncing) return
        _uiState.update { it.copy(syncActionError = null) }
        viewModelScope.launch {
            try {
                syncRepository.synchronize(user.uid)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: FirebaseFirestoreException) {
                _uiState.update {
                    it.copy(
                        syncActionError = if (
                            exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                        ) {
                            AuthUiError.SYNC_PERMISSION
                        } else {
                            AuthUiError.SYNC_FAILED
                        },
                    )
                }
            } catch (exception: FirebaseNetworkException) {
                _uiState.update { it.copy(syncActionError = AuthUiError.SYNC_NETWORK) }
            } catch (exception: IOException) {
                _uiState.update { it.copy(syncActionError = AuthUiError.SYNC_NETWORK) }
            } catch (exception: SyncDataException) {
                _uiState.update { it.copy(syncActionError = AuthUiError.SYNC_FAILED) }
            } catch (exception: FirebaseException) {
                _uiState.update { it.copy(syncActionError = AuthUiError.SYNC_FAILED) }
            }
        }
    }

    fun signOut(context: Context, deleteLocalData: Boolean) {
        if (_uiState.value.isSigningOut) return
        if (_uiState.value.syncStatus.isSyncing) {
            _uiState.update { it.copy(syncActionError = AuthUiError.SYNC_IN_PROGRESS) }
            return
        }
        _uiState.update {
            it.copy(isSigningOut = true, syncActionError = null)
        }
        viewModelScope.launch {
            try {
                SyncScheduler.cancel(context)
                if (deleteLocalData) {
                    syncRepository.clearLocalData()
                    WaterReminderScheduler.cancelReminders(context)
                }
                authRepository.signOut()
                syncRepository.resetSyncStatus()
                try {
                    val credentialContext = context.findActivity() ?: context
                    CredentialManager.create(credentialContext)
                        .clearCredentialState(ClearCredentialStateRequest())
                } catch (exception: GetCredentialException) {
                    _uiState.update {
                        it.copy(isSigningOut = false, error = AuthUiError.CREDENTIAL)
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(isSigningOut = false, error = null, syncActionError = null)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                if (deleteLocalData) SyncScheduler.scheduleAfterSignIn(context)
                _uiState.update {
                    it.copy(isSigningOut = false, syncActionError = AuthUiError.LOCAL_DATA)
                }
            } catch (exception: SQLiteException) {
                if (deleteLocalData) SyncScheduler.scheduleAfterSignIn(context)
                _uiState.update {
                    it.copy(isSigningOut = false, syncActionError = AuthUiError.LOCAL_DATA)
                }
            }
        }
    }

    private fun hasNetworkConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    }

    private fun Exception.isGoogleConfigurationError(): Boolean {
        val errorMessage = message.orEmpty()
        return this is GetCredentialProviderConfigurationException ||
            this is FirebaseAuthInvalidCredentialsException ||
            errorMessage.contains("DEVELOPER_ERROR", ignoreCase = true) ||
            errorMessage.contains("SHA-1", ignoreCase = true) ||
            errorMessage.contains("client id", ignoreCase = true) ||
            errorMessage.contains("client_id", ignoreCase = true)
    }

    private fun authDebug(message: String) {
        if (BuildConfig.DEBUG) Log.d(AUTH_DEBUG_TAG, message)
    }

    private fun authDebugFailure(step: String, exception: Exception) {
        if (BuildConfig.DEBUG) {
            Log.w(
                AUTH_DEBUG_TAG,
                "step=$step exception=${exception.javaClass.name} message=${exception.message}",
            )
        }
    }

    private companion object {
        const val AUTH_DEBUG_TAG = "AuthDebug"
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext: Context = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return currentContext as? Activity
}
