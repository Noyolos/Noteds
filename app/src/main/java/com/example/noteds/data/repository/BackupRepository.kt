package com.example.noteds.data.repository

import androidx.room.withTransaction
import com.example.noteds.data.dao.CustomerDao
import com.example.noteds.data.dao.LedgerDao
import com.example.noteds.data.db.AppDatabase
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity

class BackupRepository(
    private val database: AppDatabase,
    private val customerDao: CustomerDao,
    private val ledgerDao: LedgerDao
) {
    /**
     * Replace all persisted business data with the provided backup snapshot.
     *
     * The operation runs inside a single Room transaction so the database is
     * either fully replaced or left untouched when any error occurs.
     */
    suspend fun replaceAllData(
        customers: List<CustomerEntity>,
        entries: List<LedgerEntryEntity>
    ) {
        val activeCustomers = customers.filterNot { it.isDeleted }
        val activeCustomerIds = activeCustomers.map { it.id }.toSet()
        val filteredEntries = entries.filter { it.customerId in activeCustomerIds }

        database.withTransaction {
            ledgerDao.deleteAllEntries()
            customerDao.deleteAllCustomers()

            // 先用 id = 0 的临时记录写入一次，强制 Room 重新分配主键，确保映射关系一致。
            val sanitizedCustomers = activeCustomers.map { it.copy(id = 0L) }
            val insertedIds = customerDao.insertCustomers(sanitizedCustomers)

            // 将“旧 ID -> 新 ID”对照表保存下来，用于重写 parentId、entry.customerId 等外键引用。
            val idMap = activeCustomers
                .zip(insertedIds)
                .associate { (customer, newId) -> customer.id to newId }

            // 重新构建客户列表：
            // 1) 主键改成 Room 分配的新 ID。
            // 2) parentId 替换成新的主键（如果父级不在备份中则置空，避免悬挂引用）。
            val normalizedCustomers = activeCustomers.mapIndexed { index, customer ->
                val newParentId = customer.parentId?.let(idMap::get)
                customer.copy(id = insertedIds[index], parentId = newParentId)
            }

            // 用新的主键重写账本条目的 customerId。
            val normalizedEntries = filteredEntries.mapNotNull { entry ->
                val newCustomerId = idMap[entry.customerId] ?: return@mapNotNull null
                entry.copy(customerId = newCustomerId)
            }

            // 清理首次插入的临时记录，再写入修正后的数据，保证父子关系与条目引用完整一致。
            customerDao.deleteAllCustomers()
            customerDao.insertCustomers(normalizedCustomers)
            ledgerDao.insertEntries(normalizedEntries)
        }
    }
}
