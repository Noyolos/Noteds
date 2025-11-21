package com.example.noteds.ui.customers

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.sp
import com.example.noteds.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Initial Debt Date
    var purchaseDate by remember { mutableStateOf("2025-11-20") } // Mock default like HTML

    // Photo URIs lists (3 slots each)
    val profilePhotoUris = remember { mutableStateListOf<String?>(null, null, null) }
    val passportPhotoUris = remember { mutableStateListOf<String?>(null, null, null) }

    // Launchers - we need separate logic or pass index to launcher if we want to support multiple slots properly
    // Or just update the specific index.
    // Compose launchers must be registered at top level.
    // A trick is to have one launcher and a state variable holding which index initiated the request.

    var currentPhotoType by remember { mutableStateOf<String?>(null) } // "PROFILE" or "PASSPORT"
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
                            profilePhotoUris = profilePhotoUris,
                            passportPhotoUris = passportPhotoUris,
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

                // Date Mock
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("購買日期", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = CardSurface, shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                             Spacer(modifier = Modifier.width(4.dp))
                             Text(purchaseDate, style = MaterialTheme.typography.bodySmall, color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoSlot(
    uri: String?,
    onClick: () -> Unit,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    placeholderIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (uri != null) CardSurface else BackgroundColor)
            .border(2.dp, if (uri != null) MidnightBlue else Color.LightGray, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
             // In real app: AsyncImage(model = uri, contentScale = ContentScale.Crop)
             Text("Photo", color = MidnightBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        } else {
            Icon(placeholderIcon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(VibrantOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TextWhite, modifier = Modifier.size(10.dp))
            }
        }
    }
}
