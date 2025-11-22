package com.example.noteds.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.noteds.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerScreen(
    customerId: Long,
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val customers by customerViewModel.customersWithBalance.collectAsState()
    val customer = customers.firstOrNull { it.customer.id == customerId }?.customer

    if (customer == null) {
        // Loading or error
        return
    }

    var code by remember { mutableStateOf(customer.code) }
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var note by remember { mutableStateOf(customer.note) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    val profilePhotos = remember {
        mutableStateListOf(customer.profilePhotoUri, customer.profilePhotoUri2, customer.profilePhotoUri3)
    }
    val passportPhotos = remember {
        mutableStateListOf(
            customer.passportPhotoUri ?: customer.idCardPhotoUri,
            customer.passportPhotoUri2,
            customer.passportPhotoUri3
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun validatePhone(input: String): Boolean {
        val normalized = input.trim()
        val pattern = Regex("^[0-9+\\-\\s]{6,20}")
        return normalized.isEmpty() || pattern.matches(normalized)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("編輯客戶", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    nameError = null
                    phoneError = null
                    val trimmedName = name.trim()
                    val trimmedPhone = phone.trim()
                    val isNameValid = trimmedName.isNotEmpty()
                    val isPhoneValid = validatePhone(trimmedPhone)
                    if (!isNameValid) {
                        nameError = "姓名必填"
                    }
                    if (!isPhoneValid) {
                        phoneError = "電話格式不正確"
                    }
                    if (isNameValid && isPhoneValid) {
                        customerViewModel.updateCustomer(
                            customerId = customerId,
                            code = code,
                            name = name,
                            phone = phone,
                            note = note,
                            profilePhotoUris = profilePhotos.toList(),
                            passportPhotoUris = passportPhotos.toList(),
                            repaymentDate = customer.expectedRepaymentDate
                        )
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("保存修改", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PhotoGrid(
                title = "頭像照片",
                subtitle = "可拍照或相冊上傳，最多 3 張",
                photoUris = profilePhotos,
                onPhotoChanged = { index, uri -> profilePhotos[index] = uri }
            )

            // Basic Info
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("基本資料", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    placeholder = { Text("客戶編號") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = MidnightBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    placeholder = { Text("客戶姓名 *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = MidnightBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = nameError != null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MidnightBlue)
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        color = FunctionalRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneError = null
                    },
                    placeholder = { Text("電話號碼") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = MidnightBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )
                if (phoneError != null) {
                    Text(
                        text = phoneError!!,
                        color = FunctionalRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            PhotoGrid(
                title = "證件照片",
                subtitle = "護照 / 證件最多 3 張",
                photoUris = passportPhotos,
                onPhotoChanged = { index, uri -> passportPhotos[index] = uri }
            )

            // Delete Button
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FunctionalRed),
                border = BorderStroke(1.dp, FunctionalRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("刪除此客戶檔案", fontWeight = FontWeight.Bold)
            }
        }

        if (showDeleteConfirm) {
             AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("確定要刪除嗎？") },
                text = { Text("此操作將永久刪除 ${customer.name} 及其所有交易記錄。") },
                confirmButton = {
                    Button(
                        onClick = {
                            customerViewModel.deleteCustomer(customerId)
                            onSaved()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FunctionalRed)
                    ) {
                        Text("確認刪除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
