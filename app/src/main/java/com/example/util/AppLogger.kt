package com.example.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object AppLogger {
    private val logQueue = ConcurrentLinkedQueue<String>()
    private const val MAX_LOGS = 1500

    init {
        setupUncaughtExceptionHandler()
        log("AppLogger", "Android Production Telemetry & Log Tracer initialized.")
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = throwable.stackTraceToString()
            error("CRASH", "Uncaught Exception on thread ${thread.name}: $stackTrace", throwable)
            // Delegate to default handler so app terminates cleanly or restarts
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun log(tag: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = "[$timestamp] [$tag]: $message"
        logQueue.add(entry)
        if (logQueue.size > MAX_LOGS) {
            logQueue.poll()
        }
        Log.d(tag, message)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = "[$timestamp] [ERROR] [$tag]: $message ${throwable?.stackTraceToString() ?: ""}"
        logQueue.add(entry)
        if (logQueue.size > MAX_LOGS) {
            logQueue.poll()
        }
        Log.e(tag, message, throwable)
    }

    fun getLogsString(): String {
        return logQueue.joinToString("\n")
    }

    /**
     * Professional Android Developer Best Practice:
     * Combines in-memory structured telemetry event queue with a live logcat dump
     * for complete system trace diagnosis.
     */
    fun exportLogs(context: Context): File? {
        return try {
            val sb = StringBuilder()
            sb.append("=== SMARTLEDGER ANDROID TELEMETRY & APP TRACE REPORT ===\n")
            sb.append("Generated at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
            sb.append("Android SDK: ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.RELEASE})\n")
            sb.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n")

            sb.append("--- IN-MEMORY AUDIT & TELEMETRY QUEUE ---\n")
            sb.append(getLogsString())
            sb.append("\n\n")

            sb.append("--- LIVE ANDROID LOGCAT BUFFER DUMP ---\n")
            try {
                val process = Runtime.getRuntime().exec("logcat -d -v time")
                val logcatOutput = process.inputStream.bufferedReader().use { it.readText() }
                // Filter logcat for app package or relevant tags to keep report clean
                val filteredLines = logcatOutput.lines().filter { 
                    it.contains("com.example") || it.contains("SmartLedger") || it.contains("ERROR") || it.contains("AndroidRuntime")
                }
                sb.append(filteredLines.joinToString("\n"))
            } catch (e: Exception) {
                sb.append("Failed to capture logcat buffer: ${e.message}\n")
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "SmartLedger_TelemetryReport_$timeStamp.txt"
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(dir, fileName)
            file.parentFile?.mkdirs()
            file.writeText(sb.toString(), Charsets.UTF_8)

            if (file.exists() && file.length() > 0) file else null
        } catch (e: Exception) {
            error("AppLogger", "Failed to export telemetry report", e)
            null
        }
    }
}
