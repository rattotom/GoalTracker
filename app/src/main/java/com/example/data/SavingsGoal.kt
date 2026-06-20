package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val currencySymbol: String = "$",
    val displayStyle: String = "ring", // "ring", "jar", "thermometer", "tree"
    val isPrimary: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
