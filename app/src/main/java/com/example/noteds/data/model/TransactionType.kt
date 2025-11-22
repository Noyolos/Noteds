package com.example.noteds.data.model

enum class TransactionType(val dbValue: String) {
    DEBT("DEBT"),
    PAYMENT("PAYMENT");

    companion object {
        fun fromString(raw: String): TransactionType? =
            values().firstOrNull { it.dbValue.equals(raw.trim(), ignoreCase = true) }
    }
}
