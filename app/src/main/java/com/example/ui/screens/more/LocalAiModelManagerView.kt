package com.example.ui.screens.more

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.screens.ai.ModelManagerScreen
import com.example.ui.viewmodel.LedgerViewModel

/**
 * Local AI Model Manager View.
 * Delegates to the unified ModelManagerScreen in com.example.ui.screens.ai.
 */
@Composable
fun LocalAiModelManagerView(
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModelManagerScreen(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}
