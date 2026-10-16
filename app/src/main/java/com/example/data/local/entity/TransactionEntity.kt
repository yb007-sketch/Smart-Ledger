package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("transactionDateMillis"),
        Index("createdAtMillis"),
        Index("linkedExpenseId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val accountId: Long,
    val toAccountId: Long? = null, // for TRANSFER
    val categoryId: Long? = null,
    val subCategory: String? = null,
    val merchantName: String = "",
    val transactionDateMillis: Long = System.currentTimeMillis(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val tags: String = "", // Comma-separated tags
    val receiptImageUri: String? = null,
    val warrantyExpiryMillis: Long? = null,
    val renewalDateMillis: Long? = null,
    val isRecurring: Boolean = false,
    val isSubscription: Boolean = false,
    val recurrenceRule: String? = null,
    val splitDetailsJson: String? = null, // Split categories details in JSON format
    val linkedExpenseId: Long? = null, // For reimbursement netting
    val isReimbursement: Boolean = false
) {
    // Backward-compatible alias for transactionDateMillis
    val timestamp: Long
        get() = transactionDateMillis

    val date: java.util.Date
        get() = java.util.Date(transactionDateMillis)

    fun getTagList(): List<String> {
        return if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }
    }

    val isWarrantyValid: Boolean
        get() = warrantyExpiryMillis != null && warrantyExpiryMillis > System.currentTimeMillis()
}
