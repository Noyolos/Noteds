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

    // 确保包含此方法 (级联删除需要)
    suspend fun getSubordinatesSnapshot(parentId: Long): List<CustomerEntity> =
        customerDao.getSubordinatesSnapshot(parentId)

    // 释放下属 (备用)
    suspend fun releaseSubordinates(parentId: Long) = customerDao.releaseSubordinates(parentId)

    suspend fun getCustomerById(customerId: Long): CustomerEntity? = customerDao.getCustomerById(customerId)

    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>> =
        customerDao.getCustomersWithBalance()

    // 确保这里调用的是 customerDao.getRootCustomersWithBalance()
    fun getCustomersByParent(parentId: Long?): Flow<List<CustomerWithBalance>> {
        return if (parentId == null) {
            customerDao.getRootCustomersWithBalance()
        } else {
            customerDao.getSubordinatesWithBalance(parentId)
        }
    }
}