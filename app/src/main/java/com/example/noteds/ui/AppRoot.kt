package com.example.noteds.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
            BottomDestination("首頁", Icons.Default.Home),
            BottomDestination("客戶", Icons.Default.People),
            BottomDestination("報表", Icons.Default.PieChart)
        )
    }
    val selectedIndex = remember { mutableIntStateOf(0) }

    var screenStack by remember { mutableStateOf(listOf<Screen>(Screen.Main)) }

    val currentScreen = screenStack.lastOrNull() ?: Screen.Main

    fun navigateTo(screen: Screen) {
        screenStack = if (screen == Screen.Main) {
            listOf(Screen.Main)
        } else {
            screenStack + screen
        }
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack = screenStack.dropLast(1)
        }
    }

    BackHandler(enabled = screenStack.size > 1) {
        navigateBack()
    }

    when (val screen = currentScreen) {
        is Screen.Main -> {
            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        destinations.forEachIndexed { index, destination ->
                            NavigationBarItem(
                                selected = selectedIndex.intValue == index,
                                onClick = {
                                    selectedIndex.intValue = index
                                    navigateTo(Screen.Main)
                                },
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
                            onCustomerClick = { id ->
                                navigateTo(Screen.CustomerDetail(id))
                            }
                        )
                        1 -> CustomerListScreen(
                            customerViewModel = customerViewModel,
                            onCustomerClick = {
                                navigateTo(Screen.CustomerDetail(it.customer.id))
                            },
                            onAddCustomerClick = { navigateTo(Screen.AddCustomer) }
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
                onBack = { navigateBack() },
                onSaved = {
                    navigateTo(Screen.Main)
                }
            )
        }
        is Screen.CustomerDetail -> {
            CustomerDetailScreen(
                customerId = screen.customerId,
                customerViewModel = customerViewModel,
                onClose = { navigateTo(Screen.Main) },
                onEditClick = { id -> navigateTo(Screen.EditCustomer(id)) }
            )
        }
        is Screen.EditCustomer -> {
            EditCustomerScreen(
                customerId = screen.customerId,
                customerViewModel = customerViewModel,
                onBack = { navigateBack() },
                onSaved = { navigateTo(Screen.CustomerDetail(screen.customerId)) }
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
