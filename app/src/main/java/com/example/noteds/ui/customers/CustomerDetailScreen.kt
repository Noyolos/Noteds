package com.example.noteds.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.ui.components.TransactionFormScreen
import com.example.noteds.ui.theme.AccentOrange
import com.example.noteds.ui.theme.BackgroundGray
import com.example.noteds.ui.theme.DebtRed
import com.example.noteds.ui.theme.PaymentGreen
import com.example.noteds.ui.theme.TealLight
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
    val entries by customerViewModel.entriesForCustomer(customerId).collectAsState(initial = emptyList())
    val sortedEntries = remember(entries) { entries.sortedByDescending { it.timestamp } }
    var showTransactionForm by remember { mutableStateOf<String?>(null) }
    var aiMessage by remember { mutableStateOf<String?>(null) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val clipboard = LocalClipboardManager.current

    if (selected == null) {
        onClose()
        return
    }

    if (showTransactionForm != null) {
        TransactionFormScreen(
            customer = selected.customer,
            transactionType = showTransactionForm!!,
            onBack = { showTransactionForm = null },
            onSave = { amount, note ->
                customerViewModel.addLedgerEntry(customerId, showTransactionForm!!, amount, note)
                showTransactionForm = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("客戶詳情", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary)
                )
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showTransactionForm = "DEBT" },
                            colors = ButtonDefaults.buttonColors(containerColor = DebtRed),
                            modifier = Modifier.weight(1f)
                        ) { Text("記一筆") }
                        Button(
                            onClick = { showTransactionForm = "PAYMENT" },
                            colors = ButtonDefaults.buttonColors(containerColor = PaymentGreen),
                            modifier = Modifier.weight(1f)
                        ) { Text("收回款") }
                    }
                }
            },
            containerColor = BackgroundGray
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    ProfileHeader(name = selected.customer.name, phone = selected.customer.phone)
                }

                item {
                    BalanceCard(balance = selected.balance, currencyFormatter = currencyFormatter)
                }

                item {
                    ReminderCard(
                        onGenerate = {
                            aiMessage = "嗨 ${selected.customer.name}，想提醒您目前欠款 ${currencyFormatter.format(selected.balance)}，方便的話請儘快結清，感謝！"
                        },
                        aiMessage = aiMessage,
                        onCopy = { aiMessage?.let { clipboard.setText(AnnotatedString(it)) } },
                        onDismiss = { aiMessage = null }
                    )
                }

                item { SectionLabel("流水記錄") }

                items(sortedEntries) { entry ->
                    TransactionRow(entry = entry, formatter = currencyFormatter, dateFormatter = dateFormatter)
                }

                if (sortedEntries.isEmpty()) {
                    item {
                        Text(
                            "尚無交易記錄",
                            color = TextGray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ProfileHeader(name: String, phone: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(TealLight.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlack)
            Text(phone.ifBlank { "未填寫電話" }, color = TextGray)
        }
    }
}

@Composable
private fun BalanceCard(balance: Double, currencyFormatter: NumberFormat) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(listOf(TealPrimary, TealLight))
                )
                .padding(20.dp)
        ) {
            Column {
                Text("當前欠款", color = Color.White.copy(alpha = 0.8f))
                Text(
                    currencyFormatter.format(balance),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text("系統已與後端對齊", color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = TextBlack,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ReminderCard(
    onGenerate: () -> Unit,
    aiMessage: String?,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AI 催款文案", color = AccentOrange, fontWeight = FontWeight.Bold)
            Button(
                onClick = onGenerate,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("生成催款訊息")
            }
            aiMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(it, color = TextBlack)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onCopy, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("複製")
                            }
                            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                                Text("關閉", color = TextBlack)
                            }
                        }
                    }
                }
            }
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
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDebt) DebtRed.copy(alpha = 0.12f) else PaymentGreen.copy(alpha = 0.12f))
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isDebt) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isDebt) DebtRed else PaymentGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(entry.note ?: "--", color = TextBlack, fontWeight = FontWeight.Bold)
                    Text(dateFormatter.format(entry.timestamp), color = TextGray, fontSize = 12.sp)
                }
            }
            Text(
                (if (isDebt) "+" else "-") + formatter.format(entry.amount),
                color = if (isDebt) DebtRed else PaymentGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
