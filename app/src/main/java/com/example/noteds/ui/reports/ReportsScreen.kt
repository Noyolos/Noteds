package com.example.noteds.ui.reports

import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.noteds.ui.components.AppScreenHeader
import com.example.noteds.ui.i18n.AppLanguage
import com.example.noteds.ui.i18n.LocalAppLanguage
import com.example.noteds.ui.i18n.pick
import com.example.noteds.ui.i18n.rememberCurrencyFormatter
import com.example.noteds.ui.theme.BackgroundColor
import com.example.noteds.ui.theme.CardSurface
import com.example.noteds.ui.theme.DebtColor
import com.example.noteds.ui.theme.MidnightBlue
import com.example.noteds.ui.theme.PaymentColor
import com.example.noteds.ui.theme.TextSecondary
import com.example.noteds.ui.theme.TextWhite
import com.example.noteds.ui.theme.VibrantOrange
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val language = LocalAppLanguage.current
    val monthlyStats by reportsViewModel.last6MonthsStats.collectAsStateWithLifecycle()
    val agingStats by reportsViewModel.agingStats.collectAsStateWithLifecycle()
    val topDebtors by reportsViewModel.topDebtors.collectAsStateWithLifecycle()
    val totalTransactions by reportsViewModel.totalTransactions.collectAsStateWithLifecycle()
    val averageCollectionPeriod by reportsViewModel.averageCollectionPeriod.collectAsStateWithLifecycle()

    val currencyFormatter = rememberCurrencyFormatter()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            reportsViewModel.exportBackup(it) { success, _ ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (success) language.pick("备份完成", "Backup completed")
                        else language.pick("备份失败", "Backup failed")
                    )
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            reportsViewModel.importBackup(it) { success, _ ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (success) language.pick("导入完成", "Import completed")
                        else language.pick("导入失败", "Import failed")
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AppScreenHeader(
                title = language.pick("财务报表", "Financial Reports"),
                subtitle = language.pick("备份、趋势和回款概览", "Backups, trends, and collection overview")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { exportLauncher.launch("noteds_backup.zip") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(language.pick("导出备份", "Export backup"), color = TextWhite, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { importLauncher.launch("application/zip") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(language.pick("导入备份", "Import backup"), fontWeight = FontWeight.Bold)
                }
            }

            ChartCard(
                title = language.pick("每月 Debt vs Payment", "Monthly Debt vs Payment"),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                MonthlyBarChart(stats = monthlyStats, modifier = Modifier.fillMaxWidth().height(240.dp))
            }

            ChartCard(
                title = language.pick("账龄分布 (0-90+天)", "Aging Distribution (0-90+ days)"),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                AgingDonutChart(stats = agingStats, modifier = Modifier.fillMaxWidth().height(200.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    agingStats.forEach { stat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(stat.colorHex), androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val label = stat.bucket.replace(" Days", "")
                            Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            ChartCard(
                title = language.pick("Top 欠款客户", "Top Debtors"),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                TopDebtorsHorizontalChart(debtors = topDebtors, currencyFormatter = currencyFormatter)
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InsightCard(
                    title = language.pick("本月总交易数", "Transactions this month"),
                    value = language.pick("$totalTransactions 笔", "$totalTransactions entries"),
                    borderColor = MidnightBlue
                )
                InsightCard(
                    title = language.pick("平均回款周期", "Average collection period"),
                    value = if (language == AppLanguage.ENGLISH) {
                        String.format(Locale.ENGLISH, "%.0f days", averageCollectionPeriod)
                    } else {
                        String.format(Locale.SIMPLIFIED_CHINESE, "%.0f 天", averageCollectionPeriod)
                    },
                    borderColor = VibrantOrange
                )
            }

            LanguageCard(
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun LanguageCard(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Language",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MidnightBlue
            )
            Text(
                text = language.pick("切换后整个 app 会立即更新语言。", "Changing this updates the whole app immediately."),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppLanguage.entries.forEach { option ->
                    val selected = option == currentLanguage
                    OutlinedButton(
                        onClick = { onLanguageChange(option) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MidnightBlue.copy(alpha = 0.08f) else Color.Transparent,
                            contentColor = if (selected) MidnightBlue else TextSecondary
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(option.displayLabel, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCard(title: String, value: String, borderColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(4.dp)
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
                Text(title, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Medium)
                Text(value, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (borderColor == MidnightBlue) MidnightBlue else VibrantOrange)
            }
        }
    }
}

@Composable
fun ChartCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                title,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MidnightBlue,
                modifier = Modifier.fillMaxWidth()
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
        val labelSpace = 24.dp.toPx()
        val chartHeight = size.height - labelSpace
        val textPaint = Paint().apply {
            color = Color.Gray.toArgb()
            textSize = 12.sp.toPx()
            textAlign = Paint.Align.CENTER
        }

        stats.forEachIndexed { index, stat ->
            val centerX = index * groupWidth + groupWidth / 2
            val debtHeight = (stat.debt.toFloat() / maxVal) * chartHeight * 0.9f
            if (debtHeight > 0) {
                drawRoundRect(
                    color = DebtColor,
                    topLeft = Offset(centerX - barWidth - gap / 2, chartHeight - debtHeight),
                    size = Size(barWidth, debtHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            val paymentHeight = (stat.payment.toFloat() / maxVal) * chartHeight * 0.9f
            if (paymentHeight > 0) {
                drawRoundRect(
                    color = PaymentColor,
                    topLeft = Offset(centerX + gap / 2, chartHeight - paymentHeight),
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

        drawLine(
            color = Color.LightGray.copy(alpha = 0.5f),
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
                    Text(debtor.name, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MidnightBlue)
                    Text(currencyFormatter.format(debtor.amount), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = DebtColor, fontWeight = FontWeight.Bold)
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
