package com.example.noteds.ui.customers

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.noteds.data.entity.CustomerEntity
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

    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var note by remember { mutableStateOf(customer.note) }

    // Load initial URIs
    val profilePhotoUris = remember { mutableStateListOf(customer.profilePhotoUri, customer.profilePhotoUri2, customer.profilePhotoUri3) }
    val passportPhotoUris = remember { mutableStateListOf(customer.passportPhotoUri, customer.passportPhotoUri2, customer.passportPhotoUri3) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    var currentPhotoType by remember { mutableStateOf<String?>(null) }
    var currentPhotoIndex by remember { mutableIntStateOf(-1) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (currentPhotoType == "PROFILE" && currentPhotoIndex in 0..2) {
                profilePhotoUris[currentPhotoIndex] = it.toString()
            } else if (currentPhotoType == "PASSPORT" && currentPhotoIndex in 0..2) {
                passportPhotoUris[currentPhotoIndex] = it.toString()
            }
        }
    }

    fun launchPhotoPicker(type: String, index: Int) {
        currentPhotoType = type
        currentPhotoIndex = index
        photoPickerLauncher.launch("image/*")
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
                    if (name.isNotBlank()) {
                        val updatedCustomer = customer.copy(
                            name = name,
                            phone = phone,
                            note = note,
                            profilePhotoUri = profilePhotoUris[0],
                            profilePhotoUri2 = profilePhotoUris[1],
                            profilePhotoUri3 = profilePhotoUris[2],
                            passportPhotoUri = passportPhotoUris[0],
                            passportPhotoUri2 = passportPhotoUris[1],
                            passportPhotoUri3 = passportPhotoUris[2]
                        )
                        customerViewModel.updateCustomer(updatedCustomer)
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
            // Profile Photos (3 Slots)
            Text("頭像照片 (最多3張)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 0..2) {
                    PhotoSlot(
                        uri = profilePhotoUris[i],
                        onClick = { launchPhotoPicker("PROFILE", i) },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        placeholderIcon = Icons.Default.Person
                    )
                }
            }

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )
            }

            // Passport Photos (3 Slots)
            Text("證件資料 (最多3張)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 0..2) {
                    PhotoSlot(
                        uri = passportPhotoUris[i],
                        onClick = { launchPhotoPicker("PASSPORT", i) },
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholderIcon = Icons.Default.CameraAlt
                    )
                }
            }

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
