package com.example.noteds.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.noteds.ui.components.ModernCard
import com.example.noteds.ui.components.TotalDebtCard
import com.example.noteds.ui.reports.TopDebtorsBarChart

@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val totalDebt by reportsViewModel.totalDebt.collectAsState()
    val topDebtors by reportsViewModel.topDebtors.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TotalDebtCard(totalDebt = totalDebt)

        ModernCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Top Debtors",
                    style = MaterialTheme.typography.titleMedium
                )
                TopDebtorsBarChart(
                    customerDebts = topDebtors,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}
