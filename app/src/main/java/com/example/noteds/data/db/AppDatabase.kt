package com.example.noteds.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.noteds.data.dao.CustomerDao
import com.example.noteds.data.dao.LedgerDao
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity

@Database(
    entities = [CustomerEntity::class, LedgerEntryEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun ledgerDao(): LedgerDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE customers ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )

                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS ledger_entries_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            customerId INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            amount REAL NOT NULL,
                            timestamp INTEGER NOT NULL,
                            note TEXT
                        )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_ledger_entries_new_customerId ON ledger_entries_new(customerId)"
                )
                database.execSQL(
                    """
                        INSERT INTO ledger_entries_new (id, customerId, type, amount, timestamp, note)
                        SELECT id, customerId, type, amount, timestamp, note FROM ledger_entries
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE ledger_entries")
                database.execSQL("ALTER TABLE ledger_entries_new RENAME TO ledger_entries")
            }
        }
    }
}
