package com.uow.guardify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uow.guardify.data.entity.AlertEntity

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC LIMIT 100")
    suspend fun getAll(): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<AlertEntity>)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAsRead(alertId: String)

    @Query("SELECT COUNT(*) FROM alerts WHERE isRead = 0")
    suspend fun getUnreadCount(): Int

    @Query("DELETE FROM alerts")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM alerts")
    suspend fun getCount(): Int

    @Query("DELETE FROM alerts WHERE id IN (SELECT id FROM alerts ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
