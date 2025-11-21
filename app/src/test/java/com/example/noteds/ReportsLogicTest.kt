package com.example.noteds

import android.content.Context
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository
import com.example.noteds.ui.reports.ReportsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsLogicTest {

    private val customerRepository: CustomerRepository = mockk()
    private val ledgerRepository: LedgerRepository = mockk()
    private val context: Context = mockk(relaxed = true)
    private lateinit var viewModel: ReportsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { customerRepository.getAllCustomers() } returns flowOf(emptyList())
        every { ledgerRepository.getAllEntries() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test total debt calculation logic`() = runTest {
        // 場景：
        // 客戶 A: 欠 100, 還 20 (剩 80)
        // 客戶 B: 欠 50 (剩 50)
        // 客戶 C: 欠 100, 還 120 (溢繳 20，不應減少總欠款)

        val customers = listOf(
            CustomerEntity(id = 1, name = "A", phone = "", note = ""),
            CustomerEntity(id = 2, name = "B", phone = "", note = ""),
            CustomerEntity(id = 3, name = "C", phone = "", note = "")
        )
        val entries = listOf(
            LedgerEntryEntity(customerId = 1, type = "DEBT", amount = 100.0, timestamp = 1000, note = ""),
            LedgerEntryEntity(customerId = 1, type = "PAYMENT", amount = 20.0, timestamp = 2000, note = ""),
            LedgerEntryEntity(customerId = 2, type = "DEBT", amount = 50.0, timestamp = 1000, note = ""),
            LedgerEntryEntity(customerId = 3, type = "DEBT", amount = 100.0, timestamp = 1000, note = ""),
            LedgerEntryEntity(customerId = 3, type = "PAYMENT", amount = 120.0, timestamp = 1000, note = "")
        )

        every { customerRepository.getAllCustomers() } returns flowOf(customers)
        every { ledgerRepository.getAllEntries() } returns flowOf(entries)

        viewModel = ReportsViewModel(customerRepository, ledgerRepository, context)

        // 【修復點】：在背景啟動收集 (Collect)，激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.totalDebt.collect()
        }

        advanceUntilIdle() // 等待數據計算完成

        // 預期結果: 80 (A) + 50 (B) = 130.0
        val result = viewModel.totalDebt.value
        assertEquals(130.0, result, 0.01)
    }

    @Test
    fun `test aging analysis FIFO logic`() = runTest {
        // 場景：
        // 舊賬 (95天前) 100元
        // 新賬 (10天前) 50元
        // 今天還款 100元 (應該優先抵消舊賬)

        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        val entries = listOf(
            LedgerEntryEntity(id=1, customerId=1, type="DEBT", amount=100.0, timestamp=now - (95 * dayMillis), note="Old"), // 90+ Days
            LedgerEntryEntity(id=2, customerId=1, type="DEBT", amount=50.0, timestamp=now - (10 * dayMillis), note="New"), // 0-30 Days
            LedgerEntryEntity(id=3, customerId=1, type="PAYMENT", amount=100.0, timestamp=now, note="Pay") // 應抵消 "Old"
        )

        every { ledgerRepository.getAllEntries() } returns flowOf(entries)
        viewModel = ReportsViewModel(customerRepository, ledgerRepository, context)

        // 【修復點】：啟動收集 agingStats
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.agingStats.collect()
        }

        advanceUntilIdle() // 等待計算

        val stats = viewModel.agingStats.value

        // 使用安全查找，防止列表為空時報錯
        val bucket90 = stats.find { it.bucket == "90+ Days" }?.amount ?: 0.0
        val bucket30 = stats.find { it.bucket == "0-30 Days" }?.amount ?: 0.0

        // 90+ Days 的欠款應該被清零
        assertEquals("90+ Days bucket should be cleared", 0.0, bucket90, 0.01)

        // 0-30 Days 的欠款應該保留
        assertEquals("0-30 Days bucket should remain", 50.0, bucket30, 0.01)
    }
}