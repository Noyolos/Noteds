package com.example.noteds.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.ui.reports.ReportsViewModel
import com.example.noteds.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    reportsViewModel: ReportsViewModel,
    onCustomerClick: (Long) -> Unit // 新增回調函數
) {
    val totalDebt by reportsViewModel.totalDebt.collectAsState()
    val topDebtors by reportsViewModel.topDebtors.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("赊账小本本", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary),
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                    }
                }
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Card (Total Debt)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("当前总欠款", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            currencyFormatter.format(totalDebt),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Stats Row
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = "本月赊账",
                        amount = "RM 455.00",
                        amountColor = DebtRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "本月已还",
                        amount = "RM 120.00",
                        amountColor = PaymentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Top Debtors Header
            item {
                Text(
                    "欠款最多的客人",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextGray,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // 4. Top Debtors List
            items(
                items = topDebtors,
                key = { it.id } // 使用 ID 作為 Key
            ) { debtor ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustomerClick(debtor.id) }, // 點擊事件
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TealLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    debtor.name.take(1),
                                    color = TealDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(debtor.name, fontWeight = FontWeight.SemiBold, color = TextBlack)
                                Text("01x-xxxxxxx", style = MaterialTheme.typography.bodySmall, color = TextGray)
                            }
                        }
                        Text(
                            currencyFormatter.format(debtor.amount),
                            color = DebtRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun StatCard(title: String, amount: String, amountColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(amount, style = MaterialTheme.typography.titleLarge, color = amountColor, fontWeight = FontWeight.Bold)
        }
    }
}