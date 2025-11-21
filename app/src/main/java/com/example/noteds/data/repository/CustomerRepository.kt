package com.example.noteds.data.repository

import com.example.noteds.data.dao.CustomerDao
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.model.CustomerWithBalance
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerDao: CustomerDao
) {
    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    suspend fun getAllCustomersSnapshot(): List<CustomerEntity> =
        customerDao.getAllCustomersSnapshot()

    suspend fun insertCustomer(customer: CustomerEntity): Long = customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: CustomerEntity) = customerDao.updateCustomer(customer)

    suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.deleteCustomer(customer)

    suspend fun deleteCustomerById(customerId: Long) = customerDao.deleteCustomerById(customerId)

    suspend fun softDeleteCustomerById(customerId: Long) = customerDao.softDeleteCustomerById(customerId)

    suspend fun getCustomerById(customerId: Long): CustomerEntity? = customerDao.getCustomerById(customerId)

    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>> =
        customerDao.getCustomersWithBalance()
}
