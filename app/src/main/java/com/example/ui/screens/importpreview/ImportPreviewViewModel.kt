package com.example.ui.screens.importpreview

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.importer.BulkFileImporter
import com.example.data.importer.StagedTransactionRow
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportFilter {
    ALL,
    VALID,
    DUPLICATES,
    NEEDS_ATTENTION
}

@Immutable
data class ImportPreviewUiState(
    val stagedRows: List<StagedTransactionRow> = emptyList(),
    val selectedFilter: ImportFilter = ImportFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val fileName: String = "",
    val lastDeletedRow: Pair<Int, StagedTransactionRow>? = null
) {
    val filteredRows: List<StagedTransactionRow>
        get() = when (selectedFilter) {
            ImportFilter.ALL -> stagedRows
            ImportFilter.VALID -> stagedRows.filter { it.validationError == null && !it.isPossibleDuplicate }
            ImportFilter.DUPLICATES -> stagedRows.filter { it.isPossibleDuplicate }
            ImportFilter.NEEDS_ATTENTION -> stagedRows.filter { it.validationError != null }
        }

    val selectedCount: Int
        get() = stagedRows.count { it.isSelected }

    val validSelectedCount: Int
        get() = stagedRows.count { it.isSelected && it.validationError == null }

    val allSelected: Boolean
        get() = stagedRows.isNotEmpty() && stagedRows.all { it.isSelected }

    // Live Totals for Checked Rows Only
    val stagedIncome: Double
        get() = stagedRows.filter { it.isSelected && it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }

    val stagedExpenses: Double
        get() = stagedRows.filter { it.isSelected && it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }

    val stagedNetTotal: Double
        get() = stagedIncome - stagedExpenses
}

class ImportPreviewViewModel(
    private val repository: LedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportPreviewUiState())
    val uiState: StateFlow<ImportPreviewUiState> = _uiState.asStateFlow()

    fun loadFile(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, fileName = fileName) }
            try {
                val existingTxs = repository.transactions.first()
                val accounts = repository.accounts.first()
                val categories = repository.categories.first()

                val rows = BulkFileImporter.parseFile(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    existingTransactions = existingTxs,
                    accounts = accounts,
                    categories = categories
                )

                _uiState.update {
                    it.copy(
                        stagedRows = rows,
                        isLoading = false,
                        errorMessage = if (rows.isEmpty()) "No valid transaction rows found in file" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error reading spreadsheet: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun setFilter(filter: ImportFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleRowSelection(tempId: String) {
        _uiState.update { state ->
            val updated = state.stagedRows.map { row ->
                if (row.tempId == tempId) row.copy(isSelected = !row.isSelected) else row
            }
            state.copy(stagedRows = updated)
        }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            val targetState = !state.allSelected
            val updated = state.stagedRows.map { it.copy(isSelected = targetState) }
            state.copy(stagedRows = updated)
        }
    }

    fun updateRow(updatedRow: StagedTransactionRow) {
        _uiState.update { state ->
            val updated = state.stagedRows.map {
                if (it.tempId == updatedRow.tempId) {
                    val validation = if (updatedRow.amount <= 0.0) "Amount must be greater than 0" else null
                    updatedRow.copy(validationError = validation)
                } else it
            }
            state.copy(stagedRows = updated)
        }
    }

    fun batchEdit(newAccount: String?, newCategory: String?) {
        _uiState.update { state ->
            val updated = state.stagedRows.map { row ->
                if (row.isSelected) {
                    row.copy(
                        account = newAccount ?: row.account,
                        category = newCategory ?: row.category
                    )
                } else row
            }
            state.copy(stagedRows = updated)
        }
    }

    fun deleteRow(tempId: String) {
        _uiState.update { state ->
            val index = state.stagedRows.indexOfFirst { it.tempId == tempId }
            if (index != -1) {
                val removed = state.stagedRows[index]
                val updatedList = state.stagedRows.toMutableList().apply { removeAt(index) }
                state.copy(
                    stagedRows = updatedList,
                    lastDeletedRow = Pair(index, removed)
                )
            } else state
        }
    }

    fun undoDelete() {
        _uiState.update { state ->
            val last = state.lastDeletedRow ?: return@update state
            val updatedList = state.stagedRows.toMutableList()
            val insertIndex = last.first.coerceIn(0, updatedList.size)
            updatedList.add(insertIndex, last.second)
            state.copy(stagedRows = updatedList, lastDeletedRow = null)
        }
    }

    fun discardAll() {
        _uiState.update { it.copy(stagedRows = emptyList(), errorMessage = null) }
    }

    suspend fun commitSelectedTransactions(): Int = withContext(Dispatchers.IO) {
        val currentState = _uiState.value
        val validSelected = currentState.stagedRows.filter { it.isSelected && it.validationError == null }
        if (validSelected.isEmpty()) return@withContext 0

        val existingAccounts = repository.accounts.first().toMutableList()
        val existingCategories = repository.categories.first().toMutableList()

        val accountMap = existingAccounts.associateBy { it.name.lowercase() }.toMutableMap()
        val categoryMap = existingCategories.associateBy { it.name.lowercase() }.toMutableMap()

        val commitTimestamp = System.currentTimeMillis()
        val entities = mutableListOf<TransactionEntity>()

        for (row in validSelected) {
            // Ensure Account exists
            val accKey = row.account.trim().lowercase()
            var acc = accountMap[accKey]
            if (acc == null) {
                val accType = if (accKey.contains("amex") || accKey.contains("credit")) {
                    AccountType.CREDIT_CARD
                } else if (accKey.contains("debit") || accKey.contains("card")) {
                    AccountType.DEBIT_CARD
                } else {
                    AccountType.CASH
                }
                val newAcc = AccountEntity(
                    name = row.account.ifBlank { "DebitCard" },
                    type = accType,
                    balance = 0.0,
                    colorHex = if (accKey.contains("amex")) "#EC4899" else "#3B82F6"
                )
                val newId = repository.addAccount(newAcc)
                acc = newAcc.copy(id = newId)
                accountMap[accKey] = acc
            }

            // Ensure Category exists
            val catKey = row.category.trim().lowercase()
            var cat = categoryMap[catKey]
            val rowType = try {
                TransactionType.valueOf(row.type.uppercase())
            } catch (_: Exception) {
                TransactionType.EXPENSE
            }

            if (cat == null && row.category.isNotBlank()) {
                val newCat = CategoryEntity(
                    name = row.category,
                    type = rowType,
                    colorHex = "#3B82F6"
                )
                val newCatId = repository.addCategory(newCat)
                cat = newCat.copy(id = newCatId)
                categoryMap[catKey] = cat
            }

            val entity = TransactionEntity(
                amount = row.amount,
                type = rowType,
                accountId = acc.id,
                toAccountId = null,
                categoryId = cat?.id,
                subCategory = null,
                merchantName = row.description,
                transactionDateMillis = row.transactionDateMillis,
                createdAtMillis = commitTimestamp, // Records exact commit moment for 24h rollback
                notes = row.description,
                tags = row.tag ?: "imported"
            )
            entities.add(entity)
        }

        val committedCount = repository.bulkImportTransactionsAtomic(entities)
        _uiState.update { it.copy(stagedRows = emptyList()) }
        committedCount
    }
}
