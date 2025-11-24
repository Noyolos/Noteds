package com.example.noteds.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val code: String = "",
    val name: String,
    val phone: String,
    val note: String,
    val profilePhotoUri: String? = null,
    val profilePhotoUri2: String? = null,
    val profilePhotoUri3: String? = null,
    val idCardPhotoUri: String? = null,
    // New fields
    val passportPhotoUri: String? = null,
    val passportPhotoUri2: String? = null,
    val passportPhotoUri3: String? = null,
    val parentId: Long? = null,
    val isGroup: Boolean = false,
    val expectedRepaymentDate: Long? = null,
    val initialTransactionDone: Boolean = false,
    val isDeleted: Boolean = false
)
