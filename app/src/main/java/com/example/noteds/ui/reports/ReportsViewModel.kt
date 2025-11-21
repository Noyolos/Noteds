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

    // Aging Distribution (FIFO logic)
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

    val averageCollectionPeriod: StateFlow<Double> = ledgerRepository.getAllEntries()
        .map { entries ->
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

            val totalDebt = entries.filter { it.type.uppercase() == "DEBT" }.sumOf { it.amount }
            val totalPayment = entries.filter { it.type.uppercase() == "PAYMENT" }.sumOf { it.amount }
            val outstanding = (totalDebt - totalPayment).coerceAtLeast(0.0)

            val recentDebt = entries
                .filter { it.type.uppercase() == "DEBT" && it.timestamp >= thirtyDaysAgo }
                .sumOf { it.amount }

            if (recentDebt == 0.0) 0.0 else (outstanding / recentDebt) * 30.0
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    // Real Trend Data (Last 6 Months Total Outstanding Debt)
    val totalDebtTrend: StateFlow<List<Float>> = ledgerRepository.getAllEntries()
        .map { entries ->
            calculateDebtTrend(entries)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private fun calculateLast6Months(entries: List<LedgerEntryEntity>): List<MonthlyStats> {
        val calendar = Calendar.getInstance()
        val stats = mutableListOf<MonthlyStats>()

        for (i in 0..5) {
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
        // FIFO Logic
        // 1. Get Total Payments
        var totalPayments = entries.filter { it.type.uppercase() == "PAYMENT" }.sumOf { it.amount }

        // 2. Get Debts sorted by oldest first
        val debts = entries.filter { it.type.uppercase() == "DEBT" }.sortedBy { it.timestamp }

        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        var bucket0to30 = 0.0
        var bucket31to60 = 0.0
        var bucket61to90 = 0.0
        var bucket90plus = 0.0

        for (debt in debts) {
            if (totalPayments >= debt.amount) {
                // Fully paid
                totalPayments -= debt.amount
            } else {
                // Partially paid or unpaid
                val remainingDebt = debt.amount - totalPayments
                totalPayments = 0.0

                // Bucket the remaining debt based on age
                val age = (now - debt.timestamp) / day
                when {
                    age <= 30 -> bucket0to30 += remainingDebt
                    age <= 60 -> bucket31to60 += remainingDebt
                    age <= 90 -> bucket61to90 += remainingDebt
                    else -> bucket90plus += remainingDebt
                }
            }
        }

        return listOf(
            AgingData("0-30 Days", bucket0to30, 0xFF00C853), // Green
            AgingData("31-60 Days", bucket31to60, 0xFFFFAB00), // Yellow
            AgingData("61-90 Days", bucket61to90, 0xFFFF6D00), // Orange
            AgingData("90+ Days", bucket90plus, 0xFFD50000)   // Red
        )
    }

    private fun calculateDebtTrend(entries: List<LedgerEntryEntity>): List<Float> {
        val calendar = Calendar.getInstance()
        // Go to end of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1) // End of current month

        val points = mutableListOf<Double>()

        // Calculate for last 6 months (including current)
        for (i in 0..5) {
            val endTime = calendar.timeInMillis

            val debtsUntilNow = entries.filter { it.timestamp <= endTime && it.type.uppercase() == "DEBT" }.sumOf { it.amount }
            val paymentsUntilNow = entries.filter { it.timestamp <= endTime && it.type.uppercase() == "PAYMENT" }.sumOf { it.amount }
            val outstanding = (debtsUntilNow - paymentsUntilNow).coerceAtLeast(0.0)

            points.add(0, outstanding) // Add to front (oldest first)

            // Move to previous month
            calendar.add(Calendar.MONTH, -1)
        }

        // Normalize to 0.0 - 1.0
        val maxVal = points.maxOrNull() ?: 1.0
        val minVal = points.minOrNull() ?: 0.0
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal

        // If all are zero, return straight line 0.5
        if (maxVal == 0.0) return listOf(0f, 0f, 0f, 0f, 0f, 0f)

        return points.map {
             // Normalize relative to max to fill chart, but keep 0 at bottom if relevant?
             // Usually trend line fits min-max.
             // Let's normalize between min and max.
             // If we want 0 to be bottom, normalize value / maxVal.
             ((it - minVal) / range).toFloat()
             // Or simple ratio if we want absolute scale?
             // Design usually prefers relative shape.
        }
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
