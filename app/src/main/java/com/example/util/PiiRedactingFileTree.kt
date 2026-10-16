package com.example.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PiiRedactingFileTree(private val context: Context) {
    private val diagnosticsDir = File(context.filesDir, "diagnostics").apply { mkdirs() }
    private val logFile = File(diagnosticsDir, "telemetry_ringbuffer.jsonl")
    private val maxFileSize = 2 * 1024 * 1024 // 2 MB

    private val currencyRegex = Regex("""([£\$€₹]|GBP|USD|EUR)\s*\d+(\.\d{1,2})?""")
    private val sensitiveMerchants = listOf("asda", "tesco", "aldi", "sainsbury", "shell", "bp", "amazon", "starbucks", "mcdonalds", "dvla", "amex", "monzo", "revolut", "chase")

    @Synchronized
    fun log(priority: Int, tag: String?, message: String, t: Throwable? = null) {
        val redactedMessage = redactPii(message)
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())
        val priorityStr = when (priority) {
            Log.ERROR -> "ERROR"
            Log.WARN -> "WARN"
            Log.INFO -> "INFO"
            else -> "DEBUG"
        }

        val jsonLine = "{\"timestamp\":\"$timestamp\",\"level\":\"$priorityStr\",\"tag\":\"${tag ?: "SmartLedger"}\",\"message\":\"${escapeJson(redactedMessage)}\"}"

        try {
            if (logFile.exists() && logFile.length() > maxFileSize) {
                val backup = File(diagnosticsDir, "telemetry_ringbuffer.old.jsonl")
                if (backup.exists()) backup.delete()
                logFile.renameTo(backup)
            }

            FileWriter(logFile, true).use { writer ->
                writer.append(jsonLine).append("\n")
            }
        } catch (e: Exception) {
            Log.e("PiiRedactingFileTree", "Failed to write ring-buffer log", e)
        }
    }

    private fun redactPii(input: String): String {
        var output = currencyRegex.replace(input) { matchResult ->
            val symbol = matchResult.groupValues[1]
            "$symbol[REDACTED]"
        }
        val lower = output.lowercase()
        for (merchant in sensitiveMerchants) {
            if (lower.contains(merchant)) {
                output = output.replace(merchant, "[REDACTED_MERCHANT]", ignoreCase = true)
            }
        }
        return output
    }

    private fun escapeJson(input: String): String {
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }

    fun getLogFile(): File = logFile

    fun readLogLines(): List<String> {
        return try {
            if (logFile.exists()) logFile.readLines() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearLogs() {
        try {
            if (logFile.exists()) logFile.delete()
        } catch (e: Exception) {
            // ignore
        }
    }
}
