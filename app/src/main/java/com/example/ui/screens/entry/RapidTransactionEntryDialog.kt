package com.example.ui.screens.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.frostedGlass
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Rapid Transaction Entry Dialog:
 * A lightweight, ultra-fast dialog for logging transactions in seconds,
 * with streamlined support for date, amount, category, account, and notes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RapidTransactionEntryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "£",
    onSave: (
        amount: Double,
        type: TransactionType,
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long?,
        subCategory: String?,
        merchantName: String,
        timestamp: Long,
        notes: String,
        tags: String
    ) -> Unit,
    onOpenFullDetail: () -> Unit = {}
) {
    if (!isOpen) return

    val tactile = com.example.ui.util.rememberTactileHaptic()
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var isSplitCategory by remember { mutableStateOf(false) }
    var isReimbursement by remember { mutableStateOf(false) }
    var receiptImageUriState by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember(categories) {
        mutableStateOf(categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id)
    }
    var selectedAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull()?.id ?: 1L)
    }
    var toAccountId by remember(accounts) {
        mutableStateOf(accounts.getOrNull(1)?.id ?: 2L)
    }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountFocusRequester = remember { FocusRequester() }

    // Auto-focus amount input when opened
    LaunchedEffect(isOpen) {
        if (isOpen) {
            try {
                amountFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Filter categories by selected transaction type
    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    // Ensure selected category matches active type
    LaunchedEffect(selectedType) {
        if (filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = filteredCategories.firstOrNull()?.id
        }
    }

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    val activeColor = when (selectedType) {
        TransactionType.EXPENSE -> CoralAccent
        TransactionType.INCOME -> EmeraldGreen
        TransactionType.TRANSFER -> ExpenseBlue
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .clip(RoundedCornerShape(22.dp))
                .frostedGlass(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = Color(0xFA151822),
                    surfaceAlpha = 0.96f,
                    borderWidth = 1.dp,
                    borderColor = Color.White,
                    borderAlpha = 0.22f
                )
                .testTag("rapid_entry_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(activeColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = "Rapid Entry",
                                tint = activeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Rapid Transaction",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenFullDetail,
                            modifier = Modifier.testTag("rapid_open_full_sheet")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Full Detail Mode",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("rapid_cancel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Transaction Type Toggle (Expense, Income, Transfer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1B202D))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransactionType.entries.forEach { type ->
                        val isSelected = selectedType == type
                        val pillColor = when (type) {
                            TransactionType.EXPENSE -> CoralAccent
                            TransactionType.INCOME -> EmeraldGreen
                            TransactionType.TRANSFER -> ExpenseBlue
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) pillColor else Color.Transparent)
                                .clickable {
                                    selectedType = type
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Amount Input Field with Quick Increments
                Text(
                    text = "Amount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        // Allow digits and up to one decimal point
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            amountText = input
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocusRequester)
                        .testTag("rapid_amount_input"),
                    prefix = {
                        Text(
                            text = "$currencySymbol ",
                            color = activeColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    placeholder = {
                        Text(
                            text = "0.00",
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Color(0xFF141721),
                        unfocusedContainerColor = Color(0xFF141721)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Increment Chips (+5, +10, +20, +50, Clear)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val increments = listOf(5, 10, 20, 50)
                    for (inc in increments) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222838))
                                .clickable {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    val updated = current + inc
                                    amountText = String.format(Locale.US, "%.2f", updated)
                                    errorMessage = null
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$currencySymbol$inc",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E2225))
                            .clickable {
                                amountText = ""
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            color = CoralAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Date Selection Field with Quick Chips (Today, Yesterday, Custom Date)
                Text(
                    text = "Date",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick "Today" Chip
                    val todayCal = Calendar.getInstance()
                    val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                    val isToday = todayCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                            todayCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) activeColor.copy(alpha = 0.25f) else Color(0xFF1E2434))
                            .border(1.dp, if (isToday) activeColor else GlassBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedTimestamp = System.currentTimeMillis()
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("rapid_date_today"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Today",
                            color = if (isToday) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Quick "Yesterday" Chip
                    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    val isYesterday = yesterdayCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                            yesterdayCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isYesterday) activeColor.copy(alpha = 0.25f) else Color(0xFF1E2434))
                            .border(1.dp, if (isYesterday) activeColor else GlassBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedTimestamp = yesterdayCal.timeInMillis
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("rapid_date_yesterday"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Yesterday",
                            color = if (isYesterday) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isYesterday) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Clickable Date Picker Display
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2434))
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("rapid_date_picker_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateFormat.format(Date(selectedTimestamp)),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Category Selection Field (Chips / Compact Grid)
                Text(
                    text = if (selectedType == TransactionType.TRANSFER) "Accounts" else "Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (selectedType == TransactionType.TRANSFER) {
                    // Transfer: From Account -> To Account
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From", color = TextSecondary, fontSize = 11.sp)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                accounts.forEach { acc ->
                                    val isSelected = acc.id == selectedAccountId
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) ExpenseBlue else Color(0xFF1E2434))
                                            .clickable { selectedAccountId = acc.id }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = acc.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("To", color = TextSecondary, fontSize = 11.sp)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                accounts.forEach { acc ->
                                    val isSelected = acc.id == toAccountId
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) ExpenseBlue else Color(0xFF1E2434))
                                            .clickable { toAccountId = acc.id }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = acc.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // FlowRow of rapid category chips
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredCategories.forEach { category ->
                            val isSelected = category.id == selectedCategoryId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) activeColor else Color(0xFF1E2434))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.5f) else GlassBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedCategoryId = category.id
                                        errorMessage = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .testTag("rapid_category_${category.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = category.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Split Categories, Reimbursement, and Receipt/Warranty Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedType == TransactionType.EXPENSE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSplitCategory) ExpenseBlue else Color(0xFF1E2434))
                                .clickable {
                                    tactile.click()
                                    isSplitCategory = !isSplitCategory
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("split_category_toggle")
                        ) {
                            Text(
                                text = if (isSplitCategory) "✓ Split Active" else "＋ Split Category",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (selectedType == TransactionType.INCOME) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isReimbursement) EmeraldGreen else Color(0xFF1E2434))
                                .clickable {
                                    tactile.click()
                                    isReimbursement = !isReimbursement
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("reimbursement_toggle")
                        ) {
                            Text(
                                text = if (isReimbursement) "✓ Reimbursement Net" else "⇄ Reimbursement / Net",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (receiptImageUriState != null) EmeraldGreen else Color(0xFF1E2434))
                            .clickable {
                                tactile.click()
                                receiptImageUriState = if (receiptImageUriState == null) "content://mock_receipt_${System.currentTimeMillis()}" else null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("attach_receipt_toggle")
                    ) {
                        Text(
                            text = if (receiptImageUriState != null) "✓ Receipt Attached" else "📷 Attach Receipt",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 5. Account & Optional Note Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedType != TransactionType.TRANSFER) {
                        // Quick Account Dropdown/Pill
                        Column(modifier = Modifier.weight(0.45f)) {
                            Text("Account", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1C212E))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentAccName = accounts.find { it.id == selectedAccountId }?.name ?: "CASH"
                                Text(
                                    text = currentAccName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Optional Note / Merchant Field
                    Column(modifier = Modifier.weight(if (selectedType != TransactionType.TRANSFER) 0.55f else 1f)) {
                        Text("Note (Optional)", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("e.g. Asda, Coffee", color = TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("rapid_note_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeColor,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = Color(0xFF141721),
                                unfocusedContainerColor = Color(0xFF141721),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Error validation message if triggered
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = CoralAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 6. Action Buttons: Cancel and Save Transaction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("rapid_cancel_action_button")
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0.0) {
                                errorMessage = "Please enter an amount greater than 0"
                                return@Button
                            }
                            if (selectedType != TransactionType.TRANSFER && selectedCategoryId == null) {
                                errorMessage = "Please select a category"
                                return@Button
                            }

                            val cat = categories.find { it.id == selectedCategoryId }
                            val merchant = noteText.ifBlank { cat?.name ?: selectedType.name }

                            tactile.success()
                            onSave(
                                parsedAmount,
                                selectedType,
                                selectedAccountId,
                                if (selectedType == TransactionType.TRANSFER) toAccountId else null,
                                selectedCategoryId,
                                null,
                                merchant,
                                selectedTimestamp,
                                noteText,
                                "rapid"
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("rapid_save_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save Entry",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog when user requests custom calendar date
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedTimestamp = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = CoralAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}
