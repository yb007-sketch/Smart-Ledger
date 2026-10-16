package com.example.ui.screens.accounts

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.TransactionType
import com.example.ui.components.frostedGlass
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.AppLogger
import java.util.Locale

@Composable
fun AccountsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currencySymbol = userSettings.mainCurrencySymbol

    var isAddAccountOpen by remember { mutableStateOf(false) }
    var isTransferOpen by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }

    // Calculate Assets, Liabilities, Net Worth
    val assets = accounts.filter { it.balance > 0 }.sumOf { it.balance }
    val liabilities = accounts.filter { it.balance < 0 }.sumOf { kotlin.math.abs(it.balance) }
    val netWorth = assets - liabilities

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgCol = Color.Transparent
    val cardBg = if (isLight) Color.White else Color(0xB31B1E29)
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val textSec = if (isLight) Color(0xFF475569) else TextSecondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgCol)
    ) {
        // Frosted Top Pill Bar with 20.dp outer margin
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .frostedGlass(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = Color(0xFF151A24),
                    surfaceAlpha = 0.65f,
                    borderAlpha = 0.30f
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Accounts & Balances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { isTransferOpen = true },
                    modifier = Modifier.testTag("transfer_button")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer", tint = textColor)
                }
                IconButton(
                    onClick = { isAddAccountOpen = true },
                    modifier = Modifier.testTag("add_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account", tint = CoralAccent)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Net Worth Hero Card (Pure Frosted Glass)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(
                            shape = RoundedCornerShape(22.dp),
                            backgroundColor = Color(0xFF151A24),
                            surfaceAlpha = 0.65f,
                            borderAlpha = 0.30f
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "TOTAL NET WORTH",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••.••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", netWorth)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (netWorth >= 0) Color.White else CoralAccent
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Assets", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", assets)}",
                                    color = EmeraldGreen,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Liabilities", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", liabilities)}",
                                    color = CoralAccent,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Accounts List (Clickable to Edit/Update Balance)
            items(accounts) { account ->
                val icon = when {
                    account.isCreditCard -> Icons.Default.CreditCard
                    account.type == AccountType.CASH -> Icons.Default.Payments
                    else -> Icons.Default.AccountBalance
                }
                val iconColor = when {
                    account.isCreditCard -> CoralAccent
                    account.type == AccountType.CASH -> EmeraldGreen
                    else -> ExpenseBlue
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = if (isLight) Color.White else Color(0xFF151A24),
                            surfaceAlpha = 0.65f,
                            borderAlpha = 0.30f
                        )
                        .clickable { editingAccount = account },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(iconColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = iconColor)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = when {
                                            account.isCreditCard -> "Credit Card • Tap to edit balance/limit"
                                            account.type == AccountType.CASH -> "Cash Wallet • Tap to edit balance"
                                            else -> "Bank Account • Tap to edit balance"
                                        },
                                        color = textSec,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Text(
                                text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••.••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", account.balance)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (account.balance >= 0) textColor else CoralAccent
                            )
                        }

                        // Credit Card Statement Cycle & 1-Tap Settlement
                        if (account.isCreditCard) {
                            val debitAcc = accounts.firstOrNull { it.type == AccountType.DEBIT_CARD || it.type == AccountType.BANK } ?: accounts.firstOrNull { !it.isCreditCard }
                            val billedAmount = if (account.billedBalance > 0) account.billedBalance else kotlin.math.abs(account.balance)

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF2B3142))
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Billed Balance", color = textSec, fontSize = 11.sp)
                                    Text(
                                        text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••.••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", billedAmount)}",
                                        color = CoralAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Column {
                                    Text("Unbilled Spend", color = textSec, fontSize = 11.sp)
                                    Text(
                                        text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••.••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", account.unbilledCurrentSpend)}",
                                        color = textColor,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }

                                Column {
                                    Text("Available Credit", color = textSec, fontSize = 11.sp)
                                    Text(
                                        text = if (userSettings.stealthModeEnabled) "$currencySymbol ••••.••" else "$currencySymbol ${String.format(Locale.US, "%,.2f", account.availableCredit)}",
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                if (debitAcc != null && billedAmount > 0) {
                                    Button(
                                        onClick = {
                                            viewModel.settleCreditCardStatement(
                                                creditAccountId = account.id,
                                                debitAccountId = debitAcc.id,
                                                amount = billedAmount
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseBlue),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("settle_statement_button")
                                    ) {
                                        Text("Settle Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Section: Account & System Audit Log Trace
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = ExpenseBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Account & System Audit Log Trace", fontWeight = FontWeight.Bold, color = textColor, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = AppLogger.getLogsString().takeLast(1200),
                            color = textSec,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Add Account Dialog
    if (isAddAccountOpen) {
        var accName by remember { mutableStateOf("") }
        var accBalance by remember { mutableStateOf("") }
        var accLimit by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(AccountType.BANK) }

        AlertDialog(
            onDismissRequest = { isAddAccountOpen = false },
            containerColor = cardBg,
            title = { Text("Add New Account", color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text("Account Name", color = textSec) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                    OutlinedTextField(
                        value = accBalance,
                        onValueChange = { accBalance = it },
                        label = { Text("Initial Balance ($currencySymbol)", color = textSec) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = accBalance.toDoubleOrNull() ?: 0.0
                        val lim = accLimit.toDoubleOrNull() ?: 0.0
                        if (accName.isNotBlank()) {
                            viewModel.saveAccount(accName, selectedType, bal, lim)
                            isAddAccountOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                ) {
                    Text("Add Account", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddAccountOpen = false }) {
                    Text("Cancel", color = textSec)
                }
            }
        )
    }

    // Edit Account Dialog (Manual Override / Balance Adjustment / Delete)
    val accToEdit = editingAccount
    if (accToEdit != null) {
        var editName by remember { mutableStateOf(accToEdit.name) }
        var editBalance by remember { mutableStateOf(accToEdit.balance.toString()) }
        var editLimit by remember { mutableStateOf(accToEdit.creditLimit.toString()) }

        AlertDialog(
            onDismissRequest = { editingAccount = null },
            containerColor = cardBg,
            title = { Text("Edit Account: ${accToEdit.name}", color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Account Name", color = textSec) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                    OutlinedTextField(
                        value = editBalance,
                        onValueChange = { editBalance = it },
                        label = { Text("Current Balance ($currencySymbol)", color = textSec) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                    if (accToEdit.isCreditCard) {
                        OutlinedTextField(
                            value = editLimit,
                            onValueChange = { editLimit = it },
                            label = { Text("Credit Limit", color = textSec) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.deleteAccount(accToEdit)
                            editingAccount = null
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", color = CoralAccent)
                        }
                    }
                    Button(
                        onClick = {
                            val newBal = editBalance.toDoubleOrNull() ?: accToEdit.balance
                            val newLim = editLimit.toDoubleOrNull() ?: accToEdit.creditLimit
                            if (editName.isNotBlank()) {
                                val updated = accToEdit.copy(
                                    name = editName,
                                    balance = newBal,
                                    creditLimit = newLim
                                )
                                viewModel.updateAccount(updated)
                                editingAccount = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAccount = null }) {
                    Text("Cancel", color = textSec)
                }
            }
        )
    }

    // Inter-Account Transfer Dialog
    if (isTransferOpen && accounts.size >= 2) {
        var transferAmount by remember { mutableStateOf("") }
        var fromAccId by remember { mutableStateOf(accounts.first().id) }
        var toAccId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.first().id) }

        AlertDialog(
            onDismissRequest = { isTransferOpen = false },
            containerColor = cardBg,
            title = { Text("Transfer Between Accounts", color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("From: ${accounts.firstOrNull { it.id == fromAccId }?.name}", color = textSec)
                    Text("To: ${accounts.firstOrNull { it.id == toAccId }?.name}", color = textSec)
                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        label = { Text("Transfer Amount", color = textSec) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = transferAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            viewModel.saveTransaction(
                                amount = amt,
                                type = TransactionType.TRANSFER,
                                accountId = fromAccId,
                                toAccountId = toAccId,
                                categoryId = null,
                                subCategory = null,
                                merchantName = "Account Transfer",
                                timestamp = System.currentTimeMillis(),
                                notes = "Internal transfer",
                                tags = "Transfer"
                            )
                            isTransferOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                ) {
                    Text("Transfer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isTransferOpen = false }) {
                    Text("Cancel", color = textSec)
                }
            }
        )
    }
}
