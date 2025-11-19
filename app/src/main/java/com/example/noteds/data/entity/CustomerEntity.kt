package com.example.noteds.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String,
    val note: String,
    val profilePhotoUri: String? = null,
    val idCardPhotoUri: String? = null
)
