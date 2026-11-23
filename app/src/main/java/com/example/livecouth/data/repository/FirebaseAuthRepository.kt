package com.example.livecouth.data.repository

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.livecouth.BuildConfig
import com.example.livecouth.domain.model.AuthUser
import com.example.livecouth.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override suspend fun signInWithGoogle(activity: Activity, webClientId: String) {
        authDebug(
            "step=configuration GOOGLE_WEB_CLIENT_ID_empty=${webClientId.isBlank()} " +
                "suffix=${webClientId.takeLast(12).ifEmpty { "(empty)" }}",
        )
        if (webClientId.isBlank()) {
            throw GoogleAuthConfigurationException("GOOGLE_WEB_CLIENT_ID is empty")
        }

        val credentialManager = CredentialManager.create(activity)
        val googleIdToken = try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(webClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            authDebug("step=google_id_option request_started")
            extractGoogleIdToken(
                getCredential(
                    credentialManager,
                    activity,
                    request,
                    "google_id_option",
                ),
            )
        } catch (exception: NoCredentialException) {
            authDebugFailure("google_id_option", exception)
            authDebug("step=google_sign_in_fallback request_started")

            val fallback = GetSignInWithGoogleOption.Builder(webClientId).build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fallback)
                .build()
            try {
                extractGoogleIdToken(
                    getCredential(
                        credentialManager,
                        activity,
                        fallbackRequest,
                        "google_sign_in_fallback",
                    ),
                )
            } catch (fallbackException: Exception) {
                authDebugFailure("google_sign_in_fallback", fallbackException)
                throw fallbackException
            }
        } catch (exception: GoogleIdTokenParsingException) {
            authDebugFailure("google_id_token_parsing", exception)
            throw exception
        } catch (exception: GetCredentialException) {
            authDebugFailure("credential_request", exception)
            throw exception
        } catch (exception: Exception) {
            authDebugFailure("credential_request", exception)
            throw exception
        }

        authDebug("step=firebase_sign_in request_started")
        signInWithGoogleIdToken(googleIdToken)
    }

    private suspend fun getCredential(
        credentialManager: CredentialManager,
        activity: Activity,
        request: GetCredentialRequest,
        step: String,
    ): Credential {
        val selectorOpenedAt = SystemClock.elapsedRealtime()
        try {
            return credentialManager.getCredential(activity, request).credential
        } catch (exception: GetCredentialCancellationException) {
            val elapsedMs = SystemClock.elapsedRealtime() - selectorOpenedAt
            authDebug(
                "step=$step cancellation message=${exception.message} " +
                    "cause=${exception.cause} elapsedMs=$elapsedMs",
            )
            throw GoogleSignInCancelledException(elapsedMs, exception)
        }
    }

    private fun extractGoogleIdToken(credential: androidx.credentials.Credential): String {
        val customCredential = credential as? CustomCredential
            ?: throw UnexpectedGoogleCredentialException()
        if (customCredential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw UnexpectedGoogleCredentialException()
        }
        return GoogleIdTokenCredential.createFrom(customCredential.data).idToken
    }

    private fun authDebug(message: String) {
        if (BuildConfig.DEBUG) Log.d(AUTH_DEBUG_TAG, message)
    }

    private fun authDebugFailure(step: String, exception: Exception) {
        if (BuildConfig.DEBUG) {
            val credentialType = (exception as? GetCredentialException)
                ?.type
                ?.let { " type=$it" }
                .orEmpty()
            val cause = exception.cause?.let { "${it.javaClass.name}: $it" } ?: "none"
            Log.w(
                AUTH_DEBUG_TAG,
                "step=$step exception=${exception.javaClass.name}$credentialType " +
                    "message=${exception.message} cause=$cause",
            )
        }
    }

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(user?.let { AuthUser(it.uid, it.displayName, it.email) })
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        try {
            firebaseAuth.signInWithCredential(
                GoogleAuthProvider.getCredential(idToken, null),
            ).await()
        } catch (exception: Exception) {
            authDebugFailure("firebase_sign_in", exception)
            throw exception
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    private companion object {
        const val AUTH_DEBUG_TAG = "AuthDebug"
    }
}

class GoogleAuthConfigurationException(message: String) : Exception(message)
class GoogleSignInCancelledException(
    val elapsedMs: Long,
    cause: GetCredentialCancellationException,
) : Exception(cause.message, cause)

private class UnexpectedGoogleCredentialException : Exception()
