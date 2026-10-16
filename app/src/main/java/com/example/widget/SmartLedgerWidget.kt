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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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

class SmartLedgerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmartLedgerWidget()
}

class SmartLedgerWidget : GlanceAppWidget() {

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
            GlanceContent(
                context = context,
                income = income,
                expenses = expenses,
                netTotal = netTotal
            )
        }
    }

    @Composable
    private fun GlanceContent(
        context: Context,
        income: Double,
        expenses: Double,
        netTotal: Double
    ) {
        val component = ComponentName(context, MainActivity::class.java)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF121622))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONEY MANAGER • " + SimpleDateFormat("MMM yyyy", Locale.US).format(Date()),
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF9CA3AF)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )

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
                    Text("£${String.format(Locale.US, "%.2f", expenses)}", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF4A90E2)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("Net", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF9CA3AF)), fontSize = 10.sp))
                    Text("£${String.format(Locale.US, "%.2f", netTotal)}", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Action buttons row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(Color(0xFF1E2838))
                        .padding(vertical = 6.dp)
                        .clickable(actionStartActivity(component)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Add Expense", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFFF5A5F)), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(Color(0xFF1E2838))
                        .padding(vertical = 6.dp)
                        .clickable(actionStartActivity(component)),
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
                        .clickable(actionStartActivity(component)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Scan Receipt", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
