package com.example.data.importer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Immutable
data class StagedTransactionRow(
    val tempId: String = UUID.randomUUID().toString(),
    val isSelected: Boolean = true,
    val transactionDateMillis: Long,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val account: String,
    val category: String,
    val description: String,
    val amount: Double,
    val tag: String? = null,
    val isPossibleDuplicate: Boolean = false,
    val validationError: String? = null
)

object BulkFileImporter {

    private val supportedDateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.UK),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.UK),
        SimpleDateFormat("dd/MM/yyyy", Locale.UK),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK),
        SimpleDateFormat("yyyy-MM-dd", Locale.UK),
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.UK),
        SimpleDateFormat("dd-MM-yyyy", Locale.UK),
        SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US)
    )

    /**
     * Parses a .csv, .xlsx, or .xls file from the given SAF Uri into a list of [StagedTransactionRow]s,
     * cross-checking against existing database transactions to flag duplicates.
     */
    suspend fun parseFile(
        context: Context,
        uri: Uri,
        fileName: String,
        existingTransactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>
    ): List<StagedTransactionRow> = withContext(Dispatchers.IO) {
        val lowerName = fileName.lowercase(Locale.ROOT)
        val rawRows: List<RawParsedRow> = if (lowerName.endsWith(".csv") || lowerName.endsWith(".txt")) {
            parseCsv(context, uri)
        } else {
            // .xlsx or .xls via Apache POI
            parseExcel(context, uri)
        }

        // Map raw parsed rows to StagedTransactionRow and cross-check for duplicates & validation
        processRawRows(rawRows, existingTransactions, accounts, categories)
    }

    private data class RawParsedRow(
        val rawDate: String = "",
        val dateMillis: Long? = null,
        val type: String = "",
        val account: String = "",
        val category: String = "",
        val description: String = "",
        val amountStr: String = "",
        val tag: String? = null
    )

    private fun parseCsv(context: Context, uri: Uri): List<RawParsedRow> {
        val result = mutableListOf<RawParsedRow>()
        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            val lines = reader.lineSequence().filter { it.isNotBlank() }.toList()
            if (lines.isEmpty()) return emptyList()

            // Header mapping
            val headerTokens = parseCsvLine(lines[0]).map { it.trim().lowercase(Locale.ROOT) }
            val colMap = mapHeaders(headerTokens)

            for (lineIndex in 1 until lines.size) {
                val tokens = parseCsvLine(lines[lineIndex])
                if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue

                val row = extractRawRow(tokens, colMap)
                if (row != null) {
                    result.add(row)
                }
            }
        }
        return result
    }

    private fun parseExcel(context: Context, uri: Uri): List<RawParsedRow> {
        val result = mutableListOf<RawParsedRow>()
        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

        inputStream.use { stream ->
            val workbook = WorkbookFactory.create(stream)
            val sheet = workbook.getNumberOfSheets().takeIf { it > 0 }?.let { workbook.getSheetAt(0) } ?: return emptyList()

            val headerRow = sheet.getRow(0) ?: return emptyList()
            val headerTokens = mutableListOf<String>()
            for (c in 0 until headerRow.lastCellNum) {
                val cell = headerRow.getCell(c)
                headerTokens.add(getCellValueAsString(cell).trim().lowercase(Locale.ROOT))
            }

            val colMap = mapHeaders(headerTokens)

            for (r in 1..sheet.lastRowNum) {
                val row = sheet.getRow(r) ?: continue
                val tokens = mutableListOf<String>()
                var cellDateMillis: Long? = null

                val maxCol = maxOf(row.lastCellNum.toInt(), headerTokens.size)
                for (c in 0 until maxCol) {
                    val cell = row.getCell(c)
                    if (c == colMap.dateCol && cell != null && cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        cellDateMillis = cell.dateCellValue.time
                    }
                    tokens.add(getCellValueAsString(cell))
                }

                if (tokens.all { it.isBlank() }) continue

                val parsedRow = extractRawRow(tokens, colMap, cellDateMillis)
                if (parsedRow != null) {
                    result.add(parsedRow)
                }
            }
            workbook.close()
        }
        return result
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.UK)
                    df.format(cell.dateCellValue)
                } else {
                    val num = cell.numericCellValue
                    if (num == num.toLong().toDouble()) {
                        num.toLong().toString()
                    } else {
                        String.format(Locale.US, "%.2f", num)
                    }
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue
                } catch (_: Exception) {
                    try {
                        String.format(Locale.US, "%.2f", cell.numericCellValue)
                    } catch (_: Exception) {
                        ""
                    }
                }
            }
            else -> ""
        }
    }

    private data class HeaderColumnMap(
        val dateCol: Int = -1,
        val typeCol: Int = -1,
        val accountCol: Int = -1,
        val categoryCol: Int = -1,
        val descriptionCol: Int = -1,
        val amountCol: Int = -1,
        val tagCol: Int = -1
    )

    private fun mapHeaders(headers: List<String>): HeaderColumnMap {
        var dateCol = -1
        var typeCol = -1
        var accountCol = -1
        var categoryCol = -1
        var descriptionCol = -1
        var amountCol = -1
        var tagCol = -1

        headers.forEachIndexed { idx, h ->
            val clean = h.replace("\"", "").replace("_", " ").trim()
            when {
                clean.contains("date") || clean.contains("time") -> if (dateCol == -1) dateCol = idx
                clean == "type" || clean.contains("trans type") || clean.contains("kind") -> if (typeCol == -1) typeCol = idx
                clean.contains("account") || clean.contains("card") || clean.contains("method") -> if (accountCol == -1) accountCol = idx
                clean.contains("category") || clean == "cat" -> if (categoryCol == -1) categoryCol = idx
                clean.contains("desc") || clean.contains("note") || clean.contains("merchant") || clean.contains("payee") || clean.contains("memo") || clean.contains("details") -> if (descriptionCol == -1) descriptionCol = idx
                clean.contains("amount") || clean.contains("value") || clean.contains("total") || clean.contains("spent") || clean.contains("price") -> if (amountCol == -1) amountCol = idx
                clean.contains("tag") -> if (tagCol == -1) tagCol = idx
            }
        }

        // Sensible fallbacks if no headers were matched
        if (dateCol == -1 && headers.isNotEmpty()) dateCol = 0
        if (amountCol == -1) {
            // Find first header resembling a numeric column or column 7
            amountCol = if (headers.size > 7) 7 else headers.size - 1
        }
        if (descriptionCol == -1) {
            descriptionCol = if (headers.size > 6) 6 else if (headers.size > 4) 4 else 1
        }

        return HeaderColumnMap(
            dateCol = dateCol,
            typeCol = typeCol,
            accountCol = accountCol,
            categoryCol = categoryCol,
            descriptionCol = descriptionCol,
            amountCol = amountCol,
            tagCol = tagCol
        )
    }

    private fun extractRawRow(tokens: List<String>, colMap: HeaderColumnMap, explicitDateMillis: Long? = null): RawParsedRow? {
        fun get(idx: Int): String = if (idx in tokens.indices) tokens[idx].trim().replace("\"", "") else ""

        val rawDate = get(colMap.dateCol)
        val rawType = get(colMap.typeCol)
        val rawAccount = get(colMap.accountCol)
        val rawCategory = get(colMap.categoryCol)
        val rawDesc = get(colMap.descriptionCol)
        val rawAmount = get(colMap.amountCol)
        val rawTag = if (colMap.tagCol != -1) get(colMap.tagCol).takeIf { it.isNotBlank() } else null

        if (rawDate.isBlank() && rawDesc.isBlank() && rawAmount.isBlank()) return null

        return RawParsedRow(
            rawDate = rawDate,
            dateMillis = explicitDateMillis,
            type = rawType,
            account = rawAccount,
            category = rawCategory,
            description = rawDesc,
            amountStr = rawAmount,
            tag = rawTag
        )
    }

    private fun processRawRows(
        rawRows: List<RawParsedRow>,
        existingTransactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>
    ): List<StagedTransactionRow> {
        val result = mutableListOf<StagedTransactionRow>()
        val defaultAccountName = accounts.firstOrNull()?.name ?: "DebitCard"

        for (raw in rawRows) {
            // 1. Parse Date
            val dateMillis: Long = raw.dateMillis ?: parseDateToMillis(raw.rawDate)

            // 2. Parse Amount (Strip £, $, €, commas)
            val cleanedAmountStr = raw.amountStr
                .replace("£", "")
                .replace("$", "")
                .replace("€", "")
                .replace(",", "")
                .trim()

            var parsedAmount = cleanedAmountStr.toDoubleOrNull() ?: 0.0

            // 3. Resolve Transaction Type
            var resolvedType = raw.type.uppercase(Locale.ROOT)
            if (resolvedType.isBlank()) {
                resolvedType = if (parsedAmount < 0) {
                    parsedAmount = kotlin.math.abs(parsedAmount)
                    "EXPENSE"
                } else if (isLikelyIncome(raw.description, raw.category)) {
                    "INCOME"
                } else {
                    "EXPENSE"
                }
            } else {
                when {
                    resolvedType.contains("INC") -> resolvedType = "INCOME"
                    resolvedType.contains("TRANS") -> resolvedType = "TRANSFER"
                    else -> resolvedType = "EXPENSE"
                }
                parsedAmount = kotlin.math.abs(parsedAmount)
            }

            // 4. Resolve Description
            val cleanDesc = raw.description.ifBlank {
                raw.category.ifBlank { "Imported $resolvedType" }
            }

            // 5. Smart Category Mapping & Auto-Categorization
            val autoCat = autoCategorize(cleanDesc, raw.category, resolvedType)

            // 6. Resolve Account
            val resolvedAccount = resolveAccount(raw.account, cleanDesc, defaultAccountName)

            // 7. Validation Check
            val validationError = when {
                parsedAmount <= 0.0 -> "Amount must be greater than 0"
                autoCat.isBlank() -> "Category is required"
                else -> null
            }

            // 8. Cross-check for Possible Duplicates
            // Check existing transactions matching same day + amount + account
            val isDuplicate = isDuplicateMatch(dateMillis, parsedAmount, resolvedAccount, existingTransactions, accounts)

            result.add(
                StagedTransactionRow(
                    tempId = UUID.randomUUID().toString(),
                    isSelected = (validationError == null && !isDuplicate),
                    transactionDateMillis = dateMillis,
                    type = resolvedType,
                    account = resolvedAccount,
                    category = autoCat,
                    description = cleanDesc,
                    amount = parsedAmount,
                    tag = raw.tag,
                    isPossibleDuplicate = isDuplicate,
                    validationError = validationError
                )
            )
        }

        // Return sorted in descending order by date
        return result.sortedByDescending { it.transactionDateMillis }
    }

    private fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        for (format in supportedDateFormats) {
            try {
                val parsed = format.parse(dateStr)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    /**
     * Auto-categorizes missing or generic categories based on description keywords.
     */
    fun autoCategorize(description: String, rawCategory: String, type: String): String {
        if (rawCategory.isNotBlank() && rawCategory.lowercase(Locale.ROOT) !in listOf("uncategorized", "general", "other", "expense", "income")) {
            return rawCategory.replaceFirstChar { it.uppercase() }
        }

        val lower = description.lowercase(Locale.ROOT)
        return when {
            // Grocery
            lower.contains("asda") || lower.contains("tesco") || lower.contains("aldi") ||
                    lower.contains("sainsbury") || lower.contains("morrison") || lower.contains("lidl") ||
                    lower.contains("m&s") || lower.contains("waitrose") || lower.contains("grocery") ||
                    lower.contains("groceries") || lower.contains("supermarket") -> "Grocery"

            // Transportation
            lower.contains("petrol") || lower.contains("fuel") || lower.contains("parking") ||
                    lower.contains("car parking") || lower.contains("dvla") || lower.contains("dvla test") ||
                    lower.contains("shell") || lower.contains("bp") || lower.contains("esso") ||
                    lower.contains("texaco") || lower.contains("uber") || lower.contains("train") ||
                    lower.contains("tfl") || lower.contains("rail") || lower.contains("bus") -> "Transportation"

            // Apparel
            lower.contains("amazon prime") || lower.contains("amazon") || lower.contains("zara") ||
                    lower.contains("h&m") || lower.contains("asos") || lower.contains("nike") ||
                    lower.contains("primark") || lower.contains("next") || lower.contains("clothing") ||
                    lower.contains("apparel") -> "Apparel"

            // Electronics
            lower.contains("currys") || lower.contains("pc world") || lower.contains("apple") ||
                    lower.contains("argos") || lower.contains("samsung") || lower.contains("electronics") -> "Electronics"

            // Food & Dining
            lower.contains("restaurant") || lower.contains("cafe") || lower.contains("starbucks") ||
                    lower.contains("costa") || lower.contains("mcdonald") || lower.contains("kfc") ||
                    lower.contains("nando") || lower.contains("pub") || lower.contains("bar") ||
                    lower.contains("dinner") || lower.contains("lunch") || lower.contains("takeaway") -> "Food"

            // Entertainment
            lower.contains("cinema") || lower.contains("cineworld") || lower.contains("odeon") ||
                    lower.contains("netflix") || lower.contains("spotify") || lower.contains("disney") ||
                    lower.contains("steam") || lower.contains("playstation") -> "Entertainment"

            // Health
            lower.contains("boots") || lower.contains("pharmacy") || lower.contains("nhs") ||
                    lower.contains("gym") || lower.contains("dentist") || lower.contains("doctor") ||
                    lower.contains("health") -> "Health"

            // House Rent & utilities
            lower.contains("rent") || lower.contains("council tax") || lower.contains("water") ||
                    lower.contains("british gas") || lower.contains("electricity") || lower.contains("energy") -> "House Rent &etc"

            // Income: Salary & Bonus
            lower.contains("salary") || lower.contains("payroll") || lower.contains("wage") -> "Salary"
            lower.contains("bonus") || lower.contains("dividend") || lower.contains("interest") || lower.contains("cashback") -> "Bonus"

            type == "INCOME" -> "Salary"
            else -> "Food"
        }
    }

    private fun resolveAccount(rawAccount: String, desc: String, defaultAccount: String): String {
        val combined = "$rawAccount $desc".lowercase(Locale.ROOT)
        return when {
            combined.contains("amex") || combined.contains("american express") -> "AMEX"
            combined.contains("debit") || combined.contains("card") || combined.contains("barclays") ||
                    combined.contains("monzo") || combined.contains("revolut") || combined.contains("lloyds") ||
                    combined.contains("hsbc") -> "DebitCard"
            combined.contains("cash") -> "CASH"
            rawAccount.isNotBlank() -> rawAccount
            else -> defaultAccount
        }
    }

    private fun isLikelyIncome(desc: String, cat: String): Boolean {
        val lower = "$desc $cat".lowercase(Locale.ROOT)
        return lower.contains("salary") || lower.contains("payroll") || lower.contains("bonus") ||
                lower.contains("refund") || lower.contains("dividend") || lower.contains("deposit") ||
                lower.contains("interest") || lower.contains("wage")
    }

    private fun isDuplicateMatch(
        dateMillis: Long,
        amount: Double,
        accountName: String,
        existing: List<TransactionEntity>,
        accounts: List<AccountEntity>
    ): Boolean {
        val accountMap = accounts.associateBy { it.id }
        val targetCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)
        val targetYear = targetCal.get(Calendar.YEAR)

        return existing.any { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.transactionDateMillis }
            val sameDay = txCal.get(Calendar.DAY_OF_YEAR) == targetDay && txCal.get(Calendar.YEAR) == targetYear
            val sameAmount = kotlin.math.abs(tx.amount - amount) < 0.01
            val txAccountName = accountMap[tx.accountId]?.name ?: ""
            val sameAccount = txAccountName.equals(accountName, ignoreCase = true) || accountName.isBlank()

            sameDay && sameAmount && sameAccount
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val c = line[i]
            when {
                c == '\"' -> {
                    inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
