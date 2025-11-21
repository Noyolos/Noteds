package com.example.noteds.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.noteds.data.entity.CustomerEntity

data class CustomerWithBalance(
    @Embedded val customer: CustomerEntity,
    @ColumnInfo(name = "totalDebt") val totalDebt: Double,
    @ColumnInfo(name = "totalPayment") val totalPayment: Double
) {
    val balance: Double
        get() = totalDebt - totalPayment
}
