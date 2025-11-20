package com.example.noteds.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.noteds.ui.customers.CustomerListScreen
import com.example.noteds.ui.customers.CustomerDetailScreen
import com.example.noteds.ui.customers.CustomerViewModel
import com.example.noteds.ui.home.DashboardScreen
import com.example.noteds.ui.reports.ReportsScreen
import com.example.noteds.ui.reports.ReportsViewModel
import com.example.noteds.ui.theme.TealPrimary

@Composable
fun AppRoot(
    customerViewModel: CustomerViewModel,
    reportsViewModel: ReportsViewModel
) {
    // ... (前段不變) ...
    val destinations = remember {
        listOf(
            BottomDestination("首頁", Icons.Default.Home),
            BottomDestination("客人", Icons.Default.People),
            BottomDestination("報表", Icons.Default.BarChart)
        )
    }
    val selectedIndex = rememberSaveable { mutableIntStateOf(0) }
    val selectedCustomerId = rememberSaveable { mutableStateOf<Long?>(null) }
    val customers by customerViewModel.customersWithBalance.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    destinations.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = selectedIndex.intValue == index,
                            onClick = { selectedIndex.intValue = index },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealPrimary,
                                selectedTextColor = TealPrimary,
                                indicatorColor = TealPrimary.copy(alpha = 0.1f)
                            )
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
                    0 -> DashboardScreen(
                        reportsViewModel = reportsViewModel,
                        customers = customers,
                        onCustomerClick = { id -> selectedCustomerId.value = id }
                    )

                    1 -> CustomerListScreen(
                        customerViewModel = customerViewModel,
                        onCustomerClick = { selectedCustomerId.value = it.customer.id }
                    )

                    2 -> ReportsScreen(
                        reportsViewModel = reportsViewModel
                    )
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