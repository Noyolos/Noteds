package com.example.noteds.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// 定義一個簡單的資料類別給排行榜用，包含 ID
data class DebtorData(
    val id: Long,
    val name: String,
    val amount: Double
)

class ReportsViewModel(
    private val customerRepository: CustomerRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val allEntries: StateFlow<List<LedgerEntryEntity>> =
        ledgerRepository.getAllEntries()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // 改為回傳 DebtorData 列表
    private val customersWithBalance: StateFlow<List<DebtorData>> =
        combineCustomerBalances(
            customerRepository.getAllCustomers(),
            allEntries
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

    val debtThisMonth: StateFlow<Double> = allEntries
        .map { entries ->
            val (start, end) = currentMonthRange()
            entries.filter { entry ->
                entry.type.equals("DEBT", ignoreCase = true) &&
                        entry.timestamp in start until end
            }.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val repaymentThisMonth: StateFlow<Double> = allEntries
        .map { entries ->
            val (start, end) = currentMonthRange()
            entries.filter { entry ->
                entry.type.equals("PAYMENT", ignoreCase = true) &&
                        entry.timestamp in start until end
            }.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

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

    private fun currentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis
        return start to end
    }
}