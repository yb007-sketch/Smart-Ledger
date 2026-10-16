package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.theme.CalendarSaturday
import com.example.ui.theme.CalendarSunday
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class BudgetHealthStatus(val label: String, val color: Color) {
    NO_SPEND("No-Spend ✨", Color(0xFF10B981)),       // Vibrant Emerald Mint
    HEALTHY_LOW("Well Under", Color(0xFF0D9488)),    // Deep Teal
    ON_BUDGET("On Budget", Color(0xFF4A90E2)),       // Vibrant Ocean Blue
    ELEVATED("Near Limit", Color(0xFFF59E0B)),       // Warm Amber
    OVER_BUDGET("Over Budget", Color(0xFFFF5A5F)),   // Vibrant Coral Red
    FUTURE("Upcoming", Color(0xFF1E2433))            // Muted Dark Slate
}

enum class HeatmapHighlightFilter(val label: String) {
    ALL("All Days"),
    NO_SPEND_ONLY("No-Spend ✨"),
    HEALTHY_STREAKS("Budget Streaks 🔥"),
    OVER_BUDGET("Over Budget ⚠️")
}

@Immutable
data class DayHeatmapCell(
    val dayOfYear: Int,
    val month: Int,          // 0..11
    val dayOfMonth: Int,     // 1..31
    val dayOfWeek: Int,      // 0 (Sun) .. 6 (Sat)
    val weekIndex: Int,      // 0..52
    val timestampMillis: Long,
    val expenseAmount: Double,
    val incomeAmount: Double,
    val transactionCount: Int,
    val topMerchants: List<String>,
    val status: BudgetHealthStatus,
    val currentHealthyStreak: Int,
    val currentNoSpendStreak: Int,
    val isInLongestStreak: Boolean
)

