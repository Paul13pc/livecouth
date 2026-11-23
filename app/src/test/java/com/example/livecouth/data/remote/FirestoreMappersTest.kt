package com.example.livecouth.data.remote

import com.example.livecouth.data.local.entity.QuickAddButton
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.domain.model.ActivityLevel
import com.example.livecouth.domain.model.CityLocation
import com.example.livecouth.domain.model.DrinkType
import com.example.livecouth.domain.model.SyncableUserProfile
import com.example.livecouth.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FirestoreMappersTest {

    @Test
    fun waterLogMapperKeepsStableIdAndSoftDeletion() {
        val local = WaterLog(
            id = 12,
            cantidadMl = 450,
            fechaHora = 1_700_000_000_000,
            drinkType = DrinkType.TEA,
            hydrationFactor = 0.9f,
            syncId = "water-log-id",
            updatedAt = 1_700_000_100_000,
            sincronizado = false,
            eliminado = true,
        )

        val mapped = local.toFirestoreMap().toWaterLog(local.syncId)

        assertEquals(local.copy(id = 0, sincronizado = true), mapped)
    }

    @Test
    fun quickButtonMapperKeepsOrderAndStableId() {
        val local = QuickAddButton(
            id = 4,
            name = "Mi botella",
            amountMl = 600,
            sortOrder = 2,
            syncId = "button-id",
            updatedAt = 1_700_000_100_000,
            sincronizado = false,
            eliminado = false,
        )

        val mapped = local.toFirestoreMap().toQuickAddButton(local.syncId)

        assertEquals(local.copy(id = 0, sincronizado = true), mapped)
    }

    @Test
    fun profileMapperKeepsCityAndUpdatedAt() {
        val profile = UserProfile(
            name = "Ana",
            weightKg = 62.5f,
            age = 28,
            activityLevel = ActivityLevel.MEDIO,
            isOnboardingCompleted = true,
            reminderEnabled = false,
            reminderStartHour = 9,
            reminderEndHour = 20,
            city = CityLocation.MANTA,
        )
        val syncable = SyncableUserProfile(
            profile = profile,
            updatedAt = 1_700_000_100_000,
            sincronizado = false,
            eliminado = false,
        )

        val firestoreFields = syncable.toFirestoreMap()
        assertEquals(false, firestoreFields.containsKey("cityLatitude"))
        assertEquals(false, firestoreFields.containsKey("cityLongitude"))
        assertEquals(
            syncable.copy(sincronizado = true),
            firestoreFields.toSyncableUserProfile(),
        )
    }

    @Test
    fun waterLogMapperRejectsOutOfRangeRestoredAmounts() {
        val corrupted = WaterLog(cantidadMl = 3001).toFirestoreMap()

        assertThrows(SyncDataException::class.java) {
            corrupted.toWaterLog("water-log-id")
        }
    }

    @Test
    fun profileMapperRejectsCorruptRestoredAge() {
        val profile = UserProfile(name = "Ana", age = 4)
        val syncable = SyncableUserProfile(
            profile = profile,
            updatedAt = 1_700_000_100_000,
            sincronizado = false,
            eliminado = false,
        )

        assertThrows(SyncDataException::class.java) {
            syncable.toFirestoreMap().toSyncableUserProfile()
        }
    }
}
