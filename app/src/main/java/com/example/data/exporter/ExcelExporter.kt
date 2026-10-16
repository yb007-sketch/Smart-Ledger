package com.example.data.exporter

import android.content.Context
import android.os.Environment
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    fun exportTransactionsToExcel(
        context: Context,
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        currencySymbol: String
    ): File? {
        return try {
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val sb = StringBuilder()
            // CSV Header with all transaction fields including category amounts, accounts, subcategories, tags, notes
            sb.append("ID,Date,Type,Merchant,Category,SubCategory,Account,Amount ($currencySymbol),Tags,Notes\n")

            // Populate all data rows
            for (tx in transactions) {
                val id = tx.id
                val dateStr = dateFormat.format(Date(tx.timestamp))
                val typeStr = tx.type.name
                val merchant = escapeCsv(tx.merchantName)
                val categoryName = escapeCsv(categoryMap[tx.categoryId]?.name ?: "Uncategorized")
                val subCategory = escapeCsv(tx.subCategory ?: "")
                val accountName = escapeCsv(accountMap[tx.accountId]?.name ?: "Default")
                val amount = tx.amount
                val tags = escapeCsv(tx.tags)
                val notes = escapeCsv(tx.notes)

                sb.append("$id,$dateStr,$typeStr,$merchant,$categoryName,$subCategory,$accountName,$amount,$tags,$notes\n")
            }

            // Save file to guaranteed downloads or files directory
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "SmartLedger_Transactions_$timeStamp.csv"
            
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(dir, fileName)
            
            file.parentFile?.mkdirs()
            file.writeText(sb.toString(), Charsets.UTF_8)

            if (file.exists() && file.length() > 0) {
                file
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
