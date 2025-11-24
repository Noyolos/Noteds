package com.example.noteds.data.repository

import android.util.Log
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
     * 使用“直接恢复模式”替换所有数据。
     * 保留原始 ID，避免因 ID 变更导致的层级关系丢失问题。
     */
    suspend fun replaceAllData(
        customers: List<CustomerEntity>,
        entries: List<LedgerEntryEntity>
    ) {
        // 1. 过滤逻辑：建议恢复所有数据，或者仅恢复未删除的。
        // 这里我们只恢复活跃数据，但通过保留 ID，即使父文件夹丢失，
        // 子文件也不会错误的变成“根目录”文件（它们会变成“孤儿”，这比跑出来占位更好），
        // 如果父文件夹也在备份中，关系将完美保留。
        val activeCustomers = customers.filterNot { it.isDeleted }
        val activeIds = activeCustomers.map { it.id }.toSet()

        // 过滤掉那些引用了不存在客户的账目
        val validEntries = entries.filter { it.customerId in activeIds }

        // 调试日志：查看导入的数据特征
        activeCustomers.firstOrNull { it.isGroup }?.let {
            Log.d("BackupRestore", "检测到文件夹数据: ${it.name}, ID: ${it.id}")
        } ?: Log.d("BackupRestore", "警告：导入的数据中没有任何文件夹(isGroup=true)，这可能是旧备份文件导致的问题。")

        database.withTransaction {
            // 2. 清空旧数据
            ledgerDao.deleteAllEntries()
            customerDao.deleteAllCustomers()

            // 3. 直接插入客户数据（保留原 JSON 中的 ID、parentId 和 isGroup）
            // 注意：Room 的 @Insert(onConflict = REPLACE) 会尊重我们传入的 ID
            if (activeCustomers.isNotEmpty()) {
                customerDao.insertCustomers(activeCustomers)
            }

            // 4. 直接插入账目数据
            if (validEntries.isNotEmpty()) {
                ledgerDao.insertEntries(validEntries)
            }

            // 5. 关键步骤：重置 SQLite 自增计数器
            // 防止下次新建客户时 ID 冲突 (即生成的 ID 比导入的最大 ID 小)
            if (activeCustomers.isNotEmpty()) {
                val maxId = activeCustomers.maxOfOrNull { it.id } ?: 0L
                try {
                    database.openHelper.writableDatabase.execSQL(
                        "UPDATE sqlite_sequence SET seq = $maxId WHERE name = 'customers'"
                    )
                    database.openHelper.writableDatabase.execSQL(
                        "UPDATE sqlite_sequence SET seq = ${(validEntries.maxOfOrNull { it.id } ?: 0L)} WHERE name = 'ledger_entries'"
                    )
                } catch (e: Exception) {
                    Log.e("BackupRestore", "重置自增 ID 失败，可能需要手动处理", e)
                }
            }
        }
    }
}