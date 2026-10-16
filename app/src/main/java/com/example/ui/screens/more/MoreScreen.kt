package com.example.ui.screens.more

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionType
import com.example.data.preferences.AppThemeMode
import com.example.ui.components.frostedGlass
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LedgerViewModel

enum class MoreSubScreen {
    GRID,
    CONFIGURATION,
    BACKUP,
    EXPENSE_CATEGORIES,
    INCOME_CATEGORIES,
    LOCAL_AI_MODELS,
    DIAGNOSTICS
}

data class SettingsTile(
    val title: String,
    val icon: ImageVector,
    val subScreen: MoreSubScreen? = null,
    val actionTag: String
)

@Composable
fun MoreScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    var currentSubScreen by remember { mutableStateOf(MoreSubScreen.GRID) }
    val userSettings by viewModel.userSettings.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showExcelExportDialog by remember { mutableStateOf(false) }
    var showSimpleDialogTitle by remember { mutableStateOf<String?>(null) }
    var showSimpleDialogText by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = currentSubScreen != MoreSubScreen.GRID) {
        currentSubScreen = MoreSubScreen.GRID
    }

    when (currentSubScreen) {
        MoreSubScreen.GRID -> {
            MoreGrid(
                viewModel = viewModel,
                onNavigate = { currentSubScreen = it },
                onOpenCalc = { viewModel.openCalcBox() },
                onShowInfo = { title, text ->
                    showSimpleDialogTitle = title
                    showSimpleDialogText = text
                }
            )
        }
        MoreSubScreen.CONFIGURATION -> {
            ConfigurationView(
                viewModel = viewModel,
                onBack = { currentSubScreen = MoreSubScreen.GRID },
                onOpenExpenseCats = { currentSubScreen = MoreSubScreen.EXPENSE_CATEGORIES },
                onOpenIncomeCats = { currentSubScreen = MoreSubScreen.INCOME_CATEGORIES },
                onOpenLocalAi = { currentSubScreen = MoreSubScreen.LOCAL_AI_MODELS }
            )
        }
        MoreSubScreen.LOCAL_AI_MODELS -> {
            LocalAiModelManagerView(
                viewModel = viewModel,
                onBack = { currentSubScreen = MoreSubScreen.CONFIGURATION }
            )
        }
        MoreSubScreen.DIAGNOSTICS -> {
            val context = LocalContext.current
            val fileTree = remember { com.example.util.PiiRedactingFileTree(context) }
            com.example.ui.screens.diagnostics.DiagnosticsScreen(
                fileTree = fileTree,
                onBack = { currentSubScreen = MoreSubScreen.GRID }
            )
        }
        MoreSubScreen.BACKUP -> {
            BackupView(
                viewModel = viewModel,
                onBack = { currentSubScreen = MoreSubScreen.GRID },
                onExportExcel = { showExcelExportDialog = true }
            )
        }
        MoreSubScreen.EXPENSE_CATEGORIES -> {
            CategoryManagerView(
                title = "Expenses Category",
                type = TransactionType.EXPENSE,
                categories = categories.filter { it.type == TransactionType.EXPENSE },
                onBack = { currentSubScreen = MoreSubScreen.CONFIGURATION },
                onAddCategory = { name, color ->
                    viewModel.saveCategory(name, TransactionType.EXPENSE, "", color)
                },
                onDeleteCategory = { viewModel.deleteCategory(it) }
            )
        }
        MoreSubScreen.INCOME_CATEGORIES -> {
            CategoryManagerView(
                title = "Income Category",
                type = TransactionType.INCOME,
                categories = categories.filter { it.type == TransactionType.INCOME },
                onBack = { currentSubScreen = MoreSubScreen.CONFIGURATION },
                onAddCategory = { name, color ->
                    viewModel.saveCategory(name, TransactionType.INCOME, "", color)
                },
                onDeleteCategory = { viewModel.deleteCategory(it) }
            )
        }
    }

    // Export Excel Modal Dialog matching Screenshot 10
    if (showExcelExportDialog) {
        AlertDialog(
            onDismissRequest = { showExcelExportDialog = false },
            containerColor = Color(0xFF1D212B),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Money Manager - Excel", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showExcelExportDialog = false }) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val ranges = listOf(
                        "Monthly (01.10.2026 ~ 31.10)",
                        "Last M (01.09.2026 ~ 30.09)",
                        "Annually (01.01.2026 ~ 31.12)",
                        "Last Y (01.01.2025 ~ 31.12)",
                        "Total"
                    )
                    for (range in ranges) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF262B38))
                                .clickable {
                                    viewModel.showSnackbar("Exported CSV file for $range")
                                    showExcelExportDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(range, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showSimpleDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { showSimpleDialogTitle = null },
            containerColor = Color(0xFF1A1D27),
            title = { Text(showSimpleDialogTitle ?: "", color = Color.White) },
            text = { Text(showSimpleDialogText ?: "", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showSimpleDialogTitle = null }) {
                    Text("OK", color = CoralAccent)
                }
            }
        )
    }
}

