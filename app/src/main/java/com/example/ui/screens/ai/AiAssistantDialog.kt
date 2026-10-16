package com.example.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiFinancialAssistantRepository
import com.example.data.ai.SpendingInsight
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AiAssistantDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onParseQuery: (String) -> Unit,
    insights: List<SpendingInsight>,
    activeModelName: String = "Gemma 3 1B Instruct",
    onOpenModelManager: (() -> Unit)? = null,
    coachedInsight: String = "",
    aiPersona: String = "Supportive Coach",
    onSelectPersona: (String) -> Unit = {},
    smartBudgets: List<AiFinancialAssistantRepository.SmartBudgetRecommendation> = emptyList(),
    anomalyAlerts: List<AiFinancialAssistantRepository.AnomalyAlert> = emptyList(),
    onOpenChat: (() -> Unit)? = null
) {
    if (!isOpen) return

    val tactile = com.example.ui.util.rememberTactileHaptic()
    var queryInput by remember { mutableStateOf("") }
    val personas = listOf("Supportive Coach", "Strict Budget Planner", "Sarcastic UK Banker")

    val samplePrompts = listOf(
        "Petrol 31.51 AMEX",
        "Spent 16.40 at Asda on DebitCard",
        "Received £ 2800 salary into DebitCard",
        "Dinner at Tesco cafe 14.07 with AMEX",
        "Amazon prime subscription 12.97 with AMEX"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161923),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "On-Device Local AI & Coach",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$activeModelName • 100% Offline GPU",
                        color = EmeraldGreen,
                        fontSize = 11.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenModelManager != null) {
                        IconButton(
                            onClick = onOpenModelManager,
                            modifier = Modifier.testTag("ai_open_model_manager_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Model Manager",
                                tint = EmeraldGreen
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Persona Selector Chips
                Text("Select Coach Persona:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    personas.forEach { p ->
                        val selected = p == aiPersona
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) EmeraldGreen else Color(0xFF222738))
                                .clickable {
                                    tactile.click()
                                    onSelectPersona(p)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p.split(" ").first(),
                                color = if (selected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Coached Insight Speech Bubble
                if (coachedInsight.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x2610B981))
                            .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("💬 AI Coach ($aiPersona):", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(coachedInsight, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }

                // Anomaly & Fraud Guard Banner
                if (anomalyAlerts.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33F59E0B))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = "Anomaly", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Text(
                                text = "AI Fraud Guard: Detected ${anomalyAlerts.size} unusual spending spikes.",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { queryInput = it },
                        placeholder = { Text("e.g. Paid 35 at Shell...", color = TextSecondary, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_input_text")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (queryInput.isNotBlank()) {
                                tactile.click()
                                onParseQuery(queryInput)
                                queryInput = ""
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                    }
                }

                if (onOpenChat != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            tactile.click()
                            onOpenChat()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseBlue),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open 'Ask My Ledger' AI Chat", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
