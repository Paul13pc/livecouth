package com.example.livecouth.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.livecouth.R
import com.example.livecouth.domain.model.SyncError
import com.example.livecouth.ui.auth.AuthUiError
import com.example.livecouth.ui.auth.AuthUiState
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountSignInCard(
    state: AuthUiState,
    onSignIn: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: (deleteLocalData: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.account_backup_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.account_backup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.user == null) {
                Button(
                    onClick = onSignIn,
                    enabled = !state.isSigningIn && !state.isSigningOut,
                ) {
                    if (state.isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(
                            if (state.isSigningIn) {
                                R.string.auth_signing_in
                            } else {
                                R.string.auth_sign_in_google
                            },
                        ),
                    )
                }
                state.error?.let { error ->
                    ErrorText(text = stringResource(error.toMessageResource()))
                }
                state.syncStatus.error?.let { error ->
                    ErrorText(text = stringResource(error.toMessageResource()))
                }
                if (state.syncStatus.error == null) {
                    state.syncActionError?.let { error ->
                        ErrorText(text = stringResource(error.toMessageResource()))
                    }
                }
            } else {
                val accountLabel = state.user.email
                    ?: state.user.displayName
                    ?: stringResource(R.string.account_google)
                Text(
                    text = stringResource(R.string.auth_signed_in_as, accountLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (state.syncStatus.isSyncing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (state.syncStatus.isRestoring) {
                                    R.string.auth_restoring
                                } else {
                                    R.string.auth_syncing
                                },
                            ),
                        )
                    }
                } else {
                    val lastSyncLabel = state.syncStatus.lastSuccessfulSyncAt?.let { timestamp ->
                        DateFormat.getDateTimeInstance(
                            DateFormat.SHORT,
                            DateFormat.SHORT,
                            Locale.getDefault(),
                        ).format(Date(timestamp))
                    } ?: stringResource(R.string.auth_never_synced)
                    Text(
                        text = stringResource(R.string.auth_last_sync, lastSyncLabel),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (state.syncStatus.error == null) {
                    state.syncActionError?.let { error ->
                        ErrorText(text = stringResource(error.toMessageResource()))
                    }
                }
                state.error?.let { error ->
                    ErrorText(text = stringResource(error.toMessageResource()))
                }

                Button(
                    onClick = onSyncNow,
                    enabled = !state.syncStatus.isSyncing && !state.isSigningOut,
                ) {
                    Text(text = stringResource(R.string.auth_sync_now))
                }
                OutlinedButton(
                    onClick = { showSignOutDialog = true },
                    enabled = !state.syncStatus.isSyncing && !state.isSigningOut,
                ) {
                    if (state.isSigningOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = stringResource(R.string.auth_sign_out))
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(text = stringResource(R.string.auth_sign_out_title)) },
            text = { Text(text = stringResource(R.string.auth_sign_out_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut(false)
                    },
                ) {
                    Text(text = stringResource(R.string.auth_keep_local_data))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showSignOutDialog = false
                            onSignOut(true)
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.auth_delete_local_data),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text(text = stringResource(R.string.custom_dialog_cancel))
                    }
                }
            },
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun AuthUiError.toMessageResource(): Int = when (this) {
    AuthUiError.CONFIGURATION -> R.string.auth_configuration_missing
    AuthUiError.CONFIGURATION_ERROR -> R.string.auth_configuration_error
    AuthUiError.FAST_CANCELLATION_CONFIGURATION -> R.string.auth_fast_cancel_configuration
    AuthUiError.NO_GOOGLE_ACCOUNT -> R.string.auth_no_google_account
    AuthUiError.USER_CANCELLED -> R.string.auth_user_cancelled
    AuthUiError.CREDENTIAL -> R.string.auth_credential_error
    AuthUiError.NETWORK -> R.string.auth_network_error
    AuthUiError.FIREBASE -> R.string.auth_firebase_error
    AuthUiError.UNKNOWN -> R.string.auth_unknown_error
    AuthUiError.LOCAL_DATA -> R.string.auth_local_data_error
    AuthUiError.SYNC_NETWORK -> R.string.auth_network_error
    AuthUiError.SYNC_PERMISSION -> R.string.auth_permission_error
    AuthUiError.SYNC_FAILED -> R.string.auth_sync_error
    AuthUiError.SYNC_IN_PROGRESS -> R.string.auth_sync_in_progress
}

private fun SyncError.toMessageResource(): Int = when (this) {
    SyncError.NETWORK -> R.string.auth_network_error
    SyncError.PERMISSION_DENIED -> R.string.auth_permission_error
    SyncError.AUTH_REQUIRED -> R.string.auth_session_expired
    SyncError.UNKNOWN -> R.string.auth_sync_error
}
