package com.example.data.preferences

import androidx.compose.runtime.Immutable
import java.text.NumberFormat
import java.util.Locale

@Immutable
enum class AppCurrency(val code: String, val symbol: String, val label: String, val localeTag: String) {
    GBP("GBP", "£", "British Pound (£)", "en-GB"),
    INR("INR", "₹", "Indian Rupee (₹)", "en-IN"),
    USD("USD", "$", "US Dollar ($)", "en-US");

    companion object {
        fun fromCode(code: String): AppCurrency {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: GBP
        }
    }
}

@Immutable
data class CurrencyState(
    val activeCurrency: AppCurrency = AppCurrency.GBP,
    val exchangeRate: Double = 1.0,
    val savedInrRate: Double = 110.0,
    val savedUsdRate: Double = 1.30
) {
    fun formatFromBaseGbp(amountGbp: Double): String {
        val converted = amountGbp * exchangeRate
        val locale = when (activeCurrency) {
            AppCurrency.GBP -> Locale.UK
            AppCurrency.INR -> Locale("en", "IN")
            AppCurrency.USD -> Locale.US
        }
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "${activeCurrency.symbol} ${nf.format(converted)}"
    }

    fun convertInputToBaseGbp(enteredAmount: Double): Double {
        if (exchangeRate <= 0.0) return enteredAmount
        return enteredAmount / exchangeRate
    }
}

val LocalCurrencyState = androidx.compose.runtime.compositionLocalOf { CurrencyState() }
