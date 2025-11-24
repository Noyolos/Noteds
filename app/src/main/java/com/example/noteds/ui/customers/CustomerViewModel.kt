package com.example.noteds.ui.customers

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.model.BackupData
import com.example.noteds.data.model.CustomerWithBalance
import com.example.noteds.data.model.TransactionType
import com.example.noteds.data.repository.BackupRepository
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStream
import java.util.UUID

class CustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val ledgerRepository: LedgerRepository,
    private val backupRepository: BackupRepository,
    private val appContext: Context
) : ViewModel() {

    private val gson = GsonBuilder()
        .serializeNulls()
        .setPrettyPrinting()
        .create()

    // --- 新增：获取所有文件夹列表，用于移动功能的下拉选择 ---
    val allFolders: StateFlow<List<CustomerEntity>> = customerRepository.getAllCustomers()
        .map { list -> list.filter { it.isGroup && !it.isDeleted } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun getTransactionsForCustomer(customerId: Long): Flow<List<LedgerEntryEntity>> =
        ledgerRepository.getEntriesForCustomer(customerId)

    // 基础数据流：包含所有活跃客户及其自身余额
    val customersWithBalance: StateFlow<List<CustomerWithBalance>> =
        customerRepository.getCustomersWithBalance()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // --- 核心逻辑：获取列表并计算文件夹总金额 ---
    fun getCustomers(parentId: Long?): Flow<List<CustomerWithBalance>> {
        return customersWithBalance.map { all ->
            val childrenMap = all.groupBy { it.customer.parentId }
            val computedSums = mutableMapOf<Long, Pair<Double, Double>>()

            fun computeSum(id: Long): Pair<Double, Double> {
                if (computedSums.containsKey(id)) return computedSums[id]!!

                val children = childrenMap[id] ?: emptyList()
                if (children.isEmpty()) return 0.0 to 0.0

                var debt = 0.0
                var payment = 0.0

                children.forEach { child ->
                    if (child.customer.isGroup) {
                        val (d, p) = computeSum(child.customer.id)
                        debt += d
                        payment += p
                    } else {
                        debt += child.totalDebt
                        payment += child.totalPayment
                    }
                }
                computedSums[id] = debt to payment
                return debt to payment
            }

            val currentLevelItems = all.filter { it.customer.parentId == parentId }
                .sortedByDescending { it.customer.isGroup }

            currentLevelItems.map { item ->
                if (item.customer.isGroup) {
                    val (d, p) = computeSum(item.customer.id)
                    item.copy(totalDebt = d, totalPayment = p)
                } else {
                    item
                }
            }
        }
    }

    suspend fun getCustomer(id: Long): CustomerEntity? =
        customerRepository.getCustomerById(id)

    // --- 新增：移动客户/文件夹 ---
    fun moveCustomer(customer: CustomerEntity, newParentId: Long?) {
        viewModelScope.launch {
            // 防止将文件夹移动到自己内部（简单检查：目标 ID 不能等于自己 ID）
            if (customer.id == newParentId) return@launch

            val updated = customer.copy(parentId = newParentId)
            customerRepository.updateCustomer(updated)
        }
    }

    fun createFolder(name: String, parentId: Long?) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val folder = CustomerEntity(
                name = name.trim(),
                isGroup = true,
                parentId = parentId,
                code = "", phone = "", note = "", initialTransactionDone = true
            )
            customerRepository.insertCustomer(folder)
        }
    }

    fun addCustomer(
        code: String,
        name: String,
        phone: String,
        note: String,
        profilePhotoUris: List<String?>,
        passportPhotoUris: List<String?>,
        initialDebtAmount: Double?,
        initialDebtNote: String?,
        initialDebtDate: Long?,
        repaymentDate: Long?,
        parentId: Long? = null,
        isGroup: Boolean = false
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedPhone = phone.trim()
            if (trimmedName.isEmpty()) return@launch

            val sanitizedInitial = initialDebtAmount?.takeIf { it > 0 }
            val savedProfilePhotos = persistPhotos(profilePhotoUris, "profile")
            val savedPassportPhotos = persistPhotos(passportPhotoUris, "passport")

            val customer = CustomerEntity(
                code = code,
                name = trimmedName,
                phone = trimmedPhone,
                note = note.trim(),
                profilePhotoUri = savedProfilePhotos.getOrNull(0),
                profilePhotoUri2 = savedProfilePhotos.getOrNull(1),
                profilePhotoUri3 = savedProfilePhotos.getOrNull(2),
                passportPhotoUri = savedPassportPhotos.getOrNull(0),
                passportPhotoUri2 = savedPassportPhotos.getOrNull(1),
                passportPhotoUri3 = savedPassportPhotos.getOrNull(2),
                expectedRepaymentDate = repaymentDate,
                initialTransactionDone = sanitizedInitial != null,
                parentId = parentId,
                isGroup = isGroup
            )
            val customerId = customerRepository.insertCustomer(customer)

            if (sanitizedInitial != null) {
                val entry = LedgerEntryEntity(
                    customerId = customerId,
                    type = TransactionType.DEBT.dbValue,
                    amount = sanitizedInitial,
                    timestamp = initialDebtDate ?: System.currentTimeMillis(),
                    note = initialDebtNote ?: "Initial Balance"
                )
                ledgerRepository.insertEntry(entry)
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            deleteCustomerRecursive(customer)
        }
    }

    private suspend fun deleteCustomerRecursive(target: CustomerEntity) {
        if (target.isGroup) {
            val children = customerRepository.getSubordinatesSnapshot(target.id)
            children.forEach { child ->
                deleteCustomerRecursive(child)
            }
        }

        ledgerRepository.deleteEntriesForCustomer(target.id)
        removeCustomerFiles(target)

        val deletedTarget = target.copy(
            isDeleted = true,
            profilePhotoUri = null,
            profilePhotoUri2 = null,
            profilePhotoUri3 = null,
            idCardPhotoUri = null,
            passportPhotoUri = null,
            passportPhotoUri2 = null,
            passportPhotoUri3 = null
        )
        customerRepository.updateCustomer(deletedTarget)
    }

    fun exportBackup(file: File, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val customers = customerRepository.getAllCustomersSnapshot()
                val entries = ledgerRepository.getAllEntriesSnapshot()
                val backupData = BackupData(customers = customers, entries = entries)
                val json = gson.toJson(backupData)
                FileWriter(file).use { it.write(json) }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Export Failed") }
            }
        }
    }

    fun importBackup(file: File, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = file.readText()
                val backupData = gson.fromJson(json, BackupData::class.java)
                backupRepository.replaceAllData(backupData.customers, backupData.entries)
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Import Failed") }
            }
        }
    }

    fun updateCustomer(
        customerId: Long,
        code: String,
        name: String,
        phone: String,
        note: String,
        profilePhotoUris: List<String?>,
        passportPhotoUris: List<String?>,
        repaymentDate: Long? = null
    ) {
        viewModelScope.launch {
            val existing = customerRepository.getCustomerById(customerId) ?: return@launch
            val trimmedName = name.trim()
            val trimmedPhone = phone.trim()
            if (trimmedName.isEmpty()) return@launch

            val savedProfilePhotos = persistPhotos(profilePhotoUris, "profile")
            val savedPassportPhotos = persistPhotos(passportPhotoUris, "passport")

            cleanupReplacedFiles(
                old = listOf(existing.profilePhotoUri, existing.profilePhotoUri2, existing.profilePhotoUri3),
                new = savedProfilePhotos
            )
            cleanupReplacedFiles(
                old = listOf(existing.passportPhotoUri, existing.passportPhotoUri2, existing.passportPhotoUri3),
                new = savedPassportPhotos
            )

            val updated = existing.copy(
                code = code,
                name = trimmedName,
                phone = trimmedPhone,
                note = note.trim(),
                profilePhotoUri = savedProfilePhotos.getOrNull(0),
                profilePhotoUri2 = savedProfilePhotos.getOrNull(1),
                profilePhotoUri3 = savedProfilePhotos.getOrNull(2),
                passportPhotoUri = savedPassportPhotos.getOrNull(0),
                passportPhotoUri2 = savedPassportPhotos.getOrNull(1),
                passportPhotoUri3 = savedPassportPhotos.getOrNull(2),
                expectedRepaymentDate = repaymentDate
            )
            customerRepository.updateCustomer(updated)
        }
    }

    fun updateProfilePhoto(customerId: Long, photoUri: String?) {
        updateCustomerPhotoInternal(customerId) { current ->
            current.copy(profilePhotoUri = photoUri)
        }
    }

    fun updateLedgerEntry(entry: LedgerEntryEntity) {
        viewModelScope.launch {
            val normalizedType = TransactionType.fromString(entry.type)?.dbValue ?: return@launch
            ledgerRepository.updateEntry(entry.copy(type = normalizedType))
        }
    }

    fun deleteLedgerEntry(entryId: Long) {
        viewModelScope.launch {
            ledgerRepository.deleteEntryById(entryId)
        }
    }

    fun updatePassportPhoto(customerId: Long, photoUri: String?) {
        updateCustomerPhotoInternal(customerId) { current ->
            current.copy(passportPhotoUri = photoUri)
        }
    }

    private fun updateCustomerPhotoInternal(
        customerId: Long,
        transform: (CustomerEntity) -> CustomerEntity
    ) {
        viewModelScope.launch {
            val existing = customerRepository.getCustomerById(customerId) ?: return@launch
            customerRepository.updateCustomer(transform(existing))
        }
    }

    fun addLedgerEntry(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        note: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val sanitizedAmount = amount.takeIf { it > 0 } ?: return@launch
            val entry = LedgerEntryEntity(
                customerId = customerId,
                type = type.dbValue,
                amount = sanitizedAmount,
                timestamp = timestamp,
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )
            ledgerRepository.insertEntry(entry)
        }
    }

    fun saveImageToGallery(sourcePath: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                copyImageToGallery(sourcePath)
            }
            onResult(success)
        }
    }

    private fun isValidPhone(phone: String): Boolean {
        val normalized = phone.trim()
        val pattern = Regex("^[0-9+\\-\\s]{6,20}")
        return normalized.isEmpty() || pattern.matches(normalized)
    }

    private suspend fun persistPhotos(photoUris: List<String?>, prefix: String): List<String?> =
        withContext(Dispatchers.IO) {
            photoUris.mapIndexed { index, uri ->
                persistPhoto(uri, "${'$'}prefix_${'$'}index")
            }
        }

    private suspend fun cleanupReplacedFiles(old: List<String?>, new: List<String?>) {
        withContext(Dispatchers.IO) {
            old.forEachIndexed { index, oldUri ->
                val newUri = new.getOrNull(index)
                if (!oldUri.isNullOrBlank() && oldUri != newUri &&
                    oldUri.startsWith(appContext.filesDir.absolutePath)
                ) {
                    val file = File(oldUri)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }

    private suspend fun removeCustomerFiles(customer: CustomerEntity) {
        withContext(Dispatchers.IO) {
            listOf(
                customer.profilePhotoUri,
                customer.profilePhotoUri2,
                customer.profilePhotoUri3,
                customer.idCardPhotoUri,
                customer.passportPhotoUri,
                customer.passportPhotoUri2,
                customer.passportPhotoUri3
            ).forEach { path ->
                if (!path.isNullOrBlank() && path.startsWith(appContext.filesDir.absolutePath)) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun persistPhoto(uriString: String?, prefix: String): String? {
        if (uriString.isNullOrBlank()) return null

        if (uriString.startsWith(appContext.filesDir.absolutePath)) {
            return uriString
        }

        val targetDir = File(appContext.filesDir, "customer_photos").apply { mkdirs() }
        val extension = resolveExtension(uriString)
        val targetFile = File(targetDir, "${'$'}prefix_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")

        val uri = Uri.parse(uriString)
        val inputStream: InputStream = when {
            uri.scheme.isNullOrEmpty() && File(uriString).exists() -> FileInputStream(File(uriString))
            uri.scheme == "file" -> FileInputStream(File(uri.path ?: return null))
            else -> appContext.contentResolver.openInputStream(uri) ?: return null
        }

        inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return targetFile.absolutePath
    }

    private fun resolveExtension(uriString: String): String {
        val uri = Uri.parse(uriString)
        val mimeType = when {
            uri.scheme == null || uri.scheme == "file" ->
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uriString)
                )
            else -> appContext.contentResolver.getType(uri)
        }

        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: MimeTypeMap.getFileExtensionFromUrl(uriString).takeIf { it.isNotBlank() }
            ?: "jpg"
    }

    private fun copyImageToGallery(sourcePath: String): Boolean {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return false

        return try {
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${android.os.Environment.DIRECTORY_PICTURES}/Noteds"
                )
            }

            val resolver = appContext.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false

            resolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(outputStream)
                }
            } ?: return false

            true
        } catch (e: Exception) {
            false
        }
    }
}