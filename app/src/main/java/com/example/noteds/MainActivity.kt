package com.example.noteds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.noteds.ui.AppRoot
import com.example.noteds.ui.customers.CustomerViewModel
import com.example.noteds.ui.reports.ReportsViewModel

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { (application as NotedsApp).container }

    private val customerViewModel: CustomerViewModel by viewModels {
        CustomerViewModelFactory(appContainer)
    }

    private val reportsViewModel: ReportsViewModel by viewModels {
        ReportsViewModelFactory(appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                appContext = appContainer.appContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${'$'}modelClass")
    }
}
