package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.ai.AiFinancialAssistantRepository
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NightlyAiCategorizationWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(appContext)
            val txDao = db.transactionDao()
            val catDao = db.categoryDao()

            val allTxs = txDao.getAllTransactionsSync()
            val categories = catDao.getAllCategoriesSync()
            if (categories.isEmpty() || allTxs.isEmpty()) {
                return@withContext Result.success()
            }

            val aiRepo = AiFinancialAssistantRepository()
            var updatedCount = 0

            for (tx in allTxs) {
                // If category is null or defaulted to generic/auto-notify, re-evaluate using AI smart categorization
                val currentCategory = categories.find { id -> id.id == tx.categoryId }
                val isGeneric = currentCategory == null || 
                    currentCategory.name.equals("Uncategorized", ignoreCase = true) || 
                    tx.tags.contains("auto-notify")

                if (isGeneric && tx.merchantName.isNotBlank()) {
                    val searchString = "${tx.merchantName} ${tx.notes}"
                    val (suggestedCategory, _) = aiRepo.smartAutoCategorize(searchString, categories)
                    if (suggestedCategory != null && suggestedCategory.id != tx.categoryId) {
                        val updatedTx = tx.copy(
                            categoryId = suggestedCategory.id,
                            tags = if (tx.tags.isBlank()) "nightly-ai-sorted" else "${tx.tags},nightly-ai-sorted"
                        )
                        txDao.updateTransaction(updatedTx)
                        updatedCount++
                    }
                }
            }

            Log.i("NightlyAiCategorizationWorker", "Successfully categorized and sorted $updatedCount transactions overnight.")
            Result.success()
        } catch (e: Exception) {
            Log.e("NightlyAiCategorizationWorker", "Nightly AI categorization failed", e)
            Result.retry()
        }
    }
}
