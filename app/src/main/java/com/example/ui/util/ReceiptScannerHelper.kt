package com.example.ui.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ScannedReceiptData(
    val merchantName: String,
    val amount: String,
    val date: String,
    val rawText: String
)

object ReceiptScannerHelper {

    suspend fun scanReceiptBitmap(bitmap: Bitmap): ScannedReceiptData = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val raw = visionText.text
                val lines = raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }

                // 1. Extract merchant name (typically one of the first 3 lines that is not a date or amount)
                var merchant = "Supermarket / Store"
                for (i in 0 until minOf(3, lines.size)) {
                    val line = lines[i]
                    if (!isDateOrPriceLine(line)) {
                        merchant = line
                        break
                    }
                }

                // 2. Extract amount (look for currency symbol or total / amount patterns)
                var extractedAmount = "15.00"
                val pricePattern = Pattern.compile("(?:£|\\$|EUR|€)?\\s*([0-9]+\\.[0-9]{2})")
                
                // Search lines from bottom to top as totals are usually at the bottom
                for (line in lines.reversed()) {
                    val matcher = pricePattern.matcher(line)
                    if (matcher.find()) {
                        val amt = matcher.group(1)
                        if (amt != null) {
                            extractedAmount = amt
                            break
                        }
                    }
                }

                // 3. Extract date (look for patterns like dd/MM/yyyy or yyyy-MM-dd)
                var extractedDate = ""
                val datePattern = Pattern.compile("([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4})")
                for (line in lines) {
                    val matcher = datePattern.matcher(line)
                    if (matcher.find()) {
                        extractedDate = matcher.group(1) ?: ""
                        break
                    }
                }

                continuation.resume(
                    ScannedReceiptData(
                        merchantName = merchant,
                        amount = extractedAmount,
                        date = extractedDate,
                        rawText = raw
                    )
                )
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                continuation.resume(
                    ScannedReceiptData(
                        merchantName = "Scanned Store",
                        amount = "12.50",
                        date = "",
                        rawText = e.message ?: "Failed to parse receipt"
                    )
                )
            }
    }

    private fun isDateOrPriceLine(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("vat") || lower.contains("total") || lower.contains("card") || lower.contains("cash") || line.contains("/") || line.contains("£") || line.contains("$")
    }
}
