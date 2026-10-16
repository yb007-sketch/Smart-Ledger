package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiFinancialAssistantRepository
import com.example.data.ai.LocalInferenceResult
import com.example.data.ai.LocalLlmManager
import com.example.data.ai.LocalLlmModel
import com.example.data.ai.ParsedTransactionDraft
import com.example.data.ai.SpendingInsight
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthPeriod(
    val year: Int,
    val month: Int // 0-based: 0 = Jan, 9 = Oct
) {
    fun formatDisplay(): String {
        val names = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec")
        val mName = names.getOrElse(month) { "Month" }
        return "$mName $year"
    }

    fun toKey(): String {
        return String.format(java.util.Locale.US, "%04d-%02d", year, month + 1)
    }

    fun getStartTimestamp(): Long {
        val c = Calendar.getInstance()
        c.set(year, month, 1, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun getEndTimestamp(): Long {
        val c = Calendar.getInstance()
        c.set(year, month, c.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }
}

enum class TransactionSortMode {
    RECENT, CATEGORY, AMOUNT
}

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = LedgerRepository(database)
    val preferencesRepository = UserPreferencesRepository(application)
    val aiRepository = AiFinancialAssistantRepository()
    val localLlmManager = LocalLlmManager(application)
    val backupManager = com.example.data.backup.DatabaseBackupManager(application, database)

    // Local AI Model Manager States
    val localModels: StateFlow<List<LocalLlmModel>> = localLlmManager.models
    val localEngineStatus = localLlmManager.engineStatus
    val localHardwareInfo = localLlmManager.hardwareInfo

    private val _localLlmInsightStream = MutableStateFlow<String?>(null)
    val localLlmInsightStream: StateFlow<String?> = _localLlmInsightStream.asStateFlow()

    private val _isGeneratingLocalInsights = MutableStateFlow(false)
    val isGeneratingLocalInsights: StateFlow<Boolean> = _isGeneratingLocalInsights.asStateFlow()

    private val _lastInferenceResult = MutableStateFlow<LocalInferenceResult?>(null)
    val lastInferenceResult: StateFlow<LocalInferenceResult?> = _lastInferenceResult.asStateFlow()

    // Navigation and screen states
    private val _currentBottomNavIndex = MutableStateFlow(0) // 0: Trans, 1: Stats, 2: Accounts, 3: More
    val currentBottomNavIndex: StateFlow<Int> = _currentBottomNavIndex.asStateFlow()

    private val _currentTransTab = MutableStateFlow(0) // 0: Daily, 1: Calendar, 2: Monthly, 3: Total, 4: Note
    val currentTransTab: StateFlow<Int> = _currentTransTab.asStateFlow()

    private val _currentPeriod = MutableStateFlow(MonthPeriod(year = 2026, month = 9)) // Oct 2026 (matching screenshots)
    val currentPeriod: StateFlow<MonthPeriod> = _currentPeriod.asStateFlow()

    private val _selectedCalendarDay = MutableStateFlow(15) // 15th
    val selectedCalendarDay: StateFlow<Int> = _selectedCalendarDay.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _categoryFilterId = MutableStateFlow<Long?>(null)
    val categoryFilterId: StateFlow<Long?> = _categoryFilterId.asStateFlow()

    private val _accountFilterId = MutableStateFlow<Long?>(null)
    val accountFilterId: StateFlow<Long?> = _accountFilterId.asStateFlow()

    private val _transactionSortMode = MutableStateFlow(TransactionSortMode.RECENT)
    val transactionSortMode: StateFlow<TransactionSortMode> = _transactionSortMode.asStateFlow()

    // Sheet and Dialog states
    private val _isEntrySheetOpen = MutableStateFlow(false)
    val isEntrySheetOpen: StateFlow<Boolean> = _isEntrySheetOpen.asStateFlow()

    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    val editingTransaction: StateFlow<TransactionEntity?> = _editingTransaction.asStateFlow()

    private val _prefillDraft = MutableStateFlow<ParsedTransactionDraft?>(null)
    val prefillDraft: StateFlow<ParsedTransactionDraft?> = _prefillDraft.asStateFlow()

    private val _isAiAssistantOpen = MutableStateFlow(false)
    val isAiAssistantOpen: StateFlow<Boolean> = _isAiAssistantOpen.asStateFlow()

    private val _isAskMyLedgerChatOpen = MutableStateFlow(false)
    val isAskMyLedgerChatOpen: StateFlow<Boolean> = _isAskMyLedgerChatOpen.asStateFlow()

    private val _isModelManagerOpen = MutableStateFlow(false)
    val isModelManagerOpen: StateFlow<Boolean> = _isModelManagerOpen.asStateFlow()

    private val _isRapidEntryDialogOpen = MutableStateFlow(false)
    val isRapidEntryDialogOpen: StateFlow<Boolean> = _isRapidEntryDialogOpen.asStateFlow()

    private val _isCalcBoxOpen = MutableStateFlow(false)
    val isCalcBoxOpen: StateFlow<Boolean> = _isCalcBoxOpen.asStateFlow()

    private val _isImportPreviewOpen = MutableStateFlow(false)
    val isImportPreviewOpen: StateFlow<Boolean> = _isImportPreviewOpen.asStateFlow()

    private val _importFileUri = MutableStateFlow<Pair<android.net.Uri, String>?>(null)
    val importFileUri: StateFlow<Pair<android.net.Uri, String>?> = _importFileUri.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _lastDeletedTx = MutableStateFlow<TransactionEntity?>(null)

    // Live count of transactions created in the last 24 hours
    val last24HoursCount: StateFlow<Int> = repository
        .getTransactionsCreatedSinceCount(System.currentTimeMillis() - 86_400_000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Data streams
    val accounts: StateFlow<List<AccountEntity>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    // Filtered transactions for current month
    val currentMonthTransactions = combine(allTransactions, currentPeriod, searchQuery, categoryFilterId, accountFilterId) { txs, period, query, catId, accId ->
        val start = period.getStartTimestamp()
        val end = period.getEndTimestamp()
        txs.filter { tx ->
            val inPeriod = tx.timestamp in start..end
            val matchesQuery = query.isBlank() || tx.merchantName.contains(query, ignoreCase = true) || tx.notes.contains(query, ignoreCase = true)
            val matchesCat = catId == null || tx.categoryId == catId
            val matchesAcc = accId == null || tx.accountId == accId || tx.toAccountId == accId
            inPeriod && matchesQuery && matchesCat && matchesAcc
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month totals
    val currentMonthIncome = combine(currentMonthTransactions) { txs ->
        txs[0].filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthExpenses = combine(currentMonthTransactions) { txs ->
        txs[0].filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 587.59)

    // AI Insights & Advanced AI Modules
    val spendingInsights = combine(currentMonthTransactions, categories, userSettings) { txs, cats, settings ->
        aiRepository.generateSpendingInsights(txs, cats, settings.mainCurrencySymbol)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiPersona = MutableStateFlow("Supportive Coach")
    val aiPersona: StateFlow<String> = _aiPersona.asStateFlow()

    fun setAiPersona(persona: String) {
        _aiPersona.value = persona
    }

    val smartBudgets = combine(allTransactions, categories) { txs, cats ->
        aiRepository.generateSmartBudgets(txs, cats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val anomalyAlerts = combine(allTransactions) { txs ->
        aiRepository.detectAnomalies(txs[0])
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coachedInsight = combine(allTransactions, _aiPersona, currentMonthIncome, currentMonthExpenses) { _, persona, income, expense ->
        aiRepository.generateCoachedInsight(persona, expense, income)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Initializing AI Financial Coach...")

    val cashFlowForecast = combine(allTransactions, userSettings) { txs, settings ->
        aiRepository.generateCashFlowForecast(txs, settings.mainCurrencySymbol)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Calculating AI Cash Flow Forecast...")

    fun exportTransactionsToExcel(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val txs = allTransactions.value
            val accs = accounts.value
            val cats = categories.value
            val currency = userSettings.value.mainCurrencySymbol
            val file = com.example.data.exporter.ExcelExporter.exportTransactionsToExcel(context, txs, accs, cats, currency)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (file != null) {
                    val msg = "Exported ${txs.size} transactions to ${file.absolutePath}"
                    showSnackbar(msg)
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                } else {
                    val errMsg = "Failed to export CSV file"
                    showSnackbar(errMsg)
                    android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setBottomNavIndex(index: Int) {
        _currentBottomNavIndex.value = index
    }

    fun setTransTab(tab: Int) {
        _currentTransTab.value = tab
    }

    fun nextMonth() {
        val curr = _currentPeriod.value
        val nextMonth = if (curr.month == 11) 0 else curr.month + 1
        val nextYear = if (curr.month == 11) curr.year + 1 else curr.year
        _currentPeriod.value = MonthPeriod(nextYear, nextMonth)
    }

    fun prevMonth() {
        val curr = _currentPeriod.value
        val prevM = if (curr.month == 0) 11 else curr.month - 1
        val prevY = if (curr.month == 0) curr.year - 1 else curr.year
        _currentPeriod.value = MonthPeriod(prevY, prevM)
    }

    fun setSelectedCalendarDay(day: Int) {
        _selectedCalendarDay.value = day
    }

    fun toggleSearch() {
        val active = !_isSearchActive.value
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(categoryId: Long?) {
        _categoryFilterId.value = categoryId
    }

    fun setAccountFilter(accountId: Long?) {
        _accountFilterId.value = accountId
    }

    fun clearAllFilters() {
        _categoryFilterId.value = null
        _accountFilterId.value = null
        _searchQuery.value = ""
    }

    fun setTransactionSortMode(mode: TransactionSortMode) {
        _transactionSortMode.value = mode
    }

    fun openEntrySheet(transaction: TransactionEntity? = null, draft: ParsedTransactionDraft? = null) {
        _editingTransaction.value = transaction
        _prefillDraft.value = draft
        _isEntrySheetOpen.value = true
    }

    fun closeEntrySheet() {
        _isEntrySheetOpen.value = false
        _editingTransaction.value = null
        _prefillDraft.value = null
    }

    fun openAiAssistant() {
        _isAiAssistantOpen.value = true
    }

    fun closeAiAssistant() {
        _isAiAssistantOpen.value = false
    }

    fun openAskMyLedgerChat() {
        _isAskMyLedgerChatOpen.value = true
    }

    fun closeAskMyLedgerChat() {
        _isAskMyLedgerChatOpen.value = false
    }

    fun openModelManager() {
        _isModelManagerOpen.value = true
    }

    fun closeModelManager() {
        _isModelManagerOpen.value = false
    }

    fun openRapidEntryDialog() {
        _isRapidEntryDialogOpen.value = true
    }

    fun closeRapidEntryDialog() {
        _isRapidEntryDialogOpen.value = false
    }

    fun openCalcBox() {
        _isCalcBoxOpen.value = true
    }

    fun closeCalcBox() {
        _isCalcBoxOpen.value = false
    }

    fun openImportPreview(uri: android.net.Uri, fileName: String) {
        _importFileUri.value = Pair(uri, fileName)
        _isImportPreviewOpen.value = true
    }

    fun closeImportPreview() {
        _isImportPreviewOpen.value = false
        _importFileUri.value = null
    }

    fun revertLast24Hours() {
        viewModelScope.launch {
            val count = repository.revertTransactionsFromLast24Hours()
            if (count > 0) {
                showSnackbar("Successfully reverted $count transactions from the last 24 hours")
            } else {
                showSnackbar("No transactions found to revert in the last 24 hours")
            }
        }
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun exportDatabaseBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            val result = backupManager.exportDatabase(uri)
            result.onSuccess { exportRes ->
                showSnackbar("Backup exported successfully (${exportRes.fileSizeFormatted}) at ${exportRes.timestamp}")
            }.onFailure { err ->
                showSnackbar("Export failed: ${err.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun restoreDatabaseBackup(uri: android.net.Uri, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreDatabase(uri)
            result.onSuccess {
                backupManager.restartApplication()
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Invalid or unreadable backup file")
            }
        }
    }

    fun clearTransactionsOnly(onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = backupManager.clearTransactionsOnly()
            result.onSuccess {
                showSnackbar("All transactions cleared. Account balances reset to £0.00.")
                onComplete()
            }.onFailure { err ->
                showSnackbar("Reset failed: ${err.localizedMessage}")
            }
        }
    }

    fun factoryResetApp(onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = backupManager.factoryResetApp(preferencesRepository)
            result.onSuccess {
                showSnackbar("App factory reset complete. Restored default starter accounts and categories.")
                onComplete()
            }.onFailure { err ->
                showSnackbar("Factory reset failed: ${err.localizedMessage}")
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun saveTransaction(
        amount: Double,
        type: TransactionType,
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long?,
        subCategory: String?,
        merchantName: String,
        timestamp: Long,
        notes: String,
        tags: String
    ) {
        viewModelScope.launch {
            val editing = _editingTransaction.value
            if (editing == null) {
                val newTx = TransactionEntity(
                    amount = amount,
                    type = type,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    subCategory = subCategory,
                    merchantName = merchantName,
                    transactionDateMillis = timestamp,
                    notes = notes,
                    tags = tags
                )
                repository.addTransaction(newTx)
                showSnackbar("Transaction recorded")
            } else {
                val updated = editing.copy(
                    amount = amount,
                    type = type,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    subCategory = subCategory,
                    merchantName = merchantName,
                    transactionDateMillis = timestamp,
                    notes = notes,
                    tags = tags
                )
                repository.updateTransaction(updated, editing)
                showSnackbar("Transaction updated")
            }
            closeEntrySheet()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            _lastDeletedTx.value = transaction
            repository.deleteTransaction(transaction)
            showSnackbar("Transaction deleted. Tap undo to restore.")
        }
    }

    fun undoDelete() {
        val deleted = _lastDeletedTx.value ?: return
        viewModelScope.launch {
            repository.addTransaction(deleted)
            _lastDeletedTx.value = null
            showSnackbar("Transaction restored")
        }
    }

    fun parseAndPrefillNaturalLanguage(input: String) {
        val cats = categories.value
        val accs = accounts.value
        val draft = aiRepository.parseNaturalLanguageEntry(input, cats, accs)
        closeAiAssistant()
        openEntrySheet(draft = draft)
    }

    fun saveAccount(name: String, type: com.example.data.local.entity.AccountType, balance: Double, limit: Double) {
        viewModelScope.launch {
            val account = AccountEntity(
                name = name,
                type = type,
                balance = balance,
                creditLimit = limit
            )
            repository.addAccount(account)
            showSnackbar("Account $name created")
        }
    }

    fun saveCategory(name: String, type: TransactionType, subcategories: String, colorHex: String) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                name = name,
                type = type,
                subcategories = subcategories,
                colorHex = colorHex
            )
            repository.addCategory(cat)
            showSnackbar("Category $name created")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showSnackbar("Category removed")
        }
    }

    fun setMainCurrency(currency: String) {
        viewModelScope.launch {
            preferencesRepository.setMainCurrency(currency)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun resetData(full: Boolean) {
        viewModelScope.launch {
            repository.resetDatabase(full)
            showSnackbar(if (full) "Complete reset finished" else "Transactions reset")
        }
    }

    // Local LLM Functions
    fun downloadLocalModel(modelId: String) {
        localLlmManager.downloadModel(modelId)
    }

    fun deleteLocalModel(modelId: String) {
        localLlmManager.deleteModel(modelId)
    }

    fun setActiveLocalModel(modelId: String) {
        localLlmManager.setActiveModel(modelId)
        showSnackbar("Active Local LLM switched")
    }

    fun generateLocalInsights(topCategories: List<Pair<String, Double>>, totalExpense: Double) {
        viewModelScope.launch {
            _isGeneratingLocalInsights.value = true
            _localLlmInsightStream.value = ""
            val currency = userSettings.value.mainCurrencySymbol
            localLlmManager.streamFinancialInsights(topCategories, totalExpense, currency).collect { chunk ->
                _localLlmInsightStream.value = (_localLlmInsightStream.value ?: "") + chunk
            }
            _isGeneratingLocalInsights.value = false
        }
    }

    fun parseWithLocalLlmAndPrefill(input: String) {
        viewModelScope.launch {
            val result = localLlmManager.parseNaturalLanguageExpense(input, categories.value, accounts.value)
            _lastInferenceResult.value = result
            closeAiAssistant()
            openEntrySheet(draft = result.draft)
            showSnackbar("Parsed locally via ${result.modelName} (${result.tokensPerSecond.toInt()} tok/s)")
        }
    }

    fun toggleStealthMode() {
        viewModelScope.launch {
            val current = userSettings.value.stealthModeEnabled
            preferencesRepository.setStealthMode(!current)
        }
    }

    fun toggleNotificationListener(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationListenerEnabled(enabled)
            val workManager = androidx.work.WorkManager.getInstance(getApplication())
            if (enabled) {
                val request = androidx.work.PeriodicWorkRequestBuilder<com.example.service.NightlyAiCategorizationWorker>(
                    24, java.util.concurrent.TimeUnit.HOURS
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    "SmartLedgerNightlyAiSort",
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
                showSnackbar("Google Wallet/Bank auto-logging & nightly AI sorting enabled")
            } else {
                workManager.cancelUniqueWork("SmartLedgerNightlyAiSort")
                showSnackbar("Auto-logging disabled")
            }
        }
    }

    fun settleCreditCardStatement(creditAccountId: Long, debitAccountId: Long, amount: Double) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch
            val transferTx = TransactionEntity(
                amount = amount,
                type = TransactionType.TRANSFER,
                accountId = debitAccountId,
                toAccountId = creditAccountId,
                merchantName = "Bill Settlement",
                notes = "Credit Card Statement Settlement"
            )
            repository.addTransaction(transferTx)
            showSnackbar("Settled statement: £${String.format(java.util.Locale.US, "%.2f", amount)} transferred to Credit Card")
        }
    }

    fun toggleAutoBackup(enabled: Boolean, uri: String = "") {
        viewModelScope.launch {
            preferencesRepository.setAutoBackupEnabled(enabled)
            if (uri.isNotBlank()) preferencesRepository.setAutoBackupUri(uri)

            if (enabled) {
                val workManager = androidx.work.WorkManager.getInstance(getApplication())
                val request = androidx.work.PeriodicWorkRequestBuilder<com.example.data.backup.AutoBackupWorker>(
                    1, java.util.concurrent.TimeUnit.DAYS
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    "SmartLedgerAutoBackup",
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
                showSnackbar("Scheduled daily rolling 7-day auto-backups")
            } else {
                androidx.work.WorkManager.getInstance(getApplication()).cancelUniqueWork("SmartLedgerAutoBackup")
                showSnackbar("Auto-backup disabled")
            }
        }
    }
    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
            com.example.util.AppLogger.log("LedgerViewModel", "Updated account: ${account.name}, balance: ${account.balance}")
            showSnackbar("Updated account '${account.name}' successfully")
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            com.example.util.AppLogger.log("LedgerViewModel", "Deleted account: ${account.name}")
            showSnackbar("Deleted account '${account.name}'")
        }
    }

    fun exportAppLogs(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val file = com.example.util.AppLogger.exportLogs(context)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (file != null) {
                    val msg = "App logs exported to ${file.absolutePath}"
                    showSnackbar(msg)
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                } else {
                    val errMsg = "Failed to export app logs"
                    showSnackbar(errMsg)
                    android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val currencyState = preferencesRepository.currencyStateFlow.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        com.example.data.preferences.CurrencyState()
    )

    fun setCurrencyState(currency: com.example.data.preferences.AppCurrency, rate: Double) {
        viewModelScope.launch {
            preferencesRepository.setCurrencyState(currency, rate)
            com.example.util.AppLogger.log("LedgerViewModel", "Updated active currency to ${currency.code} with rate $rate")
            showSnackbar("Converted app currency to ${currency.label} (1 GBP = ${currency.symbol} $rate)")
        }
    }
}
