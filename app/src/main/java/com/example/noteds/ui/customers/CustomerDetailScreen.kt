package com.example.noteds.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.ui.components.TransactionFormScreen
import com.example.noteds.ui.theme.BackgroundGray
import com.example.noteds.ui.theme.DebtRed
import com.example.noteds.ui.theme.DebtRedBg
import com.example.noteds.ui.theme.PaymentGreen
import com.example.noteds.ui.theme.TealPrimary
import com.example.noteds.ui.theme.TextBlack
import com.example.noteds.ui.theme.TextGray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    customerViewModel: CustomerViewModel,
    onClose: () -> Unit
) {
    val customers by customerViewModel.customersWithBalance.collectAsState()
    val selected = customers.firstOrNull { it.customer.id == customerId }
    val ledgerEntriesFlow = remember(customerId) {
        customerViewModel.ledgerEntriesForCustomer(customerId)
    }
    val ledgerEntries by ledgerEntriesFlow.collectAsState(initial = emptyList())
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var showTransactionForm by remember { mutableStateOf<String?>(null) }

    if (selected == null) {
        onClose()
        return
    }

    if (showTransactionForm != null) {
        TransactionFormScreen(
            customer = selected.customer,
            transactionType = showTransactionForm!!,
            onBack = { showTransactionForm = null },
            onSave = { amount, note, dateMillis ->
                customerViewModel.addLedgerEntry(customerId, showTransactionForm!!, amount, note, dateMillis)
                showTransactionForm = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Customer Details", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary)
                )
            },
            bottomBar = {
                Surface(
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showTransactionForm = "DEBT" },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Debt", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showTransactionForm = "PAYMENT" },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            border = BorderStroke(1.dp, TealPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Payment", color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            containerColor = BackgroundGray
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CustomerHeader(selected.customer.name, selected.customer.phone)
                }

                item {
                    CurrentDebtCard(balance = selected.balance, formatter = currencyFormatter)
                }

                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextBlack,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (ledgerEntries.isEmpty()) {
                    item {
                        Text(
                            text = "No transactions yet",
                            color = TextGray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(
                        items = ledgerEntries,
                        key = { it.id }
                    ) { entry ->
                        TransactionRow(entry = entry, formatter = currencyFormatter, dateFormatter = dateFormatter)
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun CustomerHeader(name: String, phone: String) {
    Surface(
        tonalElevation = 0.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(TealPrimary, TealPrimary.copy(alpha = 0.85f))
                    )
                )
                .padding(vertical = 24.dp, horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        name.take(1),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(phone.ifBlank { "No phone" }, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun CurrentDebtCard(balance: Double, formatter: NumberFormat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DebtRedBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DebtRed.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Current Debt", color = TextGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatter.format(balance),
                color = DebtRed,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Keep track of upcoming repayments", color = TextGray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TransactionRow(
    entry: LedgerEntryEntity,
    formatter: NumberFormat,
    dateFormatter: SimpleDateFormat
) {
    val isDebt = entry.type.equals("DEBT", ignoreCase = true)
    val pillColor = if (isDebt) DebtRed else PaymentGreen
    val amountPrefix = if (isDebt) "+" else "-"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(dateFormatter.format(entry.timestamp), color = TextGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = pillColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            if (isDebt) "Debt" else "Payment",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(entry.note.orEmpty().ifBlank { "No note" }, color = TextBlack)
                }
            }
            Text(
                "$amountPrefix${formatter.format(entry.amount)}",
                color = pillColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
