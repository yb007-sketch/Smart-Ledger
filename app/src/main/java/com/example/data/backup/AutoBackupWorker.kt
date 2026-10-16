package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutoBackupWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefRepo = UserPreferencesRepository(appContext)
            val settings = prefRepo.userSettingsFlow.first()
            if (!settings.autoBackupEnabled) {
                return@withContext Result.success()
            }

            val db = AppDatabase.getInstance(appContext)
            // Flush WAL checkpoints
            try {
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
            } catch (e: Exception) {
                Log.e("AutoBackupWorker", "WAL checkpoint failed", e)
            }

            val dbFile = appContext.getDatabasePath("smart_ledger.db")
            if (!dbFile.exists()) return@withContext Result.failure()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val backupFileName = "SmartLedger_AutoBackup_$timestamp.db"

            if (settings.autoBackupUri.isNotBlank()) {
                val targetUri = Uri.parse(settings.autoBackupUri)
                appContext.contentResolver.openOutputStream(targetUri, "wa")?.use { output ->
                    dbFile.inputStream().use { input -> input.copyTo(output) }
                }
            } else {
                // Fallback to internal/external backup directory
                val backupDir = File(appContext.getExternalFilesDir(null), "AutoBackups").apply { mkdirs() }
                val targetFile = File(backupDir, backupFileName)
                dbFile.copyTo(targetFile, overwrite = true)

                // Retain latest 7 backups
                val backups = backupDir.listFiles { file -> file.name.startsWith("SmartLedger_AutoBackup_") }
                    ?.sortedByDescending { it.lastModified() } ?: emptyList()

                if (backups.size > 7) {
                    backups.drop(7).forEach { oldFile ->
                        runCatching { oldFile.delete() }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("AutoBackupWorker", "Scheduled backup failed", e)
            Result.retry()
        }
    }
}
