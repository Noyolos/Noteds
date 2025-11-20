package com.example.noteds.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TopDebtorsBarChart(
    customerDebts: List<DebtorData>, // ★ 修改這裡：改用 DebtorData
    modifier: Modifier = Modifier
) {
    if (customerDebts.isEmpty()) {
        Text(
            text = "No debt data yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    // ★ 修改這裡：it.second -> it.amount
    val maxDebt = remember(customerDebts) { customerDebts.maxOf { it.amount }.coerceAtLeast(1.0) }

    Column(modifier = modifier.fillMaxWidth()) {
        // ★ 修改這裡：解構 DebtorData
        customerDebts.take(5).forEach { debtor ->
            val progress = (debtor.amount / maxDebt).toFloat().coerceIn(0f, 1f)
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = debtor.name, // ★ name
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currencyFormatter.format(debtor.amount), // ★ amount
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}