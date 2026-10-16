package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.preferences.AppThemeMode

// ==========================================
// 1. Frosted Dark Color Scheme (Default)
// ==========================================
val FrostedDarkColorScheme = darkColorScheme(
    primary = CoralAccent,
    onPrimary = Color.White,
    primaryContainer = CoralContainer,
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = ExpenseBlue,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0F2E40),
    onSecondaryContainer = Color(0xFFBCE9FF),
    tertiary = EmeraldPrimary,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

// ==========================================
// 2. Frosted Light Color Scheme (High-Contrast Glass)
// ==========================================
val FrostedLightColorScheme = lightColorScheme(
    primary = CoralAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF3B1D22),
    secondary = ExpenseBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = EmeraldDark,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0x330F172A)
)

// ==========================================
// 2. AMOLED Pitch-Black Color Scheme (#000000)
// ==========================================
val AmoledColorScheme = darkColorScheme(
    primary = CoralAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF281216),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = ExpenseBlue,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0A1E29),
    onSecondaryContainer = Color(0xFFBCE9FF),
    tertiary = EmeraldPrimary,
    onTertiary = Color.White,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = AmoledSurface,
    onSurface = TextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

// ==========================================
// 3. Financial Emerald & Slate Schemes
// ==========================================
val EmeraldFinancialDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Slate950,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = ExpenseBlue,
    onSecondary = Slate950,
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = CoralAccent,
    onTertiary = Color.White,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700
)

val EmeraldFinancialLightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = CoralAccent,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate300
)

@Composable
fun SmartLedgerTheme(
    themeMode: AppThemeMode = AppThemeMode.FROSTED_DARK,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.FROSTED_DARK -> FrostedDarkColorScheme
        AppThemeMode.FROSTED_LIGHT -> FrostedLightColorScheme
        AppThemeMode.AMOLED_BLACK -> AmoledColorScheme
        AppThemeMode.EMERALD_FINANCIAL -> {
            if (isDarkTheme) EmeraldFinancialDarkColorScheme else EmeraldFinancialLightColorScheme
        }
        AppThemeMode.DYNAMIC_SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDarkTheme) FrostedDarkColorScheme else EmeraldFinancialLightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
