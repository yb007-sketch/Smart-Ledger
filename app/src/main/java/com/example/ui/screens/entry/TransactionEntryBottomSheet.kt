package com.example.ui.screens.entry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ParsedTransactionDraft
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.KeypadCalculator
import com.example.ui.components.evaluateSimpleExpression
import com.example.ui.components.frostedGlass
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DockedPickerField {
    NONE,
    ACCOUNT,
    TO_ACCOUNT,
    CATEGORY,
    AMOUNT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEntryBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    editingTransaction: TransactionEntity?,
    prefillDraft: ParsedTransactionDraft?,
    currencySymbol: String,
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
    ) -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountExpression by remember { mutableStateOf("0") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var subCategory by remember { mutableStateOf("") }
    var merchantName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    var focusedPicker by remember { mutableStateOf(DockedPickerField.AMOUNT) }
    var isSubcategoryMode by remember { mutableStateOf(false) }
    var isStarred by remember { mutableStateOf(false) }
    var showTemplatesMenu by remember { mutableStateOf(false) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    var recurringInterval by remember { mutableStateOf("None") }

    val scope = rememberCoroutineScope()
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                val scanned = com.example.ui.util.ReceiptScannerHelper.scanReceiptBitmap(bitmap)
                merchantName = scanned.merchantName
                notes = "Scanned Receipt: ${scanned.merchantName}"
                if (scanned.amount.isNotBlank()) {
                    amountExpression = scanned.amount
                }
                val lower = scanned.merchantName.lowercase(Locale.ROOT)
                selectedCategoryId = categories.firstOrNull { c ->
                    val cName = c.name.lowercase()
                    lower.contains(cName) || cName.contains("grocery") || cName.contains("shopping")
                }?.id ?: categories.firstOrNull()?.id
            }
        }
    }

    // Prefill logic
    LaunchedEffect(editingTransaction, prefillDraft, accounts, categories) {
        if (editingTransaction != null) {
            type = editingTransaction.type
            amountExpression = if (editingTransaction.amount % 1.0 == 0.0) editingTransaction.amount.toLong().toString() else editingTransaction.amount.toString()
            selectedAccountId = editingTransaction.accountId
            toAccountId = editingTransaction.toAccountId
            selectedCategoryId = editingTransaction.categoryId
            subCategory = editingTransaction.subCategory ?: ""
            merchantName = editingTransaction.merchantName
            notes = editingTransaction.notes
            tags = editingTransaction.tags
            timestamp = editingTransaction.timestamp
            focusedPicker = DockedPickerField.AMOUNT
        } else if (prefillDraft != null) {
            type = prefillDraft.type
            amountExpression = if (prefillDraft.amount > 0) prefillDraft.amount.toString() else "0"
            selectedAccountId = prefillDraft.accountId ?: (accounts.firstOrNull()?.id ?: 1L)
            selectedCategoryId = prefillDraft.categoryId
            merchantName = prefillDraft.merchantName
            notes = prefillDraft.notes
            tags = prefillDraft.suggestedTags.joinToString(", ")
            timestamp = prefillDraft.timestamp
            focusedPicker = DockedPickerField.AMOUNT
        } else {
            type = TransactionType.EXPENSE
            amountExpression = "0"
            selectedAccountId = accounts.firstOrNull()?.id ?: 1L
            selectedCategoryId = categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
            merchantName = ""
            notes = ""
            tags = ""
            timestamp = System.currentTimeMillis()
            focusedPicker = DockedPickerField.AMOUNT
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy (EEE)  HH:mm", Locale.getDefault()) }
    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
    val selectedToAccount = accounts.firstOrNull { it.id == toAccountId }
    val filteredCategories = categories.filter { it.type == type }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }

    // Smooth animated accent color depending on mode (CoralRed for Income, OceanBlue for Expense, Crisp White for Transfer)
    val activeAccentColor by androidx.compose.animation.animateColorAsState(
        targetValue = when (type) {
            TransactionType.EXPENSE -> ExpenseBlue
            TransactionType.INCOME -> CoralAccent
            TransactionType.TRANSFER -> Color.White
        },
        label = "entryModeAccentColor"
    )
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E121B),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Top Bar with Favorites Star-List Button & 20.dp Horizontal Padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("entry_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = when (type) {
                            TransactionType.EXPENSE -> "Expense"
                            TransactionType.INCOME -> "Income"
                            TransactionType.TRANSFER -> "Transfer"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Favorites / Template Selector
                    Box {
                        IconButton(onClick = { showTemplatesMenu = true }) {
                            Icon(
                                imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "Templates / Favorites",
                                tint = if (isStarred) CoralAccent else Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showTemplatesMenu,
                            onDismissRequest = { showTemplatesMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("⭐ Save as Favorite Template") },
                                onClick = {
                                    isStarred = true
                                    showTemplatesMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚡ Petrol at Shell (£ 31.51)") },
                                onClick = {
                                    merchantName = "Petrol"
                                    amountExpression = "31.51"
                                    notes = "Shell"
                                    selectedCategoryId = categories.firstOrNull { it.name.contains("Transportation", ignoreCase = true) }?.id
                                    showTemplatesMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🛒 Supermarket Asda (£ 16.40)") },
                                onClick = {
                                    merchantName = "Asda"
                                    amountExpression = "16.40"
                                    notes = "Weekly groceries"
                                    selectedCategoryId = categories.firstOrNull { it.name.contains("Grocery", ignoreCase = true) }?.id
                                    showTemplatesMenu = false
                                }
                            )
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            val finalAmount = evaluateSimpleExpression(amountExpression).toDoubleOrNull() ?: 0.0
                            if (finalAmount > 0.0) {
                                onSave(
                                    finalAmount,
                                    type,
                                    selectedAccountId,
                                    if (type == TransactionType.TRANSFER) toAccountId else null,
                                    selectedCategoryId,
                                    subCategory.ifBlank { null },
                                    merchantName.ifBlank { selectedCategory?.name ?: "Expense" },
                                    timestamp,
                                    notes,
                                    tags
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("entry_save_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, color = if (type == TransactionType.TRANSFER) Color.Black else Color.White)
                    }
                }
            }

            // 3-Button Frosted Mode Selector: [Income] [Expense] [Transfer] with 20.dp padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val modes = listOf(
                    Triple(TransactionType.INCOME, "Income", CoralAccent),
                    Triple(TransactionType.EXPENSE, "Expense", ExpenseBlue),
                    Triple(TransactionType.TRANSFER, "Transfer", Color.White)
                )

                for ((t, label, accent) in modes) {
                    val isSelected = type == t
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .frostedGlass(
                                shape = RoundedCornerShape(14.dp),
                                backgroundColor = if (isSelected) Color(0xFF1B2232) else Color(0xFF151A24),
                                surfaceAlpha = 0.65f,
                                borderWidth = if (isSelected) 1.5.dp else 1.dp,
                                borderColor = if (isSelected) accent else Color.White,
                                borderAlpha = if (isSelected) 0.85f else 0.22f
                            )
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                type = t
                                if (t != TransactionType.TRANSFER) {
                                    selectedCategoryId = categories.firstOrNull { it.type == t }?.id
                                }
                            }
                            .testTag("mode_$label"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accent else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Labeled Form Rows with 20.dp horizontal padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Row with Rep/Inst. Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(68.dp))
                    Text(
                        text = dateFormat.format(Date(timestamp)),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222632))
                            .clickable { showRecurringDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Repeat or Installment",
                            tint = if (recurringInterval != "None") CoralAccent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " Rep/Inst.",
                            color = if (recurringInterval != "None") CoralAccent else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Account Row (Underlined in active color when focused)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { focusedPicker = DockedPickerField.ACCOUNT },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(68.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (focusedPicker == DockedPickerField.ACCOUNT) activeAccentColor else Color(0xFF2C303E),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(vertical = 7.dp, horizontal = 10.dp)
                    ) {
                        Text(
                            text = selectedAccount?.name ?: "Select Account",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Category Row OR To Account Row if Transfer
                if (type == TransactionType.TRANSFER) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { focusedPicker = DockedPickerField.TO_ACCOUNT },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("To Acc.", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(68.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 1.dp,
                                    color = if (focusedPicker == DockedPickerField.TO_ACCOUNT) activeAccentColor else Color(0xFF2C303E),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(vertical = 7.dp, horizontal = 10.dp)
                        ) {
                            Text(
                                text = selectedToAccount?.name ?: "Select Destination Account",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { focusedPicker = DockedPickerField.CATEGORY },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(68.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 1.dp,
                                    color = if (focusedPicker == DockedPickerField.CATEGORY) activeAccentColor else Color(0xFF2C303E),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(vertical = 7.dp, horizontal = 10.dp)
                        ) {
                            Text(
                                text = selectedCategory?.name ?: "Select Category",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Amount Row with inline keypad trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { focusedPicker = DockedPickerField.AMOUNT },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amount", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(68.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (focusedPicker == DockedPickerField.AMOUNT) activeAccentColor else Color(0xFF2C303E),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(vertical = 7.dp, horizontal = 10.dp)
                    ) {
                        Text(
                            text = "$currencySymbol $amountExpression",
                            color = activeAccentColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Note Row with Autocomplete & Receipt Camera Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Note", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(68.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            if (merchantName.isBlank()) merchantName = it
                            // Smart auto category trigger
                            val lower = it.lowercase(Locale.ROOT)
                            if (lower.contains("petrol") || lower.contains("shell")) {
                                selectedCategoryId = categories.firstOrNull { c -> c.name.contains("Transportation", ignoreCase = true) }?.id
                            } else if (lower.contains("asda") || lower.contains("tesco") || lower.contains("aldi")) {
                                selectedCategoryId = categories.firstOrNull { c -> c.name.contains("Grocery", ignoreCase = true) }?.id
                            }
                        },
                        placeholder = { Text("e.g. Asda, Shell, Tesco", color = TextSecondary, fontSize = 13.sp) },
                        trailingIcon = {
                            IconButton(onClick = {
                                cameraLauncher.launch(null)
                            }) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Receipt Scan", tint = activeAccentColor)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeAccentColor,
                            unfocusedBorderColor = Color(0xFF2C303E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("note_input")
                    )
                }

                // + Tag Pill Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF232733))
                            .clickable {
                                tags = if (tags.isBlank()) "Personal" else "$tags, Work"
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("+ Tag", color = TextSecondary, fontSize = 12.sp)
                    }

                    if (tags.isNotBlank()) {
                        for (tag in tags.split(",").map { it.trim() }) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(activeAccentColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("#$tag", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ========================================================
            // CUSTOM DOCKED BOTTOM PICKER PANEL (Replaces System Keyboard)
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161922))
                    .border(width = 0.5.dp, color = Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Docked Panel Header Bar matching Screenshot 2 & 3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1D212B))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (focusedPicker) {
                                DockedPickerField.ACCOUNT -> "Accounts"
                                DockedPickerField.TO_ACCOUNT -> "Destination Account"
                                DockedPickerField.CATEGORY -> "Categories"
                                DockedPickerField.AMOUNT -> "Calculator Keypad"
                                DockedPickerField.NONE -> ""
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (focusedPicker == DockedPickerField.CATEGORY) {
                                IconButton(
                                    onClick = { isSubcategoryMode = !isSubcategoryMode },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "Toggle Subcategories",
                                        tint = if (isSubcategoryMode) CoralAccent else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { /* Edit list */ }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit items", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { focusedPicker = DockedPickerField.NONE }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close Picker", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Content of the Docked Picker
                    when (focusedPicker) {
                        DockedPickerField.ACCOUNT, DockedPickerField.TO_ACCOUNT -> {
                            // 3-Column Docked Grid for Accounts (CASH, DebitCard, AMEX, etc.)
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(accounts) { acc ->
                                    val isSelected = if (focusedPicker == DockedPickerField.ACCOUNT) {
                                        acc.id == selectedAccountId
                                    } else {
                                        acc.id == toAccountId
                                    }
                                    Box(
                                        modifier = Modifier
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) activeAccentColor else Color(0xFF222633))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) activeAccentColor else Color(0xFF333849),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (focusedPicker == DockedPickerField.ACCOUNT) {
                                                    selectedAccountId = acc.id
                                                    // Auto advance to Category (or To Account)
                                                    focusedPicker = if (type == TransactionType.TRANSFER) DockedPickerField.TO_ACCOUNT else DockedPickerField.CATEGORY
                                                } else {
                                                    toAccountId = acc.id
                                                    focusedPicker = DockedPickerField.AMOUNT
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = acc.name,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected && type == TransactionType.TRANSFER) Color.Black else Color.White,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        DockedPickerField.CATEGORY -> {
                            // 3-Column Bordered Grid for Categories
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredCategories) { cat ->
                                    val isSelected = cat.id == selectedCategoryId
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) activeAccentColor else Color(0xFF222633))
                                            .border(1.dp, Color(0xFF333849), RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedCategoryId = cat.id
                                                // Auto-advance to Amount keypad
                                                focusedPicker = DockedPickerField.AMOUNT
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        DockedPickerField.AMOUNT -> {
                            // Fast 4x4 Financial Calculator Keypad
                            KeypadCalculator(
                                currentExpression = amountExpression,
                                onValueChange = { amountExpression = it },
                                onDone = {
                                    amountExpression = evaluateSimpleExpression(amountExpression)
                                    focusedPicker = DockedPickerField.NONE
                                }
                            )
                        }

                        DockedPickerField.NONE -> {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
