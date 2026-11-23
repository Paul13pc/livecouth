package com.example.livecouth.domain.model

data class SyncableUserProfile(
    val profile: UserProfile,
    val updatedAt: Long,
    val sincronizado: Boolean,
    val eliminado: Boolean,
)
