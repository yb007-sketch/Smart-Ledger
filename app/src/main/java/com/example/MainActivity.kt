package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.frostedGlass
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.ai.AiAssistantDialog
import com.example.ui.screens.calculator.CalcBoxDialog
import com.example.ui.screens.entry.RapidTransactionEntryDialog
import com.example.ui.screens.entry.TransactionEntryBottomSheet
import com.example.ui.screens.importpreview.ImportPreviewScreen
import com.example.ui.screens.importpreview.ImportPreviewViewModel
import com.example.ui.screens.more.MoreScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.SmartLedgerTheme
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LedgerViewModel
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private val viewModel: LedgerViewModel by viewModels()

    companion object {
        var pendingAction: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        if (pendingAction != null) {
            handleIncomingIntent(Intent().setAction(pendingAction))
            pendingAction = null
        }

        setContent {
            val userSettings by viewModel.userSettings.collectAsState()
            val currencyState by viewModel.currencyState.collectAsState()

            SmartLedgerTheme(themeMode = userSettings.themeMode) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.example.data.preferences.LocalCurrencyState provides currencyState
                ) {
                    SmartLedgerApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            "ACTION_QUICK_EXPENSE", "ACTION_ADD_EXPENSE" -> viewModel.openRapidEntryDialog()
            "ACTION_VOICE_AI" -> viewModel.openAiAssistant()
            "ACTION_SCAN_RECEIPT" -> viewModel.openEntrySheet()
        }
    }
}

