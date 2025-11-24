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
    // 获取所有活跃客户（用于备份导出）
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    // 快照：获取所有活跃客户（用于备份导出）
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name")
    suspend fun getAllCustomersSnapshot(): List<CustomerEntity>

    // 获取所有含余额的客户（用于 ViewModel 计算文件夹总金额）
    @Query(
        """
        SELECT c.*, 
               COALESCE(SUM(CASE WHEN le.type = 'DEBT' THEN le.amount END), 0) AS totalDebt,
               COALESCE(SUM(CASE WHEN le.type = 'PAYMENT' THEN le.amount END), 0) AS totalPayment
        FROM customers c
        LEFT JOIN ledger_entries le ON le.customerId = c.id
        WHERE c.isDeleted = 0
        GROUP BY c.id
        ORDER BY c.isGroup DESC, c.name ASC
        """
    )
    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>>

    // 获取某父级下的所有下属（用于 UI 列表显示）
    @Query(
        """
        SELECT c.*, 
               COALESCE(SUM(CASE WHEN le.type = 'DEBT' THEN le.amount END), 0) AS totalDebt,
               COALESCE(SUM(CASE WHEN le.type = 'PAYMENT' THEN le.amount END), 0) AS totalPayment
        FROM customers c
        LEFT JOIN ledger_entries le ON le.customerId = c.id
        WHERE c.isDeleted = 0 AND c.parentId = :parentId
        GROUP BY c.id
        ORDER BY c.name ASC
        """
    )
    fun getSubordinatesWithBalance(parentId: Long): Flow<List<CustomerWithBalance>>

    // --- 核心修复：必须包含此方法，否则编译失败 ---
    @Query(
        """
        SELECT c.*, 
               COALESCE(SUM(CASE WHEN le.type = 'DEBT' THEN le.amount END), 0) AS totalDebt,
               COALESCE(SUM(CASE WHEN le.type = 'PAYMENT' THEN le.amount END), 0) AS totalPayment
        FROM customers c
        LEFT JOIN ledger_entries le ON le.customerId = c.id
        WHERE c.isDeleted = 0 AND c.parentId IS NULL
        GROUP BY c.id
        ORDER BY c.isGroup DESC, c.name ASC
        """
    )
    fun getRootCustomersWithBalance(): Flow<List<CustomerWithBalance>>

    // --- 新增：获取指定文件夹下的所有直接下属（快照，用于递归删除逻辑） ---
    @Query("SELECT * FROM customers WHERE parentId = :parentId AND isDeleted = 0")
    suspend fun getSubordinatesSnapshot(parentId: Long): List<CustomerEntity>

    // --- 新增：释放下属（备用） ---
    @Query("UPDATE customers SET parentId = NULL WHERE parentId = :parentId")
    suspend fun releaseSubordinates(parentId: Long)

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