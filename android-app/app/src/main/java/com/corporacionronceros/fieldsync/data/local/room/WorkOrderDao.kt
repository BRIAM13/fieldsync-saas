package com.corporacionronceros.fieldsync.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderDao {

    /** Room emite automáticamente en este Flow cada vez que cambia la tabla. */
    @Query("SELECT * FROM work_orders")
    fun observeAll(): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM work_orders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<WorkOrderEntity?>

    @Query("SELECT * FROM work_orders WHERE pendingSync = 1")
    suspend fun getPending(): List<WorkOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(orders: List<WorkOrderEntity>)

    @Query("UPDATE work_orders SET status = :status, pendingSync = 1 WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE work_orders SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)
}
