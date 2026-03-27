package com.uow.guardify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val appName: String,
    val permissions: String,       // comma-separated
    val dataUsedBytes: Long,
    val backgroundDurationMs: Long,
    val timestamp: Long,
    val isRead: Boolean = false
)
