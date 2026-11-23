package com.example.livecouth.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.livecouth.data.remote.SyncDataException
import com.example.livecouth.domain.repository.AuthRepository
import com.example.livecouth.domain.repository.SyncRepository
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val user = authRepository.currentUser.first() ?: return Result.success()
        return try {
            syncRepository.synchronize(user.uid)
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: FirebaseFirestoreException) {
            if (
                exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                exception.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
            ) {
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (exception: FirebaseNetworkException) {
            Result.retry()
        } catch (exception: IOException) {
            Result.retry()
        } catch (exception: SyncDataException) {
            Result.failure()
        } catch (exception: FirebaseException) {
            Result.failure()
        }
    }
}
