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
            customerDao.insertCustomers(activeCustomers)
            ledgerDao.insertEntries(filteredEntries)
        }
    }
}
