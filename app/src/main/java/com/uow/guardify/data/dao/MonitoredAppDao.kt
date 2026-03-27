package com.uow.guardify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uow.guardify.data.entity.MonitoredAppEntity

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps ORDER BY CASE riskLevel WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'LOW' THEN 2 END")
    suspend fun getAll(): List<MonitoredAppEntity>

    @Query("SELECT * FROM monitored_apps WHERE isMonitored = 1")
    suspend fun getMonitored(): List<MonitoredAppEntity>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): MonitoredAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: MonitoredAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<MonitoredAppEntity>)

    @Query("UPDATE monitored_apps SET isMonitored = :enabled WHERE packageName = :packageName")
    suspend fun setMonitored(packageName: String, enabled: Boolean)

    @Query("UPDATE monitored_apps SET isMonitored = :enabled")
    suspend fun setAllMonitored(enabled: Boolean)

    @Query("UPDATE monitored_apps SET lastDataUsageBytes = :bytes, lastCheckedAt = :time WHERE packageName = :packageName")
    suspend fun updateDataUsage(packageName: String, bytes: Long, time: Long)

    @Query("SELECT COUNT(*) FROM monitored_apps WHERE isMonitored = 1")
    suspend fun getMonitoredCount(): Int

    @Query("SELECT COUNT(*) FROM monitored_apps")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM monitored_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
