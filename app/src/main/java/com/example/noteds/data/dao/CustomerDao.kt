package com.example.noteds.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.model.CustomerWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name")
    suspend fun getAllCustomersSnapshot(): List<CustomerEntity>

    @Query(
        """
        SELECT c.*, 
               COALESCE(SUM(CASE WHEN le.type = 'DEBT' THEN le.amount END), 0) AS totalDebt,
               COALESCE(SUM(CASE WHEN le.type = 'PAYMENT' THEN le.amount END), 0) AS totalPayment
        FROM customers c
        LEFT JOIN ledger_entries le ON le.customerId = c.id
        WHERE c.isDeleted = 0
        GROUP BY c.id
        ORDER BY c.name
        """
    )
    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET isDeleted = 1 WHERE id = :customerId")
    suspend fun softDeleteCustomerById(customerId: Long)

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: Long): CustomerEntity?

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerById(customerId: Long)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>): List<Long>
}
