package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PieSliceData(
    val id: Long,
    val label: String,
    val value: Double,
    val percentage: Float,
    val color: Color
)

/**
 * Interactive Donut Chart with:
 * - Hardware-accelerated entrance sweep animation from 0f to 360f using [Animatable].
 * - Expressive spring physics (`DampingRatioMediumBouncy`, `StiffnessLow`) when selecting slices.
 * - Tactile haptic feedback (`HapticFeedbackType.LongPress`) on slice tap.
 * - Live animated center total (`headlineMedium`) and subtitle (`labelSmall`) inside the Donut hole.
 * - Clean external leader lines with anti-collision label spacing.
 */
@Composable
fun InteractivePieChart(
    slices: List<PieSliceData>,
    selectedSliceId: Long?,
    onSliceSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    chartSize: Dp = 270.dp,
    centerText: String = "",
    centerSubtext: String = ""
) {
    val haptic = LocalHapticFeedback.current
    val total = slices.sumOf { it.value }.coerceAtLeast(0.01)

    // Entrance sweep progress from 0f -> 360f using Animatable
    val sweepAnimatable = remember { Animatable(0f) }
    LaunchedEffect(slices.size, total) {
        sweepAnimatable.snapTo(0f)
        sweepAnimatable.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    // Per-slice spring scale / pop-out animation
    val selectedPopProgress by animateFloatAsState(
        targetValue = if (selectedSliceId != null) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "donutSliceSpring"
    )

    val selectedSlice = remember(slices, selectedSliceId) {
        slices.find { it.id == selectedSliceId }
    }

    Box(
        modifier = modifier
            .size(chartSize)
            .testTag("interactive_pie_chart"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices, selectedSliceId) {
                    detectTapGestures { tapOffset ->
                        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                        val dx = tapOffset.x - canvasCenter.x
                        val dy = tapOffset.y - canvasCenter.y
                        val dist = sqrt(dx * dx + dy * dy)
                        val minDim = minOf(size.width, size.height).toFloat()
                        val outerRadius = minDim * 0.38f
                        val innerRadius = minDim * 0.14f

                        if (dist in innerRadius..outerRadius && slices.isNotEmpty()) {
                            var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            touchAngle = (touchAngle + 90f + 360f) % 360f

                            var currentStart = 0f
                            for (slice in slices) {
                                val sweep = (slice.value / total * 360f).toFloat()
                                if (touchAngle >= currentStart && touchAngle <= currentStart + sweep) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSliceSelected(if (selectedSliceId == slice.id) null else slice.id)
                                    break
                                }
                                currentStart += sweep
                            }
                        } else if (dist < innerRadius && selectedSliceId != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSliceSelected(null)
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val radius = size.minDimension * 0.255f
            val baseStrokeWidth = size.minDimension * 0.115f

            if (slices.isEmpty()) {
                drawCircle(
                    color = Color(0xFF23293A),
                    radius = radius,
                    center = center,
                    style = Stroke(width = baseStrokeWidth)
                )
                return@Canvas
            }

            // Ambient subtle ring glow behind the donut
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                radius = radius + baseStrokeWidth * 0.62f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            val maxSweepAllowed = sweepAnimatable.value
            var currentAngle = -90f
            var accumulatedSweep = 0f
            val gapDegrees = if (slices.size > 1) 2.2f else 0f

            var lastLeftY = -1000f
            var lastRightY = -1000f
            val minLabelDistance = 19.dp.toPx()

            slices.forEachIndexed { index, slice ->
                val rawSweep = (slice.value / total * 360f).toFloat()
                val remainingBudget = (maxSweepAllowed - accumulatedSweep).coerceAtLeast(0f)
                val visibleSweep = rawSweep.coerceAtMost(remainingBudget)

                if (visibleSweep > 0.2f) {
                    val isSelected = slice.id == selectedSliceId
                    val shiftDistance = if (isSelected) 7.dp.toPx() * selectedPopProgress else 0f
                    val activeStroke = if (isSelected) {
                        baseStrokeWidth * (1f + 0.16f * selectedPopProgress)
                    } else {
                        baseStrokeWidth
                    }

                    val midAngleDeg = currentAngle + rawSweep / 2f
                    val midAngleRad = Math.toRadians(midAngleDeg.toDouble())
                    val sliceCenter = Offset(
                        x = center.x + (cos(midAngleRad) * shiftDistance).toFloat(),
                        y = center.y + (sin(midAngleRad) * shiftDistance).toFloat()
                    )

                    val effectiveSweep = (visibleSweep - gapDegrees).coerceAtLeast(0.8f)

                    // Subtle outer glow on selected slice
                    if (isSelected) {
                        drawArc(
                            color = slice.color.copy(alpha = 0.28f),
                            startAngle = currentAngle + gapDegrees / 2f,
                            sweepAngle = effectiveSweep,
                            useCenter = false,
                            topLeft = Offset(sliceCenter.x - radius, sliceCenter.y - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = activeStroke + 8.dp.toPx(), cap = StrokeCap.Butt)
                        )
                    }

                    // Main Donut Arc
                    drawArc(
                        color = if (selectedSliceId == null || isSelected) slice.color else slice.color.copy(alpha = 0.45f),
                        startAngle = currentAngle + gapDegrees / 2f,
                        sweepAngle = effectiveSweep,
                        useCenter = false,
                        topLeft = Offset(sliceCenter.x - radius, sliceCenter.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = activeStroke, cap = StrokeCap.Butt)
                    )

                    // External leader lines for top 5 categories (>= 3.5%) once entrance sweep reaches them
                    if (slice.percentage >= 3.5f && index < 5 && maxSweepAllowed > accumulatedSweep + rawSweep * 0.5f) {
                        val lineStartDist = radius + activeStroke / 2f + 2.dp.toPx()
                        val lineMidDist = radius + activeStroke / 2f + 13.dp.toPx()

                        val startPt = Offset(
                            x = sliceCenter.x + (cos(midAngleRad) * lineStartDist).toFloat(),
                            y = sliceCenter.y + (sin(midAngleRad) * lineStartDist).toFloat()
                        )
                        var midPtY = sliceCenter.y + (sin(midAngleRad) * lineMidDist).toFloat()
                        val isRightSide = cos(midAngleRad) >= 0

                        if (isRightSide) {
                            if (kotlin.math.abs(midPtY - lastRightY) < minLabelDistance) {
                                midPtY = lastRightY + minLabelDistance
                            }
                            lastRightY = midPtY
                        } else {
                            if (kotlin.math.abs(midPtY - lastLeftY) < minLabelDistance) {
                                midPtY = lastLeftY + minLabelDistance
                            }
                            lastLeftY = midPtY
                        }

                        val midPt = Offset(
                            x = sliceCenter.x + (cos(midAngleRad) * lineMidDist).toFloat(),
                            y = midPtY.coerceIn(18.dp.toPx(), canvasHeight - 18.dp.toPx())
                        )
                        val horizontalLen = 14.dp.toPx()
                        val endPt = Offset(
                            x = if (isRightSide) midPt.x + horizontalLen else midPt.x - horizontalLen,
                            y = midPt.y
                        )

                        drawLine(
                            color = slice.color.copy(alpha = 0.85f),
                            start = startPt,
                            end = midPt,
                            strokeWidth = 1.25.dp.toPx()
                        )
                        drawLine(
                            color = slice.color.copy(alpha = 0.85f),
                            start = midPt,
                            end = endPt,
                            strokeWidth = 1.25.dp.toPx()
                        )
                        drawCircle(
                            color = slice.color,
                            radius = 2.5.dp.toPx(),
                            center = endPt
                        )

                        val labelText = "${slice.label.take(10)} ${String.format(Locale.US, "%.1f%%", slice.percentage)}"
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.WHITE
                            textSize = 10.dp.toPx()
                            typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.DEFAULT,
                                if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
                            )
                            textAlign = if (isRightSide) android.graphics.Paint.Align.LEFT else android.graphics.Paint.Align.RIGHT
                        }

                        val textOffsetX = if (isRightSide) endPt.x + 5.dp.toPx() else endPt.x - 5.dp.toPx()
                        val textOffsetY = endPt.y + 3.5.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(labelText, textOffsetX, textOffsetY, paint)
                    }
                }

                currentAngle += rawSweep
                accumulatedSweep += rawSweep
            }
        }

        // Center Donut Hole Live Total & Label
        if (centerText.isNotEmpty() || selectedSlice != null) {
            val activeTitle = selectedSlice?.let {
                val symbolPrefix = centerText.takeWhile { ch -> !ch.isDigit() && ch != '-' }.trim()
                "$symbolPrefix ${String.format(Locale.US, "%,.2f", it.value)}".trim()
            } ?: centerText

            val activeSubtitle = selectedSlice?.let {
                "${it.label} • ${String.format(Locale.US, "%.1f%%", it.percentage)}"
            } ?: centerSubtext

            AnimatedContent(
                targetState = Pair(activeTitle, activeSubtitle),
                transitionSpec = {
                    (scaleIn(initialScale = 0.92f) + fadeIn(tween(180)))
                        .togetherWith(scaleOut(targetScale = 0.92f) + fadeOut(tween(120)))
                },
                label = "donutCenterTotal"
            ) { (titleStr, subStr) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (subStr.isNotEmpty()) {
                        Text(
                            text = subStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = selectedSlice?.color ?: TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = titleStr,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
