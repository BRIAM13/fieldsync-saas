package com.corporacionronceros.fieldsync.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.corporacionronceros.fieldsync.domain.usecase.SyncPendingChangesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker de WorkManager que sincroniza los cambios pendientes en segundo plano.
 *
 * - `@HiltWorker` + `@AssistedInject` permiten que Hilt inyecte el use case en el worker.
 * - Es un `CoroutineWorker`: `doWork()` es `suspend`, encaja con Coroutines/Flow.
 * - Devuelve `retry()` ante fallo para que WorkManager reintente con backoff (resiliencia offline).
 */
@HiltWorker
class SyncWorkOrdersWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncPendingChanges: SyncPendingChangesUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return syncPendingChanges().fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        const val UNIQUE_NAME = "fieldsync-sync-pending"
    }
}
