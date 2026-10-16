package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.ExpenseBlue

/**
 * Pure Frosted Glassmorphism (`Modifier.frostedGlass()`):
 * - Translucent surface fill (`alpha = 0.65f`)
 * - Soft, hairline specular border (`1.dp` gradient from white/30% to transparent)
 * - Subtle diagonal specular sheen reflection
 * - 100% sharp child text, icons, and badges
 */
fun Modifier.frostedGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0xFF151A24),
    surfaceAlpha: Float = 0.65f,
    blurRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White,
    borderAlpha: Float = 0.30f,
    includeSpecularSheen: Boolean = true
): Modifier = this
    .clip(shape)
    .background(
        color = backgroundColor.copy(alpha = surfaceAlpha),
        shape = shape
    )
    .drawWithContent {
        if (includeSpecularSheen) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.08f),
                        borderColor.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height * 0.55f)
                )
            )
        }
        drawContent()
    }
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                borderColor.copy(alpha = borderAlpha),
                borderColor.copy(alpha = borderAlpha * 0.35f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset.Infinite
        ),
        shape = shape
    )

/**
 * Rich, deep ambient mesh gradient (`#0B0E14` to `#151A24`) with subtle atmospheric light orbs
 * providing true depth underneath all frosted glass surfaces.
 */
@Composable
fun AmbientMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    val isLight = bg.luminance() > 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = if (isLight) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC),
                            Color(0xFFEEF2F6),
                            Color(0xFFE2E8F0)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0E14), // Deep Obsidian Mesh Start (#0B0E14)
                            Color(0xFF10141D), // Mid Atmospheric Slate
                            Color(0xFF151A24)  // Deep Slate Mesh End (#151A24)
                        )
                    )
                }
            )
            .drawBehind {
                // Top-left Coral ambient orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CoralAccent.copy(alpha = if (isLight) 0.06f else 0.11f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.10f),
                        radius = size.width * 0.72f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.10f),
                    radius = size.width * 0.72f
                )
                // Center-right Ocean Blue ambient orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ExpenseBlue.copy(alpha = if (isLight) 0.06f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.42f),
                        radius = size.width * 0.78f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.42f),
                    radius = size.width * 0.78f
                )
                // Bottom-left Emerald subtle glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = if (isLight) 0.04f else 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.88f),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(size.width * 0.25f, size.height * 0.88f),
                    radius = size.width * 0.65f
                )
            },
        content = content
    )
}
