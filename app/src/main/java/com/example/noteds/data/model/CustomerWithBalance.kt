package com.example.noteds.data.model

import com.example.noteds.data.entity.CustomerEntity

data class CustomerWithBalance(
    val customer: CustomerEntity,
    val balance: Double
)
