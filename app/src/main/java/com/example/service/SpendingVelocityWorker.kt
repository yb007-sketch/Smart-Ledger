package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.ai.AiFinancialAssistantRepository
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpendingVelocityWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val transactions = db.transactionDao().getAllTransactionsSync()
            val aiRepo = AiFinancialAssistantRepository()

            val anomalies = aiRepo.detectAnomalies(transactions)
            if (anomalies.isNotEmpty()) {
                Log.w("SpendingVelocityWorker", "Detected ${anomalies.size} spending velocity anomalies / category spikes.")
                com.example.util.AppLogger.log("SpendingVelocityWorker", "Weekly velocity check found ${anomalies.size} category spikes.")
            } else {
                Log.i("SpendingVelocityWorker", "Weekly spending velocity check completed. No anomalies detected.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SpendingVelocityWorker", "Failed to run spending velocity worker", e)
            Result.retry()
        }
    }
}
