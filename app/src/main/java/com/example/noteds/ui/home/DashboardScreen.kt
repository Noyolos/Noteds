package com.example.noteds.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.ui.reports.DebtorData
import com.example.noteds.ui.reports.ReportsViewModel
import com.example.noteds.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // 1. Header Background Section (Custom Shape Container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(MidnightBlue)
        ) {
            // Header Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello Boss 👋",
                        color = TextWhite.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Overview",
                        color = TextWhite,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { /* Notification Action */ },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextWhite
                    )
                }
            }
        }

        // 2. Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 140.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Card (Total Outstanding Debt) with Trend Line
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Trend Line Background
                        TrendLineChart(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 60.dp),
                            lineColor = VibrantOrange.copy(alpha = 0.2f)
                        )

                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Text(
                                text = "Total Outstanding Debt",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currencyFormatter.format(totalDebt),
                                color = MidnightBlue,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp
                            )
                        }

                        // Accent Indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopRight)
                                .padding(24.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(VibrantOrange)
                        )
                    }
                }
            }

            // Stats Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Debt This Month",
                        amount = currencyFormatter.format(debtThisMonth),
                        amountColor = DebtColor,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Repayment",
                        amount = currencyFormatter.format(repaymentThisMonth),
                        amountColor = PaymentColor,
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Top Debtors Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Debtors",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { /* View All */ }) {
                        Text(
                            text = "See All",
                            color = VibrantOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top Debtors List
            items(
                items = topDebtors,
                key = { it.id }
            ) { debtor ->
                TopDebtorItem(
                    debtor = debtor,
                    currencyFormatter = currencyFormatter,
                    onClick = { onCustomerClick(debtor.id) }
                )
            }
        }
    }
}

@Composable
fun TrendLineChart(
    modifier: Modifier = Modifier,
    lineColor: Color
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Simulate trend points (mock data for visualization)
        // In a real app, pass points from ViewModel
        val points = listOf(0.8f, 0.6f, 0.7f, 0.4f, 0.5f, 0.2f, 0.3f)

        val path = Path()
        val stepX = width / (points.size - 1)

        points.forEachIndexed { index, ratio ->
            val x = index * stepX
            val y = height * ratio
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                // Smooth curve
                val prevX = (index - 1) * stepX
                val prevY = height * points[index - 1]
                val controlX1 = prevX + stepX / 2
                val controlX2 = x - stepX / 2
                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

        // Draw gradient fill below line
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw line
        drawPath(
            path = path,
            color = lineColor.copy(alpha = 0.8f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
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
        Column(modifier = Modifier.padding(20.dp)) {
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TopDebtorItem(
    debtor: DebtorData,
    currencyFormatter: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
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
            // Avatar (Initial)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = debtor.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name & ID
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debtor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ID: ...${debtor.id.toString().takeLast(4).padStart(4, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Amount
            Text(
                text = currencyFormatter.format(debtor.amount),
                style = MaterialTheme.typography.titleMedium,
                color = DebtColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
