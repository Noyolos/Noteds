package com.example.noteds.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onClose: () -> Unit
) {
    val customers by customerViewModel.customersWithBalance.collectAsState()
    val selected = customers.firstOrNull { it.customer.id == customerId }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Fetch transactions
    val transactions by customerViewModel.getTransactionsForCustomer(customerId)
        .collectAsState(initial = emptyList())

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
            onSave = { amount, note, timestamp ->
                customerViewModel.addLedgerEntry(customerId, showTransactionForm!!, amount, note, timestamp)
                showTransactionForm = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("客人详情", color = Color.White) },
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
                    shadowElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showTransactionForm = "DEBT" },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("记赊账")
                        }
                        OutlinedButton(
                            onClick = { showTransactionForm = "PAYMENT" },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            border = BorderStroke(1.dp, TealPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("登记还钱", color = TealPrimary)
                        }
                    }
                }
            },
            containerColor = BackgroundGray
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(TealLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                selected.customer.name.take(1),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealDark
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(selected.customer.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(selected.customer.phone, color = TextGray)
                            Text("B98765432", color = TextGray)
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DebtRedBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DebtRed.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("当前欠款", color = TextGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                currencyFormatter.format(selected.balance),
                                color = DebtRed,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val lastTransaction = transactions.maxByOrNull { it.timestamp }
                            val lastDate = if (lastTransaction != null) dateFormatter.format(Date(lastTransaction.timestamp)) else "无记录"
                            Text("最后还钱: $lastDate", color = TextGray, fontSize = 14.sp)
                        }
                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = 0,
                        containerColor = Color.White,
                        contentColor = TealPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[0]),
                                color = TealPrimary
                            )
                        }
                    ) {
                        Tab(selected = true, onClick = {}, text = { Text("流水记录") })
                        Tab(selected = false, onClick = {}, text = { Text("图表") })
                    }
                }

                items(transactions.sortedByDescending { it.timestamp }) { transaction ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                Text(dateFormatter.format(Date(transaction.timestamp)), color = TextGray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isDebt = transaction.type == "DEBT"
                                    Surface(
                                        color = if (isDebt) DebtRed else PaymentGreen,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            if (isDebt) "赊账" else "还款",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(transaction.note ?: (if(isDebt) "商品" else "还款"), color = TextBlack)
                                }
                            }
                            Text(
                                "${if (transaction.type == "DEBT") "+" else "-"} ${currencyFormatter.format(transaction.amount)}",
                                color = if (transaction.type == "DEBT") DebtRed else PaymentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
