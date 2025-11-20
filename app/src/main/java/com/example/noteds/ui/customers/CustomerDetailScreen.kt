package com.example.noteds.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                    title = { Text("Details", color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextWhite)
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEditClick(customerId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(CardSurface)
                        .navigationBarsPadding()
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
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Add Debt", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showTransactionForm = "PAYMENT" },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FunctionalGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Repay", fontWeight = FontWeight.Bold)
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
                // Header Profile
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MidnightBlue)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White),
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
                            color = TextWhite
                        )
                        Text(
                            text = selected.customer.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite.copy(alpha = 0.7f)
                        )
                    }
                }

                // Debt Card (Overlapping)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-20).dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(MidnightBlue, Color(0xFF303F9F))
                                        )
                                    )
                                    .padding(24.dp)
                                    .fillMaxWidth()
                            ) {
                                Column {
                                    Text("Current Balance", color = TextWhite.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currencyFormatter.format(selected.balance),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextWhite
                                    )
                                }
                            }
                        }
                    }
                }

                // Passport Photo (If available)
                if (selected.customer.passportPhotoUri != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Passport / ID", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.LightGray)
                                        .clickable { /* View Full Screen */ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Photo Preview", color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // Transactions Header
                item {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
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
                        Text("Delete Customer")
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Customer?") },
                text = { Text("This action cannot be undone. All transaction history will be lost.") },
                confirmButton = {
                    Button(
                        onClick = {
                            customerViewModel.deleteCustomer(customerId)
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FunctionalRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(1.dp)
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
                Text(
                    text = if (isDebt) "D" else "P",
                    fontWeight = FontWeight.Bold,
                    color = if (isDebt) DebtColor else PaymentColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note ?: (if (isDebt) "Debt" else "Payment"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = dateFormatter.format(Date(transaction.timestamp)),
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
