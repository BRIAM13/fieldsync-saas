package com.corporacionronceros.fieldsync

import com.corporacionronceros.fieldsync.domain.usecase.SyncPendingChangesUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPendingChangesUseCaseTest {

    @Test
    fun `returns count of synced orders on success`() = runTest {
        val repo = FakeWorkOrderRepository().apply { syncResult = Result.success(3) }
        val result = SyncPendingChangesUseCase(repo)()
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun `propagates failure from repository`() = runTest {
        val repo = FakeWorkOrderRepository().apply {
            syncResult = Result.failure(IllegalStateException("sin red"))
        }
        val result = SyncPendingChangesUseCase(repo)()
        assertTrue(result.isFailure)
    }
}
