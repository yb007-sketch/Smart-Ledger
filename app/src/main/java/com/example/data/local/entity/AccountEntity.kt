package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    CASH,
    DEBIT_CARD,
    CREDIT_CARD,
    BANK,
    SAVINGS,
    INVESTMENT
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String = "GBP",
    val creditLimit: Double = 0.0,
    val billingCycleDay: Int = 14,
    val dueDayOfMonth: Int = 1,
    val billedBalance: Double = 0.0,
    val colorHex: String = "#10B981",
    val isArchived: Boolean = false
) {
    val isCreditCard: Boolean
        get() = type == AccountType.CREDIT_CARD || name.contains("AMEX", ignoreCase = true) || name.contains("Credit", ignoreCase = true)

    val statementDayOfMonth: Int
        get() = billingCycleDay

    val unbilledCurrentSpend: Double
        get() = if (isCreditCard) {
            val totalSpent = if (balance < 0) -balance else balance
            (totalSpent - billedBalance).coerceAtLeast(0.0)
        } else 0.0

    val availableCredit: Double
        get() = if (isCreditCard && creditLimit > 0) {
            val totalSpent = if (balance < 0) -balance else balance
            (creditLimit - totalSpent).coerceAtLeast(0.0)
        } else 0.0

    fun getCurrencyInstance(): java.util.Currency {
        return try {
            java.util.Currency.getInstance(currency)
        } catch (e: Exception) {
            java.util.Currency.getInstance("GBP")
        }
    }
}
