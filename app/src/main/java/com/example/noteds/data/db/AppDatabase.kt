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
                // 1. 给 customers 表添加 isDeleted 字段
                database.execSQL(
                    "ALTER TABLE customers ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )

                // 2. 创建新的流水表 (必须包含 FOREIGN KEY 定义，这正是之前崩溃的原因！)
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS ledger_entries_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            customerId INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            amount REAL NOT NULL,
                            timestamp INTEGER NOT NULL,
                            note TEXT,
                            FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE NO ACTION
                        )
                    """.trimIndent()
                )

                // 3. 复制数据
                database.execSQL(
                    """
                        INSERT INTO ledger_entries_new (id, customerId, type, amount, timestamp, note)
                        SELECT id, customerId, type, amount, timestamp, note FROM ledger_entries
                    """.trimIndent()
                )

                // 4. 删除旧表
                database.execSQL("DROP TABLE ledger_entries")

                // 5. 重命名新表
                database.execSQL("ALTER TABLE ledger_entries_new RENAME TO ledger_entries")

                // 6. 重建索引 (索引名必须符合 Room 规范)
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_ledger_entries_customerId ON ledger_entries(customerId)"
                )
            }
        }
    }
}