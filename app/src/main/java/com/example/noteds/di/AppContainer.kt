package com.example.noteds.di

import android.content.Context
import androidx.room.Room
import com.example.noteds.data.db.AppDatabase
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "noteds-db"
    )
        .addMigrations(AppDatabase.MIGRATION_4_5)
        .build()

    val customerRepository: CustomerRepository by lazy {
        CustomerRepository(
            customerDao = database.customerDao()
        )
    }

    val ledgerRepository: LedgerRepository by lazy {
        LedgerRepository(database.ledgerDao())
    }
}
