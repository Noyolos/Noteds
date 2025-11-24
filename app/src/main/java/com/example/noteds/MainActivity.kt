package com.example.noteds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.noteds.di.AppContainer
import com.example.noteds.ui.AppRoot
import com.example.noteds.ui.customers.CustomerViewModel
import com.example.noteds.ui.reports.ReportsViewModel

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var reportsViewModel: ReportsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = (application as NotedsApp).container
        customerViewModel = ViewModelProvider(this, CustomerViewModelFactory(appContainer))
            [CustomerViewModel::class.java]
        reportsViewModel = ViewModelProvider(this, ReportsViewModelFactory(appContainer))
            [ReportsViewModel::class.java]
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        customerViewModel = customerViewModel,
                        reportsViewModel = reportsViewModel
                    )
                }
            }
        }
    }
}

private class CustomerViewModelFactory(
    private val appContainer: com.example.noteds.di.AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerViewModel(
                customerRepository = appContainer.customerRepository,
                ledgerRepository = appContainer.ledgerRepository,
                backupRepository = appContainer.backupRepository, // 修复：传入 BackupRepository
                appContext = appContainer.appContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${'$'}modelClass")
    }
}

private class ReportsViewModelFactory(
    private val appContainer: com.example.noteds.di.AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(
                customerRepository = appContainer.customerRepository,
                ledgerRepository = appContainer.ledgerRepository,
                backupRepository = appContainer.backupRepository,
                appContext = appContainer.appContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${'$'}modelClass")
    }
}