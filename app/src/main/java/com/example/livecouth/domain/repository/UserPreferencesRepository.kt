package com.example.livecouth.domain.repository

import com.example.livecouth.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userProfile: Flow<UserProfile>
    suspend fun saveUserProfile(userProfile: UserProfile)
}
