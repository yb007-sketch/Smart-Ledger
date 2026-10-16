package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.TransactionType
import java.util.Currency
import java.util.Date

class Converters {
    // Date Converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // Currency Converters
    @TypeConverter
    fun fromCurrencyCode(value: String?): Currency? {
        return value?.let {
            try {
                Currency.getInstance(it)
            } catch (e: Exception) {
                Currency.getInstance("GBP")
            }
        }
    }

    @TypeConverter
    fun currencyToCode(currency: Currency?): String? {
        return currency?.currencyCode
    }

    // AccountType Enum Converters
    @TypeConverter
    fun fromAccountType(value: AccountType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toAccountType(value: String?): AccountType {
        return value?.let {
            try {
                AccountType.valueOf(it)
            } catch (e: Exception) {
                AccountType.CASH
            }
        } ?: AccountType.CASH
    }

    // TransactionType Enum Converters
    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType {
        return value?.let {
            try {
                TransactionType.valueOf(it)
            } catch (e: Exception) {
                TransactionType.EXPENSE
            }
        } ?: TransactionType.EXPENSE
    }
}
