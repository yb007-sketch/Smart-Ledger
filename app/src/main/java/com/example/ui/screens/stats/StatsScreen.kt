package com.example.ui.screens.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TransactionType
import com.example.ui.components.InteractivePieChart
import com.example.ui.components.PieSliceData
import com.example.ui.components.YearlyCalendarHeatmapCard
import com.example.ui.components.frostedGlass
import com.example.ui.theme.CategoryColors
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LedgerViewModel
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val period by viewModel.currentPeriod.collectAsState()
    val transactions by viewModel.currentMonthTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currencySymbol = userSettings.mainCurrencySymbol
    val haptic = LocalHapticFeedback.current

    var interval by remember { mutableStateOf("Monthly") } // Weekly, Monthly, Annually
    var isIntervalMenuOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(1) } // 0: Income, 1: Expenses
    var selectedSliceId by remember { mutableStateOf<Long?>(null) }

    val activeTxs = remember(interval, transactions, allTransactions) {
        if (interval == "Annually") allTransactions else transactions
    }

    val incomeTxs = activeTxs.filter { it.type == TransactionType.INCOME }
    val expenseTxs = activeTxs.filter { it.type == TransactionType.EXPENSE }

    val totalIncome = incomeTxs.sumOf { it.amount }
    val totalExpense = expenseTxs.sumOf { it.amount }

    val currentModeTxs = if (selectedTab == 0) incomeTxs else expenseTxs
    val currentTotal = if (selectedTab == 0) totalIncome else totalExpense

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val slices = remember(currentModeTxs, categoryMap, currentTotal, selectedTab) {
        val grouped = currentModeTxs.groupBy { it.categoryId ?: 0L }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }

        grouped.mapIndexed { index, (catId, sum) ->
            val cat = categoryMap[catId]
            val label = cat?.name ?: if (selectedTab == 0) "Salary" else "Other"
            val percentage = if (currentTotal > 0) (sum / currentTotal * 100f).toFloat() else 0f
            val color = try {
                Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#3B82F6"))
            } catch (e: Exception) {
                CategoryColors.getOrElse(index % CategoryColors.size) { CoralAccent }
            }
            PieSliceData(
                id = catId,
                label = label,
                value = sum,
                percentage = percentage,
                color = color
            )
        }
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val activeAccent = if (selectedTab == 0) CoralAccent else ExpenseBlue

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Frosted Top Pill Bar with 20.dp horizontal breathing room
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .frostedGlass(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = Color(0xFF151A24),
                    surfaceAlpha = 0.65f,
                    borderAlpha = 0.30f
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.prevMonth()
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = textColor)
                    }
                    Text(
                        text = if (interval == "Annually") period.year.toString() else period.formatDisplay(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.nextMonth()
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = textColor)
                    }
                }

                Box {
                    Row(
                        modifier = Modifier
                            .frostedGlass(
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = Color(0xFF1E2534),
                                surfaceAlpha = 0.65f,
                                borderAlpha = 0.30f
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isIntervalMenuOpen = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = interval,
                            color = textColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Interval", tint = textColor)
                    }

                    DropdownMenu(
                        expanded = isIntervalMenuOpen,
                        onDismissRequest = { isIntervalMenuOpen = false }
                    ) {
                        listOf("Weekly", "Monthly", "Annually").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    interval = item
                                    isIntervalMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Secondary Tabs: Income vs Expenses with 20.dp horizontal padding
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = activeAccent,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = activeAccent,
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
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 0
                        selectedSliceId = null
                    },
                    text = {
                        Text(
                            text = "Income  $currencySymbol ${String.format(Locale.US, "%,.2f", totalIncome)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) CoralAccent else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("stats_income_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 1
                        selectedSliceId = null
                    },
                    text = {
                        Text(
                            text = "Expenses  $currencySymbol ${String.format(Locale.US, "%,.2f", totalExpense)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) ExpenseBlue else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("stats_expense_tab")
                )
            }
        }

        val topCategories = remember(slices) { slices.map { it.label to it.value } }

        // Smooth scaleIn + fadeIn / scaleOut + fadeOut when switching between Income & Expense analytics
        AnimatedContent(
            targetState = selectedTab,
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
            label = "statsModeContent"
        ) { modeIndex ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 112.dp, top = 8.dp)
            ) {
                // 1. Glowing Collapsible Local AI Insights Card
                item {
                    LocalAiInsightsCard(
                        viewModel = viewModel,
                        topCategories = topCategories,
                        totalExpense = if (modeIndex == 1) currentTotal else totalExpense
                    )
                }

                // 2. Frosted Glass Donut Chart Container with 0->360 Animatable Sweep & Center Live Total
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .frostedGlass(
                                shape = RoundedCornerShape(24.dp),
                                backgroundColor = Color(0xFF151A24),
                                surfaceAlpha = 0.65f,
                                borderAlpha = 0.30f
                            )
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        InteractivePieChart(
                            slices = slices,
                            selectedSliceId = selectedSliceId,
                            onSliceSelected = { selectedSliceId = it },
                            centerText = "$currencySymbol ${String.format(Locale.US, "%,.2f", currentTotal)}",
                            centerSubtext = if (modeIndex == 0) "Total Income" else "Total Expenses"
                        )
                    }
                }

                // 3. Yearly Calendar Heatmap Visualization
                item {
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        YearlyCalendarHeatmapCard(
                            year = period.year,
                            transactions = allTransactions,
                            currencySymbol = currencySymbol
                        )
                    }
                }

                // 4. Section Header for Ranked Category Breakdown
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor
                        )
                        Text(
                            text = "${slices.size} categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // 5. Ranked Breakdown Rows with Haptic Feedback & Spring Scale Physics
                items(slices, key = { it.id }) { slice ->
                    val isSelected = slice.id == selectedSliceId
                    val rowScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.025f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "breakdownRowScale_${slice.id}"
                    )
                    val animatedProgress by animateFloatAsState(
                        targetValue = (slice.percentage / 100f).coerceIn(0.02f, 1f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "breakdownBar_${slice.id}"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .scale(rowScale)
                            .frostedGlass(
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = if (isSelected) Color(0xFF1F2738) else Color(0xFF151A24),
                                surfaceAlpha = 0.65f,
                                borderColor = if (isSelected) slice.color else Color.White,
                                borderAlpha = if (isSelected) 0.65f else 0.30f
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedSliceId = if (isSelected) null else slice.id
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(slice.color)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", slice.percentage),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = slice.label,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "$currencySymbol ${String.format(Locale.US, "%,.2f", slice.value)}",
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        // Animated Category Share Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = animatedProgress)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(slice.color)
                            )
                        }
                    }
                }
            }
        }
    }
}