@Composable
fun SmartLedgerApp(viewModel: LedgerViewModel) {
    val currentNavIndex by viewModel.currentBottomNavIndex.collectAsState()
    val isEntryOpen by viewModel.isEntrySheetOpen.collectAsState()
    val isRapidEntryOpen by viewModel.isRapidEntryDialogOpen.collectAsState()
    val editingTx by viewModel.editingTransaction.collectAsState()
    val prefillDraft by viewModel.prefillDraft.collectAsState()
    val isAiOpen by viewModel.isAiAssistantOpen.collectAsState()
    val isAskMyLedgerChatOpen by viewModel.isAskMyLedgerChatOpen.collectAsState()
    val isModelManagerOpen by viewModel.isModelManagerOpen.collectAsState()
    val isCalcOpen by viewModel.isCalcBoxOpen.collectAsState()
    val isImportPreviewOpen by viewModel.isImportPreviewOpen.collectAsState()
    val importFileUri by viewModel.importFileUri.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val insights by viewModel.spendingInsights.collectAsState()
    val localModels by viewModel.localModels.collectAsState()
    val coachedInsight by viewModel.coachedInsight.collectAsState()
    val aiPersona by viewModel.aiPersona.collectAsState()
    val smartBudgets by viewModel.smartBudgets.collectAsState()
    val anomalyAlerts by viewModel.anomalyAlerts.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (msg.contains("deleted", ignoreCase = true)) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
            viewModel.clearSnackbar()
        }
    }

    AmbientMeshBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Floating Frosted Capsule Navigation Dock (20.dp horizontal margins, alpha = 0.65f, 1.dp specular border)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("main_bottom_nav_container")
                ) {
                    // 1. Pure Frosted Glass Capsule Surface Layer strictly behind navigation content
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .frostedGlass(
                                shape = RoundedCornerShape(32.dp),
                                backgroundColor = Color(0xFF151A24),
                                surfaceAlpha = 0.65f,
                                borderWidth = 1.dp,
                                borderColor = Color.White,
                                borderAlpha = 0.30f
                            )
                            .testTag("bottom_nav_frosted_surface")
                    )

                    // 2. Main Navigation Items with Expressive Spring Scale & Glowing Indicator Dot
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .zIndex(1f)
                            .testTag("main_bottom_nav"),
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        val navItems = listOf(
                            Triple("Trans.", Icons.Default.MenuBook, 0),
                            Triple("Stats", Icons.Default.BarChart, 1),
                            Triple("Accounts", Icons.Default.AccountBalance, 2),
                            Triple("More", Icons.Default.MoreHoriz, 3)
                        )

                        for ((title, icon, index) in navItems) {
                            val isSelected = currentNavIndex == index
                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "navIconSpring_$title"
                            )

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setBottomNavIndex(index)
                                },
                                icon = {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .scale(iconScale)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Color.Black.copy(alpha = 0.70f),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .offset(x = 0.dp, y = 1.dp)
                                        )
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = if (isSelected) CoralAccent else Color.White.copy(alpha = 0.75f),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .testTag("nav_icon_$title")
                                        )
                                    }
                                },
                                label = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) CoralAccent else TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(if (isSelected) 5.dp else 0.dp)
                                                .background(
                                                    color = if (isSelected) CoralAccent else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = CoralAccent,
                                    unselectedIconColor = Color.White.copy(alpha = 0.75f),
                                    selectedTextColor = CoralAccent,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_$title")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentNavIndex,
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.96f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                            (scaleOut(
                                targetScale = 0.96f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                    },
                    label = "ScreenTransition"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> TransactionsScreen(viewModel = viewModel)
                        1 -> StatsScreen(viewModel = viewModel)
                        2 -> AccountsScreen(viewModel = viewModel)
                        3 -> MoreScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Transaction Entry Bottom Sheet
        TransactionEntryBottomSheet(
            isOpen = isEntryOpen,
            onDismiss = { viewModel.closeEntrySheet() },
            accounts = accounts,
            categories = categories,
            editingTransaction = editingTx,
            prefillDraft = prefillDraft,
            currencySymbol = userSettings.mainCurrencySymbol,
            onSave = { amount, type, accountId, toAccountId, categoryId, subCategory, merchantName, timestamp, notes, tags ->
                viewModel.saveTransaction(
                    amount = amount,
                    type = type,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    subCategory = subCategory,
                    merchantName = merchantName,
                    timestamp = timestamp,
                    notes = notes,
                    tags = tags
                )
            }
        )

        // Rapid Transaction Entry Dialog (Fast logging of Date, Amount, Category)
        RapidTransactionEntryDialog(
            isOpen = isRapidEntryOpen,
            onDismiss = { viewModel.closeRapidEntryDialog() },
            accounts = accounts,
            categories = categories,
            currencySymbol = userSettings.mainCurrencySymbol,
            onSave = { amount, type, accountId, toAccountId, categoryId, subCategory, merchantName, timestamp, notes, tags ->
                viewModel.saveTransaction(
                    amount = amount,
                    type = type,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    subCategory = subCategory,
                    merchantName = merchantName,
                    timestamp = timestamp,
                    notes = notes,
                    tags = tags
                )
            },
            onOpenFullDetail = {
                viewModel.closeRapidEntryDialog()
                viewModel.openEntrySheet()
            }
        )

        // AI Assistant Dialog (100% Offline Local SLM with Native Kotlin Tool Calling)
        val activeLocalModel = remember(localModels) {
            localModels.find { it.isActive }?.name ?: "Gemma 3 1B Instruct"
        }
        AiAssistantDialog(
            isOpen = isAiOpen,
            onDismiss = { viewModel.closeAiAssistant() },
            onParseQuery = { query ->
                viewModel.parseWithLocalLlmAndPrefill(query)
            },
            activeModelName = activeLocalModel,
            insights = insights,
            coachedInsight = coachedInsight,
            aiPersona = aiPersona,
            onSelectPersona = { viewModel.setAiPersona(it) },
            smartBudgets = smartBudgets,
            anomalyAlerts = anomalyAlerts,
            onOpenModelManager = {
                viewModel.closeAiAssistant()
                viewModel.openModelManager()
            },
            onOpenChat = {
                viewModel.closeAiAssistant()
                viewModel.openAskMyLedgerChat()
            }
        )

        // CalcBox Calculator Dialog
        CalcBoxDialog(
            isOpen = isCalcOpen,
            onDismiss = { viewModel.closeCalcBox() },
            currencySymbol = userSettings.mainCurrencySymbol
        )

        // Full Screen Model Manager Overlay
        if (isModelManagerOpen) {
            com.example.ui.screens.ai.ModelManagerScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeModelManager() }
            )
        }

        // Ask My Ledger AI Chat Screen Overlay
        if (isAskMyLedgerChatOpen) {
            com.example.ui.screens.ai.AskMyLedgerChatScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeAskMyLedgerChat() }
            )
        }

        // Bulk Import Preview & Staging Screen
        if (isImportPreviewOpen && importFileUri != null) {
            val (uri, fileName) = importFileUri!!
            val context = LocalContext.current
            val importPreviewViewModel = remember(uri) {
                ImportPreviewViewModel(viewModel.repository).also { ipvm ->
                    ipvm.loadFile(context, uri, fileName)
                }
            }
            ImportPreviewScreen(
                viewModel = importPreviewViewModel,
                accounts = accounts,
                categories = categories,
                currencySymbol = userSettings.mainCurrencySymbol,
                onClose = { viewModel.closeImportPreview() },
                onCommitted = { count ->
                    viewModel.closeImportPreview()
                    viewModel.showSnackbar("Successfully imported $count transactions")
                }
            )
        }
    }
}
