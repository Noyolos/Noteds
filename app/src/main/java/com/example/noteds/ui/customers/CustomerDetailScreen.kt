package com.example.noteds.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.noteds.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
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

    var showTransactionForm by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var zoomedPhotoUri by remember { mutableStateOf<String?>(null) }

    if (selected == null) {
        onClose()
        return
    }

    // Collect all photos
    val allPhotos = remember(selected.customer) {
        listOfNotNull(
            selected.customer.profilePhotoUri,
            selected.customer.profilePhotoUri2,
            selected.customer.profilePhotoUri3,
            selected.customer.passportPhotoUri,
            selected.customer.passportPhotoUri2,
            selected.customer.passportPhotoUri3
        )
    }

    // Last Payment Calculation
    val lastPaymentDate = remember(transactions) {
        transactions
            .filter { it.type.equals("PAYMENT", ignoreCase = true) }
            .maxByOrNull { it.timestamp }
            ?.timestamp
    }

    val lastPaymentText = remember(lastPaymentDate) {
        if (lastPaymentDate != null) {
            val diff = System.currentTimeMillis() - lastPaymentDate
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            if (days == 0L) "今天" else "${days}天前"
        } else {
            "無記錄"
        }
    }

    if (showTransactionForm != null) {
        TransactionFormScreen(
            customer = selected.customer,
            transactionType = showTransactionForm!!,
            onBack = { showTransactionForm = null },
            onSave = { amount, note, timestamp ->
                customerViewModel.addLedgerEntry(customerId, showTransactionForm!!, amount, note, timestamp)
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
                            onClick = { showTransactionForm = "DEBT" },
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
                            onClick = { showTransactionForm = "PAYMENT" },
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
                // Profile Section (Header)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(BackgroundColor)
                                .border(4.dp, Color.White, RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected.customer.profilePhotoUri != null) {
                                 AsyncImage(
                                     model = selected.customer.profilePhotoUri,
                                     contentDescription = null,
                                     contentScale = ContentScale.Crop,
                                     modifier = Modifier.fillMaxSize()
                                 )
                            } else {
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
                            .padding(horizontal = 24.dp, vertical = 8.dp),
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
                                Text("最近一次還款：$lastPaymentText", color = TextWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Photo Gallery
                if (allPhotos.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 16.dp)) {
                             Text(
                                text = "照片資料",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(allPhotos) { photoUri ->
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.LightGray)
                                            .clickable { zoomedPhotoUri = photoUri }
                                    ) {
                                        AsyncImage(
                                            model = photoUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
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
                    TransactionItem(transaction, dateFormatter, currencyFormatter)
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

        // Full Screen Zoom Dialog
        if (zoomedPhotoUri != null) {
            Dialog(
                onDismissRequest = { zoomedPhotoUri = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { zoomedPhotoUri = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = zoomedPhotoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("確定要刪除嗎？") },
                text = { Text("此操作將永久刪除該客戶及其所有交易記錄，且無法復原。") },
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
    }
}

@Composable
fun TransactionItem(
    transaction: com.example.noteds.data.entity.LedgerEntryEntity,
    dateFormatter: SimpleDateFormat,
    currencyFormatter: NumberFormat
) {
    val isDebt = transaction.type == "DEBT"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
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
