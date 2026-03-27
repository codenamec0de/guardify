package com.uow.guardify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uow.guardify.data.entity.ScanResultEntity

@Dao
interface ScanResultDao {

    @Query("SELECT * FROM scan_results ORDER BY CASE riskLevel WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'LOW' THEN 2 END")
    suspend fun getAll(): List<ScanResultEntity>

    @Query("SELECT * FROM scan_results WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): ScanResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<ScanResultEntity>)

    @Query("SELECT COUNT(*) FROM scan_results WHERE riskLevel = :level")
    suspend fun countByRiskLevel(level: String): Int

    @Query("SELECT COUNT(*) FROM scan_results")
    suspend fun getTotalCount(): Int

    @Query("SELECT MAX(scannedAt) FROM scan_results")
    suspend fun getLastScanTime(): Long?

    @Query("DELETE FROM scan_results")
    suspend fun clearAll()
}
