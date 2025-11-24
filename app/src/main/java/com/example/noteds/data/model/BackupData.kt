package com.example.noteds.data.model

import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.entity.LedgerEntryEntity

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val customers: List<CustomerEntity>,
    val entries: List<LedgerEntryEntity>
)