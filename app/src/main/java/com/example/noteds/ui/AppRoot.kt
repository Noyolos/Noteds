package com.example.noteds.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.noteds.ui.customers.AddCustomerScreen
import com.example.noteds.ui.customers.CustomerDetailScreen
import com.example.noteds.ui.customers.CustomerListScreen
import com.example.noteds.ui.customers.CustomerViewModel
import com.example.noteds.ui.customers.EditCustomerScreen
import com.example.noteds.ui.home.DashboardScreen
import com.example.noteds.ui.i18n.AppLanguage
import com.example.noteds.ui.i18n.LocalAppLanguage
import com.example.noteds.ui.i18n.loadAppLanguage
import com.example.noteds.ui.i18n.pick
import com.example.noteds.ui.i18n.saveAppLanguage
import com.example.noteds.ui.reports.ReportsScreen
import com.example.noteds.ui.reports.ReportsViewModel
import com.example.noteds.ui.theme.MidnightBlue
import com.example.noteds.ui.theme.VibrantOrange

@Composable
fun AppRoot(
    customerViewModel: CustomerViewModel,
    reportsViewModel: ReportsViewModel
) {
    val context = LocalContext.current
    var appLanguage by remember { mutableStateOf(context.loadAppLanguage()) }
    val destinations = remember {
        listOf(
            BottomDestination(BottomTab.HOME, Icons.Default.Home),
            BottomDestination(BottomTab.CUSTOMERS, Icons.Default.People),
            BottomDestination(BottomTab.REPORTS, Icons.Default.PieChart)
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

    CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
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
                                    label = {
                                        Text(
                                            when (destination.tab) {
                                                BottomTab.HOME -> appLanguage.pick("首页", "Home")
                                                BottomTab.CUSTOMERS -> appLanguage.pick("客户", "Customers")
                                                BottomTab.REPORTS -> appLanguage.pick("报表", "Reports")
                                            }
                                        )
                                    },
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
                                parentId = null,
                                onCustomerClick = { item ->
                                    if (item.customer.isGroup) {
                                        navigateTo(Screen.GroupList(item.customer.id))
                                    } else {
                                        navigateTo(Screen.CustomerDetail(item.customer.id))
                                    }
                                },
                                onAddCustomerClick = { navigateTo(Screen.AddCustomer(parentId = null)) }
                            )

                            2 -> ReportsScreen(
                                reportsViewModel = reportsViewModel,
                                currentLanguage = appLanguage,
                                onLanguageChange = { language ->
                                    appLanguage = language
                                    context.saveAppLanguage(language)
                                }
                            )
                        }
                    }
                }
            }

            is Screen.GroupList -> {
                CustomerListScreen(
                    customerViewModel = customerViewModel,
                    parentId = screen.groupCustomerId,
                    onBack = { navigateBack() },
                    onCustomerClick = { item ->
                        if (item.customer.isGroup) {
                            navigateTo(Screen.GroupList(item.customer.id))
                        } else {
                            navigateTo(Screen.CustomerDetail(item.customer.id))
                        }
                    },
                    onAddCustomerClick = {
                        navigateTo(Screen.AddCustomer(parentId = screen.groupCustomerId))
                    }
                )
            }

            is Screen.AddCustomer -> {
                AddCustomerScreen(
                    customerViewModel = customerViewModel,
                    parentId = screen.parentId,
                    onBack = { navigateBack() },
                    onSaved = {
                        navigateBack()
                    }
                )
            }

            is Screen.CustomerDetail -> {
                CustomerDetailScreen(
                    customerId = screen.customerId,
                    customerViewModel = customerViewModel,
                    onClose = { navigateBack() },
                    onEditClick = { id -> navigateTo(Screen.EditCustomer(id)) }
                )
            }

            is Screen.EditCustomer -> {
                EditCustomerScreen(
                    customerId = screen.customerId,
                    customerViewModel = customerViewModel,
                    onBack = { navigateBack() },
                    onSaved = { navigateBack() }
                )
            }
        }
    }
}

sealed class Screen {
    object Main : Screen()
    data class AddCustomer(val parentId: Long?) : Screen()
    data class CustomerDetail(val customerId: Long) : Screen()
    data class EditCustomer(val customerId: Long) : Screen()
    data class GroupList(val groupCustomerId: Long) : Screen()
}

data class BottomDestination(
    val tab: BottomTab,
    val icon: ImageVector
)

enum class BottomTab {
    HOME,
    CUSTOMERS,
    REPORTS
}
