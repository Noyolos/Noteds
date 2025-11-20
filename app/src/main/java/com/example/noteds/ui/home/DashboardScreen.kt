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
    onCustomerClick: (Long) -> Unit
) {
    val totalDebt by reportsViewModel.totalDebt.collectAsState()
    val topDebtors by reportsViewModel.topDebtors.collectAsState()
    val debtThisMonth by reportsViewModel.debtThisMonth.collectAsState()
    val repaymentThisMonth by reportsViewModel.repaymentThisMonth.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", color = Color.White, fontWeight = FontWeight.Bold) },
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
            item {
                HeroCard(totalDebt = totalDebt, currencyFormatter = currencyFormatter)
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = "Debt this Month",
                        amount = currencyFormatter.format(debtThisMonth),
                        amountColor = DebtRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Repayment this Month",
                        amount = currencyFormatter.format(repaymentThisMonth),
                        amountColor = PaymentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    "Top Debtors",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextBlack,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(
                items = topDebtors,
                key = { it.id }
            ) { debtor ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustomerClick(debtor.id) },
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
                                Text(
                                    "Outstanding",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
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
private fun HeroCard(totalDebt: Double, currencyFormatter: NumberFormat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TealPrimary),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Total Outstanding Debt", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                currencyFormatter.format(totalDebt),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "FinTech Overview",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 12.sp
                    )
                }
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