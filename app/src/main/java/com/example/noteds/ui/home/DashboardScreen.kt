package com.example.noteds.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.noteds.ui.components.rememberThumbnailImageRequest
import com.example.noteds.ui.i18n.LocalAppLanguage
import com.example.noteds.ui.i18n.pick
import com.example.noteds.ui.i18n.rememberCurrencyFormatter
import com.example.noteds.ui.reports.DebtorData
import com.example.noteds.ui.reports.ReportsViewModel
import com.example.noteds.ui.theme.BackgroundColor
import com.example.noteds.ui.theme.CardSurface
import com.example.noteds.ui.theme.DebtColor
import com.example.noteds.ui.theme.MidnightBlue
import com.example.noteds.ui.theme.PaymentColor
import com.example.noteds.ui.theme.TextSecondary
import com.example.noteds.ui.theme.TextWhite
import com.example.noteds.ui.theme.VibrantOrange
import java.text.NumberFormat
import kotlin.math.abs

@Composable
fun DashboardScreen(
    reportsViewModel: ReportsViewModel,
    onCustomerClick: (Long) -> Unit
) {
    val language = LocalAppLanguage.current
    val totalDebt by reportsViewModel.totalDebt.collectAsStateWithLifecycle()
    val topDebtors by reportsViewModel.topDebtors.collectAsStateWithLifecycle()
    val debtThisMonth by reportsViewModel.debtThisMonth.collectAsStateWithLifecycle()
    val repaymentThisMonth by reportsViewModel.repaymentThisMonth.collectAsStateWithLifecycle()
    val outstandingCustomerCount by reportsViewModel.outstandingCustomerCount.collectAsStateWithLifecycle()
    val currencyFormatter = rememberCurrencyFormatter()

    val netChangeThisMonth = debtThisMonth - repaymentThisMonth
    val netChangeLabel = when {
        netChangeThisMonth > 0 -> language.pick(
            "本月净变化 +${currencyFormatter.format(abs(netChangeThisMonth))}",
            "Net this month +${currencyFormatter.format(abs(netChangeThisMonth))}"
        )
        netChangeThisMonth < 0 -> language.pick(
            "本月净变化 -${currencyFormatter.format(abs(netChangeThisMonth))}",
            "Net this month -${currencyFormatter.format(abs(netChangeThisMonth))}"
        )
        else -> language.pick("本月净变化 RM0.00", "Net this month RM0.00")
    }
    val outstandingCustomersLabel = language.pick(
        "$outstandingCustomerCount 位未结清",
        "$outstandingCustomerCount active debtors"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = language.pick("财务总览", "Dashboard"),
                    color = MidnightBlue,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = language.pick("先看最重要的欠款与回款", "Your most important debt and payment numbers"),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            HeroCard(
                totalDebt = currencyFormatter.format(totalDebt),
                netChangeLabel = netChangeLabel,
                outstandingCustomersLabel = outstandingCustomersLabel,
                debtThisMonth = currencyFormatter.format(debtThisMonth),
                repaymentThisMonth = currencyFormatter.format(repaymentThisMonth)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.pick("重点关注 Top Debtors", "Top Debtors"),
                    color = MidnightBlue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = language.pick("查看全部", "View all"),
                    color = VibrantOrange,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {}
                )
            }
        }

        items(
            items = topDebtors,
            key = { it.id },
            contentType = { "debtor" }
        ) { debtor ->
            TopDebtorItem(
                debtor = debtor,
                currencyFormatter = currencyFormatter,
                onClick = { onCustomerClick(debtor.id) }
            )
        }
    }
}

@Composable
private fun HeroCard(
    totalDebt: String,
    netChangeLabel: String,
    outstandingCustomersLabel: String,
    debtThisMonth: String,
    repaymentThisMonth: String
) {
    val language = LocalAppLanguage.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, CardSurface)
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Top total debt",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                text = totalDebt,
                color = MidnightBlue,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold
            )
            HeroInsightRow(
                primaryText = netChangeLabel,
                secondaryText = outstandingCustomersLabel
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroMetricBlock(
                    title = language.pick("本月新增 Debt", "Debt added"),
                    value = debtThisMonth,
                    accent = DebtColor,
                    icon = Icons.Default.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricBlock(
                    title = language.pick("本月收回 Payment", "Payments"),
                    value = repaymentThisMonth,
                    accent = PaymentColor,
                    icon = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroMetricBlock(
    title: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeroInsightRow(
    primaryText: String,
    secondaryText: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeroInsightChip(text = primaryText)
        HeroInsightChip(text = secondaryText)
    }
}

@Composable
private fun HeroInsightChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MidnightBlue.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MidnightBlue,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatCard(
    title: String,
    amount: String,
    amountColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .height(80.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(amountColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TopDebtorItem(
    debtor: DebtorData,
    currencyFormatter: NumberFormat,
    onClick: () -> Unit
) {
    val imageRequest = rememberThumbnailImageRequest(debtor.photoUri, 48.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundColor)
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!debtor.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = debtor.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MidnightBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debtor.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue,
                    maxLines = 1
                )
                Text(
                    text = "ID: ...${debtor.id.toString().takeLast(4).padStart(4, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = currencyFormatter.format(debtor.amount),
                style = MaterialTheme.typography.titleMedium,
                color = DebtColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
