package com.example.ui.screens.transactions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.ai.SpendingInsight
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.YearlyCalendarHeatmapCard
import com.example.ui.components.frostedGlass
import com.example.ui.theme.CalendarDayText
import com.example.ui.theme.CalendarExpenseBlue
import com.example.ui.theme.CalendarSaturday
import com.example.ui.theme.CalendarSunday
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LedgerViewModel
import com.example.ui.viewmodel.MonthPeriod
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTransTab.collectAsState()
    val period by viewModel.currentPeriod.collectAsState()
    val transactions by viewModel.currentMonthTransactions.collectAsState()
    val totalIncome by viewModel.currentMonthIncome.collectAsState()
    val totalExpenses by viewModel.currentMonthExpenses.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val insights by viewModel.spendingInsights.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDay by viewModel.selectedCalendarDay.collectAsState()
    val last24hCount by viewModel.last24HoursCount.collectAsState()
    val sortMode by viewModel.transactionSortMode.collectAsState()
    val categoryFilterId by viewModel.categoryFilterId.collectAsState()
    val accountFilterId by viewModel.accountFilterId.collectAsState()

    var showChecklistMenu by remember { mutableStateOf(false) }
    var showRevertConfirmDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var isFabSpeedDialOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val tactile = com.example.ui.util.rememberTactileHaptic()

    val dailyListState = rememberLazyListState()
    // Smooth List Scroll Parallax & Alpha Fade calculation from LazyListState
    val scrollCollapseFraction by remember {
        derivedStateOf {
            if (currentTab != 0) {
                0f
            } else if (dailyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (dailyListState.firstVisibleItemScrollOffset / 220f).coerceIn(0f, 1f)
            }
        }
    }
    val animatedCollapse by animateFloatAsState(
        targetValue = scrollCollapseFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "headerParallaxCollapse"
    )

    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
                }
            }.getOrNull() ?: uri.lastPathSegment ?: "spreadsheet.csv"
            viewModel.openImportPreview(uri, fileName)
        }
    }

    val currencySymbol = userSettings.mainCurrencySymbol
    val netTotal = totalIncome - totalExpenses

    // Smooth animated financial figures when filtering by date or category
    val animatedIncome by animateFloatAsState(
        targetValue = totalIncome.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "animatedIncome"
    )
    val animatedExpenses by animateFloatAsState(
        targetValue = totalExpenses.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "animatedExpenses"
    )
    val animatedNetTotal by animateFloatAsState(
        targetValue = netTotal.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "animatedNetTotal"
    )

    val tabs = listOf("Daily", "Calendar", "Monthly", "Total", "Note")
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White

    Box(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================================
            // 1. UNIFIED FROSTED PILL HEADER (Collapsing Parallax & Alpha Fade)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = (10f * (1f - animatedCollapse * 0.45f)).dp
                    )
                    .graphicsLayer {
                        translationY = -animatedCollapse * 10f
                        alpha = 1f - (animatedCollapse * 0.12f)
                    }
                    .testTag("home_top_bar_container")
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .frostedGlass(
                            shape = RoundedCornerShape(22.dp),
                            backgroundColor = Color(0xFF151A24),
                            surfaceAlpha = 0.65f,
                            borderWidth = 1.dp,
                            borderColor = Color.White,
                            borderAlpha = 0.30f
                        )
                        .testTag("home_top_bar_frosted_surface")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.prevMonth()
                            },
                            modifier = Modifier.testTag("prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = textColor
                            )
                        }
                        Text(
                            text = period.formatDisplay(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.nextMonth()
                            },
                            modifier = Modifier.testTag("next_month")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = textColor
                            )
                        }
                    }

                    // Unified Action Pill Controls (Stealth, Search, Export, Filter)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleStealthMode()
                            },
                            modifier = Modifier.testTag("stealth_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (userSettings.stealthModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Privacy Stealth Mode",
                                tint = if (userSettings.stealthModeEnabled) CoralAccent else textColor
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleSearch()
                            },
                            modifier = Modifier.testTag("search_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchActive) CoralAccent else textColor
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.exportTransactionsToExcel(context)
                            },
                            modifier = Modifier.testTag("export_excel_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export to Excel",
                                tint = EmeraldGreen
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showFilterDialog = true
                            },
                            modifier = Modifier.testTag("filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter",
                                tint = if (categoryFilterId != null || accountFilterId != null) CoralAccent else textColor
                            )
                        }
                    }
                }
            }

            // Expandable Search Bar with 20.dp horizontal padding
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            "Search transactions, merchants, notes...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color.White)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoralAccent,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // =========================================================================
            // 2. 5 SUB-TABS SWITCHER (`Daily`, `Calendar`, `Monthly`, `Total`, `Note`)
            // =========================================================================
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                TabRow(
                    selectedTabIndex = currentTab,
                    containerColor = Color.Transparent,
                    contentColor = CoralAccent,
                    indicator = { tabPositions ->
                        if (currentTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                                color = CoralAccent,
                                height = 3.dp
                            )
                        }
                    },
                    divider = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = currentTab == index
                        Tab(
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setTransTab(index)
                            },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) textColor else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("tab_$title")
                        )
                    }
                }
            }

            // =========================================================================
            // 3. STICKY FROSTED 3-COLUMN SUMMARY CARD (Income | Expenses | Total)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .frostedGlass(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color(0xFF151A24),
                        surfaceAlpha = 0.65f,
                        borderAlpha = 0.30f
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Income",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", animatedIncome)}",
                        color = CoralAccent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Expenses",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", animatedExpenses)}",
                        color = ExpenseBlue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••" else "${if (animatedNetTotal < 0) "-" else ""}$currencySymbol ${String.format(Locale.US, "%,.2f", abs(animatedNetTotal))}",
                        color = textColor,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Collapsing Sort & Active Filter Pill Row (smoothly hides on downward scroll)
            AnimatedVisibility(
                visible = animatedCollapse < 0.65f,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .graphicsLayer { alpha = 1f - animatedCollapse },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort:",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    val sortModes = listOf(
                        com.example.ui.viewmodel.TransactionSortMode.RECENT to "Recent",
                        com.example.ui.viewmodel.TransactionSortMode.CATEGORY to "Category",
                        com.example.ui.viewmodel.TransactionSortMode.AMOUNT to "Amount"
                    )
                    sortModes.forEach { (mode, label) ->
                        val selected = sortMode == mode
                        Box(
                            modifier = Modifier
                                .frostedGlass(
                                    shape = RoundedCornerShape(14.dp),
                                    backgroundColor = if (selected) CoralAccent else Color(0xFF151A24),
                                    surfaceAlpha = if (selected) 0.90f else 0.65f,
                                    borderAlpha = if (selected) 0.50f else 0.25f
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setTransactionSortMode(mode)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 4. ANIMATED CONTENT SUB-TAB HOST (`scaleIn + fadeIn` / `scaleOut + fadeOut`)
            // =========================================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.96f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn()) togetherWith
                            (scaleOut(targetScale = 0.96f) + fadeOut())
                    },
                    label = "TransSubTabTransition"
                ) { tabIndex ->
                    when (tabIndex) {
                        0 -> DailyView(
                            listState = dailyListState,
                            transactions = transactions,
                            accounts = accounts,
                            categories = categories,
                            currencySymbol = currencySymbol,
                            stealthMode = userSettings.stealthModeEnabled,
                            insights = insights,
                            sortMode = sortMode,
                            onTransactionClick = { viewModel.openEntrySheet(transaction = it) },
                            onDeleteClick = { viewModel.deleteTransaction(it) },
                            onOpenAi = { viewModel.openAiAssistant() }
                        )
                        1 -> CalendarView(
                            period = period,
                            transactions = transactions,
                            accounts = accounts,
                            categories = categories,
                            currencySymbol = currencySymbol,
                            selectedDay = selectedDay,
                            onSelectDay = { viewModel.setSelectedCalendarDay(it) },
                            onTransactionClick = { viewModel.openEntrySheet(transaction = it) }
                        )
                        2 -> MonthlyView(
                            period = period,
                            transactions = transactions,
                            currencySymbol = currencySymbol
                        )
                        3 -> TotalView(
                            year = period.year,
                            transactions = transactions,
                            accounts = accounts,
                            totalIncome = totalIncome,
                            totalExpenses = totalExpenses,
                            currencySymbol = currencySymbol,
                            onExportClick = {
                                viewModel.exportTransactionsToExcel(context)
                            }
                        )
                        4 -> NoteView(
                            transactions = transactions,
                            currencySymbol = currencySymbol,
                            onTransactionClick = { viewModel.openEntrySheet(transaction = it) }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 5. STAGGERED SPRING SPEED-DIAL FLOATING ACTION BUTTONS
        // =========================================================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .zIndex(2f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Staggered Expandable Speed-Dial Mini-FABs (Scan Receipt & Detailed Entry)
            val scanScale by animateFloatAsState(
                targetValue = if (isFabSpeedDialOpen) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "speedDialScanScale"
            )
            val fullEntryScale by animateFloatAsState(
                targetValue = if (isFabSpeedDialOpen) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "speedDialFullScale"
            )

            if (scanScale > 0.05f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .scale(scanScale)
                        .frostedGlass(
                            shape = CircleShape,
                            backgroundColor = Color(0xFF151A24),
                            surfaceAlpha = 0.82f,
                            borderColor = EmeraldGreen,
                            borderAlpha = 0.50f
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isFabSpeedDialOpen = false
                            viewModel.openEntrySheet()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Scan Receipt",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Scan Receipt",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .scale(fullEntryScale)
                        .frostedGlass(
                            shape = CircleShape,
                            backgroundColor = Color(0xFF151A24),
                            surfaceAlpha = 0.82f,
                            borderColor = CoralAccent,
                            borderAlpha = 0.50f
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isFabSpeedDialOpen = false
                            viewModel.openEntrySheet()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Detailed Form",
                        tint = CoralAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Detailed Form",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Primary Docked Action Row (Checklist, AI Hub Pill, Rapid Add FAB)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (last24hCount > 0) {
                            Badge(
                                containerColor = CoralAccent,
                                contentColor = Color.White
                            ) {
                                Text("$last24hCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showChecklistMenu = true
                        },
                        containerColor = Color(0xFF181F2E),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, GlassBorder, CircleShape)
                            .testTag("secondary_checklist_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Checklist Actions",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Quick AI Hub Floating Frosted Pill
                Box(
                    modifier = Modifier
                        .frostedGlass(
                            shape = CircleShape,
                            backgroundColor = Color(0xFF151E2E),
                            surfaceAlpha = 0.78f,
                            borderColor = ExpenseBlue,
                            borderAlpha = 0.55f
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openAiAssistant()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("ai_fab_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Quick Entry",
                            tint = ExpenseBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Entry",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Primary Coral-Red FAB with Spring Scale & Speed-Dial Support
                val fabRotation by animateFloatAsState(
                    targetValue = if (isFabSpeedDialOpen) 45f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "primaryFabRotation"
                )
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.openRapidEntryDialog()
                    },
                    containerColor = CoralAccent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("rapid_entry_fab")
                        .testTag("main_add_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Rapid Add Transaction",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(fabRotation)
                    )
                }
            }
        }

        // Filter Dialog (Category & Account Filter)
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                containerColor = Color(0xFF151A24),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Filter Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        if (categoryFilterId != null || accountFilterId != null) {
                            TextButton(
                                onClick = {
                                    viewModel.setCategoryFilter(null)
                                    viewModel.setAccountFilter(null)
                                    showFilterDialog = false
                                }
                            ) {
                                Text("Clear All", color = CoralAccent)
                            }
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("By Account:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { acc ->
                                val selected = accountFilterId == acc.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) ExpenseBlue else Color(0xFF1E2534))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.setAccountFilter(if (selected) null else acc.id)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(acc.name, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("By Category:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.take(12).forEach { cat ->
                                val selected = categoryFilterId == cat.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) CoralAccent else Color(0xFF1E2534))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.setCategoryFilter(if (selected) null else cat.id)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(cat.name, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showFilterDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            )
        }

        // Secondary Checklist Actions Bottom Sheet
        if (showChecklistMenu) {
            ModalBottomSheet(
                onDismissRequest = { showChecklistMenu = false },
                containerColor = Color(0xFF121620),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        "Quick Actions & Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 1. Bulk Import from Excel / CSV
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(shape = RoundedCornerShape(16.dp))
                            .clickable {
                                showChecklistMenu = false
                                importFilePickerLauncher.launch(
                                    arrayOf(
                                        "text/comma-separated-values",
                                        "text/csv",
                                        "application/vnd.ms-excel",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "*/*"
                                    )
                                )
                            }
                            .testTag("action_bulk_import"),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = ExpenseBlue, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Import from Excel / CSV", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Preview, stage, and batch-edit .xlsx, .xls, or .csv files", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 2. Revert Last 24 Hours
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(shape = RoundedCornerShape(16.dp))
                            .clickable {
                                showChecklistMenu = false
                                showRevertConfirmDialog = true
                            }
                            .testTag("action_revert_24h"),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Revert Last 24 Hours", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (last24hCount > 0) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            color = CoralAccent.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "$last24hCount added",
                                                color = CoralAccent,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text("Rollback all transactions imported or added in the last 24h", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 3. Rapid Transaction Entry
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(shape = RoundedCornerShape(16.dp))
                            .clickable {
                                showChecklistMenu = false
                                viewModel.openRapidEntryDialog()
                            }
                            .testTag("action_rapid_entry"),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Rapid Transaction Entry", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Quick 3-field entry dialog (Date, Amount, Category)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 4. Full Detailed Entry
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(shape = RoundedCornerShape(16.dp))
                            .clickable {
                                showChecklistMenu = false
                                viewModel.openEntrySheet()
                            }
                            .testTag("action_full_entry"),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Full Detailed Entry", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Complete form with accounts, tags, and notes", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Revert Last 24 Hours Confirmation Dialog
        if (showRevertConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showRevertConfirmDialog = false },
                title = { Text("Revert Last 24 Hours", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (last24hCount > 0)
                            "Are you sure you want to revert $last24hCount transactions added in the last 24 hours? This will delete them from the database and automatically restore previous account balances."
                        else
                            "No transactions were added in the last 24 hours to revert.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    if (last24hCount > 0) {
                        Button(
                            onClick = {
                                showRevertConfirmDialog = false
                                viewModel.revertLast24Hours()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                        ) {
                            Text("Revert $last24hCount Items", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRevertConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = Color(0xFF151A24)
            )
        }
    }
}

// ---------------- 1. DAILY VIEW (Grouped Frosted Cards + Hairline Dividers) ----------------
@Composable
fun DailyView(
    listState: LazyListState,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    stealthMode: Boolean,
    insights: List<SpendingInsight>,
    sortMode: com.example.ui.viewmodel.TransactionSortMode,
    onTransactionClick: (TransactionEntity) -> Unit,
    onDeleteClick: (TransactionEntity) -> Unit,
    onOpenAi: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val textSecondary = if (isLight) Color(0xFF475569) else TextSecondary
    val haptic = LocalHapticFeedback.current

    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val dayFormat = remember { SimpleDateFormat("dd EEE MM.yyyy", Locale.getDefault()) }

    val grouped = remember(transactions, sortMode, categoryMap) {
        when (sortMode) {
            com.example.ui.viewmodel.TransactionSortMode.RECENT -> {
                transactions.groupBy {
                    dayFormat.format(Date(it.timestamp))
                }.toList().sortedByDescending { it.second.firstOrNull()?.timestamp ?: 0L }
            }
            com.example.ui.viewmodel.TransactionSortMode.CATEGORY -> {
                transactions.groupBy { tx ->
                    categoryMap[tx.categoryId]?.name ?: "Uncategorized"
                }.toList().sortedBy { it.first }
            }
            com.example.ui.viewmodel.TransactionSortMode.AMOUNT -> {
                transactions.groupBy { tx ->
                    when {
                        tx.amount >= 100.0 -> "Large Amount (≥ 100)"
                        tx.amount >= 20.0 -> "Medium Amount (20 - 99)"
                        else -> "Small Amount (< 20)"
                    }
                }.toList().sortedByDescending { pair ->
                    pair.second.maxOfOrNull { it.amount } ?: 0.0
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 112.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Insights Banner Card with 20.dp horizontal margin
        if (insights.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .frostedGlass(
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color(0xFF151A24),
                            surfaceAlpha = 0.65f,
                            borderColor = ExpenseBlue,
                            borderAlpha = 0.38f
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Insights",
                                    tint = ExpenseBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SmartLedger AI Insights",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Ask AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = ExpenseBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenAi()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        for (insight in insights.take(2)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = ExpenseBlue, fontWeight = FontWeight.Bold)
                                Text(
                                    text = insight.message,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        if (grouped.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No transactions found for this period",
                        color = TextSecondary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap + to log your first expense or income",
                        color = CoralAccent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Unified Frosted Glass Group Cards with Hairline Intra-Day Dividers
        items(grouped, key = { it.first }) { (groupHeader, txsInGroup) ->
            val groupIncome = txsInGroup.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val groupExpense = txsInGroup.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val sortedTxsInGroup = remember(txsInGroup, sortMode) {
                if (sortMode == com.example.ui.viewmodel.TransactionSortMode.AMOUNT) {
                    txsInGroup.sortedByDescending { it.amount }
                } else {
                    txsInGroup.sortedByDescending { it.timestamp }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .frostedGlass(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color(0xFF151A24),
                        surfaceAlpha = 0.65f,
                        borderAlpha = 0.30f
                    )
            ) {
                // Group Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (sortMode == com.example.ui.viewmodel.TransactionSortMode.RECENT) {
                        val parts = groupHeader.split(" ")
                        val dayNum = parts.getOrNull(0) ?: ""
                        val dayOfWeek = parts.getOrNull(1) ?: ""
                        val monthYear = parts.getOrNull(2) ?: ""

                        val isSunday = dayOfWeek.equals("Sun", ignoreCase = true)
                        val isSaturday = dayOfWeek.equals("Sat", ignoreCase = true)
                        val dayAccent = when {
                            isSunday -> CoralAccent
                            isSaturday -> ExpenseBlue
                            else -> Color(0xFFE2E8F0)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = dayAccent
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(dayAccent.copy(alpha = 0.16f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = dayOfWeek,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dayAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = monthYear,
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary
                            )
                        }
                    } else {
                        Text(
                            text = groupHeader,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (groupIncome > 0) {
                            Text(
                                text = if (stealthMode) "$currencySymbol ••••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", groupIncome)}",
                                color = CoralAccent,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (groupExpense > 0) {
                            Text(
                                text = if (stealthMode) "$currencySymbol ••••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", groupExpense)}",
                                color = ExpenseBlue,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Hairline Divider under Date Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.10f))
                )

                // Intra-Day Transactions Separated by Subtle Hairline Dividers
                sortedTxsInGroup.forEachIndexed { index, tx ->
                    val cat = categoryMap[tx.categoryId]
                    val acc = accountMap[tx.accountId]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTransactionClick(tx)
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Category Pill Badge
                        Box(
                            modifier = Modifier
                                .width(92.dp)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = cat?.name ?: "General",
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Center: Merchant & Account/Note Metadata
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = tx.merchantName.ifBlank { cat?.name ?: "Expense" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = buildString {
                                    append(acc?.name ?: "CASH")
                                    if (tx.notes.isNotBlank() && tx.notes != tx.merchantName) {
                                        append(" • ")
                                        append(tx.notes)
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right: Tabular Amount & Subtle Delete Action
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (stealthMode) "$currencySymbol ••••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", tx.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (tx.type) {
                                    TransactionType.INCOME -> CoralAccent
                                    TransactionType.EXPENSE -> ExpenseBlue
                                    TransactionType.TRANSFER -> textColor
                                }
                            )
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDeleteClick(tx)
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = TextSecondary.copy(alpha = 0.65f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    if (index < sortedTxsInGroup.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 2. CALENDAR VIEW ----------------
@Composable
fun CalendarView(
    period: MonthPeriod,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val cal = remember(period) {
        Calendar.getInstance().apply {
            set(period.year, period.month, 1)
        }
    }

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1

    val dayTotals = remember(transactions) {
        val map = mutableMapOf<Int, Double>()
        for (tx in transactions) {
            if (tx.type == TransactionType.EXPENSE) {
                val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                val d = c.get(Calendar.DAY_OF_MONTH)
                map[d] = (map[d] ?: 0.0) + tx.amount
            }
        }
        map
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        // Frosted Glass Calendar Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0xFF151A24),
                    surfaceAlpha = 0.65f,
                    borderAlpha = 0.30f
                )
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    val color = when (index) {
                        0 -> CalendarSunday
                        6 -> CalendarSaturday
                        else -> CalendarDayText
                    }
                    Text(
                        text = day,
                        color = color,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val cellDays = remember(firstDayOfWeek, maxDays) {
                val list = mutableListOf<Int?>()
                for (i in 0 until firstDayOfWeek) list.add(null)
                for (i in 1..maxDays) list.add(i)
                while (list.size % 7 != 0 || list.size < 35) list.add(null)
                list
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cellDays) { day ->
                    val isSelected = day != null && day == selectedDay
                    val expenseForDay = if (day != null) dayTotals[day] else null

                    Box(
                        modifier = Modifier
                            .aspectRatio(0.92f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable(enabled = day != null) {
                                if (day != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectDay(day)
                                }
                            }
                            .padding(2.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (day != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) CoralAccent else Color.Transparent)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = if (isSelected) Color.White else TextPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }

                                if (expenseForDay != null && expenseForDay > 0) {
                                    Text(
                                        text = String.format(Locale.US, "%.0f", expenseForDay),
                                        color = CalendarExpenseBlue,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val txsForSelectedDay = remember(transactions, selectedDay) {
            transactions.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                c.get(Calendar.DAY_OF_MONTH) == selectedDay
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .frostedGlass(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0xFF151A24),
                    surfaceAlpha = 0.65f,
                    borderAlpha = 0.30f
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Day $selectedDay Overview (${txsForSelectedDay.size} items)",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(txsForSelectedDay) { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTransactionClick(tx)
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tx.merchantName,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                tx.notes.ifBlank { "Expense" },
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = "$currencySymbol ${String.format(Locale.US, "%,.2f", tx.amount)}",
                            color = if (tx.type == TransactionType.INCOME) CoralAccent else ExpenseBlue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                }
            }
        }
    }
}

// ---------------- 3. MONTHLY VIEW ----------------
@Composable
fun MonthlyView(
    period: MonthPeriod,
    transactions: List<TransactionEntity>,
    currencySymbol: String
) {
    val weeks = listOf(
        "25.10 ~ 31.10" to Pair(0.00, 0.00),
        "18.10 ~ 24.10" to Pair(0.00, 0.00),
        "11.10 ~ 17.10" to Pair(0.00, 0.00),
        "04.10 ~ 10.10" to Pair(0.00, 271.85),
        "27.09 ~ 03.10" to Pair(0.00, 1031.19)
    )

    val pastMonths = listOf(
        "Sept" to Pair(4201.00, 2608.13),
        "Aug" to Pair(4524.00, 2754.06),
        "Jul" to Pair(4401.00, 3122.26),
        "Jun" to Pair(4479.55, 6688.58),
        "May" to Pair(4221.00, 2211.92),
        "Apr" to Pair(4042.00, 2305.10)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlass(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color(0xFF151A24),
                        surfaceAlpha = 0.65f,
                        borderAlpha = 0.30f
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("October Breakdown", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("01.10 ~ 31.10", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("$currencySymbol 0.00", color = CoralAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("$currencySymbol 587.59", color = ExpenseBlue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }

                weeks.forEachIndexed { index, (weekName, amounts) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(weekName, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("$currencySymbol ${String.format(Locale.US, "%,.2f", amounts.first)}", color = CoralAccent, style = MaterialTheme.typography.bodyMedium)
                            Text("$currencySymbol ${String.format(Locale.US, "%,.2f", amounts.second)}", color = ExpenseBlue, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (index < weeks.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.08f)))
                    }
                }
            }
        }

        item {
            Text(
                text = "Previous Months",
                color = TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlass(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color(0xFF151A24),
                        surfaceAlpha = 0.65f,
                        borderAlpha = 0.30f
                    )
            ) {
                pastMonths.forEachIndexed { index, (mName, amounts) ->
                    val net = amounts.first - amounts.second
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Column(horizontalAlignment = Alignment.End) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("$currencySymbol ${String.format(Locale.US, "%,.2f", amounts.first)}", color = CoralAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("$currencySymbol ${String.format(Locale.US, "%,.2f", amounts.second)}", color = ExpenseBlue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "Net: $currencySymbol ${String.format(Locale.US, "%,.2f", net)}",
                                color = if (net >= 0) EmeraldGreen else Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    if (index < pastMonths.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.08f)))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

// ---------------- 4. TOTAL VIEW ----------------
@Composable
fun TotalView(
    year: Int,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    totalIncome: Double,
    totalExpenses: Double,
    currencySymbol: String,
    onExportClick: () -> Unit
) {
    val totalBudget = 1200.00
    val budgetProgress = (totalExpenses / totalBudget).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlass(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color(0xFF151A24),
                        surfaceAlpha = 0.65f,
                        borderAlpha = 0.30f
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Monthly Budget Health", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Target: $currencySymbol 1,200", color = CoralAccent, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            budgetProgress > 0.9f -> CoralAccent
                            budgetProgress > 0.75f -> Color(0xFFF59E0B)
                            else -> EmeraldGreen
                        },
                        trackColor = Color.White.copy(alpha = 0.10f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spent: $currencySymbol ${String.format(Locale.US, "%,.2f", totalExpenses)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Remaining: $currencySymbol ${String.format(Locale.US, "%,.2f", totalBudget - totalExpenses)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            YearlyCalendarHeatmapCard(
                year = year,
                transactions = transactions,
                currencySymbol = currencySymbol,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlass(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color(0xFF151A24),
                        surfaceAlpha = 0.65f,
                        borderAlpha = 0.30f
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Accounts Summary", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("01.10.2026 ~ 31.10", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Compared Expenses (Last month)", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("23%", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Expenses (Cash, Accounts)", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("$currencySymbol 0.00", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Expenses (Card)", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("$currencySymbol ${String.format(Locale.US, "%,.2f", totalExpenses)}", color = ExpenseBlue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .frostedGlass(
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color(0xFF1B2232),
                        surfaceAlpha = 0.65f,
                        borderColor = EmeraldGreen,
                        borderAlpha = 0.40f
                    )
                    .clickable { onExportClick() }
                    .testTag("export_excel_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📊  Export Ledger to Excel (.csv)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

// ---------------- 5. NOTE VIEW (Receipt & Warranty Vault) ----------------
@Composable
fun NoteView(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var activeFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(transactions, activeFilter) {
        when (activeFilter) {
            "WARRANTIES" -> transactions.filter { it.receiptImageUri != null || it.warrantyExpiryMillis != null }
            "SUBSCRIPTIONS" -> transactions.filter { it.isSubscription || it.isRecurring || it.merchantName.contains("Amazon", ignoreCase = true) || it.merchantName.contains("Netflix", ignoreCase = true) }
            else -> transactions.filter { it.notes.isNotBlank() || it.receiptImageUri != null || it.warrantyExpiryMillis != null }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .frostedGlass(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = if (activeFilter == "ALL") CoralAccent else Color(0xFF151A24),
                            surfaceAlpha = if (activeFilter == "ALL") 0.90f else 0.65f
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activeFilter = "ALL"
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("note_filter_all")
                ) {
                    Text("All Notes", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .frostedGlass(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = if (activeFilter == "WARRANTIES") EmeraldGreen else Color(0xFF151A24),
                            surfaceAlpha = if (activeFilter == "WARRANTIES") 0.90f else 0.65f
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activeFilter = "WARRANTIES"
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("note_filter_warranties")
                ) {
                    Text("📷 Warranties", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .frostedGlass(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = if (activeFilter == "SUBSCRIPTIONS") ExpenseBlue else Color(0xFF151A24),
                            surfaceAlpha = if (activeFilter == "SUBSCRIPTIONS") 0.90f else 0.65f
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activeFilter = "SUBSCRIPTIONS"
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("note_filter_subscriptions")
                ) {
                    Text("⚡ Renewals", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No receipts, warranties, or notes found under this filter", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        items(filteredList) { tx ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlass(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = Color(0xFF151A24),
                        surfaceAlpha = 0.65f,
                        borderAlpha = 0.30f
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTransactionClick(tx)
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tx.merchantName.ifBlank { "Expense Note" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$currencySymbol ${String.format(Locale.US, "%,.2f", tx.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseBlue
                        )
                    }

                    if (tx.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tx.notes, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}
