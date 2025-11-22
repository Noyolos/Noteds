@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.example.noteds.ui.customers

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.model.TransactionType
import com.example.noteds.ui.components.FullScreenImageDialog
import com.example.noteds.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun CustomerDetailScreen(
    customerId: Long,
    customerViewModel: CustomerViewModel,
    onClose: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val customers by customerViewModel.customersWithBalance.collectAsState()
    val selected = customers.firstOrNull { it.customer.id == customerId }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val transactions by customerViewModel.getTransactionsForCustomer(customerId)
        .collectAsState(initial = emptyList())

    var showTransactionForm by remember { mutableStateOf<TransactionType?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var entryForEdit by remember { mutableStateOf<LedgerEntryEntity?>(null) }
    var entryPendingDelete by remember { mutableStateOf<LedgerEntryEntity?>(null) }
    var fullScreenPhoto by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    if (selected == null) {
        onClose()
        return
    }

    val profilePhotos = remember(selected.customer) {
        listOf(
            selected.customer.profilePhotoUri,
            selected.customer.profilePhotoUri2,
            selected.customer.profilePhotoUri3
        ).filterNotNull()
    }
    val passportPhotos = remember(selected.customer) {
        listOf(
            selected.customer.passportPhotoUri,
            selected.customer.passportPhotoUri2,
            selected.customer.passportPhotoUri3,
            selected.customer.idCardPhotoUri
        ).filterNotNull()
    }
    val allPhotos = remember(profilePhotos, passportPhotos) {
        (profilePhotos + passportPhotos).filter { it.isNotBlank() }
    }
    val primaryPhoto = profilePhotos.firstOrNull()

    val lastPaymentDaysText = remember(transactions) {
        val lastPayment = transactions
            .filter { it.type.uppercase() == TransactionType.PAYMENT.dbValue }
            .maxByOrNull { it.timestamp }
        lastPayment?.let {
            val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.timestamp)
                .coerceAtLeast(0)
            "${days}天前"
        } ?: "無記錄"
    }

    if (showTransactionForm != null) {
        TransactionFormScreen(
            customer = selected.customer,
                transactionType = showTransactionForm!!,
                onBack = { showTransactionForm = null },
                onSave = { amount, note, timestamp ->
                    customerViewModel.addLedgerEntry(
                        customerId,
                        showTransactionForm!!,
                        amount,
                        note,
                        timestamp
                    )
                showTransactionForm = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("客戶詳情", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEditClick(customerId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(CardSurface)
                        .navigationBarsPadding()
                        .shadow(elevation = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { showTransactionForm = TransactionType.DEBT },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DebtColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("記一筆", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showTransactionForm = TransactionType.PAYMENT },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaymentColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("收回款", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            containerColor = BackgroundColor
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Profile Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (primaryPhoto != null) {
                            AsyncImage(
                                model = primaryPhoto,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(4.dp, Color.White, RoundedCornerShape(24.dp))
                                    .clickable { fullScreenPhoto = primaryPhoto },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(BackgroundColor)
                                    .border(4.dp, Color.White, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selected.customer.name.take(1).uppercase(),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MidnightBlue
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = selected.customer.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (selected.customer.code.isNotBlank()) {
                            Text(
                                text = "編號: ${selected.customer.code}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = selected.customer.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Debt Card (Gradient)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(MidnightBlue, Color(0xFF534BAE))
                                    )
                                )
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            Column {
                                Text("當前欠款", color = TextWhite.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currencyFormatter.format(selected.balance),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = TextWhite.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "最近一次還款: $lastPaymentDaysText",
                                        color = TextWhite.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                if (allPhotos.isNotEmpty()) {
                    item {
                        Text(
                            text = "照片畫廊",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(allPhotos) { photo ->
                                AsyncImage(
                                    model = photo,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { fullScreenPhoto = photo },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Transactions Header
                item {
                    Text(
                        text = "流水記錄",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                // Transaction List
                items(transactions.sortedByDescending { it.timestamp }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        dateFormatter = dateFormatter,
                        currencyFormatter = currencyFormatter,
                        onLongPress = { entryForEdit = transaction }
                    )
                }

                // Delete Button
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = FunctionalRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("刪除客戶檔案")
                    }
                }
            }
        }

        entryForEdit?.let { entry ->
            EditTransactionDialog(
                entry = entry,
                onDismiss = { entryForEdit = null },
                onSave = { updated ->
                    customerViewModel.updateLedgerEntry(updated)
                    entryForEdit = null
                },
                onDelete = {
                    entryForEdit = null
                    entryPendingDelete = entry
                }
            )
        }

        entryPendingDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { entryPendingDelete = null },
                title = { Text("刪除此筆紀錄？") },
                text = { Text("此操作將移除該筆交易，但不影響其他資料。") },
                confirmButton = {
                    Button(
                        onClick = {
                            customerViewModel.deleteLedgerEntry(entry.id)
                            entryPendingDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FunctionalRed)
                    ) {
                        Text("刪除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryPendingDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("確定要刪除嗎？") },
                text = { Text("此操作將停用該客戶，但交易紀錄仍會被保留以避免財務資料遺失。") },
                confirmButton = {
                    Button(
                        onClick = {
                            customerViewModel.deleteCustomer(customerId)
                            onClose()
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

        fullScreenPhoto?.let { uri ->
            FullScreenImageDialog(
                photoUri = uri,
                onDismiss = { fullScreenPhoto = null },
                onDownload = {
                    customerViewModel.saveImageToGallery(uri) { success ->
                        Toast.makeText(
                            context,
                            if (success) "圖片已下載到相冊" else "下載失敗，請稍後再試",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: LedgerEntryEntity,
    dateFormatter: SimpleDateFormat,
    currencyFormatter: NumberFormat,
    onLongPress: () -> Unit = {}
) {
    val isDebt = transaction.type == TransactionType.DEBT.dbValue
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDebt) DebtColor.copy(alpha = 0.1f) else PaymentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDebt) Icons.Default.Add else Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = if (isDebt) DebtColor else PaymentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note ?: (if (isDebt) "賒賬" else "還款"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${dateFormatter.format(Date(transaction.timestamp))} · ${if (isDebt) "賒賬" else "還款"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Text(
                text = "${if (isDebt) "+" else "-"} ${currencyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDebt) DebtColor else PaymentColor
            )
        }
    }
}

@Composable
fun EditTransactionDialog(
    entry: LedgerEntryEntity,
    onDismiss: () -> Unit,
    onSave: (LedgerEntryEntity) -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember { mutableStateOf(entry.amount.toString()) }
    var noteText by remember { mutableStateOf(entry.note.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = entry.timestamp)
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val selectedDate = datePickerState.selectedDateMillis ?: entry.timestamp
    val selectedDateLabel = remember(selectedDate) { dateFormatter.format(Date(selectedDate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編輯交易") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金額") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("備註") }
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDateLabel)
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = FunctionalRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刪除此筆")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    onSave(
                        entry.copy(
                            amount = amount,
                            note = noteText.ifBlank { null },
                            timestamp = selectedDate
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (entry.type == TransactionType.DEBT.dbValue) DebtColor else PaymentColor
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("確認")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}