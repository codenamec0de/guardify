package com.uow.guardify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val permissions: String,       // comma-separated
    val riskLevel: String,         // HIGH, MEDIUM, LOW
    val isSystemApp: Boolean,
    val installedDate: Long,
    val lastUpdated: Long,
    val scannedAt: Long = System.currentTimeMillis()
)
