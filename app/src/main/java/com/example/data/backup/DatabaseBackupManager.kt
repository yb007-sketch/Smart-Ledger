package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupExportResult(
    val fileName: String,
    val fileSizeFormatted: String,
    val timestamp: String
)

class DatabaseBackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    /**
     * Executes a WAL checkpoint (TRUNCATE) to flush all pending -wal and -shm transactions
     * into the main smart_ledger.db file, then streams the database into the SAF target URI.
     */
    suspend fun exportDatabase(targetUri: Uri): Result<BackupExportResult> = withContext(Dispatchers.IO) {
        try {
            // 1. CRITICAL WAL Checkpoint Rule: Flush WAL log into main database file
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                if (cursor.moveToFirst()) {
                    // WAL checkpoint executed successfully
                }
            }

            // 2. Locate main SQLite database file
            val dbFile = context.getDatabasePath("smart_ledger.db")
            if (!dbFile.exists()) {
                return@withContext Result.failure(IllegalStateException("Database file not found at ${dbFile.absolutePath}"))
            }

            // 3. Stream database file to target URI
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                dbFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open output stream for target URI"))

            val sizeBytes = dbFile.length()
            val sizeFormatted = formatFileSize(sizeBytes)
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            Result.success(
                BackupExportResult(
                    fileName = dbFile.name,
                    fileSizeFormatted = sizeFormatted,
                    timestamp = nowStr
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores a previously exported .db file from the SAF source URI:
     * 1. Verifies SQLite header bytes ("SQLite format 3\u0000").
     * 2. Closes Room database.
     * 3. Wipes active database files.
     * 4. Copies input stream into smart_ledger.db.
     * 5. Performs a clean app restart.
     */
    suspend fun restoreDatabase(sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Verify SQLite header signature ("SQLite format 3\0")
            val headerBytes = ByteArray(16)
            var bytesRead = -1
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                bytesRead = stream.read(headerBytes)
            }

            if (bytesRead < 16) {
                return@withContext Result.failure(IllegalArgumentException("Invalid backup file: File is empty or corrupt"))
            }

            val headerStr = String(headerBytes, Charsets.US_ASCII)
            if (!headerStr.startsWith("SQLite format 3")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid backup file selected: Not a valid SQLite database header"))
            }

            // 2. Close active Room instance safely
            AppDatabase.closeDatabase()

            // 3. Delete existing db, wal, and shm files
            val dbFile = context.getDatabasePath("smart_ledger.db")
            val dbParent = dbFile.parentFile
            if (dbParent != null && dbParent.exists()) {
                File(dbParent, "smart_ledger.db").delete()
                File(dbParent, "smart_ledger.db-wal").delete()
                File(dbParent, "smart_ledger.db-shm").delete()
            }

            // 4. Copy restored database file into place
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                dbFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read restore file stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Triggers a clean application restart to force Hilt/Room and UI flows
     * to reconnect cleanly to the restored database without stale state.
     */
    fun restartApplication() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (intent != null) {
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

    /**
     * Option A: Clears all transactions and resets auto-increment sequence
     * while preserving user Accounts and Categories.
     */
    suspend fun clearTransactionsOnly(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionDao().clearAllTransactionsAndZeroBalances()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Option B: Complete Factory Reset - wipes all database tables,
     * clears DataStore preferences, and re-seeds default starter data.
     */
    suspend fun factoryResetApp(preferencesRepository: UserPreferencesRepository): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            preferencesRepository.resetToDefaults()
            AppDatabase.populateInitialData(database)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes Bytes"
        }
    }
}
