package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

enum class AppThemeMode {
    FROSTED_DARK,
    FROSTED_LIGHT,
    AMOLED_BLACK,
    EMERALD_FINANCIAL,
    DYNAMIC_SYSTEM
}

data class UserSettings(
    val mainCurrency: String = "GBP (£)",
    val mainCurrencySymbol: String = "£",
    val subCurrency: String = "INR (₹)",
    val subCurrencySymbol: String = "₹",
    val startScreen: String = "Daily", // Daily or Calendar
    val monthlyStartDate: Int = 1,
    val weeklyStartDay: String = "Sunday",
    val carryOverEnabled: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.FROSTED_LIGHT,
    val passcodeEnabled: Boolean = false,
    val autocompleteEnabled: Boolean = true,
    val showDescription: Boolean = true,
    val hapticEnabled: Boolean = true,
    val stealthModeEnabled: Boolean = false,
    val notificationListenerEnabled: Boolean = true,
    val autoBackupEnabled: Boolean = false,
    val autoBackupUri: String = ""
)

class UserPreferencesRepository(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_MAIN_CURRENCY = stringPreferencesKey("main_currency")
        val KEY_SUB_CURRENCY = stringPreferencesKey("sub_currency")
        val KEY_START_SCREEN = stringPreferencesKey("start_screen")
        val KEY_MONTHLY_START_DATE = intPreferencesKey("monthly_start_date")
        val KEY_WEEKLY_START_DAY = stringPreferencesKey("weekly_start_day")
        val KEY_CARRY_OVER = booleanPreferencesKey("carry_over")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_PASSCODE_ENABLED = booleanPreferencesKey("passcode_enabled")
        val KEY_AUTOCOMPLETE = booleanPreferencesKey("autocomplete")
        val KEY_SHOW_DESCRIPTION = booleanPreferencesKey("show_description")
        val KEY_HAPTIC = booleanPreferencesKey("haptic")
        val KEY_STEALTH_MODE = booleanPreferencesKey("stealth_mode")
        val KEY_NOTIFICATION_LISTENER = booleanPreferencesKey("notification_listener")
        val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val KEY_AUTO_BACKUP_URI = stringPreferencesKey("auto_backup_uri")
        val KEY_ACTIVE_CURRENCY = stringPreferencesKey("active_currency")
        val KEY_ACTIVE_EXCHANGE_RATE = doublePreferencesKey("active_exchange_rate")
        val KEY_SAVED_INR_RATE = doublePreferencesKey("saved_inr_rate")
        val KEY_SAVED_USD_RATE = doublePreferencesKey("saved_usd_rate")
    }

    val currencyStateFlow: Flow<CurrencyState> = dataStore.data.map { pref ->
        val code = pref[KEY_ACTIVE_CURRENCY] ?: "GBP"
        val curr = AppCurrency.fromCode(code)
        val rate = pref[KEY_ACTIVE_EXCHANGE_RATE] ?: when (curr) {
            AppCurrency.INR -> 110.0
            AppCurrency.USD -> 1.30
            else -> 1.0
        }
        val inrRate = pref[KEY_SAVED_INR_RATE] ?: 110.0
        val usdRate = pref[KEY_SAVED_USD_RATE] ?: 1.30

        CurrencyState(
            activeCurrency = curr,
            exchangeRate = rate,
            savedInrRate = inrRate,
            savedUsdRate = usdRate
        )
    }

    suspend fun setCurrencyState(currency: AppCurrency, rate: Double) {
        dataStore.edit { pref ->
            pref[KEY_ACTIVE_CURRENCY] = currency.code
            pref[KEY_ACTIVE_EXCHANGE_RATE] = rate
            if (currency == AppCurrency.INR) {
                pref[KEY_SAVED_INR_RATE] = rate
            } else if (currency == AppCurrency.USD) {
                pref[KEY_SAVED_USD_RATE] = rate
            }
        }
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data.map { pref ->
        val mainCurr = pref[KEY_MAIN_CURRENCY] ?: "GBP (£)"
        val symbol = extractSymbol(mainCurr)
        val subCurr = pref[KEY_SUB_CURRENCY] ?: "INR (₹)"
        val subSymbol = extractSymbol(subCurr)
        val themeStr = pref[KEY_THEME_MODE] ?: AppThemeMode.FROSTED_LIGHT.name
        val themeMode = try {
            AppThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            AppThemeMode.FROSTED_LIGHT
        }

        UserSettings(
            mainCurrency = mainCurr,
            mainCurrencySymbol = symbol,
            subCurrency = subCurr,
            subCurrencySymbol = subSymbol,
            startScreen = pref[KEY_START_SCREEN] ?: "Daily",
            monthlyStartDate = pref[KEY_MONTHLY_START_DATE] ?: 1,
            weeklyStartDay = pref[KEY_WEEKLY_START_DAY] ?: "Sunday",
            carryOverEnabled = pref[KEY_CARRY_OVER] ?: false,
            themeMode = themeMode,
            passcodeEnabled = pref[KEY_PASSCODE_ENABLED] ?: false,
            autocompleteEnabled = pref[KEY_AUTOCOMPLETE] ?: true,
            showDescription = pref[KEY_SHOW_DESCRIPTION] ?: true,
            hapticEnabled = pref[KEY_HAPTIC] ?: true,
            stealthModeEnabled = pref[KEY_STEALTH_MODE] ?: false,
            notificationListenerEnabled = pref[KEY_NOTIFICATION_LISTENER] ?: true,
            autoBackupEnabled = pref[KEY_AUTO_BACKUP_ENABLED] ?: false,
            autoBackupUri = pref[KEY_AUTO_BACKUP_URI] ?: ""
        )
    }

    private fun extractSymbol(currencyString: String): String {
        return when {
            currencyString.contains("£") -> "£"
            currencyString.contains("$") -> "$"
            currencyString.contains("€") -> "€"
            currencyString.contains("₹") -> "₹"
            currencyString.contains("¥") -> "¥"
            else -> "£"
        }
    }

    suspend fun setMainCurrency(currency: String) {
        dataStore.edit { it[KEY_MAIN_CURRENCY] = currency }
    }

    suspend fun setSubCurrency(currency: String) {
        dataStore.edit { it[KEY_SUB_CURRENCY] = currency }
    }

    suspend fun setStartScreen(screen: String) {
        dataStore.edit { it[KEY_START_SCREEN] = screen }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setPasscodeEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PASSCODE_ENABLED] = enabled }
    }

    suspend fun setCarryOver(enabled: Boolean) {
        dataStore.edit { it[KEY_CARRY_OVER] = enabled }
    }

    suspend fun setWeeklyStartDay(day: String) {
        dataStore.edit { it[KEY_WEEKLY_START_DAY] = day }
    }

    suspend fun setStealthMode(enabled: Boolean) {
        dataStore.edit { it[KEY_STEALTH_MODE] = enabled }
    }

    suspend fun setNotificationListenerEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATION_LISTENER] = enabled }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setAutoBackupUri(uri: String) {
        dataStore.edit { it[KEY_AUTO_BACKUP_URI] = uri }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }
}
