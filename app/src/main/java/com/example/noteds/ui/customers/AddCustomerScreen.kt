package com.example.noteds.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import com.example.noteds.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var initialAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var initialAmountError by remember { mutableStateOf<String?>(null) }
    val profilePhotos = remember { mutableStateListOf<String?>(null, null, null) }
    val passportPhotos = remember { mutableStateListOf<String?>(null, null, null) }

    fun validatePhone(input: String): Boolean {
        val normalized = input.trim()
        val pattern = Regex("^[0-9+\\-\\s]{6,20}")
        return normalized.isEmpty() || pattern.matches(normalized)
    }

    fun validate(): Boolean {
        var valid = true
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            nameError = "姓名必填"
            valid = false
        }
        val trimmedPhone = phone.trim()
        if (trimmedPhone.isNotEmpty() && !validatePhone(trimmedPhone)) {
            phoneError = "電話格式不正確"
            valid = false
        }
        if (initialAmount.isNotBlank()) {
            val value = initialAmount.toDoubleOrNull()
            if (value == null || value <= 0) {
                initialAmountError = "金額必須大於 0"
                valid = false
            }
        }
        return valid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新增客戶", color = TextWhite, fontWeight = FontWeight.Bold) },
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
                    initialAmountError = null
                    if (validate()) {
                        customerViewModel.addCustomer(
                            code = code,
                            name = name,
                            phone = phone,
                            note = note,
                            profilePhotoUris = profilePhotos.toList(),
                            passportPhotoUris = passportPhotos.toList(),
                            initialDebtAmount = initialAmount.toDoubleOrNull(),
                            initialDebtNote = "初始欠款",
                            initialDebtDate = System.currentTimeMillis(),
                            repaymentDate = null // Optional
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
                Text("確認建立檔案", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
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

            // Initial Debt
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("初始賒賬 (選填)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)

                OutlinedTextField(
                    value = initialAmount,
                    onValueChange = {
                        initialAmount = it
                        initialAmountError = null
                    },
                    placeholder = { Text("欠款金額 (RM)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = DebtColor.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    isError = initialAmountError != null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = DebtColor, fontWeight = FontWeight.Bold)
                )
                if (initialAmountError != null) {
                    Text(
                        text = initialAmountError!!,
                        color = FunctionalRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("購買了什麼？(備註)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = MidnightBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )
            }
        }
    }
}
