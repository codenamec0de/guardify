package com.uow.guardify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "breach_results")
data class BreachResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val breachName: String,
    val breachDate: String,
    val dataExposed: String,       // comma-separated: "Email, Password, Username"
    val description: String,
    val severity: String,          // HIGH, MEDIUM, LOW
    val checkedAt: Long = System.currentTimeMillis()
)
