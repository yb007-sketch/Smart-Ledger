package com.example.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class QuickAddActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        MainActivity.pendingAction = "ACTION_QUICK_EXPENSE"
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_QUICK_EXPENSE"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

class VoiceAiActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        MainActivity.pendingAction = "ACTION_VOICE_AI"
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_VOICE_AI"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

// 1. 2x4 Wide Widget Receiver & Implementation
class SmartLedgerWideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmartLedgerWideWidget()
}

class SmartLedgerWideWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (income, expenses, netTotal) = withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            val startOfMonth = cal.timeInMillis

            val txs = db.transactionDao().getAllTransactionsSync().filter { it.transactionDateMillis >= startOfMonth }
            val inc = txs.filter { it.type.name == "INCOME" }.sumOf { it.amount }
            val exp = txs.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
            Triple(inc, exp, inc - exp)
        }

        provideContent {
            val homeIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF121622))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SmartLedger • " + SimpleDateFormat("MMM yyyy", Locale.US).format(Date()),
                        style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF9CA3AF)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text("Income", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF9CA3AF)), fontSize = 10.sp))
                        Text("£${String.format(Locale.US, "%.2f", income)}", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF10B981)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    }
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text("Expenses", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF9CA3AF)), fontSize = 10.sp))
                        Text("£${String.format(Locale.US, "%.2f", expenses)}", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFEF4444)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    }
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text("Net Balance", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF9CA3AF)), fontSize = 10.sp))
                        Text("£${String.format(Locale.US, "%.2f", netTotal)}", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(Color(0xFF1E2838))
                            .padding(vertical = 6.dp)
                            .clickable(actionRunCallback<QuickAddActionCallback>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Quick Add", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFFF5A5F)), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(Color(0xFF1E2838))
                            .padding(vertical = 6.dp)
                            .clickable(actionRunCallback<VoiceAiActionCallback>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Voice AI", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF3B82F6)), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(Color(0xFF1E2838))
                            .padding(vertical = 6.dp)
                            .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("App Home", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF10B981)), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

// 2. 2x2 Small Dark Widget Receiver & Implementation
class SmartLedgerSmallDarkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmartLedgerSmallDarkWidget()
}

class SmartLedgerSmallDarkWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val expenses = withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            val startOfMonth = cal.timeInMillis

            val txs = db.transactionDao().getAllTransactionsSync().filter { it.transactionDateMillis >= startOfMonth }
            txs.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF121622))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("MONTHLY SPEND", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF9CA3AF)), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text("£${String.format(Locale.US, "%.2f", expenses)}", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFFF5A5F)), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = GlanceModifier.height(12.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF4444))
                        .padding(vertical = 8.dp)
                        .clickable(actionRunCallback<QuickAddActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Quick Add", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// 3. 2x2 Small Light Widget Receiver & Implementation
class SmartLedgerSmallLightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmartLedgerSmallLightWidget()
}

class SmartLedgerSmallLightWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val expenses = withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            val startOfMonth = cal.timeInMillis

            val txs = db.transactionDao().getAllTransactionsSync().filter { it.transactionDateMillis >= startOfMonth }
            txs.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFFFF))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("MONTHLY SPEND", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF64748B)), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text("£${String.format(Locale.US, "%.2f", expenses)}", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFEF4444)), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = GlanceModifier.height(12.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF4444))
                        .padding(vertical = 8.dp)
                        .clickable(actionRunCallback<QuickAddActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Quick Add", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// 4. 2x2 Voice AI Widget Receiver & Implementation
class SmartLedgerVoiceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmartLedgerVoiceWidget()
}

class SmartLedgerVoiceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AI ASSISTANT", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF9CA3AF)), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = GlanceModifier.height(6.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(Color(0xFF10B981))
                        .padding(vertical = 10.dp)
                        .clickable(actionRunCallback<VoiceAiActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎙️ Voice AI Quick", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
