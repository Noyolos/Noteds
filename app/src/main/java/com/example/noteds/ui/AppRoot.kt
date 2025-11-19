package com.example.noteds.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.noteds.ui.customers.CustomerListScreen
import com.example.noteds.ui.customers.CustomerDetailScreen
import com.example.noteds.ui.customers.CustomerViewModel
import com.example.noteds.ui.reports.ReportsScreen
import com.example.noteds.ui.reports.ReportsViewModel

@Composable
fun AppRoot(
    customerViewModel: CustomerViewModel,
    reportsViewModel: ReportsViewModel
) {
    val destinations = remember {
        listOf(
            BottomDestination("Customers", Icons.Outlined.People),
            BottomDestination("Reports", Icons.Outlined.PieChart)
        )
    }
    val selectedIndex = rememberSaveable { mutableIntStateOf(0) }
    val selectedCustomerId = rememberSaveable { mutableStateOf<Long?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    destinations.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = selectedIndex.intValue == index,
                            onClick = { selectedIndex.intValue = index },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors()
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedIndex.intValue) {
                    0 -> CustomerListScreen(
                        customerViewModel = customerViewModel,
                        totalDebtFlow = reportsViewModel.totalDebt,
                        onCustomerClick = { selectedCustomerId.value = it.customer.id }
                    )

                    else -> ReportsScreen(reportsViewModel = reportsViewModel)
                }
            }
        }

        selectedCustomerId.value?.let { id ->
            CustomerDetailScreen(
                customerId = id,
                customerViewModel = customerViewModel,
                onClose = { selectedCustomerId.value = null }
            )
        }
    }
}

data class BottomDestination(
    val label: String,
    val icon: ImageVector
)
