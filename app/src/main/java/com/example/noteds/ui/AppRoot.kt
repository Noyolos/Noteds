package com.example.noteds.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.noteds.ui.customers.AddCustomerScreen
import com.example.noteds.ui.customers.CustomerDetailScreen
import com.example.noteds.ui.customers.CustomerListScreen
import com.example.noteds.ui.customers.CustomerViewModel
import com.example.noteds.ui.customers.EditCustomerScreen
import com.example.noteds.ui.home.DashboardScreen
import com.example.noteds.ui.reports.ReportsScreen
import com.example.noteds.ui.reports.ReportsViewModel
import com.example.noteds.ui.theme.MidnightBlue
import com.example.noteds.ui.theme.VibrantOrange

@Composable
fun AppRoot(
    customerViewModel: CustomerViewModel,
    reportsViewModel: ReportsViewModel
) {
    val destinations = remember {
        listOf(
            BottomDestination("Dashboard", Icons.Default.Home),
            BottomDestination("Customers", Icons.Default.People),
            BottomDestination("Reports", Icons.Default.BarChart)
        )
    }
    val selectedIndex = rememberSaveable { mutableIntStateOf(0) }

    // Navigation State
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

    // Helper to reset to main
    fun navigateToMain() { currentScreen = Screen.Main }

    when (val screen = currentScreen) {
        is Screen.Main -> {
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
                                    selectedIconColor = MidnightBlue,
                                    selectedTextColor = MidnightBlue,
                                    indicatorColor = VibrantOrange.copy(alpha = 0.2f)
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
                            onCustomerClick = { id -> currentScreen = Screen.CustomerDetail(id) }
                        )
                        1 -> CustomerListScreen(
                            customerViewModel = customerViewModel,
                            onCustomerClick = { currentScreen = Screen.CustomerDetail(it.customer.id) },
                            onAddCustomerClick = { currentScreen = Screen.AddCustomer }
                        )
                        2 -> ReportsScreen(
                            reportsViewModel = reportsViewModel
                        )
                    }
                }
            }
        }
        is Screen.AddCustomer -> {
            AddCustomerScreen(
                customerViewModel = customerViewModel,
                onBack = { navigateToMain() },
                onSaved = { navigateToMain() }
            )
        }
        is Screen.CustomerDetail -> {
            CustomerDetailScreen(
                customerId = screen.customerId,
                customerViewModel = customerViewModel,
                onClose = { navigateToMain() },
                onEditClick = { id -> currentScreen = Screen.EditCustomer(id) }
            )
        }
        is Screen.EditCustomer -> {
            EditCustomerScreen(
                customerId = screen.customerId,
                customerViewModel = customerViewModel,
                onBack = { currentScreen = Screen.CustomerDetail(screen.customerId) },
                onSaved = { currentScreen = Screen.CustomerDetail(screen.customerId) }
            )
        }
    }
}

sealed class Screen {
    object Main : Screen()
    object AddCustomer : Screen()
    data class CustomerDetail(val customerId: Long) : Screen()
    data class EditCustomer(val customerId: Long) : Screen()
}

data class BottomDestination(
    val label: String,
    val icon: ImageVector
)
