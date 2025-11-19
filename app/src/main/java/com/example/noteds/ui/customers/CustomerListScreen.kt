package com.example.noteds.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.noteds.data.model.CustomerWithBalance
import com.example.noteds.ui.components.CustomerRow
import com.example.noteds.ui.components.ModernCard
import com.example.noteds.ui.components.TotalDebtCard
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CustomerListScreen(
    customerViewModel: CustomerViewModel,
    totalDebtFlow: StateFlow<Double>,
    modifier: Modifier = Modifier,
    onCustomerClick: (CustomerWithBalance) -> Unit = {}
) {
    val customers by customerViewModel.customersWithBalance.collectAsState()
    val totalDebt by totalDebtFlow.collectAsState(initial = 0.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            TotalDebtCard(totalDebt = totalDebt)
        }

        if (customers.isEmpty()) {
            item {
                EmptyState()
            }
        } else {
            items(customers, key = { it.customer.id }) { customer ->
                CustomerRow(
                    item = customer,
                    onClick = { onCustomerClick(customer) }
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    ModernCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No customers yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Add customers to start tracking balances.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