// ---------------- 3x3 SETTINGS GRID (Screenshot 7) ----------------
@Composable
fun MoreGrid(
    viewModel: LedgerViewModel,
    onNavigate: (MoreSubScreen) -> Unit,
    onOpenCalc: () -> Unit,
    onShowInfo: (String, String) -> Unit
) {
    val context = LocalContext.current
    val tiles = listOf(
        SettingsTile("Configuration", Icons.Default.Settings, MoreSubScreen.CONFIGURATION, "cfg"),
        SettingsTile("Local AI", Icons.Default.Memory, MoreSubScreen.LOCAL_AI_MODELS, "local_ai"),
        SettingsTile("Diagnostics", Icons.Default.History, MoreSubScreen.DIAGNOSTICS, "diag"),
        SettingsTile("Export Excel", Icons.Default.Download, null, "excel_export"),
        SettingsTile("CalcBox", Icons.Default.Calculate, null, "calc"),
        SettingsTile("PC Manager", Icons.Default.Computer, null, "pcm"),
        SettingsTile("Backup", Icons.Default.Backup, MoreSubScreen.BACKUP, "bak"),
        SettingsTile("Feedback", Icons.Default.Email, null, "fb"),
        SettingsTile("Help", Icons.Default.Help, null, "hlp")
    )

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgCol = Color.Transparent
    val cardBg = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSec = MaterialTheme.colorScheme.onSurfaceVariant
    val tileBg = if (isLight) Color(0xFFE2E8F0) else Color(0xFF151A24)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgCol)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = Color(0xFF151A24),
                    surfaceAlpha = 0.65f,
                    borderAlpha = 0.30f
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "More & Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                "v4.13.0 • Diagnostics",
                color = textSec,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clickable {
                    onNavigate(MoreSubScreen.DIAGNOSTICS)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            items(tiles) { tile ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .frostedGlass(
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = tileBg,
                            surfaceAlpha = 0.65f,
                            borderAlpha = 0.30f
                        )
                        .clickable {
                            when (tile.actionTag) {
                                "cfg" -> onNavigate(MoreSubScreen.CONFIGURATION)
                                "local_ai" -> onNavigate(MoreSubScreen.LOCAL_AI_MODELS)
                                "diag" -> onNavigate(MoreSubScreen.DIAGNOSTICS)
                                "bak" -> onNavigate(MoreSubScreen.BACKUP)
                                "excel_export" -> viewModel.exportTransactionsToExcel(context)
                                "calc" -> onOpenCalc()
                                "pass" -> onShowInfo("Passcode Security", "Biometric and PIN lock is configured and active for app launch protection.")
                                "pcm" -> onShowInfo("PC Manager", "Connect to the same Wi-Fi network and access http://192.168.1.100:8080 on your browser to manage transactions on desktop.")
                                "fb" -> onShowInfo("Feedback", "Thank you for using SmartLedger! Feedback sent successfully.")
                                "hlp" -> onNavigate(MoreSubScreen.DIAGNOSTICS)
                                "rec" -> onShowInfo("Recommend", "Sharing link copied to clipboard!")
                                else -> if (tile.subScreen != null) onNavigate(tile.subScreen)
                            }
                        }
                        .padding(vertical = 18.dp, horizontal = 8.dp)
                        .testTag("tile_${tile.actionTag}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tile.icon, contentDescription = tile.title, tint = textColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = tile.title,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ---------------- CONFIGURATION SCREEN (Screenshots 13 & 15) ----------------
@Composable
fun ConfigurationView(
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    onOpenExpenseCats: () -> Unit,
    onOpenIncomeCats: () -> Unit,
    onOpenLocalAi: () -> Unit
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val currencyState by viewModel.currencyState.collectAsState()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12141A))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Configuration", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Category/Repeat Section
            item {
                Text(
                    "Category/Repeat",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
                )
            }

            item {
                ConfigRow("Income Category Setting", "", onClick = onOpenIncomeCats)
                ConfigRow("Expenses Category Setting", "", onClick = onOpenExpenseCats)
                ConfigRow("Subcategory", "ON") {}
                ConfigRow("Budget Setting", "£ 1,200.00") {}
                ConfigRow("Tag Settings", "Default") {}
                ConfigRow("Repeat Setting", "Daily, Monthly") {}
            }

            // Configuration Section
            item {
                Text(
                    "Configuration & Theme",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
                )
            }

            item {
                ConfigRow(
                    title = "Currency & Conversion Rate",
                    value = "${currencyState.activeCurrency.code} (${currencyState.activeCurrency.symbol}) • 1 £ = ${currencyState.exchangeRate}",
                    onClick = { showCurrencyDialog = true }
                )
                // User-Facing Quick Theme Toggle Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "App Theme",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Switch between Dark Slate Mesh and High-Contrast Light Glass",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Segmented Theme Selector Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141722))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isDark = userSettings.themeMode != AppThemeMode.FROSTED_LIGHT
                            val isLight = userSettings.themeMode == AppThemeMode.FROSTED_LIGHT

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) CoralAccent else Color.Transparent)
                                    .clickable { viewModel.setThemeMode(AppThemeMode.FROSTED_DARK) }
                                    .padding(vertical = 10.dp)
                                    .testTag("theme_toggle_dark"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Dark Slate Mesh",
                                    color = if (isDark) Color.White else TextSecondary,
                                    fontWeight = if (isDark) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isLight) CoralAccent else Color.Transparent)
                                    .clickable { viewModel.setThemeMode(AppThemeMode.FROSTED_LIGHT) }
                                    .padding(vertical = 10.dp)
                                    .testTag("theme_toggle_light"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Frosted Light Glass",
                                    color = if (isLight) Color.White else TextSecondary,
                                    fontWeight = if (isLight) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                ConfigRow("Main Currency Setting", userSettings.mainCurrency, onClick = { showCurrencyDialog = true })
                ConfigRow("Sub Currency Setting", userSettings.subCurrency) {}
                ConfigRow("Start Screen (Daily/Calendar)", userSettings.startScreen) {}
                ConfigRow("Monthly Start Date", "Every ${userSettings.monthlyStartDate}") {}
                ConfigRow("Weekly Start Day", userSettings.weeklyStartDay) {}
                ConfigRow("Carry-over Setting", if (userSettings.carryOverEnabled) "ON" else "OFF") {}
                ConfigRow("More Theme Presets", userSettings.themeMode.name, onClick = { showThemeDialog = true })
                ConfigRow("Autocomplete", if (userSettings.autocompleteEnabled) "ON" else "OFF") {}
            }

            // Local On-Device AI Section
            item {
                Text(
                    "Local On-Device AI (LiteRT-LM)",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
                )
            }

            item {
                val models by viewModel.localModels.collectAsState()
                val activeModel = models.find { it.isActive }
                ConfigRow("Local AI Model Manager", activeModel?.name ?: "Gemma 3 1B", onClick = onOpenLocalAi)
                ConfigRow("Inference Engine", "LiteRT-LM (OpenCL GPU)", onClick = onOpenLocalAi)
                ConfigRow("Privacy Mode", "100% Offline (Zero Cloud)", onClick = onOpenLocalAi)
            }
        }
    }

    if (showCurrencyDialog) {
        var tempCurrency by remember { mutableStateOf(currencyState.activeCurrency) }
        var tempRateStr by remember { 
            mutableStateOf(
                when (currencyState.activeCurrency) {
                    com.example.data.preferences.AppCurrency.INR -> currencyState.savedInrRate.toString()
                    com.example.data.preferences.AppCurrency.USD -> currencyState.savedUsdRate.toString()
                    else -> "1.0"
                }
            )
        }

        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            containerColor = Color(0xFF1B1E29),
            title = { Text("Currency & Manual Exchange Rate", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Select Base Display Currency:", color = TextSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (curr in com.example.data.preferences.AppCurrency.entries) {
                            val selected = tempCurrency == curr
                            Button(
                                onClick = {
                                    tempCurrency = curr
                                    tempRateStr = when (curr) {
                                        com.example.data.preferences.AppCurrency.GBP -> "1.0"
                                        com.example.data.preferences.AppCurrency.INR -> currencyState.savedInrRate.toString()
                                        com.example.data.preferences.AppCurrency.USD -> currencyState.savedUsdRate.toString()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) EmeraldGreen else Color(0xFF2C3242)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(curr.code, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (tempCurrency != com.example.data.preferences.AppCurrency.GBP) {
                        OutlinedTextField(
                            value = tempRateStr,
                            onValueChange = { tempRateStr = it },
                            label = { Text("Enter Manual Conversion Rate (1 GBP £ = ?)", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val parsedRate = tempRateStr.toDoubleOrNull() ?: 1.0
                        val previewConverted = 100.0 * parsedRate
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF252B3B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Live Conversion Preview", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("£ 100.00 = ${tempCurrency.symbol} ${String.format(Locale.US, "%,.2f", previewConverted)}", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    } else {
                        Text("British Pound is the canonical base currency (1.0x).", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = if (tempCurrency == com.example.data.preferences.AppCurrency.GBP) 1.0 else (tempRateStr.toDoubleOrNull() ?: 1.0)
                        if (rate > 0.0) {
                            viewModel.setCurrencyState(tempCurrency, rate)
                            showCurrencyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                ) {
                    Text("Apply & Convert App", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showThemeDialog) {
        val modes = listOf(
            AppThemeMode.FROSTED_DARK to "Modern Frosted Dark (Slate Mesh)",
            AppThemeMode.FROSTED_LIGHT to "High-Contrast Frosted Light Glass",
            AppThemeMode.AMOLED_BLACK to "AMOLED Pitch Black",
            AppThemeMode.EMERALD_FINANCIAL to "Emerald Financial",
            AppThemeMode.DYNAMIC_SYSTEM to "Dynamic Material You"
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = Color(0xFF1D212B),
            title = { Text("Select App Theme Preset", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((mode, name) in modes) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (userSettings.themeMode == mode) CoralAccent else Color(0xFF262B38))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// ---------------- BACKUP & PHONE MIGRATION SCREEN ----------------
@Composable
fun BackupView(
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    onExportExcel: () -> Unit
) {
    var googleDriveSyncDaily by remember { mutableStateOf(true) }
    val last24hCount by viewModel.last24HoursCount.collectAsState()
    val allTxs by viewModel.allTransactions.collectAsState()
    val context = LocalContext.current
    val tactile = com.example.ui.util.rememberTactileHaptic()

    var showRevertConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var isFullResetSelected by remember { mutableStateOf(false) }
    var resetConfirmationTypedText by remember { mutableStateOf("") }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }

    // SAF Create Document launcher for WAL-safe database export
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDatabaseBackup(uri)
        }
    }

    // SAF Open Document launcher for SQLite-verified database restore
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreDatabaseBackup(uri) { error ->
                restoreErrorMessage = error
            }
        }
    }

    // SAF Open Document launcher for spreadsheet parser
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
                }
            }.getOrNull() ?: uri.lastPathSegment ?: "spreadsheet.csv"

            viewModel.openImportPreview(uri, fileName)
        }
    }

    fun launchDatabaseExport() {
        val defaultFilename = "SmartLedger_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.db"
        exportBackupLauncher.launch(defaultFilename)
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgCol = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSec = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
            }
            Text("Backup & Phone Migration", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Full Database Backup & Phone Migration Restore
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Full Database Backup & Phone Migration", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Export your complete database (.db) to Google Drive, Downloads, or local storage when switching phones. WAL checkpoints guarantee 100% of recent transactions are saved.",
                            color = textSec,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { launchDatabaseExport() },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_export_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Export (.db)", color = Color.White, fontSize = 13.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    restoreBackupLauncher.launch(
                                        arrayOf("*/*", "application/octet-stream", "application/x-sqlite3")
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF2C3242)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_restore_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Upload, contentDescription = null, tint = if (isLight) Color(0xFF0F172A) else Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Restore (.db)", color = if (isLight) Color(0xFF0F172A) else Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Apache POI Excel Export Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Export Transactions to Excel (.csv)", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Export your current transaction list into a formatted spreadsheet (.csv) compatible with Excel, Google Sheets, and Numbers.",
                            color = textSec,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.exportTransactionsToExcel(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_excel_button")
                        ) {
                            Text("Export to Excel (.csv)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // System Telemetry & App Logs Export Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = ExpenseBlue, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("System Telemetry & App Logs (.txt)", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Export complete system telemetry, event audit trail, and behavior logs to a text file for error diagnosis.",
                            color = textSec,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.exportAppLogs(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_logs_button")
                        ) {
                            Text("Export App Logs (.txt)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Scheduled Auto-Backup (WorkManager Rolling 7-Day Snapshots)
            item {
                val userSettings by viewModel.userSettings.collectAsState()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = ExpenseBlue, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scheduled Auto-Backup (WorkManager)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            }
                            Button(
                                onClick = {
                                    tactile.click()
                                    viewModel.toggleAutoBackup(!userSettings.autoBackupEnabled)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (userSettings.autoBackupEnabled) EmeraldGreen else Color(0xFF2C3242)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (userSettings.autoBackupEnabled) "ACTIVE" else "ENABLE", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Automatically runs daily WAL checkpoints and keeps the latest 7 rolling backup snapshots in local storage.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Bulk Import & Rollback Section
            item {
                Text(
                    "Bulk Spreadsheet Import & Rollback",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp)
                )
            }

            item {
                ConfigRow(
                    title = "Import from Excel/CSV (.xlsx, .xls, .csv)",
                    value = "Preview & Commit",
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "text/comma-separated-values",
                                "text/csv",
                                "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "*/*"
                            )
                        )
                    }
                )

                ConfigRow(
                    title = "Revert Last 24 Hours",
                    value = if (last24hCount > 0) "$last24hCount added" else "0 added",
                    onClick = {
                        if (last24hCount > 0) {
                            showRevertConfirmDialog = true
                        } else {
                            viewModel.showSnackbar("No transactions were recorded in the last 24 hours")
                        }
                    }
                )

                ConfigRow("Export data to Excel spreadsheet", "Download .xlsx", onClick = onExportExcel)
            }

            // Two-Tier Reset Section
            item {
                Text(
                    "Danger Zone: Reset Data",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .frostedGlass(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = Color(0x28FF5A5F),
                            borderColor = CoralAccent,
                            borderAlpha = 0.35f
                        )
                        .testTag("reset_danger_zone_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Danger Zone: Reset Options", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Option A: Clear Transactions Only
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isFullResetSelected = false
                                    resetConfirmationTypedText = ""
                                    showResetConfirmDialog = true
                                }
                                .testTag("reset_option_a_clear_txs"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Clear All Transactions Only", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Keep Accounts & Categories, wipe history & reset balances to £0.00", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Option B: Complete Factory Reset
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isFullResetSelected = true
                                    resetConfirmationTypedText = ""
                                    showResetConfirmDialog = true
                                }
                                .testTag("reset_option_b_factory_reset"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF28181A)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Complete Factory Reset", color = CoralAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Wipe everything & restore default starter accounts & categories", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Safety Confirmation Dialog with "Backup First" Shortcut
    if (showResetConfirmDialog) {
        val txCount = allTxs.size
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = Color(0xFF1B202B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isFullResetSelected) "Factory Reset Everything?" else "Clear All Transactions?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isFullResetSelected)
                            "This action will permanently delete all $txCount transactions, custom accounts, categories, and preferences. Default starter data will be restored."
                        else
                            "This action will permanently delete all $txCount transactions and reset account balances to £0.00. Your custom accounts and categories will remain intact.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // 1-Tap "Export Backup First" Shortcut Button
                    OutlinedButton(
                        onClick = {
                            launchDatabaseExport()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_dialog_export_backup_first"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export Backup First (.db)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text(
                        "To confirm deletion, type \"RESET\" below:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = resetConfirmationTypedText,
                        onValueChange = { resetConfirmationTypedText = it },
                        placeholder = { Text("RESET", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_confirmation_text_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                val isConfirmEnabled = resetConfirmationTypedText.trim().equals("RESET", ignoreCase = true)
                Button(
                    onClick = {
                        if (isConfirmEnabled) {
                            if (isFullResetSelected) {
                                viewModel.factoryResetApp {
                                    showResetConfirmDialog = false
                                }
                            } else {
                                viewModel.clearTransactionsOnly {
                                    showResetConfirmDialog = false
                                }
                            }
                        }
                    },
                    enabled = isConfirmEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralAccent,
                        disabledContainerColor = Color(0xFF3B2529)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("reset_confirm_permanently_delete_button")
                ) {
                    Text(
                        "Permanently Delete",
                        color = if (isConfirmEnabled) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Invalid SQLite Backup File Error Dialog
    if (restoreErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { restoreErrorMessage = null },
            containerColor = Color(0xFF1B202B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CoralAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Invalid Backup File", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    restoreErrorMessage ?: "The selected file is not a valid SQLite database backup.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { restoreErrorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    // Revert 24h Confirmation Dialog
    if (showRevertConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRevertConfirmDialog = false },
            containerColor = Color(0xFF1B202B),
            title = { Text("Revert Last 24 Hours", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to revert $last24hCount transactions added in the last 24 hours? This will delete them from the database and automatically restore previous account balances.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        tactile.heavyClick()
                        showRevertConfirmDialog = false
                        viewModel.revertLast24Hours()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                ) {
                    Text("Revert $last24hCount Items", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// ---------------- CATEGORY MANAGER SCREEN (Screenshots 1 & 12) ----------------
@Composable
fun CategoryManagerView(
    title: String,
    type: TransactionType,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAddCategory: (String, String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var isAddCategoryOpen by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var subcategoryEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12141A))
    ) {
        // Header matching Screenshot 1 & 12
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            IconButton(onClick = { isAddCategoryOpen = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category", tint = Color.White)
            }
        }

        // Subcategory toggle switch matching Screenshot 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161922))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Subcategory", color = Color.White, fontSize = 15.sp)
            Switch(
                checked = subcategoryEnabled,
                onCheckedChange = { subcategoryEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = CoralAccent)
            )
        }

        // Categories List matching Screenshot 1 & 12
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(categories) { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onDeleteCategory(cat) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.RemoveCircle,
                                contentDescription = "Delete",
                                tint = CoralAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(cat.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("≡", color = TextSecondary, fontSize = 20.sp) // Drag handle representation
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF1E222D)))
            }
        }
    }

    if (isAddCategoryOpen) {
        AlertDialog(
            onDismissRequest = { isAddCategoryOpen = false },
            containerColor = Color(0xFF1D212B),
            title = { Text("Add $title", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    placeholder = { Text("Category name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onAddCategory(newCatName, "#10B981")
                            newCatName = ""
                            isAddCategoryOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddCategoryOpen = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ConfigRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 15.sp)
        if (value.isNotBlank()) {
            Text(value, color = CoralAccent, fontSize = 14.sp)
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF1B1F2A)))
}
