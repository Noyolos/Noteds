package com.example.noteds.data.repository

import com.example.noteds.data.dao.CustomerDao
import com.example.noteds.data.dao.LedgerDao
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.model.CustomerWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val ledgerDao: LedgerDao
) {
    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    suspend fun insertCustomer(customer: CustomerEntity): Long = customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: CustomerEntity) = customerDao.updateCustomer(customer)

    suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.deleteCustomer(customer)

    suspend fun deleteCustomerById(customerId: Long) = customerDao.deleteCustomerById(customerId)

    suspend fun getCustomerById(customerId: Long): CustomerEntity? = customerDao.getCustomerById(customerId)

    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>> =
        combine(
            customerDao.getAllCustomers(),
            ledgerDao.getAllEntries()
        ) { customers, entries ->
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
                val balance = balanceByCustomer[customer.id] ?: 0.0
                CustomerWithBalance(customer, balance)
            }
        }
}
