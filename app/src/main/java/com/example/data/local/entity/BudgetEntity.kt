package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null, // null means total overall budget
    val monthlyLimit: Double,
    val monthYear: String, // e.g. "2026-10"
    val rolloverUnused: Boolean = false
)
