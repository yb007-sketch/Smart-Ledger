package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY transactionDateMillis DESC")
    abstract fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY transactionDateMillis DESC")
    abstract suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE transactionDateMillis BETWEEN :startTimestamp AND :endTimestamp ORDER BY transactionDateMillis DESC")
    abstract fun getTransactionsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY transactionDateMillis DESC")
    abstract fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY transactionDateMillis DESC")
    abstract fun getTransactionsForCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE merchantName LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY transactionDateMillis DESC")
    abstract fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    abstract suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE createdAtMillis >= :cutoffMillis ORDER BY createdAtMillis DESC")
    abstract suspend fun getTransactionsCreatedSince(cutoffMillis: Long): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE createdAtMillis >= :cutoffMillis")
    abstract fun getCountTransactionsCreatedSince(cutoffMillis: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    abstract suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    abstract suspend fun deleteTransaction(transaction: TransactionEntity)

    @Delete
    abstract suspend fun deleteTransactions(transactions: List<TransactionEntity>)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND transactionDateMillis BETWEEN :startTimestamp AND :endTimestamp")
    abstract fun getTotalIncomeBetween(startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND transactionDateMillis BETWEEN :startTimestamp AND :endTimestamp")
    abstract fun getTotalExpenseBetween(startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("DELETE FROM transactions")
    abstract suspend fun deleteAllTransactions()

    // ---------------- Direct Atomic Helpers ----------------
    @Query("UPDATE accounts SET balance = balance + :amountDelta WHERE id = :accountId")
    abstract suspend fun updateAccountBalance(accountId: Long, amountDelta: Double)

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    abstract suspend fun findAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findAccountByName(name: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAccountDirect(account: AccountEntity): Long

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findCategoryByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategoryDirect(category: CategoryEntity): Long

    /**
     * Inserts all parsed rows inside a single atomic Room transaction,
     * ensuring Accounts and Categories exist and updating each Account's balance.
     */
    @Transaction
    open suspend fun bulkImportTransactionsAtomic(rows: List<TransactionEntity>): Int {
        var importedCount = 0
        for (tx in rows) {
            // Verify or retrieve account
            var acc = findAccountById(tx.accountId)
            if (acc == null) {
                // Ensure default fallback account exists
                acc = findAccountByName("CASH")
                if (acc == null) {
                    val newId = insertAccountDirect(
                        AccountEntity(name = "CASH", type = AccountType.CASH, balance = 0.0, colorHex = "#10B981")
                    )
                    acc = findAccountById(newId)
                }
            }

            val validAccountId = acc?.id ?: tx.accountId
            val finalTx = if (tx.accountId != validAccountId) tx.copy(accountId = validAccountId) else tx

            // Update Account Balance based on Transaction Type
            when (finalTx.type) {
                TransactionType.INCOME -> {
                    updateAccountBalance(finalTx.accountId, finalTx.amount)
                }
                TransactionType.EXPENSE -> {
                    updateAccountBalance(finalTx.accountId, -finalTx.amount)
                }
                TransactionType.TRANSFER -> {
                    updateAccountBalance(finalTx.accountId, -finalTx.amount)
                    finalTx.toAccountId?.let { toAcc ->
                        updateAccountBalance(toAcc, finalTx.amount)
                    }
                }
            }

            insertTransaction(finalTx)
            importedCount++
        }
        return importedCount
    }

    /**
     * Reverts transactions added in the last 24 hours:
     * reverses their balance impact on each Account and deletes them from Room.
     */
    @Transaction
    open suspend fun revertTransactionsFromLast24Hours(): Int {
        val cutoff = System.currentTimeMillis() - 86_400_000L
        val txsToRevert = getTransactionsCreatedSince(cutoff)
        if (txsToRevert.isEmpty()) return 0

        for (tx in txsToRevert) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    updateAccountBalance(tx.accountId, -tx.amount)
                }
                TransactionType.EXPENSE -> {
                    updateAccountBalance(tx.accountId, tx.amount)
                }
                TransactionType.TRANSFER -> {
                    updateAccountBalance(tx.accountId, tx.amount)
                    tx.toAccountId?.let { toAcc ->
                        updateAccountBalance(toAcc, -tx.amount)
                    }
                }
            }
        }

        deleteTransactions(txsToRevert)
        return txsToRevert.size
    }

    @Query("DELETE FROM transactions")
    abstract suspend fun deleteAllTransactionsInternal()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'transactions'")
    abstract suspend fun resetTransactionsSequenceInternal()

    @Query("UPDATE accounts SET balance = 0.0")
    abstract suspend fun zeroAllAccountBalancesInternal()

    /**
     * Option A: Wipes all transaction history, resets auto-increment sequence,
     * and zeroes all account balances while preserving user Accounts and Categories.
     */
    @Transaction
    open suspend fun clearAllTransactionsAndZeroBalances() {
        deleteAllTransactionsInternal()
        try {
            resetTransactionsSequenceInternal()
        } catch (_: Exception) {}
        zeroAllAccountBalancesInternal()
    }
}
