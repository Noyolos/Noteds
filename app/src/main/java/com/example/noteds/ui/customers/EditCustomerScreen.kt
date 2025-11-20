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
    var profilePhotoUri by remember { mutableStateOf(customer.profilePhotoUri) }
    var passportPhotoUri by remember { mutableStateOf(customer.passportPhotoUri) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val profilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { profilePhotoUri = it.toString() }
    }

    val passportPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { passportPhotoUri = it.toString() }
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
                            profilePhotoUri = profilePhotoUri,
                            passportPhotoUri = passportPhotoUri
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
            // Profile Photo
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(if (profilePhotoUri != null) CardSurface else BackgroundColor)
                        .border(2.dp, if (profilePhotoUri != null) MidnightBlue else Color.LightGray, CircleShape)
                        .clickable { profilePhotoLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePhotoUri != null) {
                         // Assuming AsyncImage would be here
                         Text("Photo", color = MidnightBlue, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = 36.dp, y = (-4).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(VibrantOrange)
                        .border(2.dp, CardSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )
            }

            // Passport Photo
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("證件資料", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .border(2.dp, if (passportPhotoUri != null) MidnightBlue.copy(alpha = 0.1f) else Color.LightGray, RoundedCornerShape(12.dp))
                        .clickable { passportPhotoLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (passportPhotoUri != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MidnightBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("已選取護照/IC照片", color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }
                    } else {
                         Text("點擊上傳護照 / IC 照片", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
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
