package com.example.noteds.ui.customers

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.model.CustomerWithBalance
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val ledgerRepository: LedgerRepository,
    private val appContext: Context
) : ViewModel() {

    fun getTransactionsForCustomer(customerId: Long): Flow<List<LedgerEntryEntity>> =
        ledgerRepository.getEntriesForCustomer(customerId)

    val customersWithBalance: StateFlow<List<CustomerWithBalance>> =
        customerRepository.getCustomersWithBalance()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

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
        repaymentDate: Long?
    ) {
        viewModelScope.launch {
            val savedProfilePhotos = persistPhotos(profilePhotoUris, "profile")
            val savedPassportPhotos = persistPhotos(passportPhotoUris, "passport")
            val customer = CustomerEntity(
                code = code,
                name = name,
                phone = phone,
                note = note,
                profilePhotoUri = savedProfilePhotos.getOrNull(0),
                profilePhotoUri2 = savedProfilePhotos.getOrNull(1),
                profilePhotoUri3 = savedProfilePhotos.getOrNull(2),
                passportPhotoUri = savedPassportPhotos.getOrNull(0),
                passportPhotoUri2 = savedPassportPhotos.getOrNull(1),
                passportPhotoUri3 = savedPassportPhotos.getOrNull(2),
                expectedRepaymentDate = repaymentDate,
                initialTransactionDone = initialDebtAmount != null && initialDebtAmount > 0
            )
            val customerId = customerRepository.insertCustomer(customer)

            if (initialDebtAmount != null && initialDebtAmount > 0) {
                val entry = LedgerEntryEntity(
                    customerId = customerId,
                    type = "DEBT",
                    amount = initialDebtAmount,
                    timestamp = initialDebtDate ?: System.currentTimeMillis(),
                    note = initialDebtNote ?: "Initial Balance"
                )
                ledgerRepository.insertEntry(entry)
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
            val savedProfilePhotos = persistPhotos(profilePhotoUris, "profile")
            val savedPassportPhotos = persistPhotos(passportPhotoUris, "passport")
            cleanupReplacedFiles(
                old = listOf(
                    existing.profilePhotoUri,
                    existing.profilePhotoUri2,
                    existing.profilePhotoUri3
                ),
                new = savedProfilePhotos
            )
            cleanupReplacedFiles(
                old = listOf(
                    existing.passportPhotoUri,
                    existing.passportPhotoUri2,
                    existing.passportPhotoUri3
                ),
                new = savedPassportPhotos
            )
            val updated = existing.copy(
                code = code,
                name = name,
                phone = phone,
                note = note,
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

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            customerRepository.softDeleteCustomerById(customerId)
        }
    }

    fun updateProfilePhoto(customerId: Long, photoUri: String?) {
        updateCustomerPhotoInternal(customerId) { current ->
            current.copy(profilePhotoUri = photoUri)
        }
    }

    fun updateLedgerEntry(entry: LedgerEntryEntity) {
        viewModelScope.launch {
            ledgerRepository.updateEntry(entry.copy(type = entry.type.uppercase(Locale.US)))
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
        type: String,
        amount: Double,
        note: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val entry = LedgerEntryEntity(
                customerId = customerId,
                type = type.uppercase(Locale.US),
                amount = amount,
                timestamp = timestamp,
                note = note
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
