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
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var initialAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val profilePhotos = remember { mutableStateListOf<String?>(null, null, null) }
    val passportPhotos = remember { mutableStateListOf<String?>(null, null, null) }

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
                    if (name.isNotBlank()) {
                        customerViewModel.addCustomer(
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
                    value = name,
                    onValueChange = { name = it },
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
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MidnightBlue)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
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
                     textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )
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
                    onValueChange = { initialAmount = it },
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
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = DebtColor, fontWeight = FontWeight.Bold)
                )
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
