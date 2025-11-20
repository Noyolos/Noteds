package com.example.noteds.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.data.model.CustomerWithBalance
import com.example.noteds.ui.reports.ReportsViewModel
import com.example.noteds.ui.theme.AccentOrange
import com.example.noteds.ui.theme.AccentLight
import com.example.noteds.ui.theme.BackgroundGray
import com.example.noteds.ui.theme.DebtRed
import com.example.noteds.ui.theme.PaymentGreen
import com.example.noteds.ui.theme.TealLight
import com.example.noteds.ui.theme.TealPrimary
import com.example.noteds.ui.theme.TextBlack
import com.example.noteds.ui.theme.TextGray
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    reportsViewModel: ReportsViewModel,
    customers: List<CustomerWithBalance>,
    onCustomerClick: (Long) -> Unit
) {
    val snapshot by reportsViewModel.dashboardSnapshot.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("老闆，您好 👋", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text("今日賬務概覽", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary),
                actions = {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(CircleShape)
                    )
                }
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroCard(totalDebt = snapshot.totalDebt, currencyFormatter = currencyFormatter)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile(
                        title = "本月新增賒賬",
                        amount = currencyFormatter.format(snapshot.monthDebt),
                        icon = Icons.Default.TrendingUp,
                        tint = DebtRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        title = "本月已收回款",
                        amount = currencyFormatter.format(snapshot.monthPayment),
                        icon = Icons.Default.TrendingDown,
                        tint = PaymentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SectionHeader(title = "重點關注 (Top Debtors)")
            }

            items(snapshot.topDebtors, key = { it.id }) { debtor ->
                val customer = customers.firstOrNull { it.customer.id == debtor.id }
                DebtorRow(
                    name = debtor.name,
                    phone = customer?.customer?.phone ?: "",
                    balance = debtor.amount,
                    onClick = { onCustomerClick(debtor.id) },
                    currencyFormatter = currencyFormatter
                )
            }
        }
    }
}

@Composable
private fun HeroCard(totalDebt: Double, currencyFormatter: NumberFormat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 0.dp, topEnd = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(TealPrimary, TealLight)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text("總待收回款項 (Total Debt)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    currencyFormatter.format(totalDebt),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentLight)
                    Text("資料已與後端同步", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Text(title, color = TextGray, fontSize = 12.sp)
            Text(amount, color = tint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("查看全部", color = AccentOrange, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun DebtorRow(
    name: String,
    phone: String,
    balance: Double,
    onClick: () -> Unit,
    currencyFormatter: NumberFormat
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealLight.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.take(1), color = TealPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(name, color = TextBlack, fontWeight = FontWeight.Bold)
                    Text(phone.ifBlank { "無電話" }, color = TextGray, fontSize = 12.sp)
                }
            }
            Text(currencyFormatter.format(balance), color = DebtRed, fontWeight = FontWeight.Bold)
        }
    }
}