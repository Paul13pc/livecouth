package com.example.livecouth.domain.repository

import android.app.Activity
import com.example.livecouth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<AuthUser?>

    suspend fun signInWithGoogle(activity: Activity, webClientId: String)
    suspend fun signInWithGoogleIdToken(idToken: String)
    fun signOut()
}
