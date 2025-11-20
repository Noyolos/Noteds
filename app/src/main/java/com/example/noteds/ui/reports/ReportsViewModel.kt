package com.example.noteds.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// 定義排行榜用資料類別，包含 ID
data class DebtorData(
    val id: Long,
    val name: String,
    val amount: Double
)

data class DashboardSnapshot(
    val totalDebt: Double = 0.0,
    val monthDebt: Double = 0.0,
    val monthPayment: Double = 0.0,
    val topDebtors: List<DebtorData> = emptyList()
)

data class MonthlyTotal(
    val label: String,
    val debt: Double,
    val payment: Double
)

class ReportsViewModel(
    private val customerRepository: CustomerRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val customersFlow: StateFlow<List<CustomerEntity>> =
        customerRepository.getAllCustomers()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val ledgerEntries: StateFlow<List<LedgerEntryEntity>> =
        ledgerRepository.getAllEntries()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // 回傳 DebtorData 列表
    private val customersWithBalance: StateFlow<List<DebtorData>> =
        combineCustomerBalances(customersFlow, ledgerEntries)
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

    // 排行榜現在包含 ID 了
    val topDebtors: StateFlow<List<DebtorData>> = customersWithBalance
        .map { list ->
            list.filter { it.amount > 0.0 }
                .sortedByDescending { it.amount }
                .take(5)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val dashboardSnapshot: StateFlow<DashboardSnapshot> =
        combine(customersWithBalance, ledgerEntries) { balances, entries ->
            val now = Calendar.getInstance()
            val monthDebt = entries.filter { it.isSameMonth(now) && it.type.equals("DEBT", ignoreCase = true) }
                .sumOf { it.amount }
            val monthPayment = entries.filter { it.isSameMonth(now) && it.type.equals("PAYMENT", ignoreCase = true) }
                .sumOf { it.amount }
            DashboardSnapshot(
                totalDebt = balances.filter { it.amount > 0.0 }.sumOf { it.amount },
                monthDebt = monthDebt,
                monthPayment = monthPayment,
                topDebtors = balances.filter { it.amount > 0.0 }.sortedByDescending { it.amount }.take(10)
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardSnapshot()
            )

    val monthlyTotals: StateFlow<List<MonthlyTotal>> = ledgerEntries
        .map { entries ->
            val baseCalendar = Calendar.getInstance()
            (0 until 6).map { offset ->
                val calendar = (baseCalendar.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -offset)
                }
                val targetMonth = calendar.get(Calendar.MONTH)
                val targetYear = calendar.get(Calendar.YEAR)
                val label = SimpleDateFormat("M月", Locale.getDefault()).format(calendar.time)
                val monthDebt = entries.filter { it.isSameMonth(targetYear, targetMonth) && it.type.equals("DEBT", true) }
                    .sumOf { it.amount }
                val monthPayment = entries.filter { it.isSameMonth(targetYear, targetMonth) && it.type.equals("PAYMENT", true) }
                    .sumOf { it.amount }
                MonthlyTotal(label = label, debt = monthDebt, payment = monthPayment)
            }.reversed()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val agingBuckets: StateFlow<Map<String, Double>> = ledgerEntries
        .map { entries ->
            val now = System.currentTimeMillis()
            val buckets = mutableMapOf(
                "0-30" to 0.0,
                "31-60" to 0.0,
                "61-90" to 0.0,
                "90+" to 0.0
            )
            entries.filter { it.type.equals("DEBT", ignoreCase = true) }
                .forEach { entry ->
                    val days = ((now - entry.timestamp) / (1000 * 60 * 60 * 24)).toInt()
                    when {
                        days <= 30 -> buckets["0-30"] = buckets.getValue("0-30") + entry.amount
                        days <= 60 -> buckets["31-60"] = buckets.getValue("31-60") + entry.amount
                        days <= 90 -> buckets["61-90"] = buckets.getValue("61-90") + entry.amount
                        else -> buckets["90+"] = buckets.getValue("90+") + entry.amount
                    }
                }
            buckets
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = mapOf("0-30" to 0.0, "31-60" to 0.0, "61-90" to 0.0, "90+" to 0.0)
        )

    private fun LedgerEntryEntity.isSameMonth(calendar: Calendar): Boolean =
        isSameMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))

    private fun LedgerEntryEntity.isSameMonth(year: Int, month: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
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