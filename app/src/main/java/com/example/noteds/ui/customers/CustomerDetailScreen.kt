package com.example.noteds.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showAiMessage by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    if (selected == null) {
        onClose()
        return
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
                    title = { Text("客戶詳情", color = TextPrimary, fontWeight = FontWeight.Bold) }, // HTML style: dark text on white? No, previous code had dark bg. HTML has white header.
                    // HTML Detail Screen Header is WHITE with dark text.
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface) // White header
                )
            },
            bottomBar = {
                // Bottom Action Bar matching HTML style
                Column(
                    modifier = Modifier
                        .background(CardSurface)
                        .navigationBarsPadding()
                        .shadow(elevation = 10.dp) // Shadow float
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
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp)) // Use arrow down/back for payment
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

                        // Generate Reminder Button (AI)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isGenerating = true
                                // Simulate delay
                                showAiMessage = true
                                isGenerating = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue.copy(alpha = 0.1f)),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            enabled = !isGenerating
                        ) {
                            if (isGenerating) {
                                Text("生成中...", color = MidnightBlue, style = MaterialTheme.typography.labelMedium)
                            } else {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(16.dp)) // Sparkles substitute
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("生成催款訊息", color = MidnightBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Passport Photo (Preview)
                if (selected.customer.passportPhotoUri != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            elevation = CardDefaults.cardElevation(2.dp) // shadow-soft
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("證件照片", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BackgroundColor)
                                        .border(2.dp, Color.LightGray, RoundedCornerShape(12.dp), /* dashed? No simple dashed in Compose without path effect, keeping solid */)
                                        .clickable { /* View */ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("已存檔", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // AI Message Box
                if (showAiMessage) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardSurface)
                                .border(1.dp, VibrantOrange.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = VibrantOrange, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI 建議文案", color = VibrantOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp).clickable { showAiMessage = false })
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "您好 ${selected.customer.name}，這是一個溫馨提醒。您目前的未結餘額為 RM ${currencyFormatter.format(selected.balance)}。如方便請安排付款，謝謝！",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { /* Copy */ },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MidnightBlue.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("複製文案", color = MidnightBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
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
                                        colors = listOf(MidnightBlue, Color(0xFF534BAE)) // brand-blue to brand-light
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("信用評級: A+", color = TextWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                                    Text("最後交易: 3天前", color = TextWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // shadow-soft
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
                    imageVector = if (isDebt) Icons.Default.Add else Icons.Default.ArrowBack, // TrendingUp/Down substitute
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
