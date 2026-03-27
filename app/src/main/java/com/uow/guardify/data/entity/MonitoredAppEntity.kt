package com.uow.guardify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val riskLevel: String,         // HIGH, MEDIUM, LOW
    val isMonitored: Boolean = true,
    val lastDataUsageBytes: Long = 0L,
    val lastCheckedAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)