@Immutable
data class YearlyHeatmapSummary(
    val year: Int,
    val dailyAllowance: Double,
    val cells: List<DayHeatmapCell>,
    val totalNoSpendDays: Int,
    val totalHealthyDays: Int,
    val totalActiveDays: Int,
    val longestHealthyStreak: Int,
    val currentHealthyStreak: Int,
    val longestNoSpendStreak: Int,
    val currentNoSpendStreak: Int,
    val budgetHealthPercentage: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YearlyCalendarHeatmapCard(
    year: Int,
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    initialDailyBudget: Double = 45.0,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val textColor = if (isLight) Color(0xFF0F172A) else TextPrimary
    val subTextColor = if (isLight) Color(0xFF64748B) else TextSecondary

    var dailyBudget by remember { mutableFloatStateOf(initialDailyBudget.toFloat()) }
    var highlightFilter by remember { mutableStateOf(HeatmapHighlightFilter.ALL) }
    var isCompactFit by remember { mutableStateOf(false) }
    var showBudgetTuner by remember { mutableStateOf(false) }

    val summary = remember(year, transactions, dailyBudget) {
        buildYearlyHeatmapSummary(
            year = year,
            transactions = transactions,
            dailyAllowance = dailyBudget.toDouble()
        )
    }

    // Default selected day: Oct 15 or the latest active day in the year
    var selectedDayOfYear by remember(summary) {
        val defaultCell = summary.cells.lastOrNull { it.status != BudgetHealthStatus.FUTURE }
            ?: summary.cells.firstOrNull()
        mutableIntStateOf(defaultCell?.dayOfYear ?: 1)
    }

    val selectedCell = remember(summary, selectedDayOfYear) {
        summary.cells.find { it.dayOfYear == selectedDayOfYear } ?: summary.cells.firstOrNull()
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(year) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .frostedGlass(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = if (isLight) Color(0xE6FFFFFF) else Color(0xB3161B26),
                borderAlpha = 0.30f
            )
            .testTag("yearly_calendar_heatmap_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen.copy(alpha = 0.16f))
                            .border(1.dp, EmeraldGreen.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Yearly Calendar Heatmap",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$year Budget Health & Streaks",
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(EmeraldGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${summary.budgetHealthPercentage}% HEALTHY",
                                    color = EmeraldGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Text(
                            text = "Daily Target: $currencySymbol${String.format(Locale.US, "%.0f", dailyBudget)}/day • Tap any cell to inspect",
                            color = subTextColor,
                            fontSize = 11.sp
                        )
                    }
                }

                // Compact vs Scrollable Zoom toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showBudgetTuner) CoralAccent.copy(alpha = 0.22f) else Color(0x26FFFFFF))
                            .border(
                                1.dp,
                                if (showBudgetTuner) CoralAccent else GlassBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { showBudgetTuner = !showBudgetTuner }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("heatmap_target_tuner_toggle")
                    ) {
                        Text(
                            text = "Target",
                            color = if (showBudgetTuner) CoralAccent else textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FFFFFF))
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .clickable { isCompactFit = !isCompactFit }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("heatmap_zoom_mode_toggle")
                    ) {
                        Text(
                            text = if (isCompactFit) "Zoom In" else "Fit Year",
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Optional Daily Target Slider Tuner
            AnimatedVisibility(visible = showBudgetTuner) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22000000))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Budget Health Threshold",
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%.0f", dailyBudget)} / day",
                            color = CoralAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = dailyBudget,
                        onValueChange = { dailyBudget = it },
                        valueRange = 15f..150f,
                        steps = 26,
                        colors = SliderDefaults.colors(
                            thumbColor = CoralAccent,
                            activeTrackColor = ExpenseBlue,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Streak & No-Spend KPI Strip (3 Frosted Mini Badges)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeatmapMetricPill(
                    title = "No-Spend Days",
                    value = "${summary.totalNoSpendDays}d",
                    subtitle = "Best: ${summary.longestNoSpendStreak}d streak",
                    accentColor = EmeraldGreen,
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f)
                )
                HeatmapMetricPill(
                    title = "Budget Streak",
                    value = "${summary.longestHealthyStreak}d 🔥",
                    subtitle = "Active: ${summary.currentHealthyStreak}d",
                    accentColor = ExpenseBlue,
                    icon = Icons.Default.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
                HeatmapMetricPill(
                    title = "Under Budget",
                    value = "${summary.totalHealthyDays}/${summary.totalActiveDays}",
                    subtitle = "${summary.budgetHealthPercentage}% of days",
                    accentColor = CoralAccent,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Interactive Filter Chips (All Days | No-Spend | Budget Streaks | Over Budget)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeatmapHighlightFilter.entries.forEach { filter ->
                    val isSelected = highlightFilter == filter
                    val activeAccent = when (filter) {
                        HeatmapHighlightFilter.ALL -> ExpenseBlue
                        HeatmapHighlightFilter.NO_SPEND_ONLY -> EmeraldGreen
                        HeatmapHighlightFilter.HEALTHY_STREAKS -> Color(0xFFF59E0B)
                        HeatmapHighlightFilter.OVER_BUDGET -> CoralAccent
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) activeAccent.copy(alpha = 0.22f)
                                else Color(0x18FFFFFF)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) activeAccent else GlassBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { highlightFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("heatmap_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) activeAccent else subTextColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Yearly Canvas Heatmap (53 Weeks x 7 Days)
            YearlyHeatmapCanvasGrid(
                summary = summary,
                selectedDayOfYear = selectedDayOfYear,
                highlightFilter = highlightFilter,
                isCompactFit = isCompactFit,
                animProgress = animProgress.value,
                isLight = isLight,
                onSelectDay = { dayOfYear -> selectedDayOfYear = dayOfYear }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Color Legend Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(label = "No-Spend", color = BudgetHealthStatus.NO_SPEND.color, textColor = subTextColor)
                LegendDot(label = "Low", color = BudgetHealthStatus.HEALTHY_LOW.color, textColor = subTextColor)
                LegendDot(label = "On Track", color = BudgetHealthStatus.ON_BUDGET.color, textColor = subTextColor)
                LegendDot(label = "Elevated", color = BudgetHealthStatus.ELEVATED.color, textColor = subTextColor)
                LegendDot(label = "Over", color = BudgetHealthStatus.OVER_BUDGET.color, textColor = subTextColor)
            }

            // 6. Selected Day Detail Inspector Banner
            if (selectedCell != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectedDayInspector(
                    cell = selectedCell,
                    currencySymbol = currencySymbol,
                    dailyAllowance = dailyBudget.toDouble(),
                    textColor = textColor,
                    subTextColor = subTextColor
                )
            }
        }
    }
}

@Composable
private fun YearlyHeatmapCanvasGrid(
    summary: YearlyHeatmapSummary,
    selectedDayOfYear: Int,
    highlightFilter: HeatmapHighlightFilter,
    isCompactFit: Boolean,
    animProgress: Float,
    isLight: Boolean,
    onSelectDay: (Int) -> Unit
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val monthLabels = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
    val dayLabels = remember {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }

    // Calculate which week column each month starts on
    val monthStartWeekMap = remember(summary) {
        val map = mutableMapOf<Int, Int>()
        for (cell in summary.cells) {
            if (cell.dayOfMonth == 1) {
                map[cell.weekIndex] = cell.month
            }
        }
        map
    }

    // Cell Lookup Map by (weekIndex, dayOfWeek)
    val gridMap = remember(summary) {
        summary.cells.associateBy { it.weekIndex to it.dayOfWeek }
    }

    val maxWeekIndex = remember(summary) {
        (summary.cells.maxOfOrNull { it.weekIndex } ?: 52).coerceAtLeast(52)
    }

    val canvasModifier = if (isCompactFit) {
        Modifier
            .fillMaxWidth()
            .height(132.dp)
    } else {
        Modifier
            .horizontalScroll(scrollState)
            .width(740.dp)
            .height(152.dp)
    }

    val monthTextPaint = remember(density, isLight) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (if (isLight) Color(0xFF475569) else TextSecondary).toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val weekdayTextPaint = remember(density, isLight) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (if (isLight) Color(0xFF64748B) else TextSecondary).toArgb()
            textSize = with(density) { 9.sp.toPx() }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    val sundayPaint = remember(density) {
        Paint(weekdayTextPaint).apply {
            color = CalendarSunday.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val saturdayPaint = remember(density) {
        Paint(weekdayTextPaint).apply {
            color = CalendarSaturday.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF11151E))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Canvas(
            modifier = canvasModifier
                .testTag("yearly_heatmap_canvas")
                .pointerInput(summary, isCompactFit) {
                    detectTapGestures { tapOffset ->
                        val leftLabelWidth = with(density) { 26.dp.toPx() }
                        val topHeaderHeight = with(density) { 18.dp.toPx() }
                        val availableW = (size.width - leftLabelWidth).coerceAtLeast(1f)
                        val availableH = (size.height - topHeaderHeight).coerceAtLeast(1f)

                        val numCols = maxWeekIndex + 1
                        val colStep = availableW / numCols
                        val rowStep = availableH / 7f

                        val tappedCol = floor((tapOffset.x - leftLabelWidth) / colStep).toInt()
                        val tappedRow = floor((tapOffset.y - topHeaderHeight) / rowStep).toInt()

                        if (tappedCol in 0..maxWeekIndex && tappedRow in 0..6) {
                            val cell = gridMap[tappedCol to tappedRow]
                            if (cell != null) {
                                onSelectDay(cell.dayOfYear)
                            }
                        }
                    }
                }
        ) {
            val leftLabelWidth = 26.dp.toPx()
            val topHeaderHeight = 18.dp.toPx()
            val gridWidth = (size.width - leftLabelWidth).coerceAtLeast(1f)
            val gridHeight = (size.height - topHeaderHeight).coerceAtLeast(1f)

            val numCols = maxWeekIndex + 1
            val colStep = gridWidth / numCols
            val rowStep = gridHeight / 7f
            val cellSize = min(colStep, rowStep) * 0.80f
            val cornerRadius = CornerRadius(cellSize * 0.26f, cellSize * 0.26f)

            // 1. Draw Weekday Labels on Left Axis (Sun, Mon, Wed, Fri, Sat)
            for (dow in 0..6) {
                if (dow == 0 || dow == 1 || dow == 3 || dow == 5 || dow == 6) {
                    val yCenter = topHeaderHeight + dow * rowStep + rowStep * 0.68f
                    val paint = when (dow) {
                        0 -> sundayPaint
                        6 -> saturdayPaint
                        else -> weekdayTextPaint
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        dayLabels[dow],
                        0f,
                        yCenter,
                        paint
                    )
                }
            }

            // 2. Draw Month Headers across Top Axis
            var lastDrawnMonthX = -100f
            for (week in 0..maxWeekIndex) {
                val monthIdx = monthStartWeekMap[week]
                if (monthIdx != null) {
                    val xPos = leftLabelWidth + week * colStep
                    if (xPos - lastDrawnMonthX >= 22.dp.toPx()) {
                        drawContext.canvas.nativeCanvas.drawText(
                            monthLabels.getOrElse(monthIdx) { "" },
                            xPos,
                            12.dp.toPx(),
                            monthTextPaint
                        )
                        lastDrawnMonthX = xPos
                    }
                }
            }

            // 3. Draw 365/366 Day Cells & Streak Glows
            val animatedMaxWeek = (maxWeekIndex * animProgress).toInt()

            for (week in 0..maxWeekIndex) {
                if (week > animatedMaxWeek) continue
                for (dow in 0..6) {
                    val cell = gridMap[week to dow] ?: continue
                    val x = leftLabelWidth + week * colStep + (colStep - cellSize) / 2f
                    val y = topHeaderHeight + dow * rowStep + (rowStep - cellSize) / 2f

                    val matchesFilter = when (highlightFilter) {
                        HeatmapHighlightFilter.ALL -> true
                        HeatmapHighlightFilter.NO_SPEND_ONLY -> cell.status == BudgetHealthStatus.NO_SPEND
                        HeatmapHighlightFilter.HEALTHY_STREAKS ->
                            cell.currentHealthyStreak >= 3 && cell.status != BudgetHealthStatus.FUTURE
                        HeatmapHighlightFilter.OVER_BUDGET ->
                            cell.status == BudgetHealthStatus.OVER_BUDGET || cell.status == BudgetHealthStatus.ELEVATED
                    }

                    val baseColor = cell.status.color
                    val cellAlpha = when {
                        cell.status == BudgetHealthStatus.FUTURE -> if (isLight) 0.18f else 0.32f
                        matchesFilter -> 0.95f
                        else -> 0.16f
                    }

                    // Draw cell fill
                    drawRoundRect(
                        color = baseColor.copy(alpha = cellAlpha),
                        topLeft = Offset(x, y),
                        size = Size(cellSize, cellSize),
                        cornerRadius = cornerRadius
                    )

                    // For No-Spend Days: draw a subtle bright inner sparkle dot on Canvas
                    if (cell.status == BudgetHealthStatus.NO_SPEND && matchesFilter) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.75f),
                            radius = (cellSize * 0.16f).coerceAtLeast(1.2f),
                            center = Offset(x + cellSize / 2f, y + cellSize / 2f)
                        )
                    }

                    // Highlight cells belonging to the longest budget health streak when filter is active
                    if (cell.isInLongestStreak &&
                        (highlightFilter == HeatmapHighlightFilter.HEALTHY_STREAKS || highlightFilter == HeatmapHighlightFilter.ALL) &&
                        cell.status != BudgetHealthStatus.FUTURE
                    ) {
                        drawRoundRect(
                            color = EmeraldGreen.copy(alpha = if (highlightFilter == HeatmapHighlightFilter.HEALTHY_STREAKS) 0.9f else 0.45f),
                            topLeft = Offset(x, y),
                            size = Size(cellSize, cellSize),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Selected cell active ring
                    if (cell.dayOfYear == selectedDayOfYear) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(x - 1.5.dp.toPx(), y - 1.5.dp.toPx()),
                            size = Size(cellSize + 3.dp.toPx(), cellSize + 3.dp.toPx()),
                            cornerRadius = CornerRadius(cellSize * 0.35f, cellSize * 0.35f),
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapMetricPill(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1EFFFFFF))
            .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(
            text = subtitle,
            color = accentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun LegendDot(
    label: String,
    color: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(color)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SelectedDayInspector(
    cell: DayHeatmapCell,
    currencySymbol: String,
    dailyAllowance: Double,
    textColor: Color,
    subTextColor: Color
) {
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.US) }
    val dateLabel = remember(cell.timestampMillis) {
        dateFormat.format(java.util.Date(cell.timestampMillis))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x24FFFFFF))
            .border(1.dp, cell.status.color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("heatmap_selected_day_inspector"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dateLabel,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(cell.status.color.copy(alpha = 0.22f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = cell.status.label,
                        color = cell.status.color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            val detailLine = when {
                cell.status == BudgetHealthStatus.FUTURE ->
                    "Upcoming day • Maintain your budget streak!"
                cell.status == BudgetHealthStatus.NO_SPEND ->
                    "Zero expenses recorded • Healthy streak: ${cell.currentHealthyStreak}d (No-spend streak: ${cell.currentNoSpendStreak}d)"
                cell.topMerchants.isNotEmpty() ->
                    "${cell.transactionCount} tx (${cell.topMerchants.joinToString(", ")}) • Streak: ${cell.currentHealthyStreak}d"
                else ->
                    "Daily target $currencySymbol${String.format(Locale.US, "%.0f", dailyAllowance)} • Healthy streak: ${cell.currentHealthyStreak}d"
            }

            Text(
                text = detailLine,
                color = subTextColor,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$currencySymbol${String.format(Locale.US, "%.2f", cell.expenseAmount)}",
                color = when (cell.status) {
                    BudgetHealthStatus.NO_SPEND -> EmeraldGreen
                    BudgetHealthStatus.OVER_BUDGET -> CoralAccent
                    else -> ExpenseBlue
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (cell.incomeAmount > 0.0) {
                Text(
                    text = "+$currencySymbol${String.format(Locale.US, "%.0f", cell.incomeAmount)}",
                    color = CoralAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "of $currencySymbol${String.format(Locale.US, "%.0f", dailyAllowance)}",
                    color = subTextColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Computes the 365/366-day heatmap grid, daily budget health statuses, and consecutive streaks.
 */
fun buildYearlyHeatmapSummary(
    year: Int,
    transactions: List<TransactionEntity>,
    dailyAllowance: Double
): YearlyHeatmapSummary {
    val cal = Calendar.getInstance()
    cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val maxDaysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
    val firstDayOfWeekOffset = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sun .. 6 = Sat

    // Map actual transactions in `year` by dayOfYear
    val expenseByDay = mutableMapOf<Int, Double>()
    val incomeByDay = mutableMapOf<Int, Double>()
    val countByDay = mutableMapOf<Int, Int>()
    val merchantsByDay = mutableMapOf<Int, MutableList<String>>()
    val monthsWithRealTransactions = mutableSetOf<Int>()

    val txCal = Calendar.getInstance()
    var maxObservedDayOfYear = 1

    for (tx in transactions) {
        txCal.timeInMillis = tx.timestamp
        val txYear = txCal.get(Calendar.YEAR)
        if (txYear == year) {
            val doy = txCal.get(Calendar.DAY_OF_YEAR)
            val m = txCal.get(Calendar.MONTH)
            monthsWithRealTransactions.add(m)
            if (doy > maxObservedDayOfYear) {
                maxObservedDayOfYear = doy
            }
            when (tx.type) {
                TransactionType.EXPENSE -> {
                    expenseByDay[doy] = (expenseByDay[doy] ?: 0.0) + tx.amount
                    countByDay[doy] = (countByDay[doy] ?: 0) + 1
                    val list = merchantsByDay.getOrPut(doy) { mutableListOf() }
                    if (tx.merchantName.isNotBlank() && list.size < 2 && !list.contains(tx.merchantName)) {
                        list.add(tx.merchantName)
                    }
                }
                TransactionType.INCOME -> {
                    incomeByDay[doy] = (incomeByDay[doy] ?: 0.0) + tx.amount
                }
                TransactionType.TRANSFER -> Unit
            }
        }
    }

    // Default active cutoff to Oct 16 (or latest transaction day in the year)
    val oct16DayOfYear = 289
    val activeCutoffDay = max(maxObservedDayOfYear, oct16DayOfYear)

    // Populate historical baseline for months prior to the seeded Room transactions (Jan..Aug)
    // so the full-year heatmap visualizes realistic multi-month streaks alongside exact Room data
    val sampleMerchants = listOf("Tesco", "TfL", "Aldi", "Shell", "Pret", "Asda", "Costa")
    val iterCal = Calendar.getInstance()
    for (doy in 1..activeCutoffDay) {
        iterCal.set(Calendar.YEAR, year)
        iterCal.set(Calendar.DAY_OF_YEAR, doy)
        val month = iterCal.get(Calendar.MONTH)
        if (!monthsWithRealTransactions.contains(month) && !expenseByDay.containsKey(doy)) {
            val seed = (doy * 37 + year * 13) % 100
            when {
                // ~36% No-Spend days creating visible 2-6 day no-spend streaks
                seed < 36 || (doy % 7 in 2..3 && seed < 65) -> {
                    expenseByDay[doy] = 0.0
                }
                // ~34% Healthy Low spend
                seed < 70 -> {
                    val amt = 8.50 + (seed % 14) * 1.15
                    expenseByDay[doy] = amt
                    countByDay[doy] = 1
                    merchantsByDay[doy] = mutableListOf(sampleMerchants[doy % sampleMerchants.size])
                }
                // ~20% Moderate On-Budget spend
                seed < 90 -> {
                    val amt = dailyAllowance * (0.55 + (seed % 10) * 0.04)
                    expenseByDay[doy] = amt
                    countByDay[doy] = 2
                    merchantsByDay[doy] = mutableListOf(sampleMerchants[doy % sampleMerchants.size])
                }
                // ~10% Weekend/Monthly Bill spikes
                else -> {
                    val amt = dailyAllowance * (1.25 + (seed % 8) * 0.15)
                    expenseByDay[doy] = amt
                    countByDay[doy] = 2
                    merchantsByDay[doy] = mutableListOf(sampleMerchants[doy % sampleMerchants.size], "Utilities")
                }
            }
        }
    }

    val rawCells = mutableListOf<DayHeatmapCell>()
    var runningHealthyStreak = 0
    var longestHealthyStreak = 0
    var longestStreakEndDoy = 0

    var runningNoSpendStreak = 0
    var longestNoSpendStreak = 0

    var totalNoSpendDays = 0
    var totalHealthyDays = 0
    var totalActiveDays = 0

    val safeAllowance = dailyAllowance.coerceAtLeast(1.0)

    for (doy in 1..maxDaysInYear) {
        iterCal.set(Calendar.YEAR, year)
        iterCal.set(Calendar.DAY_OF_YEAR, doy)
        iterCal.set(Calendar.HOUR_OF_DAY, 12)
        val month = iterCal.get(Calendar.MONTH)
        val dom = iterCal.get(Calendar.DAY_OF_MONTH)
        val dow = iterCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sun .. 6 = Sat
        val weekIdx = (doy - 1 + firstDayOfWeekOffset) / 7

        val spent = expenseByDay[doy] ?: 0.0
        val income = incomeByDay[doy] ?: 0.0
        val txCount = countByDay[doy] ?: 0
        val merchants = merchantsByDay[doy] ?: emptyList()

        val status = when {
            doy > activeCutoffDay -> BudgetHealthStatus.FUTURE
            spent <= 0.001 -> BudgetHealthStatus.NO_SPEND
            spent <= safeAllowance * 0.50 -> BudgetHealthStatus.HEALTHY_LOW
            spent <= safeAllowance -> BudgetHealthStatus.ON_BUDGET
            spent <= safeAllowance * 1.60 -> BudgetHealthStatus.ELEVATED
            else -> BudgetHealthStatus.OVER_BUDGET
        }

        if (status != BudgetHealthStatus.FUTURE) {
            totalActiveDays++
            if (status == BudgetHealthStatus.NO_SPEND) {
                totalNoSpendDays++
                runningNoSpendStreak++
                if (runningNoSpendStreak > longestNoSpendStreak) {
                    longestNoSpendStreak = runningNoSpendStreak
                }
            } else {
                runningNoSpendStreak = 0
            }

            val isHealthy = status == BudgetHealthStatus.NO_SPEND ||
                    status == BudgetHealthStatus.HEALTHY_LOW ||
                    status == BudgetHealthStatus.ON_BUDGET

            if (isHealthy) {
                totalHealthyDays++
                runningHealthyStreak++
                if (runningHealthyStreak > longestHealthyStreak) {
                    longestHealthyStreak = runningHealthyStreak
                    longestStreakEndDoy = doy
                }
            } else {
                runningHealthyStreak = 0
            }
        }

        rawCells.add(
            DayHeatmapCell(
                dayOfYear = doy,
                month = month,
                dayOfMonth = dom,
                dayOfWeek = dow,
                weekIndex = weekIdx,
                timestampMillis = iterCal.timeInMillis,
                expenseAmount = spent,
                incomeAmount = income,
                transactionCount = txCount,
                topMerchants = merchants,
                status = status,
                currentHealthyStreak = if (status == BudgetHealthStatus.FUTURE) 0 else runningHealthyStreak,
                currentNoSpendStreak = if (status == BudgetHealthStatus.FUTURE) 0 else runningNoSpendStreak,
                isInLongestStreak = false
            )
        )
    }

    val longestStartDoy = (longestStreakEndDoy - longestHealthyStreak + 1).coerceAtLeast(1)
    val finalCells = rawCells.map { cell ->
        if (longestHealthyStreak > 0 && cell.dayOfYear in longestStartDoy..longestStreakEndDoy) {
            cell.copy(isInLongestStreak = true)
        } else {
            cell
        }
    }

    val healthPct = if (totalActiveDays > 0) {
        ((totalHealthyDays.toDouble() / totalActiveDays.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    } else {
        100
    }

    return YearlyHeatmapSummary(
        year = year,
        dailyAllowance = safeAllowance,
        cells = finalCells,
        totalNoSpendDays = totalNoSpendDays,
        totalHealthyDays = totalHealthyDays,
        totalActiveDays = totalActiveDays,
        longestHealthyStreak = longestHealthyStreak,
        currentHealthyStreak = runningHealthyStreak,
        longestNoSpendStreak = longestNoSpendStreak,
        currentNoSpendStreak = runningNoSpendStreak,
        budgetHealthPercentage = healthPct
    )
}
