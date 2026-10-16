package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class PaymentNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val ACTION_CONFIRM_NOTIFICATION_TRANSACTION = "com.example.action.CONFIRM_NOTIFICATION_TX"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_ACCOUNT_NAME = "extra_account_name"
        const val EXTRA_CATEGORY_NAME = "extra_category_name"

        private val PAYMENT_PACKAGES = setOf(
            "com.google.android.apps.walletnfcrel", // Google Wallet
            "com.americanexpress.android.acq",       // AMEX UK/US
            "co.uk.getmonzo",                       // Monzo
            "com.revolut.revolut",                  // Revolut
            "com.chase.sig.uk",                     // Chase UK
            "com.barclays.android.barclaysmobilebanking",
            "com.grppl.mbb.bb.barclays",
            "com.htsu.hsbcpersonalbanking",
            "uk.co.lloydsbank.mobilebanking",
            "uk.co.halifax.mobilebanking",
            "uk.co.santander.santanderUK",
            "com.rbs.mobile.natwest",
            "com.starlingbank.android"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val pkg = sbn?.packageName ?: return
        if (!PAYMENT_PACKAGES.contains(pkg) && !pkg.contains("wallet") && !pkg.contains("bank") && !pkg.contains("pay")) {
            return
        }

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val fullText = "$title $text"

        val parsed = parsePaymentMessage(fullText, pkg) ?: return

        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val accounts = db.accountDao().getAllAccountsSync()
            val categories = db.categoryDao().getAllCategoriesSync()

            val account = accounts.find { it.name.equals(parsed.accountName, ignoreCase = true) }
                ?: accounts.firstOrNull { it.isCreditCard }
                ?: accounts.firstOrNull() ?: return@launch

            val category = categories.find { it.name.equals(parsed.categoryName, ignoreCase = true) }
                ?: categories.find { it.name.contains("Grocery", ignoreCase = true) }
                ?: categories.firstOrNull()

            val tx = TransactionEntity(
                amount = parsed.amount,
                type = TransactionType.EXPENSE,
                accountId = account.id,
                categoryId = category?.id,
                merchantName = parsed.merchant,
                notes = "Auto-logged from ${parsed.packageNameShort}",
                tags = "auto-notify,${parsed.packageNameShort.lowercase()}"
            )

            val insertedId = db.transactionDao().insertTransaction(tx)
            postConfirmationNotification(parsed.amount, parsed.merchant, account.name, insertedId)
        }
    }

    data class ParsedPayment(
        val amount: Double,
        val merchant: String,
        val accountName: String,
        val categoryName: String,
        val packageNameShort: String
    )

    private fun parsePaymentMessage(text: String, pkg: String): ParsedPayment? {
        // Example: "You spent £12.50 at Asda on your AMEX card" or "Paid £4.20 to Tesco"
        val amountPattern = Pattern.compile("""(?:£|\$|€|\bGBP\b)\s*([0-9]+(?:\.[0-9]{2})?)""", Pattern.CASE_INSENSITIVE)
        val matcher = amountPattern.matcher(text)
        if (!matcher.find()) return null

        val amountStr = matcher.group(1) ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        var merchant = "Merchant"
        val atPattern = Pattern.compile("""(?:at|to|from)\s+([A-Za-z0-9\s&'-]+?)(?:\s+on|\s+using|\s+with|\.|$)""", Pattern.CASE_INSENSITIVE)
        val merchantMatcher = atPattern.matcher(text)
        if (merchantMatcher.find()) {
            merchant = merchantMatcher.group(1)?.trim() ?: "Merchant"
        } else {
            val words = text.split(" ")
            if (words.size >= 2) merchant = words.take(3).joinToString(" ")
        }

        val accountName = when {
            text.contains("AMEX", ignoreCase = true) || pkg.contains("americanexpress") -> "AMEX"
            text.contains("Debit", ignoreCase = true) || text.contains("Card", ignoreCase = true) -> "DebitCard"
            else -> "DebitCard"
        }

        val categoryName = autoCategorizeMerchant(merchant)
        val shortPkg = when {
            pkg.contains("wallet") -> "Google Wallet"
            pkg.contains("americanexpress") -> "AMEX"
            pkg.contains("monzo") -> "Monzo"
            pkg.contains("revolut") -> "Revolut"
            pkg.contains("chase") -> "Chase"
            else -> "Bank App"
        }

        return ParsedPayment(amount, merchant, accountName, categoryName, shortPkg)
    }

    private fun autoCategorizeMerchant(merchant: String): String {
        val m = merchant.lowercase()
        return when {
            m.contains("asda") || m.contains("tesco") || m.contains("aldi") || m.contains("sainsbury") || m.contains("lidl") || m.contains("waitrose") -> "Grocery"
            m.contains("petrol") || m.contains("shell") || m.contains("bp") || m.contains("parking") || m.contains("uber") || m.contains("train") || m.contains("dvla") -> "Transportation"
            m.contains("amazon") || m.contains("zara") || m.contains("nike") || m.contains("asos") -> "Apparel"
            m.contains("rent") || m.contains("council") || m.contains("gas") || m.contains("electricity") || m.contains("water") -> "House Rent &etc"
            m.contains("starbucks") || m.contains("costa") || m.contains("mcdonald") || m.contains("subway") || m.contains("restaurant") || m.contains("pub") -> "Food"
            else -> "Grocery"
        }
    }

    private fun postConfirmationNotification(amount: Double, merchant: String, accountName: String, txId: Long) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "smart_ledger_payments"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Smart Ledger Auto-Logging",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_TX_ID", txId)
        }
        val pendingOpen = PendingIntent.getActivity(
            applicationContext,
            txId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Auto-Logged: £${String.format("%.2f", amount)} at $merchant")
            .setContentText("Added to $accountName. Tap to view or edit.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
