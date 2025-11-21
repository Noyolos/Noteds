package com.example.noteds.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.nativeCanvas
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
    val averageCollectionPeriod by reportsViewModel.averageCollectionPeriod.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("財務報表", color = TextWhite, fontWeight = FontWeight.Bold) },
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
             // Header Title inside body to match HTML style
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            }

            // Monthly Bar Chart
            ChartCard("每月賒賬 vs 還款") {
                MonthlyBarChart(stats = monthlyStats, modifier = Modifier.fillMaxWidth().height(240.dp))
            }

            // Aging Distribution
            ChartCard("賬齡分佈 (0-90+天)") {
                AgingDonutChart(stats = agingStats, modifier = Modifier.fillMaxWidth().height(200.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    agingStats.forEach { stat ->
                         Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Box(modifier = Modifier.size(12.dp).background(Color(stat.colorHex), androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            val label = stat.bucket.replace(" Days", "天") // Translate 'Days' to Chinese
                            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            // Top Debtors Visualization
            ChartCard("Top 欠款客戶") {
                TopDebtorsHorizontalChart(debtors = topDebtors, currencyFormatter = currencyFormatter)
            }

            // Insight Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                 InsightCard(
                    title = "本月總流水",
                    value = "$totalTransactions 筆",
                    borderColor = MidnightBlue
                )
                InsightCard(
                    title = "平均回款週期 (DSO)",
                    value = "$averageCollectionPeriod 天",
                    borderColor = VibrantOrange
                )
            }
        }
    }
}

@Composable
fun InsightCard(title: String, value: String, borderColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(borderColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Medium)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if(borderColor == MidnightBlue) MidnightBlue else VibrantOrange)
            }
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(4.dp) // shadow-soft
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MidnightBlue,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            )
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
        val groupWidth = size.width / stats.size
        val barWidth = groupWidth * 0.35f
        val gap = groupWidth * 0.1f
        val height = size.height * 0.85f // Reserve bottom space for labels

        stats.forEachIndexed { index, stat ->
            val centerX = index * groupWidth + groupWidth / 2

            // Debt Bar (Left)
            val debtHeight = (stat.debt.toFloat() / maxVal) * height
            if (debtHeight > 0) {
                drawRoundRect(
                    color = DebtColor,
                    topLeft = Offset(centerX - barWidth - gap/2, height - debtHeight),
                    size = Size(barWidth, debtHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            // Payment Bar (Right)
            val paymentHeight = (stat.payment.toFloat() / maxVal) * height
            if (paymentHeight > 0) {
                 drawRoundRect(
                    color = PaymentColor,
                    topLeft = Offset(centerX + gap/2, height - paymentHeight),
                    size = Size(barWidth, paymentHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            // X-Axis Label
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    stat.month,
                    centerX,
                    size.height,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 32f // Adjust size
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }

        // Axis line
        drawLine(
            color = Color.LightGray.copy(alpha=0.5f),
            start = Offset(0f, height),
            end = Offset(size.width, height),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun AgingDonutChart(stats: List<AgingData>, modifier: Modifier) {
    if (stats.isEmpty()) return
    val total = stats.sumOf { it.amount }
    if (total == 0.0) return

    val colors = listOf(
        Color(0xFF4CAF50),
        Color(0xFFFFC107),
        Color(0xFFFF9800),
        Color(0xFFF44336)
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) {
            var startAngle = -90f
            val strokeWidth = 40.dp.toPx()

            stats.forEachIndexed { index, stat ->
                val sweepAngle = ((stat.amount / total) * 360).toFloat()
                drawArc(
                    color = colors.getOrElse(index) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun TopDebtorsHorizontalChart(debtors: List<DebtorData>, currencyFormatter: NumberFormat) {
    if (debtors.isEmpty()) return
    val maxAmount = debtors.maxOf { it.amount }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        debtors.take(5).forEach { debtor ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(debtor.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MidnightBlue)
                    Text(currencyFormatter.format(debtor.amount), style = MaterialTheme.typography.bodyMedium, color = DebtColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(BackgroundColor, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((debtor.amount / maxAmount).toFloat())
                            .height(12.dp)
                            .background(MidnightBlue, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
