package com.example.noteds.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.noteds.data.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getEntriesForCustomer(customerId: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries")
    fun getAllEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries")
    suspend fun getAllEntriesSnapshot(): List<LedgerEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: LedgerEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: LedgerEntryEntity)

    @Query("DELETE FROM ledger_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)

    @Query("DELETE FROM ledger_entries WHERE customerId = :customerId")
    suspend fun deleteEntriesForCustomer(customerId: Long)
}
