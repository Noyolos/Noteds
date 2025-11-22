package com.example.noteds.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
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
            BottomDestination("首頁", Icons.Default.Home),
            BottomDestination("客戶", Icons.Default.People),
            BottomDestination("報表", Icons.Default.PieChart)
        )
    }
    val selectedIndex = rememberSaveable { mutableIntStateOf(0) }

    val screenStack = rememberSaveable(
        saver = listSaver(
            save = { list ->
                list.map {
                    when (it) {
                        Screen.Main -> mapOf("type" to "main")
                        Screen.AddCustomer -> mapOf("type" to "add")
                        is Screen.CustomerDetail -> mapOf("type" to "detail", "id" to it.customerId)
                        is Screen.EditCustomer -> mapOf("type" to "edit", "id" to it.customerId)
                    }
                }
            },
            restore = { saved ->
                saved.mapNotNull {
                    when (it["type"]) {
                        "main" -> Screen.Main
                        "add" -> Screen.AddCustomer
                        "detail" -> (it["id"] as? Number)?.toLong()?.let(Screen::CustomerDetail)
                        "edit" -> (it["id"] as? Number)?.toLong()?.let(Screen::EditCustomer)
                        else -> null
                    }
                }
                    .toMutableList()
            }
        )
    ) { mutableListOf(Screen.Main) }

    val currentScreen = screenStack.lastOrNull() ?: Screen.Main

    fun navigateTo(screen: Screen) {
        if (screen == Screen.Main) {
            screenStack.clear()
            screenStack.add(Screen.Main)
        } else {
            screenStack.add(screen)
        }
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack.removeLast()
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
