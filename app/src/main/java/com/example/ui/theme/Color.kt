package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Financial Emerald & Slate Theme Tokens
// ==========================================
val EmeraldPrimary = Color(0xFF10B981)
val EmeraldGreen = EmeraldPrimary
val EmeraldDark = Color(0xFF059669)
val EmeraldLight = Color(0xFF34D399)
val EmeraldContainer = Color(0xFF064E3B)
val OnEmeraldContainer = Color(0xFFA7F3D0)

// Slate Palette
val Slate950 = Color(0xFF020617)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

// High-Contrast Money Manager Tokens
val CoralAccent = Color(0xFFFF5A5F) // Income Accent, Sundays & Primary FABs (#FF5A5F)
val CoralVariant = Color(0xFFF25F5C)
val CoralContainer = Color(0xFF3B1D22)
val ExpenseBlue = Color(0xFF4A90E2) // Expense Accent & Saturdays Vibrant Ocean Blue (#4A90E2)
val AmberWarning = Color(0xFFF59E0B)

// Frosted Glass Dark Theme Surfaces
val DarkBackground = Color(0xFF0B0E14) // Rich Deep Ambient Mesh Start (#0B0E14)
val DarkBackgroundEnd = Color(0xFF151A24) // Rich Deep Ambient Mesh End (#151A24)
val DarkSurface = Color(0xFF121620)
val DarkSurfaceVariant = Color(0xFF1B2130)
val DarkSurfaceGlass = Color(0xA6151A24) // alpha = 0.65f
val DarkCardGlass = Color(0xA6151A24)
val GlassBorder = Color(0x4DFFFFFF) // 1.dp white/30%-to-transparent specular border

// AMOLED Pitch-Black Mode (100% #000000)
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF050505)
val AmoledSurfaceVariant = Color(0xFF111111)

// Typography Semantic Colors
val TextPrimary = Color(0xFFFFFFFF) // Crisp White Net Totals & Primary Text (#FFFFFF)
val TextSecondary = Color(0xFF8E95A5) // Cool Gray Subtitles & Muted Labels (#8E95A5)
val TextTertiary = Color(0xFF64748B)

// Calendar Specifics
val CalendarSunday = Color(0xFFFF5A5F) // Sundays Vibrant Coral Red
val CalendarSaturday = Color(0xFF4A90E2) // Saturdays Vibrant Ocean Blue
val CalendarDayText = Color(0xFFCBD5E1)
val CalendarExpenseBlue = Color(0xFF4A90E2)
val CalendarIncomeGreen = Color(0xFFFF5A5F)

// Category Colors Palette
val CategoryColors = listOf(
    Color(0xFF10B981), // Food
    Color(0xFFEF4444), // Grocery
    Color(0xFFF59E0B), // Transportation
    Color(0xFFEC4899), // House Rent
    Color(0xFF8B5CF6), // Apparel
    Color(0xFF06B6D4), // Health
    Color(0xFF6366F1), // Education
    Color(0xFFEAB308), // Entertainment
    Color(0xFF14B8A6), // Travel
    Color(0xFFF97316), // Charity
    Color(0xFF38BDF8), // Electronics
    Color(0xFFA855F7), // Visa
    Color(0xFF64748B)  // Other
)
