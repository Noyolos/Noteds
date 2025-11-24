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
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun ledgerDao(): LedgerDao

    companion object {
        private fun migrateToVersion5(database: SupportSQLiteDatabase) {
            ensureCustomerTable(database)
            ensureLedgerTable(database)
        }

        private fun ensureCustomerTable(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS customers_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        code TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        note TEXT NOT NULL,
                        profilePhotoUri TEXT,
                        profilePhotoUri2 TEXT,
                        profilePhotoUri3 TEXT,
                        idCardPhotoUri TEXT,
                        passportPhotoUri TEXT,
                        passportPhotoUri2 TEXT,
                        passportPhotoUri3 TEXT,
                        expectedRepaymentDate INTEGER,
                        initialTransactionDone INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
            )

            if (tableExists(database, "customers")) {
                val existing = existingColumns(database, "customers")
                val selectClause = listOf(
                    if ("id" in existing) "id" else "NULL AS id",
                    if ("code" in existing) "code" else "'' AS code",
                    if ("name" in existing) "name" else "'' AS name",
                    if ("phone" in existing) "phone" else "'' AS phone",
                    if ("note" in existing) "note" else "'' AS note",
                    if ("profilePhotoUri" in existing) "profilePhotoUri" else "NULL AS profilePhotoUri",
                    if ("profilePhotoUri2" in existing) "profilePhotoUri2" else "NULL AS profilePhotoUri2",
                    if ("profilePhotoUri3" in existing) "profilePhotoUri3" else "NULL AS profilePhotoUri3",
                    if ("idCardPhotoUri" in existing) "idCardPhotoUri" else "NULL AS idCardPhotoUri",
                    if ("passportPhotoUri" in existing) "passportPhotoUri" else "NULL AS passportPhotoUri",
                    if ("passportPhotoUri2" in existing) "passportPhotoUri2" else "NULL AS passportPhotoUri2",
                    if ("passportPhotoUri3" in existing) "passportPhotoUri3" else "NULL AS passportPhotoUri3",
                    if ("expectedRepaymentDate" in existing) "expectedRepaymentDate" else "NULL AS expectedRepaymentDate",
                    if ("initialTransactionDone" in existing) "initialTransactionDone" else "0 AS initialTransactionDone",
                    if ("isDeleted" in existing) "isDeleted" else "0 AS isDeleted"
                ).joinToString(",")

                database.execSQL(
                    """
                        INSERT INTO customers_new (
                            id, code, name, phone, note, profilePhotoUri, profilePhotoUri2, profilePhotoUri3,
                            idCardPhotoUri, passportPhotoUri, passportPhotoUri2, passportPhotoUri3,
                            expectedRepaymentDate, initialTransactionDone, isDeleted
                        )
                        SELECT ${'$'}selectClause FROM customers
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE IF EXISTS customers")
            }

            database.execSQL("ALTER TABLE customers_new RENAME TO customers")
        }

        private fun ensureLedgerTable(database: SupportSQLiteDatabase) {
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

            if (tableExists(database, "ledger_entries")) {
                val existing = existingColumns(database, "ledger_entries")
                val selectClause = listOf(
                    if ("id" in existing) "id" else "NULL AS id",
                    if ("customerId" in existing) "customerId" else "0 AS customerId",
                    if ("type" in existing) "type" else "'' AS type",
                    if ("amount" in existing) "amount" else "0 AS amount",
                    if ("timestamp" in existing) "timestamp" else "0 AS timestamp",
                    if ("note" in existing) "note" else "NULL AS note"
                ).joinToString(",")

                database.execSQL(
                    """
                        INSERT INTO ledger_entries_new (id, customerId, type, amount, timestamp, note)
                        SELECT ${'$'}selectClause FROM ledger_entries
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE IF EXISTS ledger_entries")
            }

            database.execSQL("ALTER TABLE ledger_entries_new RENAME TO ledger_entries")
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_ledger_entries_customerId ON ledger_entries(customerId)"
            )
        }

        private fun tableExists(database: SupportSQLiteDatabase, tableName: String): Boolean {
            database.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(tableName)
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        }

        private fun existingColumns(
            database: SupportSQLiteDatabase,
            tableName: String
        ): Set<String> {
            database.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex == -1) return emptySet()
                val names = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    names.add(cursor.getString(nameIndex))
                }
                return names
            }
        }

        /**
         * Migration chain notes:
         * - v1-v4 were test builds; all of them are migrated by recreating the
         *   two tables with the latest schema and copying any existing columns
         *   to keep user data.
         * - v5 is the current baseline. Future versions should add a dedicated
         *   Migration X_Y and keep the vN_5 shortcuts so older installs remain
         *   upgradeable.
         */
        val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrateToVersion5(database)
            }
        }

        val MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrateToVersion5(database)
            }
        }

        val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrateToVersion5(database)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrateToVersion5(database)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE customers ADD COLUMN parentId INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE customers ADD COLUMN isGroup INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_customers_parentId ON customers(parentId)"
                )
            }
        }
    }
}