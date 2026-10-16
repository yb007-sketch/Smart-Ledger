package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LedgerRepository(private val database: AppDatabase) {
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()

    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getExpenseCategories(): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(TransactionType.EXPENSE)
    fun getIncomeCategories(): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(TransactionType.INCOME)
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(start, end)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(monthYear)

    suspend fun addTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val id = transactionDao.insertTransaction(transaction)
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                accountDao.adjustBalance(transaction.accountId, -transaction.amount)
            }
            TransactionType.INCOME -> {
                accountDao.adjustBalance(transaction.accountId, transaction.amount)
            }
            TransactionType.TRANSFER -> {
                accountDao.adjustBalance(transaction.accountId, -transaction.amount)
                transaction.toAccountId?.let { toId ->
                    accountDao.adjustBalance(toId, transaction.amount)
                }
            }
        }
        id
    }

    suspend fun updateTransaction(newTx: TransactionEntity, oldTx: TransactionEntity) = withContext(Dispatchers.IO) {
        // Revert old effect
        when (oldTx.type) {
            TransactionType.EXPENSE -> accountDao.adjustBalance(oldTx.accountId, oldTx.amount)
            TransactionType.INCOME -> accountDao.adjustBalance(oldTx.accountId, -oldTx.amount)
            TransactionType.TRANSFER -> {
                accountDao.adjustBalance(oldTx.accountId, oldTx.amount)
                oldTx.toAccountId?.let { accountDao.adjustBalance(it, -oldTx.amount) }
            }
        }
        // Apply new effect
        when (newTx.type) {
            TransactionType.EXPENSE -> accountDao.adjustBalance(newTx.accountId, -newTx.amount)
            TransactionType.INCOME -> accountDao.adjustBalance(newTx.accountId, newTx.amount)
            TransactionType.TRANSFER -> {
                accountDao.adjustBalance(newTx.accountId, -newTx.amount)
                newTx.toAccountId?.let { accountDao.adjustBalance(it, newTx.amount) }
            }
        }
        transactionDao.updateTransaction(newTx)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        // Revert balance
        when (transaction.type) {
            TransactionType.EXPENSE -> accountDao.adjustBalance(transaction.accountId, transaction.amount)
            TransactionType.INCOME -> accountDao.adjustBalance(transaction.accountId, -transaction.amount)
            TransactionType.TRANSFER -> {
                accountDao.adjustBalance(transaction.accountId, transaction.amount)
                transaction.toAccountId?.let { accountDao.adjustBalance(it, -transaction.amount) }
            }
        }
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun addAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.deleteAccount(account)
    }

    suspend fun addCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
    }

    suspend fun setBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.insertBudget(budget)
    }

    suspend fun exportToCsv(allTransactions: List<TransactionEntity>, accountsList: List<AccountEntity>, categoriesList: List<CategoryEntity>): String = withContext(Dispatchers.Default) {
        val accountMap = accountsList.associateBy { it.id }
        val categoryMap = categoriesList.associateBy { it.id }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.append("Date,Type,Account,ToAccount,Category,Subcategory,Merchant,Amount,Notes,Tags\n")

        for (tx in allTransactions) {
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val typeStr = tx.type.name
            val accName = accountMap[tx.accountId]?.name ?: "Account #${tx.accountId}"
            val toAccName = tx.toAccountId?.let { accountMap[it]?.name } ?: ""
            val catName = tx.categoryId?.let { categoryMap[it]?.name } ?: ""
            val subCat = tx.subCategory ?: ""
            val merchant = tx.merchantName.replace(",", ";")
            val amount = String.format(Locale.US, "%.2f", tx.amount)
            val notes = tx.notes.replace(",", ";")
            val tags = tx.tags.replace(",", ";")

            sb.append("\"$dateStr\",\"$typeStr\",\"$accName\",\"$toAccName\",\"$catName\",\"$subCat\",\"$merchant\",$amount,\"$notes\",\"$tags\"\n")
        }
        sb.toString()
    }

    suspend fun importFromCsv(csvContent: String): Int = withContext(Dispatchers.IO) {
        val lines = csvContent.lines()
        var importedCount = 0
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            if (parts.size >= 8) {
                try {
                    val dateStr = parts[0].replace("\"", "").trim()
                    val typeStr = parts[1].replace("\"", "").trim()
                    val amountStr = parts[7].replace("\"", "").trim()
                    val merchant = if (parts.size > 6) parts[6].replace("\"", "").trim() else ""
                    val notes = if (parts.size > 8) parts[8].replace("\"", "").trim() else ""

                    val date = try { dateFormat.parse(dateStr) } catch (e: Exception) { Date() }
                    val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                    val amount = amountStr.toDoubleOrNull() ?: 0.0

                    if (amount > 0) {
                        val tx = TransactionEntity(
                            amount = amount,
                            type = type,
                            accountId = 1, // Default CASH
                            merchantName = merchant,
                            transactionDateMillis = date?.time ?: System.currentTimeMillis(),
                            notes = notes
                        )
                        addTransaction(tx)
                        importedCount++
                    }
                } catch (e: Exception) {
                    // skip malformed row
                }
            }
        }
        importedCount
    }

    suspend fun bulkImportTransactionsAtomic(transactionsList: List<TransactionEntity>): Int = withContext(Dispatchers.IO) {
        transactionDao.bulkImportTransactionsAtomic(transactionsList)
    }

    suspend fun revertTransactionsFromLast24Hours(): Int = withContext(Dispatchers.IO) {
        transactionDao.revertTransactionsFromLast24Hours()
    }

    fun getTransactionsCreatedSinceCount(cutoffMillis: Long): Flow<Int> =
        transactionDao.getCountTransactionsCreatedSince(cutoffMillis)

    suspend fun resetDatabase(fullReset: Boolean) = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
        budgetDao.deleteAllBudgets()
        if (fullReset) {
            accountDao.deleteAllAccounts()
            categoryDao.deleteAllCategories()
            AppDatabase.populateInitialData(database)
        }
    }
}
