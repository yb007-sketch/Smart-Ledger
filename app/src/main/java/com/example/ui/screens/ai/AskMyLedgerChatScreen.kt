package com.example.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiFinancialAssistantRepository
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessageItem(
    val sender: String,
    val message: String,
    val time: String
)

@Composable
fun AskMyLedgerChatScreen(
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currencySymbol = userSettings.mainCurrencySymbol

    var chatInput by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessageItem(
                    sender = "LiteRT-LM",
                    message = "Hello! I am your 100% offline On-Device AI (Gemma 3 1B via LiteRT-LM). Your Room database aggregates are securely loaded into my prompt context. Ask me anything about your finances ('What was my biggest expense last week?', 'Can I afford a £200 purchase?').",
                    time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                )
            )
        )
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgCol = if (isLight) Color(0xFFF8FAFC) else Color(0xFF12141A)
    val cardBg = if (isLight) Color.White else Color(0xFF1B1E29)
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White

    val aiRepo = remember { AiFinancialAssistantRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgCol)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                        Text("Ask My Ledger (Offline AI)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    Text("Gemma 3 1B • Zero Cloud Transmission", fontSize = 11.sp, color = EmeraldGreen)
                }
            }
        }

        // Chat Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "User"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) ExpenseBlue else cardBg
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg.sender,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) Color.White.copy(alpha = 0.8f) else EmeraldGreen
                                )
                                Text(
                                    text = msg.time,
                                    fontSize = 9.sp,
                                    color = if (isUser) Color.White.copy(alpha = 0.7f) else TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.message,
                                fontSize = 13.sp,
                                color = if (isUser) Color.White else textColor,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf("Biggest expense last week?", "Can I afford £200?", "Weekly Summary")
            suggestions.forEach { sug ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, EmeraldGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable {
                            chatInput = sug
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(sug, fontSize = 11.sp, color = textColor)
                }
            }
        }

        // Bottom Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Ask about your finances...", color = TextSecondary) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        val userText = chatInput
                        val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                        messages = messages + ChatMessageItem("User", userText, timeStr)
                        chatInput = ""

                        val aiReply = aiRepo.askMyLedger(userText, transactions, accounts, currencySymbol)
                        messages = messages + ChatMessageItem("LiteRT-LM", aiReply, SimpleDateFormat("HH:mm", Locale.US).format(Date()))
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}
