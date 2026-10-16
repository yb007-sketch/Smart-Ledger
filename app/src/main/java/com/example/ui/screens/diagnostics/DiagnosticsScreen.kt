package com.example.ui.screens.diagnostics

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DiagnosticsManager
import com.example.util.PiiRedactingFileTree
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DiagnosticsScreen(
    fileTree: PiiRedactingFileTree,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var logLines by remember { mutableStateOf(fileTree.readLogLines()) }
    var searchQuery by remember { mutableStateOf("") }
    var exitReasons by remember { mutableStateOf(emptyList<com.example.util.ExitReasonInfo>()) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                exitReasons = DiagnosticsManager.getHistoricalExitReasons(context)
            }
        }
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgCol = if (isLight) Color(0xFFF8FAFC) else Color(0xFF12141A)
    val cardBg = if (isLight) Color.White else Color(0xFF1B1E29)
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgCol)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Text("Diagnostics & Telemetry Hub", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
            IconButton(onClick = { logLines = fileTree.readLogLines() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = textColor)
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = cardBg,
            contentColor = textColor
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Live Log Tail") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Exit & Health") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Export & Actions") })
        }

        when (selectedTab) {
            0 -> {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Logs", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val filtered = logLines.filter { it.contains(searchQuery, ignoreCase = true) }.reversed()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { line ->
                            val color = when {
                                line.contains("ERROR") -> CoralAccent
                                line.contains("WARN") -> Color(0xFFF59E0B)
                                else -> textColor
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = line,
                                    color = color,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Runtime Health & Memory", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Available Processors: ${Runtime.getRuntime().availableProcessors()}", color = TextSecondary, fontSize = 13.sp)
                                Text("Max Memory: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB", color = TextSecondary, fontSize = 13.sp)
                                Text("Total Memory: ${Runtime.getRuntime().totalMemory() / (1024 * 1024)} MB", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        Text("Historical Process Exits (ANR / OOM / Crashes)", fontWeight = FontWeight.Bold, color = textColor, fontSize = 15.sp)
                    }
                    if (exitReasons.isEmpty()) {
                        item {
                            Text("No historical exit reasons recorded or API < 30.", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        items(exitReasons) { exit ->
                            Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(exit.reason, fontWeight = FontWeight.Bold, color = CoralAccent, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Description: ${exit.description}", color = textColor, fontSize = 12.sp)
                                    Text("RSS Memory: ${exit.rss / 1024} MB | PSS Memory: ${exit.pss / 1024} MB", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = cardBg), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Diagnostic Bundle (.zip)", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Compresses PII-redacted ring-buffer JSONL logs and ANR/OOM exit reasons into a single diagnostic bundle.", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val fileTree = PiiRedactingFileTree(context)
                                    val zip = DiagnosticsManager.exportDiagnosticBundle(context, fileTree)
                                    if (zip != null) {
                                        exportStatus = "Diagnostic bundle saved: ${zip.absolutePath}"
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Diagnostic Bundle"))
                                    } else {
                                        exportStatus = "Failed to create diagnostic bundle."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Export & Share Diagnostics Zip", color = Color.White)
                            }
                        }
                    }

                    if (exportStatus != null) {
                        Text(exportStatus!!, color = ExpenseBlue, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
