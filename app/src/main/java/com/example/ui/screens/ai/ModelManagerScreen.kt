package com.example.ui.screens.ai

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.HardwareInfo
import com.example.data.ai.LocalInferenceResult
import com.example.data.ai.LocalLlmModel
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.frostedGlass
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Model Manager Compose Screen.
 * Allows users to download, inspect, switch, and delete local LLM model files
 * stored in the application's internal filesDir (`context.filesDir/models/`).
 * Features interactive progress indicators for model downloads and a live GPU test playground.
 */
@Composable
fun ModelManagerScreen(
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val models by viewModel.localModels.collectAsState()
    val hardwareInfo by viewModel.localHardwareInfo.collectAsState()
    val scope = rememberCoroutineScope()

    var testInput by remember { mutableStateOf("Petrol 31.51 AMEX") }
    var testResult by remember { mutableStateOf<LocalInferenceResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf<LocalLlmModel?>(null) }

    val modelsDirectoryPath = remember {
        viewModel.localLlmManager.getModelsDirectoryPath()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("model_manager_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Model Manager",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "On-Device SLM • Internal Storage (filesDir)",
                    fontSize = 11.sp,
                    color = EmeraldGreen
                )
            }
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x3310B981))
                    .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "100% OFFLINE",
                    color = EmeraldGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Internal filesDir Storage Banner
            item {
                InternalStorageCard(
                    directoryPath = modelsDirectoryPath,
                    hardwareInfo = hardwareInfo,
                    downloadedCount = models.count { it.isDownloaded }
                )
            }

            // Interactive Playground Card
            item {
                ModelPlaygroundCard(
                    testInput = testInput,
                    onInputChange = { testInput = it },
                    isTesting = isTesting,
                    testResult = testResult,
                    onRunTest = {
                        scope.launch {
                            isTesting = true
                            testResult = viewModel.localLlmManager.parseNaturalLanguageExpense(
                                testInput,
                                viewModel.categories.value,
                                viewModel.accounts.value
                            )
                            isTesting = false
                        }
                    },
                    onPrefillTransaction = {
                        testResult?.let {
                            viewModel.openEntrySheet(draft = it.draft)
                        }
                    }
                )
            }

            // Models Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available On-Device Models (4-bit INT4)",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Stored in filesDir/models/",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            // List of Models
            items(models, key = { it.id }) { model ->
                ModelDetailCard(
                    model = model,
                    directoryPath = modelsDirectoryPath,
                    onDownload = { viewModel.downloadLocalModel(model.id) },
                    onDeleteRequest = { modelToDelete = model },
                    onSetActive = { viewModel.setActiveLocalModel(model.id) }
                )
            }
        }
    }

    // Confirm Delete Dialog
    modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            containerColor = Color(0xFF1E222D),
            title = {
                Text(
                    text = "Delete Model Weights?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "This will remove '${model.filename}' from internal filesDir storage (${model.quantizedSize}). You can re-download it anytime.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLocalModel(model.id)
                        modelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                ) {
                    Text("Delete File", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun InternalStorageCard(
    directoryPath: String,
    hardwareInfo: HardwareInfo,
    downloadedCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(
                shape = RoundedCornerShape(14.dp),
                backgroundColor = Color(0xCC181C26),
                borderAlpha = 0.35f
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Internal filesDir",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "App Internal Storage (filesDir)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x3310B981))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$downloadedCount Active File(s)",
                        color = EmeraldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // File path indicator
            Text(
                text = "Target: $directoryPath",
                color = TextSecondary.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("GPU Acceleration", color = TextSecondary, fontSize = 11.sp)
                    Text(hardwareInfo.gpuBackend, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Available RAM", color = TextSecondary, fontSize = 11.sp)
                    Text("${hardwareInfo.ramAvailableMb} MB", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Free Internal Disk", color = TextSecondary, fontSize = 11.sp)
                    Text("${(hardwareInfo.freeStorageMb / 1024.0).format(1)} GB", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ModelDetailCard(
    model: LocalLlmModel,
    directoryPath: String,
    onDownload: () -> Unit,
    onDeleteRequest: () -> Unit,
    onSetActive: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = model.downloadProgress,
        label = "DownloadProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (model.isActive) Color(0xCC1F2A38) else Color(0xCC161A22),
                borderAlpha = if (model.isActive) 0.5f else 0.2f
            )
            .testTag("model_card_${model.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Active Indicator, Model Name, Mode Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (model.isDownloaded) {
                        IconButton(
                            onClick = onSetActive,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("toggle_active_${model.id}")
                        ) {
                            Icon(
                                imageVector = if (model.isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Active Toggle",
                                tint = if (model.isActive) EmeraldGreen else TextSecondary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = model.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (model.isActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(EmeraldGreen.copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", color = EmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = "${model.architecture} • ${model.quantizedSize} • Min RAM: ${model.minRam}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF232838))
                        .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = model.modeBadge,
                        color = ExpenseBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = model.bestUseCase,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // File path and status
            Text(
                text = "File: $directoryPath/${model.filename}",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            // Dynamic Download Progress Indicator Section
            AnimatedVisibility(visible = model.isDownloading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(16.dp),
                                color = EmeraldGreen,
                                strokeWidth = 2.dp,
                                trackColor = EmeraldGreen.copy(alpha = 0.2f)
                            )
                            Text(
                                text = "Downloading INT4 weights... ${(animatedProgress * 100).toInt()}%",
                                color = EmeraldGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = model.downloadSpeed.ifBlank { "Streaming" },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .testTag("download_progress_${model.id}"),
                        color = EmeraldGreen,
                        trackColor = Color(0x3310B981)
                    )

                    val downloadedMb = (model.sizeBytes * animatedProgress / (1024 * 1024)).toLong()
                    val totalMb = (model.sizeBytes / (1024 * 1024)).toLong()
                    Text(
                        text = "$downloadedMb MB / $totalMb MB written to filesDir/models/",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "License: ${model.license}",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (model.isDownloaded) {
                        Button(
                            onClick = onDeleteRequest,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF5252)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("delete_model_${model.id}"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete model file",
                                tint = CoralAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete File", fontSize = 11.sp, color = CoralAccent)
                        }

                        if (!model.isActive) {
                            Button(
                                onClick = onSetActive,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("activate_model_${model.id}"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Set Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    } else if (!model.isDownloading) {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("download_model_${model.id}"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download (${model.quantizedSize})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelPlaygroundCard(
    testInput: String,
    onInputChange: (String) -> Unit,
    isTesting: Boolean,
    testResult: LocalInferenceResult?,
    onRunTest: () -> Unit,
    onPrefillTransaction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xCC1A1F2C),
                borderAlpha = 0.35f
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(18.dp))
                    Text("Live Local LLM Playground", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x33FF5252))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("NATIVE @Tool", color = CoralAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Test natural language parsing with zero cloud calls. The model executes createTransactionDraft() directly on the device GPU.",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = testInput,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("model_playground_input"),
                placeholder = { Text("e.g. Asda 16.40 on DebitCard or Petrol 31.51 AMEX", color = TextSecondary, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CoralAccent,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF13151D),
                    unfocusedContainerColor = Color(0xFF13151D)
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onRunTest,
                enabled = !isTesting && testInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_inference_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inferring on GPU...", color = Color.White)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Local Inference & Tool Calling", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (testResult != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF11131A))
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${testResult.modelName} · Latency: ${testResult.latencyMs}ms",
                            color = EmeraldGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${testResult.tokensPerSecond.toInt()} tokens/sec",
                            color = CoralAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tool Call: ${testResult.toolCallInvoked}",
                        color = Color(0xFF93C5FD),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = testResult.rawTokensGenerated,
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onPrefillTransaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prefill_transaction_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open Transaction Draft with Pre-Selected Account & Category",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun Double.format(digits: Int): String = String.format(Locale.US, "%.${digits}f", this)
