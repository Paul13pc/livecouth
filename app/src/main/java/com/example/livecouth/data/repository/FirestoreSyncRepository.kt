package com.example.livecouth.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.example.livecouth.data.local.AppDatabase
import com.example.livecouth.data.local.dao.QuickAddButtonDao
import com.example.livecouth.data.local.dao.WaterLogDao
import com.example.livecouth.data.local.entity.QuickAddButton
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.data.remote.SyncDataException
import com.example.livecouth.data.remote.toFirestoreMap
import com.example.livecouth.data.remote.toQuickAddButton
import com.example.livecouth.data.remote.toSyncableUserProfile
import com.example.livecouth.data.remote.toWaterLog
import com.example.livecouth.domain.model.SyncError
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.model.SyncStatus
import com.example.livecouth.domain.model.SyncableUserProfile
import com.example.livecouth.domain.repository.SyncRepository
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: AppDatabase,
    private val waterLogDao: WaterLogDao,
    private val quickAddButtonDao: QuickAddButtonDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataStore: DataStore<Preferences>,
) : SyncRepository {

    private val syncMutex = Mutex()
    private val isSyncing = MutableStateFlow(false)
    private val isRestoring = MutableStateFlow(false)

    override val status: Flow<SyncStatus> = combine(
        dataStore.data.map { preferences ->
            SyncStatus(
                lastSuccessfulSyncAt = preferences[Keys.LAST_SUCCESSFUL_SYNC],
                error = preferences[Keys.LAST_ERROR]?.let { value ->
                    SyncError.entries.firstOrNull { it.name == value }
                },
            )
        },
        isSyncing,
        isRestoring,
    ) { stored, syncing, restoring ->
        stored.copy(isSyncing = syncing, isRestoring = restoring)
    }.distinctUntilChanged()

    override suspend fun synchronize(uid: String) {
        require(uid.isNotBlank()) { "Se requiere una sesión para sincronizar." }
        syncMutex.withLock {
            isSyncing.value = true
            try {
                dataStore.edit { it.remove(Keys.LAST_ERROR) }
                val localProfile = userPreferencesRepository.getSyncableProfile()
                val localHasData = waterLogDao.countAll() > 0 ||
                    quickAddButtonDao.getAllForSync().any { !it.sincronizado } ||
                    localProfile.profile.isOnboardingCompleted ||
                    localProfile.updatedAt > 0
                isRestoring.value = !localHasData

                val userRef = firestore.collection("users").document(uid)
                val profileRef = userRef.collection("profile").document("current")
                val logsRef = userRef.collection("waterLogs")
                val buttonsRef = userRef.collection("quickButtons")
                val uploadQueue = mutableListOf<Upload>()

                reconcileProfile(profileRef, localProfile, uploadQueue)
                reconcileWaterLogs(logsRef, uploadQueue)
                reconcileQuickButtons(buttonsRef, uploadQueue)
                uploadInBatches(uploadQueue)

                dataStore.edit {
                    it[Keys.LAST_SUCCESSFUL_SYNC] = System.currentTimeMillis()
                    it.remove(Keys.LAST_ERROR)
                }
            } catch (exception: FirebaseFirestoreException) {
                recordError(exception.toSyncError())
                throw exception
            } catch (exception: FirebaseNetworkException) {
                recordError(SyncError.NETWORK)
                throw exception
            } catch (exception: IOException) {
                recordError(SyncError.NETWORK)
                throw exception
            } catch (exception: SyncDataException) {
                recordError(SyncError.UNKNOWN)
                throw exception
            } catch (exception: FirebaseException) {
                recordError(SyncError.UNKNOWN)
                throw exception
            } finally {
                isRestoring.value = false
                isSyncing.value = false
            }
        }
    }

    override suspend fun clearLocalData() {
        database.withTransaction {
            waterLogDao.deleteAll()
            quickAddButtonDao.restoreDefaultButtons()
        }
        userPreferencesRepository.clearAll()
    }

    override suspend fun resetSyncStatus() {
        dataStore.edit {
            it.remove(Keys.LAST_SUCCESSFUL_SYNC)
            it.remove(Keys.LAST_ERROR)
        }
    }

    private suspend fun reconcileProfile(
        profileRef: DocumentReference,
        local: SyncableUserProfile,
        uploadQueue: MutableList<Upload>,
    ) {
        val remoteSnapshot = profileRef.get(Source.SERVER).await()
        if (!remoteSnapshot.exists()) {
            if (local.updatedAt > 0) {
                uploadQueue += Upload(
                    reference = profileRef,
                    data = local.toFirestoreMap(),
                    kind = UploadKind.PROFILE,
                    syncId = "",
                    updatedAt = local.updatedAt,
                )
            }
            return
        }

        if (
            remoteSnapshot.contains("cityLatitude") ||
            remoteSnapshot.contains("cityLongitude")
        ) {
            profileRef.update(
                mapOf(
                    "cityLatitude" to FieldValue.delete(),
                    "cityLongitude" to FieldValue.delete(),
                ),
            ).await()
        }

        val remote = remoteSnapshot.data?.toSyncableUserProfile()
            ?: throw SyncDataException("El perfil remoto no contiene datos.")
        if (local.updatedAt > remote.updatedAt || (
                local.updatedAt == remote.updatedAt && !local.sincronizado
            )
        ) {
            uploadQueue += Upload(
                reference = profileRef,
                data = local.toFirestoreMap(),
                kind = UploadKind.PROFILE,
                syncId = "",
                updatedAt = local.updatedAt,
            )
        } else if (remote.updatedAt > local.updatedAt) {
            userPreferencesRepository.saveSyncedProfile(
                profile = remote.profile,
                updatedAt = remote.updatedAt,
                eliminado = remote.eliminado,
            )
        } else {
            userPreferencesRepository.markProfileSynced(local.updatedAt)
        }
    }

    private suspend fun reconcileWaterLogs(
        collection: com.google.firebase.firestore.CollectionReference,
        uploadQueue: MutableList<Upload>,
    ) {
        val localLogs = waterLogDao.getAllForSync().filter { log ->
            InputValidation.isValidWaterAmount(log.cantidadMl) &&
                log.hydrationFactor.isFinite() &&
                log.hydrationFactor in 0f..2f
        }
        val localBySyncId = localLogs.associateBy(WaterLog::syncId)
        val remoteLogs = collection.get(Source.SERVER).await().documents.map { document ->
            val data = document.data ?: throw SyncDataException("Registro remoto vacío.")
            data.toWaterLog(document.id)
        }
        val remoteIds = remoteLogs.mapTo(mutableSetOf(), WaterLog::syncId)

        remoteLogs.forEach { remote ->
            val local = localBySyncId[remote.syncId]
            when {
                local == null -> waterLogDao.upsertRemote(remote)
                remote.updatedAt > local.updatedAt ->
                    waterLogDao.upsertRemote(remote.copy(id = local.id))
                local.updatedAt > remote.updatedAt || !local.sincronizado ->
                    queueWaterLog(collection, local, uploadQueue)
                else -> Unit
            }
        }
        localLogs
            .filter { it.syncId !in remoteIds && (!it.sincronizado || it.updatedAt > 0) }
            .forEach { queueWaterLog(collection, it, uploadQueue) }
    }

    private fun queueWaterLog(
        collection: com.google.firebase.firestore.CollectionReference,
        log: WaterLog,
        uploadQueue: MutableList<Upload>,
    ) {
        uploadQueue += Upload(
            reference = collection.document(log.syncId),
            data = log.toFirestoreMap(),
            kind = UploadKind.WATER_LOG,
            syncId = log.syncId,
            updatedAt = log.updatedAt,
        )
    }

    private suspend fun reconcileQuickButtons(
        collection: com.google.firebase.firestore.CollectionReference,
        uploadQueue: MutableList<Upload>,
    ) {
        val localButtons = quickAddButtonDao.getAllForSync().filter { button ->
            InputValidation.validateName(button.name) == null &&
                InputValidation.isValidWaterAmount(button.amountMl)
        }
        val localBySyncId = localButtons.associateBy(QuickAddButton::syncId)
        val remoteButtons = collection.get(Source.SERVER).await().documents.map { document ->
            val data = document.data ?: throw SyncDataException("Botón remoto vacío.")
            data.toQuickAddButton(document.id)
        }
        val remoteIds = remoteButtons.mapTo(mutableSetOf(), QuickAddButton::syncId)

        remoteButtons.forEach { remote ->
            val local = localBySyncId[remote.syncId]
            when {
                local == null -> quickAddButtonDao.upsertRemote(remote)
                remote.updatedAt > local.updatedAt ->
                    quickAddButtonDao.upsertRemote(remote.copy(id = local.id))
                local.updatedAt > remote.updatedAt || !local.sincronizado ->
                    queueQuickButton(collection, local, uploadQueue)
                else -> Unit
            }
        }
        localButtons
            .filter { it.syncId !in remoteIds && (!it.sincronizado || it.updatedAt > 0) }
            .forEach { queueQuickButton(collection, it, uploadQueue) }
    }

    private fun queueQuickButton(
        collection: com.google.firebase.firestore.CollectionReference,
        button: QuickAddButton,
        uploadQueue: MutableList<Upload>,
    ) {
        uploadQueue += Upload(
            reference = collection.document(button.syncId),
            data = button.toFirestoreMap(),
            kind = UploadKind.QUICK_BUTTON,
            syncId = button.syncId,
            updatedAt = button.updatedAt,
        )
    }

    private suspend fun uploadInBatches(uploads: List<Upload>) {
        uploads.chunked(MAX_BATCH_SIZE).forEach { batchUploads ->
            val batch = firestore.batch()
            batchUploads.forEach { upload ->
                batch.set(upload.reference, upload.data)
            }
            batch.commit().await()
            batchUploads.forEach { upload ->
                when (upload.kind) {
                    UploadKind.PROFILE ->
                        userPreferencesRepository.markProfileSynced(upload.updatedAt)
                    UploadKind.WATER_LOG ->
                        waterLogDao.markSynced(upload.syncId, upload.updatedAt)
                    UploadKind.QUICK_BUTTON ->
                        quickAddButtonDao.markSynced(upload.syncId, upload.updatedAt)
                }
            }
        }
    }

    private suspend fun recordError(error: SyncError) {
        dataStore.edit { it[Keys.LAST_ERROR] = error.name }
    }

    private fun FirebaseFirestoreException.toSyncError(): SyncError = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> SyncError.PERMISSION_DENIED
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> SyncError.AUTH_REQUIRED
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.ABORTED,
        -> SyncError.NETWORK
        else -> SyncError.UNKNOWN
    }

    private data class Upload(
        val reference: DocumentReference,
        val data: Map<String, Any>,
        val kind: UploadKind,
        val syncId: String,
        val updatedAt: Long,
    )

    private enum class UploadKind {
        PROFILE,
        WATER_LOG,
        QUICK_BUTTON,
    }

    private object Keys {
        val LAST_SUCCESSFUL_SYNC = longPreferencesKey("firebase_sync_last_successful_at")
        val LAST_ERROR = stringPreferencesKey("firebase_sync_last_error")
    }

    private companion object {
        const val MAX_BATCH_SIZE = 400
    }
}
