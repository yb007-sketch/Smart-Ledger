package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralAccent

@Composable
fun KeypadCalculator(
    currentExpression: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttons = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf(".", "0", "⌫", "+"),
        listOf("C", "=", "Done")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (row in buttons) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (btn in row) {
                    val weight = if (btn == "Done") 2f else 1f
                    val isOperator = btn in listOf("÷", "×", "-", "+", "=")
                    val isAction = btn == "Done"

                    val bg = when {
                        isAction -> CoralAccent
                        isOperator -> MaterialTheme.colorScheme.surfaceVariant
                        btn == "C" || btn == "⌫" -> Color(0xFF333846)
                        else -> Color(0xFF222630)
                    }

                    val textColor = when {
                        isAction -> Color.White
                        isOperator -> CoralAccent
                        else -> Color.White
                    }

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .clickable {
                                handleKeyPress(btn, currentExpression, onValueChange, onDone)
                            }
                            .testTag("keypad_$btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = btn,
                            fontSize = if (btn == "Done") 16.sp else 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

private fun handleKeyPress(
    key: String,
    current: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    when (key) {
        "Done" -> onDone()
        "C" -> onValueChange("0")
        "⌫" -> {
            if (current.length > 1) {
                onValueChange(current.dropLast(1))
            } else {
                onValueChange("0")
            }
        }
        "=" -> {
            val evaluated = evaluateSimpleExpression(current)
            onValueChange(evaluated)
        }
        "+", "-", "×", "÷" -> {
            val last = current.lastOrNull()
            if (last != null && last in "+-×÷") {
                onValueChange(current.dropLast(1) + key)
            } else {
                onValueChange(current + key)
            }
        }
        "." -> {
            if (!current.takeLastWhile { it.isDigit() || it == '.' }.contains('.')) {
                onValueChange(current + ".")
            }
        }
        else -> { // Numbers 0-9
            if (current == "0") {
                onValueChange(key)
            } else {
                onValueChange(current + key)
            }
        }
    }
}

fun evaluateSimpleExpression(expression: String): String {
    return try {
        val sanitized = expression.replace("×", "*").replace("÷", "/")
        // Parse simple sequential operators
        val tokens = mutableListOf<String>()
        var numberBuffer = ""
        for (ch in sanitized) {
            if (ch in "+-*/") {
                if (numberBuffer.isNotEmpty()) {
                    tokens.add(numberBuffer)
                    numberBuffer = ""
                }
                tokens.add(ch.toString())
            } else {
                numberBuffer += ch
            }
        }
        if (numberBuffer.isNotEmpty()) tokens.add(numberBuffer)

        if (tokens.isEmpty()) return "0"
        var result = tokens[0].toDoubleOrNull() ?: 0.0
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val nextVal = tokens.getOrNull(i + 1)?.toDoubleOrNull() ?: 0.0
            result = when (op) {
                "+" -> result + nextVal
                "-" -> result - nextVal
                "*" -> result * nextVal
                "/" -> if (nextVal != 0.0) result / nextVal else result
                else -> result
            }
            i += 2
        }
        if (result % 1.0 == 0.0) result.toLong().toString() else String.format(java.util.Locale.US, "%.2f", result)
    } catch (e: Exception) {
        expression
    }
}
