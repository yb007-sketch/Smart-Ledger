package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pure Frosted Glassmorphism (`Modifier.frostedGlass()`):
 * - Translucent surface fill (`alpha = 0.65f`)
 * - Soft, hairline specular border (`1.dp` gradient from white/30% to transparent)
 * - Subtle diagonal specular sheen reflection
 * - 100% crisp child text and iconography
 */
fun Modifier.frostedGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color? = null,
    surfaceAlpha: Float = 0.65f,
    blurRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White,
    borderAlpha: Float = 0.30f,
    includeSpecularSheen: Boolean = true
): Modifier = this
    .clip(shape)
    .background(
        color = (backgroundColor ?: Color(0xFF151A24)).copy(alpha = surfaceAlpha),
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
