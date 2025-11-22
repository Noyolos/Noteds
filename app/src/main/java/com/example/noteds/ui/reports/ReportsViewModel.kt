package com.example.noteds.ui.reports

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.repository.BackupRepository
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository
import com.example.noteds.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class DebtorData(
    val id: Long,
    val name: String,
    val amount: Double,
    val photoUri: String? = null // 新增字段：头像路径
)

data class MonthlyStats(
    val month: String,
    val debt: Double,
    val payment: Double
)

data class AgingData(
    val bucket: String,
    val amount: Double,
    val colorHex: Long
)

class ReportsViewModel(
    private val customerRepository: CustomerRepository,
    private val ledgerRepository: LedgerRepository,
    private val backupRepository: BackupRepository,
    private val appContext: Context
) : ViewModel() {

    private val customersWithBalance: StateFlow<List<DebtorData>> =
        combineCustomerBalances(
            customerRepository.getAllCustomers(),
            ledgerRepository.getAllEntries()
        )
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val totalDebt: StateFlow<Double> = customersWithBalance
        .map { list ->
            list.filter { it.amount > 0.0 }
                .sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val topDebtors: StateFlow<List<DebtorData>> = customersWithBalance
        .map { list ->
            list.filter { it.amount > 0.0 }
                .sortedByDescending { it.amount }
                .take(10) // Top 10 as requested
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val debtThisMonth: StateFlow<Double> = ledgerRepository.getAllEntries()
        .map { entries ->
            val (start, end) = getMonthRange()
            entries.filter {
                it.timestamp in start..end &&
                    it.type.uppercase() == TransactionType.DEBT.dbValue
            }
                .sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val repaymentThisMonth: StateFlow<Double> = ledgerRepository.getAllEntries()
        .map { entries ->
            val (start, end) = getMonthRange()
            entries.filter {
                it.timestamp in start..end &&
                    it.type.uppercase() == TransactionType.PAYMENT.dbValue
            }
                .sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    // Monthly Comparison (Last 6 Months)
    val last6MonthsStats: StateFlow<List<MonthlyStats>> = ledgerRepository.getAllEntries()
        .map { entries ->
            calculateLast6Months(entries)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Aging Distribution (FIFO logic)
    val agingStats: StateFlow<List<AgingData>> = ledgerRepository.getAllEntries()
        .map { entries ->
            calculateAging(entries)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val totalTransactions: StateFlow<Int> = ledgerRepository.getAllEntries()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val averageCollectionPeriod: StateFlow<Double> = ledgerRepository.getAllEntries()
        .map { entries ->
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

            val totalDebt = entries.filter { it.type.uppercase() == TransactionType.DEBT.dbValue }
                .sumOf { it.amount }
            val totalPayment = entries.filter { it.type.uppercase() == TransactionType.PAYMENT.dbValue }
                .sumOf { it.amount }
            val outstanding = (totalDebt - totalPayment).coerceAtLeast(0.0)

            val recentDebt = entries
                .filter {
                    it.type.uppercase() == TransactionType.DEBT.dbValue &&
                        it.timestamp >= thirtyDaysAgo
                }
                .sumOf { it.amount }

            if (recentDebt == 0.0) 0.0 else (outstanding / recentDebt) * 30.0
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    // Real Trend Data (Last 6 Months Total Outstanding Debt)
    val totalDebtTrend: StateFlow<List<Float>> = ledgerRepository.getAllEntries()
        .map { entries ->
            calculateDebtTrend(entries)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private fun calculateLast6Months(entries: List<LedgerEntryEntity>): List<MonthlyStats> {
        val calendar = Calendar.getInstance()
        val stats = mutableListOf<MonthlyStats>()

        for (i in 0..5) {
            // Start/End of this month
            val calStart = calendar.clone() as Calendar
            calStart.set(Calendar.DAY_OF_MONTH, 1)
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)

            val calEnd = calendar.clone() as Calendar
            calEnd.add(Calendar.MONTH, 1)
            calEnd.set(Calendar.DAY_OF_MONTH, 1)
            calEnd.add(Calendar.MILLISECOND, -1)

            val start = calStart.timeInMillis
            val end = calEnd.timeInMillis

            val monthEntries = entries.filter { it.timestamp in start..end }
            val debt = monthEntries
                .filter { it.type.uppercase() == TransactionType.DEBT.dbValue }
                .sumOf { it.amount }
            val payment = monthEntries
                .filter { it.type.uppercase() == TransactionType.PAYMENT.dbValue }
                .sumOf { it.amount }

            val monthName = String.format(java.util.Locale.US, "%tb", calStart)
            stats.add(0, MonthlyStats(monthName, debt, payment)) // Add to front to have chronological order

            calendar.add(Calendar.MONTH, -1)
        }
        return stats
    }

    private fun calculateAging(entries: List<LedgerEntryEntity>): List<AgingData> {
        // FIFO Logic
        // 1. Get Total Payments
        var totalPayments = entries
            .filter { it.type.uppercase() == TransactionType.PAYMENT.dbValue }
            .sumOf { it.amount }

        // 2. Get Debts sorted by oldest first
        val debts = entries
            .filter { it.type.uppercase() == TransactionType.DEBT.dbValue }
            .sortedBy { it.timestamp }

        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        var bucket0to30 = 0.0
        var bucket31to60 = 0.0
        var bucket61to90 = 0.0
        var bucket90plus = 0.0

        for (debt in debts) {
            if (totalPayments >= debt.amount) {
                // Fully paid
                totalPayments -= debt.amount
            } else {
                // Partially paid or unpaid
                val remainingDebt = debt.amount - totalPayments
                totalPayments = 0.0

                // Bucket the remaining debt based on age
                val age = (now - debt.timestamp) / day
                when {
                    age <= 30 -> bucket0to30 += remainingDebt
                    age <= 60 -> bucket31to60 += remainingDebt
                    age <= 90 -> bucket61to90 += remainingDebt
                    else -> bucket90plus += remainingDebt
                }
            }
        }

        return listOf(
            AgingData("0-30 Days", bucket0to30, 0xFF00C853), // Green
            AgingData("31-60 Days", bucket31to60, 0xFFFFAB00), // Yellow
            AgingData("61-90 Days", bucket61to90, 0xFFFF6D00), // Orange
            AgingData("90+ Days", bucket90plus, 0xFFD50000)   // Red
        )
    }

    private fun calculateDebtTrend(entries: List<LedgerEntryEntity>): List<Float> {
        val calendar = Calendar.getInstance()
        // Go to end of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1) // End of current month

        val points = mutableListOf<Double>()

        // Calculate for last 6 months (including current)
        for (i in 0..5) {
            val endTime = calendar.timeInMillis

            val debtsUntilNow = entries
                .filter { it.timestamp <= endTime && it.type.uppercase() == TransactionType.DEBT.dbValue }
                .sumOf { it.amount }
            val paymentsUntilNow = entries
                .filter { it.timestamp <= endTime && it.type.uppercase() == TransactionType.PAYMENT.dbValue }
                .sumOf { it.amount }
            val outstanding = (debtsUntilNow - paymentsUntilNow).coerceAtLeast(0.0)

            points.add(0, outstanding) // Add to front (oldest first)

            // Move to previous month
            calendar.add(Calendar.MONTH, -1)
        }

        // Normalize to 0.0 - 1.0
        val maxVal = points.maxOrNull() ?: 1.0
        val minVal = points.minOrNull() ?: 0.0
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal

        // If all are zero, return straight line 0.5
        if (maxVal == 0.0) return listOf(0f, 0f, 0f, 0f, 0f, 0f)

        return points.map {
            // Normalize relative to max to fill chart, but keep 0 at bottom if relevant?
            // Usually trend line fits min-max.
            // Let's normalize between min and max.
            // If we want 0 to be bottom, normalize value / maxVal.
            ((it - minVal) / range).toFloat()
            // Or simple ratio if we want absolute scale?
            // Design usually prefers relative shape.
        }
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun combineCustomerBalances(
        customersFlow: Flow<List<CustomerEntity>>,
        entriesFlow: Flow<List<LedgerEntryEntity>>
    ): Flow<List<DebtorData>> =
        combine(customersFlow, entriesFlow) { customers, entries ->
            val balanceByCustomer = entries.groupBy { it.customerId }
                .mapValues { (_, customerEntries) ->
                    customerEntries.fold(0.0) { acc, entry ->
                        val delta = when (entry.type.uppercase()) {
                            TransactionType.DEBT.dbValue -> entry.amount
                            TransactionType.PAYMENT.dbValue -> -entry.amount
                            else -> 0.0
                        }
                        acc + delta
                    }
                }

            customers.map { customer ->
                DebtorData(
                    id = customer.id,
                    name = customer.name,
                    amount = balanceByCustomer[customer.id] ?: 0.0,
                    photoUri = customer.profilePhotoUri // <--- 映射头像路径
                )
            }
        }

    fun exportBackup(destinationUri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (success, message) = withContext(Dispatchers.IO) {
                try {
                    val customers = customerRepository.getAllCustomersSnapshot()
                    val entries = ledgerRepository.getAllEntriesSnapshot()
                    val backupDir = File(appContext.cacheDir, "backup_temp").apply {
                        deleteRecursively()
                        mkdirs()
                    }

                    val photosDir = File(backupDir, "photos").apply { mkdirs() }
                    val dataJson = buildBackupJson(customers, entries, photosDir)
                    val dataFile = File(backupDir, "data.json").apply {
                        writeText(dataJson)
                    }

                    appContext.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        ZipOutputStream(outputStream).use { zipStream ->
                            zipStream.putNextEntry(ZipEntry("data.json"))
                            dataFile.inputStream().use { it.copyTo(zipStream) }
                            zipStream.closeEntry()

                            photosDir.walkTopDown()
                                .filter { it.isFile }
                                .forEach { photo ->
                                    val relativePath = photo.relativeTo(backupDir).invariantSeparatorsPath
                                    zipStream.putNextEntry(ZipEntry(relativePath))
                                    photo.inputStream().use { it.copyTo(zipStream) }
                                    zipStream.closeEntry()
                                }
                        }
                    } ?: return@withContext false to "無法建立檔案"

                    true to "備份完成"
                } catch (e: Exception) {
                    false to (e.localizedMessage ?: "備份失敗")
                }
            }
            onResult(success, message)
        }
    }

    fun importBackup(sourceUri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (success, message) = withContext(Dispatchers.IO) {
                val tempDir = File(appContext.cacheDir, "backup_import").apply {
                    deleteRecursively()
                    mkdirs()
                }
                try {
                    val backupBytes = appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                        input.readBytes()
                    } ?: return@withContext false to "無法讀取檔案"

                    if (backupBytes.isEmpty()) {
                        return@withContext false to "備份檔案為空或已損壞"
                    }

                    val isZip = isZipBytes(backupBytes)
                    val dataFile = if (isZip) {
                        unzipToDirectory(backupBytes, tempDir)
                        File(tempDir, "data.json")
                    } else {
                        File(tempDir, "data.json").apply {
                            writeBytes(backupBytes)
                        }
                    }
                    if (!dataFile.exists()) return@withContext false to "找不到資料檔"

                    val jsonText = dataFile.readText()
                    Log.d("ReportsViewModel", "backupJson length = ${jsonText.length}")
                    Log.d("ReportsViewModel", "backupJson head = ${jsonText.take(80)}")

                    if (jsonText.isBlank()) {
                        return@withContext false to "備份檔案內容為空或已損壞"
                    }

                    val previewVersion = runCatching { JSONObject(jsonText).optInt("backupVersion", 1) }
                        .getOrDefault(1)

                    if (previewVersion >= 2) {
                        clearCustomerPhotoDirectory()
                    }

                    val parsed = parseBackupJson(jsonText, tempDir.takeIf { isZip })

                    backupRepository.replaceAllData(parsed.customers, parsed.entries)

                    true to "導入完成"
                } catch (e: Exception) {
                    false to ((e.localizedMessage ?: "導入失敗，資料未更動。請確認備份檔案是否完整")
                        .ifBlank { "導入失敗，資料未更動。請確認備份檔案是否完整" })
                } finally {
                    tempDir.deleteRecursively()
                }
            }
            onResult(success, message)
        }
    }

    private fun buildBackupJson(
        customers: List<CustomerEntity>,
        entries: List<LedgerEntryEntity>,
        photosDir: File
    ): String {
        val customersArray = JSONArray().apply {
            customers.forEach { customer ->
                put(
                    JSONObject().apply {
                        put("id", customer.id)
                        put("code", customer.code)
                        put("name", customer.name)
                        put("phone", customer.phone)
                        put("note", customer.note)
                        put("profilePhotoUri", addPhotoToBackup(customer.profilePhotoUri, photosDir, "${'$'}{customer.id}_profile1"))
                        put("profilePhotoBase64", encodePhotoBase64(customer.profilePhotoUri))
                        put("profilePhotoUri2", addPhotoToBackup(customer.profilePhotoUri2, photosDir, "${'$'}{customer.id}_profile2"))
                        put("profilePhoto2Base64", encodePhotoBase64(customer.profilePhotoUri2))
                        put("profilePhotoUri3", addPhotoToBackup(customer.profilePhotoUri3, photosDir, "${'$'}{customer.id}_profile3"))
                        put("profilePhoto3Base64", encodePhotoBase64(customer.profilePhotoUri3))
                        put("idCardPhotoUri", addPhotoToBackup(customer.idCardPhotoUri, photosDir, "${'$'}{customer.id}_idcard"))
                        put("idCardPhotoBase64", encodePhotoBase64(customer.idCardPhotoUri))
                        put("passportPhotoUri", addPhotoToBackup(customer.passportPhotoUri, photosDir, "${'$'}{customer.id}_passport1"))
                        put("passportPhotoBase64", encodePhotoBase64(customer.passportPhotoUri))
                        put("passportPhotoUri2", addPhotoToBackup(customer.passportPhotoUri2, photosDir, "${'$'}{customer.id}_passport2"))
                        put("passportPhoto2Base64", encodePhotoBase64(customer.passportPhotoUri2))
                        put("passportPhotoUri3", addPhotoToBackup(customer.passportPhotoUri3, photosDir, "${'$'}{customer.id}_passport3"))
                        put("passportPhoto3Base64", encodePhotoBase64(customer.passportPhotoUri3))
                        put("expectedRepaymentDate", customer.expectedRepaymentDate)
                        put("initialTransactionDone", customer.initialTransactionDone)
                        put("isDeleted", customer.isDeleted)
                    }
                )
            }
        }

        val entriesArray = JSONArray().apply {
            entries.forEach { entry ->
                put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("customerId", entry.customerId)
                        put("type", entry.type)
                        put("amount", entry.amount)
                        put("timestamp", entry.timestamp)
                        put("note", entry.note)
                    }
                )
            }
        }

        return JSONObject().apply {
            put("backupVersion", 2)
            put("customers", customersArray)
            put("ledgerEntries", entriesArray)
        }.toString()
    }

    private fun parseBackupJson(json: String, extractedRoot: File?): ParsedBackup {
        val root = JSONObject(json)
        val backupVersion = root.optInt("backupVersion", 1)
        val customersArray = root.optJSONArray("customers") ?: JSONArray()
        val entriesArray = root.optJSONArray("ledgerEntries") ?: JSONArray()
        val customers = buildList {
            for (i in 0 until customersArray.length()) {
                val obj = customersArray.getJSONObject(i)
                val profilePhotoBase64 = obj.optNullableString("profilePhotoBase64")
                val profilePhoto2Base64 = obj.optNullableString("profilePhoto2Base64")
                val profilePhoto3Base64 = obj.optNullableString("profilePhoto3Base64")
                val idCardPhotoBase64 = obj.optNullableString("idCardPhotoBase64")
                val passportPhotoBase64 = obj.optNullableString("passportPhotoBase64")
                val passportPhoto2Base64 = obj.optNullableString("passportPhoto2Base64")
                val passportPhoto3Base64 = obj.optNullableString("passportPhoto3Base64")
                add(
                    CustomerEntity(
                        id = obj.optLong("id"),
                        code = obj.optString("code", ""),
                        name = obj.optString("name"),
                        phone = obj.optString("phone"),
                        note = obj.optString("note"),
                        profilePhotoUri = restorePhoto(obj.optNullableString("profilePhotoUri"), profilePhotoBase64, backupVersion, extractedRoot, "profile1"),
                        profilePhotoUri2 = restorePhoto(obj.optNullableString("profilePhotoUri2"), profilePhoto2Base64, backupVersion, extractedRoot, "profile2"),
                        profilePhotoUri3 = restorePhoto(obj.optNullableString("profilePhotoUri3"), profilePhoto3Base64, backupVersion, extractedRoot, "profile3"),
                        idCardPhotoUri = restorePhoto(obj.optNullableString("idCardPhotoUri"), idCardPhotoBase64, backupVersion, extractedRoot, "idcard"),
                        passportPhotoUri = restorePhoto(obj.optNullableString("passportPhotoUri"), passportPhotoBase64, backupVersion, extractedRoot, "passport1"),
                        passportPhotoUri2 = restorePhoto(obj.optNullableString("passportPhotoUri2"), passportPhoto2Base64, backupVersion, extractedRoot, "passport2"),
                        passportPhotoUri3 = restorePhoto(obj.optNullableString("passportPhotoUri3"), passportPhoto3Base64, backupVersion, extractedRoot, "passport3"),
                        expectedRepaymentDate = if (obj.isNull("expectedRepaymentDate")) null else obj.optLong("expectedRepaymentDate"),
                        initialTransactionDone = obj.optBoolean("initialTransactionDone", false),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
        }

        val entries = buildList {
            for (i in 0 until entriesArray.length()) {
                val obj = entriesArray.getJSONObject(i)
                add(
                    LedgerEntryEntity(
                        id = obj.optLong("id"),
                        customerId = obj.optLong("customerId"),
                        type = obj.optString("type").uppercase(Locale.US),
                        amount = obj.optDouble("amount"),
                        timestamp = obj.optLong("timestamp"),
                        note = obj.optNullableString("note")
                    )
                )
            }
        }
        return ParsedBackup(backupVersion, customers, entries)
    }

    private fun unzipToDirectory(bytes: ByteArray, targetDir: File) {
        if (bytes.isEmpty()) throw IllegalArgumentException("無法讀取檔案")
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.contains("..")) {
                    throw IllegalArgumentException("偵測到不安全的檔案路徑")
                }
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { output ->
                        zis.copyTo(output)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun addPhotoToBackup(uri: String?, photosDir: File, fileNamePrefix: String): String? {
        if (uri.isNullOrBlank()) return null
        val file = File(uri)
        if (!file.exists()) return null
        return try {
            val extension = file.extension.takeIf { it.isNotBlank() } ?: "jpg"
            val backupName = "${'$'}fileNamePrefix.$extension"
            val destination = File(photosDir, backupName)
            file.copyTo(destination, overwrite = true)
            "photos/${'$'}backupName"
        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Failed to copy photo for backup", e)
            null
        }
    }

    private fun encodePhotoBase64(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        val file = File(uri)
        if (!file.exists()) return null
        return try {
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Failed to encode photo for backup", e)
            null
        }
    }

    private fun resolveImportedUri(
        value: String?,
        backupVersion: Int,
        extractedRoot: File?,
        nameHint: String
    ): String? {
        if (value.isNullOrBlank()) return null
        val targetDir = File(appContext.filesDir, "customer_photos").apply { mkdirs() }

        if (backupVersion >= 2 && extractedRoot != null && value.startsWith("photos/")) {
            val sourceFile = File(extractedRoot, value)
            if (!sourceFile.exists()) return null

            return try {
                val extension = sourceFile.extension.takeIf { it.isNotBlank() } ?: "jpg"
                val targetFile = File(
                    targetDir,
                    "${'$'}nameHint_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
                )
                sourceFile.copyTo(targetFile, overwrite = true)
                targetFile.absolutePath
            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Failed to restore photo from backup", e)
                null
            }
        }

        if (value.startsWith("customer_photos/")) {
            return File(targetDir, value.removePrefix("customer_photos/")).absolutePath
        }
        return value
    }

    private fun restorePhoto(
        uriValue: String?,
        base64Value: String?,
        backupVersion: Int,
        extractedRoot: File?,
        nameHint: String
    ): String? {
        if (!base64Value.isNullOrBlank()) {
            decodePhotoFromBase64(base64Value, nameHint)?.let { return it }
        }

        return resolveImportedUri(uriValue, backupVersion, extractedRoot, nameHint)
    }

    private fun decodePhotoFromBase64(value: String, nameHint: String): String? {
        val targetDir = File(appContext.filesDir, "customer_photos").apply { mkdirs() }
        return try {
            val bytes = Base64.decode(value, Base64.DEFAULT)
            if (bytes.isEmpty()) return null
            val targetFile = File(
                targetDir,
                "${'$'}nameHint_${'$'}{System.currentTimeMillis()}_${'$'}{UUID.randomUUID()}.jpg"
            )
            targetFile.outputStream().use { output ->
                output.write(bytes)
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Failed to decode photo from backup", e)
            null
        }
    }

    private fun isZipBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return bytes[0] == 'P'.code.toByte() &&
            bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 3.toByte() &&
            bytes[3] == 4.toByte()
    }

    private fun clearCustomerPhotoDirectory() {
        val targetPhotoDir = File(appContext.filesDir, "customer_photos")
        if (targetPhotoDir.exists()) {
            targetPhotoDir.deleteRecursively()
        }
        targetPhotoDir.mkdirs()
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) null else optString(name, null)

    data class ParsedBackup(
        val backupVersion: Int,
        val customers: List<CustomerEntity>,
        val entries: List<LedgerEntryEntity>
    )
}