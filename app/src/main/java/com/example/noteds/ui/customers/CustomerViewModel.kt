package com.example.noteds.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.model.CustomerWithBalance
import com.example.noteds.data.repository.CustomerRepository
import com.example.noteds.data.repository.LedgerRepository
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val ledgerRepository: LedgerRepository
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
            val customer = CustomerEntity(
                name = name,
                phone = phone,
                note = note,
                profilePhotoUri = profilePhotoUris.getOrNull(0),
                profilePhotoUri2 = profilePhotoUris.getOrNull(1),
                profilePhotoUri3 = profilePhotoUris.getOrNull(2),
                passportPhotoUri = passportPhotoUris.getOrNull(0),
                passportPhotoUri2 = passportPhotoUris.getOrNull(1),
                passportPhotoUri3 = passportPhotoUris.getOrNull(2),
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
        name: String,
        phone: String,
        note: String,
        profilePhotoUris: List<String?>,
        passportPhotoUris: List<String?>,
        repaymentDate: Long? = null
    ) {
        viewModelScope.launch {
            val existing = customerRepository.getCustomerById(customerId) ?: return@launch
            val updated = existing.copy(
                name = name,
                phone = phone,
                note = note,
                profilePhotoUri = profilePhotoUris.getOrNull(0),
                profilePhotoUri2 = profilePhotoUris.getOrNull(1),
                profilePhotoUri3 = profilePhotoUris.getOrNull(2),
                passportPhotoUri = passportPhotoUris.getOrNull(0),
                passportPhotoUri2 = passportPhotoUris.getOrNull(1),
                passportPhotoUri3 = passportPhotoUris.getOrNull(2),
                expectedRepaymentDate = repaymentDate
            )
            customerRepository.updateCustomer(updated)
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
}
