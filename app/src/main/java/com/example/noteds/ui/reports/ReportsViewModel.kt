package com.example.noteds.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Date

data class DebtorData(
    val id: Long,
    val name: String,
    val amount: Double
)

data class MonthlyStats(
    val month: String,
    val debt: Double,
    val payment: Double
)

data class AgingData(
    val bucket: String,
    val amount: Double,
    val colorHex: Long
)

class ReportsViewModel(
    private val customerRepository: CustomerRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val customersWithBalance: StateFlow<List<DebtorData>> =
        combineCustomerBalances(
            customerRepository.getAllCustomers(),
            ledgerRepository.getAllEntries()
        )
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val totalDebt: StateFlow<Double> = customersWithBalance
        .map { list ->
            list.filter { it.amount > 0.0 }
                .sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val topDebtors: StateFlow<List<DebtorData>> = customersWithBalance
        .map { list ->
            list.filter { it.amount > 0.0 }
                .sortedByDescending { it.amount }
                .take(10) // Top 10 as requested
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val debtThisMonth: StateFlow<Double> = ledgerRepository.getAllEntries()
        .map { entries ->
            val (start, end) = getMonthRange()
            entries.filter { it.timestamp in start..end && it.type.uppercase() == "DEBT" }
                .sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val repaymentThisMonth: StateFlow<Double> = ledgerRepository.getAllEntries()
        .map { entries ->
            val (start, end) = getMonthRange()
            entries.filter { it.timestamp in start..end && it.type.uppercase() == "PAYMENT" }
                .sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    // Monthly Comparison (Last 6 Months)
    val last6MonthsStats: StateFlow<List<MonthlyStats>> = ledgerRepository.getAllEntries()
        .map { entries ->
            calculateLast6Months(entries)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Aging Distribution (Simplistic logic based on entry timestamp)
    // Note: True aging requires FIFO matching of payments to debts.
    // For this prototype, we will group outstanding debts by their creation date, assuming no payments were made (which is inaccurate but visualizes buckets).
    // A better approach for simplified visualization:
    // Group all DEBT entries that are not fully paid. But without FIFO, it's hard.
    // Alternative: Just show aging of *Transactions* (recent vs old debts) regardless of repayment status, or total debt split by time buckets.
    // Let's try to estimate: Sum of debts in buckets.
    val agingStats: StateFlow<List<AgingData>> = ledgerRepository.getAllEntries()
        .map { entries ->
            calculateAging(entries)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val totalTransactions: StateFlow<Int> = ledgerRepository.getAllEntries()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    private fun calculateLast6Months(entries: List<LedgerEntryEntity>): List<MonthlyStats> {
        val calendar = Calendar.getInstance()
        val stats = mutableListOf<MonthlyStats>()

        for (i in 0..5) {
            val month = calendar.get(Calendar.MONTH) // 0-11
            val year = calendar.get(Calendar.YEAR)

            // Start/End of this month
            val calStart = calendar.clone() as Calendar
            calStart.set(Calendar.DAY_OF_MONTH, 1)
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)

            val calEnd = calendar.clone() as Calendar
            calEnd.add(Calendar.MONTH, 1)
            calEnd.set(Calendar.DAY_OF_MONTH, 1)
            calEnd.add(Calendar.MILLISECOND, -1)

            val start = calStart.timeInMillis
            val end = calEnd.timeInMillis

            val monthEntries = entries.filter { it.timestamp in start..end }
            val debt = monthEntries.filter { it.type.uppercase() == "DEBT" }.sumOf { it.amount }
            val payment = monthEntries.filter { it.type.uppercase() == "PAYMENT" }.sumOf { it.amount }

            val monthName = String.format(java.util.Locale.US, "%tb", calStart)
            stats.add(0, MonthlyStats(monthName, debt, payment)) // Add to front to have chronological order

            calendar.add(Calendar.MONTH, -1)
        }
        return stats
    }

    private fun calculateAging(entries: List<LedgerEntryEntity>): List<AgingData> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        val debts = entries.filter { it.type.uppercase() == "DEBT" }

        var bucket0to30 = 0.0
        var bucket31to60 = 0.0
        var bucket61to90 = 0.0
        var bucket90plus = 0.0

        debts.forEach { entry ->
            val age = (now - entry.timestamp) / day
            when {
                age <= 30 -> bucket0to30 += entry.amount
                age <= 60 -> bucket31to60 += entry.amount
                age <= 90 -> bucket61to90 += entry.amount
                else -> bucket90plus += entry.amount
            }
        }

        return listOf(
            AgingData("0-30 Days", bucket0to30, 0xFF00C853),
            AgingData("31-60 Days", bucket31to60, 0xFFFFAB00),
            AgingData("61-90 Days", bucket61to90, 0xFFFF6D00),
            AgingData("90+ Days", bucket90plus, 0xFFD50000)
        )
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun combineCustomerBalances(
        customersFlow: Flow<List<CustomerEntity>>,
        entriesFlow: Flow<List<LedgerEntryEntity>>
    ): Flow<List<DebtorData>> =
        combine(customersFlow, entriesFlow) { customers, entries ->
            val balanceByCustomer = entries.groupBy { it.customerId }
                .mapValues { (_, customerEntries) ->
                    customerEntries.fold(0.0) { acc, entry ->
                        val delta = when (entry.type.uppercase()) {
                            "DEBT" -> entry.amount
                            "PAYMENT" -> -entry.amount
                            else -> 0.0
                        }
                        acc + delta
                    }
                }

            customers.map { customer ->
                DebtorData(
                    id = customer.id,
                    name = customer.name,
                    amount = balanceByCustomer[customer.id] ?: 0.0
                )
            }
        }
}
