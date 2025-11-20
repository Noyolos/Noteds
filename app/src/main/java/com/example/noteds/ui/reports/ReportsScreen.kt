package com.example.noteds.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.noteds.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel
) {
    val monthlyStats by reportsViewModel.last6MonthsStats.collectAsState()
    val agingStats by reportsViewModel.agingStats.collectAsState()
    val topDebtors by reportsViewModel.topDebtors.collectAsState()
    val totalTransactions by reportsViewModel.totalTransactions.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Reports", color = TextWhite, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue)
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Insight Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InsightCard(
                    title = "Total Trans.",
                    value = totalTransactions.toString(),
                    modifier = Modifier.weight(1f)
                )
                InsightCard(
                    title = "Avg Collection",
                    value = "12 Days", // Mocked calculation
                    modifier = Modifier.weight(1f)
                )
            }

            // Monthly Bar Chart
            ChartCard("Last 6 Months (Debt vs Payment)") {
                MonthlyBarChart(stats = monthlyStats, modifier = Modifier.fillMaxWidth().height(200.dp))
            }

            // Aging Distribution
            ChartCard("Debt Aging Distribution") {
                AgingDonutChart(stats = agingStats, modifier = Modifier.fillMaxWidth().height(200.dp))
            }

            // Top Debtors Visualization
            ChartCard("Top Debtors Analysis") {
                TopDebtorsHorizontalChart(debtors = topDebtors, currencyFormatter = currencyFormatter)
            }
        }
    }
}

@Composable
fun InsightCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MidnightBlue)
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
fun MonthlyBarChart(stats: List<MonthlyStats>, modifier: Modifier) {
    if (stats.isEmpty()) return

    Canvas(modifier = modifier) {
        val maxVal = stats.maxOfOrNull { maxOf(it.debt, it.payment) }?.toFloat() ?: 1f
        val barWidth = size.width / (stats.size * 3) // Space for 2 bars + gap
        val height = size.height

        stats.forEachIndexed { index, stat ->
            val x = index * (size.width / stats.size) + barWidth / 2

            // Debt Bar
            val debtHeight = (stat.debt.toFloat() / maxVal) * height
            drawRoundRect(
                color = DebtColor,
                topLeft = Offset(x, height - debtHeight),
                size = Size(barWidth, debtHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Payment Bar
            val paymentHeight = (stat.payment.toFloat() / maxVal) * height
            drawRoundRect(
                color = PaymentColor,
                topLeft = Offset(x + barWidth, height - paymentHeight),
                size = Size(barWidth, paymentHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

@Composable
fun AgingDonutChart(stats: List<AgingData>, modifier: Modifier) {
    if (stats.isEmpty()) return

    val total = stats.sumOf { it.amount }
    if (total == 0.0) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(160.dp)) {
                var startAngle = -90f
                val strokeWidth = 40f

                stats.forEach { stat ->
                    val sweepAngle = ((stat.amount / total) * 360).toFloat()
                    drawArc(
                        color = Color(stat.colorHex),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Column(modifier = Modifier.weight(0.8f)) {
            stats.forEach { stat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(stat.colorHex), CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stat.bucket, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun TopDebtorsHorizontalChart(debtors: List<DebtorData>, currencyFormatter: NumberFormat) {
    if (debtors.isEmpty()) return
    val maxAmount = debtors.maxOf { it.amount }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        debtors.take(5).forEach { debtor ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(debtor.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(currencyFormatter.format(debtor.amount), style = MaterialTheme.typography.bodyMedium, color = DebtColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(BackgroundColor, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((debtor.amount / maxAmount).toFloat())
                            .height(8.dp)
                            .background(MidnightBlue, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
