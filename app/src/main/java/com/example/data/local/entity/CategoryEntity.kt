package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val iconName: String = "category",
    val colorHex: String = "#3B82F6",
    val subcategories: String = "", // Comma-separated subcategories
    val isDefault: Boolean = true,
    val sortOrder: Int = 0
) {
    fun getSubcategoryList(): List<String> {
        return if (subcategories.isBlank()) emptyList() else subcategories.split(",").map { it.trim() }
    }
}
