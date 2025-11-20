package com.example.noteds.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.model.CustomerWithBalance
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    val customersWithBalance: StateFlow<List<CustomerWithBalance>> =
        customerRepository.getCustomersWithBalance()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun addCustomer(
        name: String,
        phone: String,
        note: String,
        profilePhotoUri: String?,
        idCardPhotoUri: String?
    ) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                name = name,
                phone = phone,
                note = note,
                profilePhotoUri = profilePhotoUri,
                idCardPhotoUri = idCardPhotoUri
            )
            customerRepository.insertCustomer(customer)
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            customerRepository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            customerRepository.deleteCustomerById(customerId)
            ledgerRepository.deleteEntriesForCustomer(customerId)
        }
    }

    fun updateProfilePhoto(customerId: Long, photoUri: String?) {
        updateCustomerPhotoInternal(customerId) { current ->
            current.copy(profilePhotoUri = photoUri)
        }
    }

    fun updateIdCardPhoto(customerId: Long, photoUri: String?) {
        updateCustomerPhotoInternal(customerId) { current ->
            current.copy(idCardPhotoUri = photoUri)
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

    fun ledgerEntriesForCustomer(customerId: Long): Flow<List<LedgerEntryEntity>> =
        ledgerRepository.getEntriesForCustomer(customerId)

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
}
