package com.example.noteds.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
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
    customerDebts: List<Pair<String, Double>>,
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
    val maxDebt = remember(customerDebts) { customerDebts.maxOf { it.second }.coerceAtLeast(1.0) }

    Column(modifier = modifier.fillMaxWidth()) {
        customerDebts.take(5).forEach { (name, amount) ->
            val progress = (amount / maxDebt).toFloat().coerceIn(0f, 1f)
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currencyFormatter.format(amount),
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
