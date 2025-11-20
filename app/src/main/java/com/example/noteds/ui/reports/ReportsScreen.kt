package com.example.noteds.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.noteds.ui.components.TotalDebtCard
import com.example.noteds.ui.theme.AccentOrange
import com.example.noteds.ui.theme.BackgroundGray
import com.example.noteds.ui.theme.DebtRed
import com.example.noteds.ui.theme.PaymentGreen
import com.example.noteds.ui.theme.TealPrimary
import com.example.noteds.ui.theme.TextBlack
import com.example.noteds.ui.theme.TextGray
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val snapshot by reportsViewModel.dashboardSnapshot.collectAsState()
    val topDebtors by reportsViewModel.topDebtors.collectAsState()
    val monthly by reportsViewModel.monthlyTotals.collectAsState()
    val aging by reportsViewModel.agingBuckets.collectAsState()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(BackgroundGray)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TotalDebtCard(totalDebt = snapshot.totalDebt)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("每月賒賬 vs 還款", style = MaterialTheme.typography.titleMedium, color = TextBlack)
                monthly.forEach { month ->
                    BarRow(label = month.label, debt = month.debt, payment = month.payment)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("賬齡分佈 (0-90+天)", style = MaterialTheme.typography.titleMedium, color = TextBlack)
                aging.forEach { (bucket, value) ->
                    AgingRow(bucket = bucket, value = value, total = aging.values.sum())
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Top 欠款客戶", style = MaterialTheme.typography.titleMedium, color = TextBlack)
                TopDebtorsBarChart(
                    customerDebts = topDebtors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("本月統計", style = MaterialTheme.typography.titleMedium, color = TextBlack)
                Text("新增賒賬：${currencyFormatter.format(snapshot.monthDebt)}", color = DebtRed)
                Text("已收回款：${currencyFormatter.format(snapshot.monthPayment)}", color = PaymentGreen)
            }
        }
    }
}

@Composable
private fun BarRow(label: String, debt: Double, payment: Double) {
    val max = (debt + payment).coerceAtLeast(1.0)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = TextGray)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .weight((debt / max).toFloat())
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DebtRed.copy(alpha = 0.7f))
            )
            Box(
                modifier = Modifier
                    .weight((payment / max).toFloat())
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PaymentGreen.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
private fun AgingRow(bucket: String, value: Double, total: Double) {
    val percent = if (total <= 0) 0f else (value / total).toFloat()
    val color = when (bucket) {
        "0-30" -> PaymentGreen
        "31-60" -> TealPrimary
        "61-90" -> AccentOrange
        else -> DebtRed
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(bucket, color = TextBlack)
            Text(String.format(Locale.getDefault(), "%.0f%%", percent * 100), color = TextGray)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent)
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
            )
        }
    }
}
