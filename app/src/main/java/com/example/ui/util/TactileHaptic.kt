package com.example.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

class TactileHaptic(
    private val haptic: HapticFeedback,
    private val vibrator: Vibrator?
) {
    /**
     * Crisp light tactile tap for standard button clicks, FAB taps, chip selections, and keypad inputs.
     */
    fun click() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (e: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /**
     * Distinct heavy tactile pulse for swipe-to-delete actions, row removals, and destructive confirmations.
     */
    fun heavyClick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (e: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /**
     * Celebratory double-pulse tactile feedback for successful transaction saves, bulk import commits,
     * and backup/restore completions.
     */
    fun success() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 25, 40, 35), -1))
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (e: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberTactileHaptic(): TactileHaptic {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    return remember(haptic, context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        TactileHaptic(haptic, vibrator)
    }
}
