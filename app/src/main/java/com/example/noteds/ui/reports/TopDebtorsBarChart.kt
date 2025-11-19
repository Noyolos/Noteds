package com.example.noteds.ui.reports

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlin.math.roundToInt

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

    val chartModelProducer = remember { CartesianChartModelProducer() }
    val truncatedNames = remember(customerDebts) {
        customerDebts.map { (name, _) ->
            if (name.length <= 10) name else name.take(9) + "\u2026"
        }
    }

    val bottomAxisValueFormatter = remember(truncatedNames) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.roundToInt().coerceIn(truncatedNames.indices)
            truncatedNames[index]
        }
    }

    LaunchedEffect(customerDebts) {
        chartModelProducer.runTransaction {
            columnSeries {
                series(customerDebts.map { (_, balance) -> balance.coerceAtLeast(0.0) })
            }
        }
    }

    ProvideVicoTheme(theme = rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisValueFormatter,
                    labelRotationDegrees = 0f
                )
            ),
            modelProducer = chartModelProducer,
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    }
}
