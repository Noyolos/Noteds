package com.example.noteds.di

import android.content.Context
import androidx.room.Room
import com.example.noteds.data.db.AppDatabase
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository

class AppContainer(context: Context) {
    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "noteds-db"
    )
    .fallbackToDestructiveMigration()
    .build()

    val customerRepository: CustomerRepository by lazy {
        CustomerRepository(
            customerDao = database.customerDao(),
            ledgerDao = database.ledgerDao()
        )
    }

    val ledgerRepository: LedgerRepository by lazy {
        LedgerRepository(database.ledgerDao())
    }
}
