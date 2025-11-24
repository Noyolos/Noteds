package com.example.noteds.data.entity

import androidx.room.Entity
import androidx.room.Index // 记得添加这个导入
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    // 新增：声明索引，与数据库迁移脚本中的 CREATE INDEX 对应
    indices = [Index(value = ["parentId"])]
)
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
    val passportPhotoUri: String? = null,
    val passportPhotoUri2: String? = null,
    val passportPhotoUri3: String? = null,
    val expectedRepaymentDate: Long? = null,
    val initialTransactionDone: Boolean = false,
    val isDeleted: Boolean = false,
    // Group Features
    val parentId: Long? = null,
    val isGroup: Boolean = false
)