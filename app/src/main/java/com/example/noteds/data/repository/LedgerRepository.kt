package com.example.noteds.data.repository

import com.example.noteds.data.dao.LedgerDao
import com.example.noteds.data.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

class LedgerRepository(
    private val ledgerDao: LedgerDao
) {
    fun getAllEntries(): Flow<List<LedgerEntryEntity>> = ledgerDao.getAllEntries()

    suspend fun getAllEntriesSnapshot(): List<LedgerEntryEntity> = ledgerDao.getAllEntriesSnapshot()

    fun getEntriesForCustomer(customerId: Long): Flow<List<LedgerEntryEntity>> =
        ledgerDao.getEntriesForCustomer(customerId)

    suspend fun insertEntry(entry: LedgerEntryEntity): Long = ledgerDao.insertEntry(entry)

    suspend fun updateEntry(entry: LedgerEntryEntity) = ledgerDao.updateEntry(entry)

    suspend fun deleteEntry(entry: LedgerEntryEntity) = ledgerDao.deleteEntry(entry)

    suspend fun deleteEntryById(entryId: Long) = ledgerDao.deleteEntryById(entryId)

    suspend fun deleteEntriesForCustomer(customerId: Long) =
        ledgerDao.deleteEntriesForCustomer(customerId)
}
