package com.example.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExitReasonInfo(
    val reason: String,
    val description: String,
    val timestamp: Long,
    val importance: Int,
    val rss: Long,
    val pss: Long
)

object DiagnosticsManager {

    @RequiresApi(Build.VERSION_CODES.R)
    fun getHistoricalExitReasons(context: Context): List<ExitReasonInfo> {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return emptyList()
            val reasons = am.getHistoricalProcessExitReasons(null, 0, 10)
            reasons.map { info ->
                val reasonStr = when (info.reason) {
                    ApplicationExitInfo.REASON_ANR -> "ANR (Application Not Responding)"
                    ApplicationExitInfo.REASON_CRASH -> "Uncaught Exception Crash"
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native Crash (LLM / SQLite)"
                    ApplicationExitInfo.REASON_LOW_MEMORY -> "Low Memory Killer (LMK)"
                    ApplicationExitInfo.REASON_USER_REQUESTED -> "User Force Closed"
                    ApplicationExitInfo.REASON_EXIT_SELF -> "Normal Exit"
                    else -> "Other (${info.reason})"
                }
                ExitReasonInfo(
                    reason = reasonStr,
                    description = info.description ?: "No description",
                    timestamp = info.timestamp,
                    importance = info.importance,
                    rss = info.rss,
                    pss = info.pss
                )
            }
        } catch (e: Exception) {
            Log.e("DiagnosticsManager", "Failed to get historical exit reasons", e)
            emptyList()
        }
    }

    fun exportDiagnosticBundle(context: Context, fileTree: PiiRedactingFileTree): File? {
        return try {
            val diagnosticsDir = File(context.filesDir, "diagnostics")
            val zipFile = File(diagnosticsDir, "SmartLedger_DiagnosticsBundle.zip")
            if (zipFile.exists()) zipFile.delete()

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val logFile = fileTree.getLogFile()
                if (logFile.exists()) {
                    zos.putNextEntry(ZipEntry(logFile.name))
                    logFile.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val exits = getHistoricalExitReasons(context)
                    val exitSummary = exits.joinToString("\n") { 
                        "Reason: ${it.reason} | Desc: ${it.description} | Time: ${it.timestamp} | RSS: ${it.rss}KB | PSS: ${it.pss}KB" 
                    }
                    zos.putNextEntry(ZipEntry("exit_reasons.txt"))
                    zos.write(exitSummary.toByteArray())
                    zos.closeEntry()
                }
            }

            if (zipFile.exists() && zipFile.length() > 0) zipFile else null
        } catch (e: Exception) {
            Log.e("DiagnosticsManager", "Failed to export diagnostic bundle", e)
            null
        }
    }
}
