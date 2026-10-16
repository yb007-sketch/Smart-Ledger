package com.example.ui.screens.importpreview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.importer.StagedTransactionRow
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.frostedGlass
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    viewModel: ImportPreviewViewModel,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "£",
    onClose: () -> Unit,
    onCommitted: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val tactile = com.example.ui.util.rememberTactileHaptic()

    var editingRow by remember { mutableStateOf<StagedTransactionRow?>(null) }
    var showBatchEditDialog by remember { mutableStateOf(false) }

    val dayFormat = remember { SimpleDateFormat("dd EEE MM.yyyy", Locale.getDefault()) }

    // Group rows by formatted date
    val groupedRows = remember(uiState.filteredRows) {
        uiState.filteredRows.groupBy {
            dayFormat.format(Date(it.transactionDateMillis))
        }.toList()
    }

    AmbientMeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                // Frosted Glass Top App Bar with live selection info and batch edit
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .frostedGlass(
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            backgroundColor = Color(0xD912151C),
                            surfaceAlpha = 0.85f,
                            borderAlpha = 0.18f
                        )
                        .testTag("import_preview_top_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("import_preview_close")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Import", tint = Color.White)
                            }
                            Column {
                                Text(
                                    text = "Review Import (${uiState.selectedCount} Selected)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (uiState.fileName.isNotBlank()) {
                                    Text(
                                        text = uiState.fileName,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Select All / None Toggle Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { viewModel.toggleSelectAll() }
                                    .padding(horizontal = 6.dp)
                            ) {
                                Checkbox(
                                    checked = uiState.allSelected,
                                    onCheckedChange = { viewModel.toggleSelectAll() },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = CoralAccent,
                                        uncheckedColor = TextSecondary,
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("select_all_checkbox")
                                )
                                Text("All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Batch Edit Icon Button
                            IconButton(
                                onClick = { showBatchEditDialog = true },
                                enabled = uiState.selectedCount > 0,
                                modifier = Modifier.testTag("batch_edit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Batch Edit",
                                    tint = if (uiState.selectedCount > 0) CoralAccent else Color.Gray
                                )
                            }
                        }
                    }

                    // Sticky 3-Column Live Summary Bar (Income, Expenses, Total)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF131620))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Income", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "$currencySymbol ${String.format(Locale.US, "%.2f", uiState.stagedIncome)}",
                                color = CoralAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Expenses", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "$currencySymbol ${String.format(Locale.US, "%.2f", uiState.stagedExpenses)}",
                                color = ExpenseBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Total", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "${if (uiState.stagedNetTotal < 0) "-" else ""}$currencySymbol ${String.format(Locale.US, "%.2f", kotlin.math.abs(uiState.stagedNetTotal))}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Filter Chips Row: All, Valid, Possible Duplicates, Needs Attention
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allCount = uiState.stagedRows.size
                        val validCount = uiState.stagedRows.count { it.validationError == null && !it.isPossibleDuplicate }
                        val dupCount = uiState.stagedRows.count { it.isPossibleDuplicate }
                        val attentionCount = uiState.stagedRows.count { it.validationError != null }

                        FilterPill(
                            label = "All ($allCount)",
                            isSelected = uiState.selectedFilter == ImportFilter.ALL,
                            onClick = { viewModel.setFilter(ImportFilter.ALL) },
                            tag = "filter_all"
                        )
                        FilterPill(
                            label = "Valid ($validCount)",
                            isSelected = uiState.selectedFilter == ImportFilter.VALID,
                            onClick = { viewModel.setFilter(ImportFilter.VALID) },
                            tag = "filter_valid"
                        )
                        if (dupCount > 0) {
                            FilterPill(
                                label = "Duplicates ($dupCount)",
                                isSelected = uiState.selectedFilter == ImportFilter.DUPLICATES,
                                onClick = { viewModel.setFilter(ImportFilter.DUPLICATES) },
                                activeColor = Color(0xFFF59E0B),
                                tag = "filter_duplicates"
                            )
                        }
                        if (attentionCount > 0) {
                            FilterPill(
                                label = "Fix Required ($attentionCount)",
                                isSelected = uiState.selectedFilter == ImportFilter.NEEDS_ATTENTION,
                                onClick = { viewModel.setFilter(ImportFilter.NEEDS_ATTENTION) },
                                activeColor = CoralAccent,
                                tag = "filter_attention"
                            )
                        }
                    }
                }
            },
            bottomBar = {
                // Sticky Frosted Bottom Commit Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .frostedGlass(
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                            backgroundColor = Color(0xFA131620),
                            surfaceAlpha = 0.95f,
                            borderAlpha = 0.22f
                        )
                        .padding(14.dp)
                        .testTag("import_commit_bar")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                tactile.click()
                                viewModel.discardAll()
                                onClose()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("import_discard_all_button")
                        ) {
                            Text("Discard All", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val committed = viewModel.commitSelectedTransactions()
                                    tactile.success()
                                    onCommitted(committed)
                                }
                            },
                            enabled = uiState.validSelectedCount > 0 && !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralAccent,
                                disabledContainerColor = Color(0xFF3B2529)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp)
                                .testTag("import_commit_button")
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "Commit ${uiState.validSelectedCount} Transactions ($currencySymbol${String.format(Locale.US, "%.2f", uiState.stagedExpenses + uiState.stagedIncome)})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.validSelectedCount > 0) Color.White else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CoralAccent)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Parsing spreadsheet rows...", color = Color.White, fontSize = 14.sp)
                        }
                    }
                } else if (uiState.filteredRows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.errorMessage ?: "No transactions found under this filter",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    // Date-wise Grouped LazyColumn with stable keys
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("import_preview_list"),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        for ((dayHeader, rowsInDay) in groupedRows) {
                            val dayIncome = rowsInDay.filter { it.isSelected && it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
                            val dayExpense = rowsInDay.filter { it.isSelected && it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }

                            // Sticky Day Header matching Daily tab: "09 Fri 10.2026 £0.00 £27.60"
                            item(key = "header_$dayHeader", contentType = "header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF161922))
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val parts = dayHeader.split(" ")
                                    val dayNum = parts.getOrNull(0) ?: ""
                                    val dayOfWeek = parts.getOrNull(1) ?: ""
                                    val monthYear = parts.getOrNull(2) ?: ""

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = dayNum, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2C3242))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(text = dayOfWeek, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(text = monthYear, fontSize = 12.sp, color = TextSecondary)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "$currencySymbol ${String.format(Locale.US, "%.2f", dayIncome)}",
                                            color = CoralAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$currencySymbol ${String.format(Locale.US, "%.2f", dayExpense)}",
                                            color = ExpenseBlue,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Items in day with SwipeToDismiss
                            items(
                                items = rowsInDay,
                                key = { it.tempId },
                                contentType = { "staged_row" }
                            ) { row ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            tactile.heavyClick()
                                            viewModel.deleteRow(row.tempId)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Removed row: ${row.description}",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.undoDelete()
                                                }
                                            }
                                            true
                                        } else false
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFDC2626))
                                                .padding(end = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                        }
                                    }
                                ) {
                                    StagedRowCard(
                                        row = row,
                                        currencySymbol = currencySymbol,
                                        onToggleSelect = { viewModel.toggleRowSelection(row.tempId) },
                                        onClick = { editingRow = row }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Inline Row Editor Modal Bottom Sheet
    editingRow?.let { row ->
        InlineRowEditorSheet(
            row = row,
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            onDismiss = { editingRow = null },
            onSave = { updated ->
                viewModel.updateRow(updated)
                editingRow = null
            }
        )
    }

    // Batch Edit Dialog
    if (showBatchEditDialog) {
        BatchEditDialog(
            accounts = accounts,
            categories = categories,
            selectedCount = uiState.selectedCount,
            onDismiss = { showBatchEditDialog = false },
            onApply = { account, category ->
                viewModel.batchEdit(account, category)
                showBatchEditDialog = false
            }
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color = CoralAccent,
    tag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.25f) else Color(0xFF1B202B))
            .border(1.dp, if (isSelected) activeColor else GlassBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}

@Composable
private fun StagedRowCard(
    row: StagedTransactionRow,
    currencySymbol: String,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    val amountColor = when (row.type.uppercase()) {
        "INCOME" -> CoralAccent
        "TRANSFER" -> Color.White
        else -> ExpenseBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (row.isSelected) Color(0xFF141720) else Color(0xFF0E1015))
            .border(0.5.dp, Color(0xFF222634))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("staged_row_${row.tempId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Checkbox
        Checkbox(
            checked = row.isSelected,
            onCheckedChange = { onToggleSelect() },
            colors = CheckboxDefaults.colors(
                checkedColor = CoralAccent,
                uncheckedColor = TextSecondary,
                checkmarkColor = Color.White
            ),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Center Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF242A38))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = row.category,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Account Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1F2430))
                        .border(0.5.dp, Color(0xFF333C4E), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = row.account,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Badges: Duplicate (Amber) & Fix Required (Coral-Red)
                if (row.isPossibleDuplicate) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33F59E0B))
                            .border(0.5.dp, Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("Possible Duplicate", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (row.validationError != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FF5A5F))
                            .border(0.5.dp, CoralAccent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("Fix Required", color = CoralAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Description and optional Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.description,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!row.tag.isNullOrBlank()) {
                    Text(
                        text = " #${row.tag}",
                        color = ExpenseBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Amount & Tap-to-edit indicator
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$currencySymbol ${String.format(Locale.US, "%.2f", row.amount)}",
                color = amountColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.Edit, contentDescription = "Edit row", tint = TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * Frosted Glass Inline Row Editor Sheet with custom docked 3-column grid picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineRowEditorSheet(
    row: StagedTransactionRow,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (StagedTransactionRow) -> Unit
) {
    var type by remember { mutableStateOf(row.type) }
    var account by remember { mutableStateOf(row.account) }
    var category by remember { mutableStateOf(row.category) }
    var description by remember { mutableStateOf(row.description) }
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", row.amount)) }
    var tagText by remember { mutableStateOf(row.tag ?: "") }

    var dockedPicker by remember { mutableStateOf<String?>(null) } // "ACCOUNT" or "CATEGORY"

    val activeBorderColor by animateColorAsState(
        if (type.equals("INCOME", ignoreCase = true)) CoralAccent else ExpenseBlue,
        label = "activeColor"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF161A24)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Mode Selector: Income | Expense | Transfer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222838))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("EXPENSE", "INCOME", "TRANSFER").forEach { t ->
                    val isSelected = type.equals(t, ignoreCase = true)
                    val bg = if (isSelected) {
                        if (t == "INCOME") CoralAccent else ExpenseBlue
                    } else Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bg)
                            .clickable { type = t }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = t.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input Field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount ($currencySymbol)", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeBorderColor,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Note", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeBorderColor,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Account & Category Pickers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Account Button
                OutlinedButton(
                    onClick = { dockedPicker = if (dockedPicker == "ACCOUNT") null else "ACCOUNT" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (dockedPicker == "ACCOUNT") activeBorderColor.copy(alpha = 0.2f) else Color(0xFF1E2330)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Acc: $account", color = Color.White, fontSize = 12.sp, maxLines = 1)
                }

                // Category Button
                OutlinedButton(
                    onClick = { dockedPicker = if (dockedPicker == "CATEGORY") null else "CATEGORY" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (dockedPicker == "CATEGORY") activeBorderColor.copy(alpha = 0.2f) else Color(0xFF1E2330)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cat: $category", color = Color.White, fontSize = 12.sp, maxLines = 1)
                }
            }

            // Docked 3-Column Grid Picker for 1-Tap Switching
            AnimatedVisibility(visible = dockedPicker != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1F2C))
                ) {
                    if (dockedPicker == "ACCOUNT") {
                        val accNames = listOf("CASH", "DebitCard", "AMEX") + accounts.map { it.name }.distinct()
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(accNames.distinct()) { name ->
                                val isSelected = account.equals(name, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) activeBorderColor else Color(0xFF262C3D))
                                        .clickable {
                                            account = name
                                            dockedPicker = null
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (dockedPicker == "CATEGORY") {
                        val allCats = (categories.map { it.name } + listOf(
                            "Grocery", "Transportation", "Apparel", "Food", "Entertainment",
                            "Electronics", "Health", "House Rent &etc", "Visa", "Travel", "Salary", "Bonus"
                        )).distinct()

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(allCats) { catName ->
                                val isSelected = category.equals(catName, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) activeBorderColor else Color(0xFF262C3D))
                                        .clickable {
                                            category = catName
                                            dockedPicker = null
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(catName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save / Cancel Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = TextSecondary)
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: row.amount
                        onSave(
                            row.copy(
                                type = type,
                                account = account,
                                category = category,
                                description = description,
                                amount = amt,
                                tag = tagText.ifBlank { null },
                                validationError = if (amt <= 0) "Amount must be greater than 0" else null
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeBorderColor),
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BatchEditDialog(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onApply: (String?, String?) -> Unit
) {
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val accNames = listOf("CASH", "DebitCard", "AMEX") + accounts.map { it.name }.distinct()
    val catNames = (categories.map { it.name } + listOf(
        "Grocery", "Transportation", "Apparel", "Food", "Entertainment",
        "Electronics", "Health", "House Rent &etc", "Salary", "Bonus"
    )).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B202D),
        title = {
            Text("Batch Edit ($selectedCount Rows)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Change Account:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("AMEX", "DebitCard", "CASH").forEach { acc ->
                        val isSel = selectedAccount == acc
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) CoralAccent else Color(0xFF262C3C))
                                .clickable { selectedAccount = if (isSel) null else acc }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(acc, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Change Category:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(catNames.take(9)) { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) CoralAccent else Color(0xFF262C3C))
                                .clickable { selectedCategory = if (isSel) null else cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedAccount, selectedCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply to Selected", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
