package com.example.noteds.ui.reports

import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            reportsViewModel.exportBackup(it) { success, message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message ?: if (success) "備份完成" else "備份失敗")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            reportsViewModel.importBackup(it) { success, message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message ?: if (success) "導入完成" else "導入失敗")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("財務報表", color = TextWhite, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue)
            )
        },
        containerColor = BackgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                 // HTML has header "財務報表" and subtitle "全面經營分析" inside the blue header.
                 // In Compose, I put "財務報表" in TopAppBar.
                 // I can add subtitle here or rely on TopAppBar.
                 // HTML style has centered text.
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { exportLauncher.launch("noteds_backup.zip") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue)
                ) {
                    Text("匯出備份", color = TextWhite, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { importLauncher.launch("application/zip") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("匯入備份", fontWeight = FontWeight.Bold)
                }
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
                            // Extract "0-30" from "0-30 Days"
                            val label = stat.bucket.replace(" Days", "")
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
                    title = "平均回款週期",
                    value = String.format(Locale.getDefault(), "%.0f 天", averageCollectionPeriod),
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
    // ... (Same logic, maybe colors check)
    // HTML colors: Debt #D50000, Payment #00C853
    // My Color.kt: DebtColor=#D50000, PaymentColor=#00C853. Correct.

    if (stats.isEmpty()) return

    Canvas(modifier = modifier) {
        val maxVal = stats.maxOfOrNull { maxOf(it.debt, it.payment) }?.toFloat() ?: 1f
        // In HTML chart.js, bars are side-by-side per month.
        // Here we draw them.
        val groupWidth = size.width / stats.size
        val barWidth = groupWidth * 0.35f
        val gap = groupWidth * 0.1f
        val labelSpace = 24.dp.toPx()
        val chartHeight = size.height - labelSpace
        val textPaint = Paint().apply {
            color = Color.Gray.toArgb()
            textSize = 12.sp.toPx()
            textAlign = Paint.Align.CENTER
        }

        stats.forEachIndexed { index, stat ->
            val centerX = index * groupWidth + groupWidth / 2

            // Debt Bar (Left)
            val debtHeight = (stat.debt.toFloat() / maxVal) * chartHeight * 0.9f // Scale 0.9 to leave top space
            if (debtHeight > 0) {
                drawRoundRect(
                    color = DebtColor,
                    topLeft = Offset(centerX - barWidth - gap/2, chartHeight - debtHeight),
                    size = Size(barWidth, debtHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            // Payment Bar (Right)
            val paymentHeight = (stat.payment.toFloat() / maxVal) * chartHeight * 0.9f
            if (paymentHeight > 0) {
                 drawRoundRect(
                    color = PaymentColor,
                    topLeft = Offset(centerX + gap/2, chartHeight - paymentHeight),
                    size = Size(barWidth, paymentHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            drawContext.canvas.nativeCanvas.drawText(
                stat.month,
                centerX,
                chartHeight + textPaint.textSize,
                textPaint
            )
        }
        // Axis line?
        drawLine(
            color = Color.LightGray.copy(alpha=0.5f),
            start = Offset(0f, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun AgingDonutChart(stats: List<AgingData>, modifier: Modifier) {
    if (stats.isEmpty()) return
    val total = stats.sumOf { it.amount }
    if (total == 0.0) return

    // HTML Colors:
    // 0-30: #4CAF50 (Green)
    // 31-60: #FFC107 (Amber)
    // 61-90: #FF9800 (Orange)
    // 90+: #F44336 (Red)

    // Override colors locally to match HTML strictly if not matched in VM
    // VM uses: 0xFF00C853, 0xFFFFAB00, 0xFFFF6D00, 0xFFD50000
    // HTML uses: #4CAF50, #FFC107, #FF9800, #F44336
    // I will update logic to use these specific colors.

    val colors = listOf(
        Color(0xFF4CAF50),
        Color(0xFFFFC107),
        Color(0xFFFF9800),
        Color(0xFFF44336)
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) { // Size similar to HTML h-48
            var startAngle = -90f
            val strokeWidth = 40.dp.toPx() // thicker

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
        // Cutout center text? HTML doesn't seem to have text inside aging chart, only legend below.
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
                        .height(12.dp) // Thicker bars
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
