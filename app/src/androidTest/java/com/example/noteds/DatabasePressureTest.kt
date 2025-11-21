package com.example.noteds

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.noteds.data.dao.CustomerDao
import com.example.noteds.data.dao.LedgerDao
import com.example.noteds.data.db.AppDatabase
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DatabasePressureTest {
    private lateinit var db: AppDatabase
    private lateinit var customerDao: CustomerDao
    private lateinit var ledgerDao: LedgerDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        customerDao = db.customerDao()
        ledgerDao = db.ledgerDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testCustomersWithBalanceComplexQuery() = runBlocking {
        val cid = customerDao.insertCustomer(CustomerEntity(name = "Boss", phone = "123", note = ""))
        ledgerDao.insertEntry(LedgerEntryEntity(customerId = cid, type = "DEBT", amount = 1000.0, timestamp = 1000, note = ""))
        ledgerDao.insertEntry(LedgerEntryEntity(customerId = cid, type = "PAYMENT", amount = 300.0, timestamp = 2000, note = ""))

        val result = customerDao.getCustomersWithBalance().first().first { it.customer.id == cid }
        
        assertEquals(700.0, result.balance, 0.01)
    }

    @Test
    fun stressTestLargeVolume() = runBlocking {
        // Simulate 100 customers with 50 transactions each (5000 records)
        val customerIds = (1..100).map {
            customerDao.insertCustomer(CustomerEntity(name = "C$it", phone = "$it", note = ""))
        }

        customerIds.forEach { cid ->
            repeat(50) {
                ledgerDao.insertEntry(
                    LedgerEntryEntity(
                        customerId = cid,
                        type = if (Random.nextBoolean()) "DEBT" else "PAYMENT",
                        amount = Random.nextDouble(10.0, 1000.0),
                        timestamp = System.currentTimeMillis(),
                        note = "Stress"
                    )
                )
            }
        }

        val allEntries = ledgerDao.getAllEntriesSnapshot()
        assertEquals(5000, allEntries.size)

        val balances = customerDao.getCustomersWithBalance().first()
        assertEquals(100, balances.size)
        assertTrue(balances.all { it.balance > -Double.MAX_VALUE }) 
    }
}
