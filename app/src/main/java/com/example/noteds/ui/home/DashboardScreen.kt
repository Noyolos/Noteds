package com.example.noteds.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun DashboardScreen(
    reportsViewModel: ReportsViewModel,
    onCustomerClick: (Long) -> Unit
) {
    val totalDebt by reportsViewModel.totalDebt.collectAsState()
    val topDebtors by reportsViewModel.topDebtors.collectAsState()
    val debtThisMonth by reportsViewModel.debtThisMonth.collectAsState()
    val repaymentThisMonth by reportsViewModel.repaymentThisMonth.collectAsState()

    // Real trend data
    val totalDebtTrend by reportsViewModel.totalDebtTrend.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // 1. Header Background Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)) // Refined radius
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
                        text = "老闆，您好 👋",
                        color = TextWhite.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "今日賬務概覽",
                        color = TextWhite,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { /* Notification Action */ },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp), // Adjusted padding
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp) // Shadow float
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Trend Line with real data
                        TrendLineChart(
                            dataPoints = totalDebtTrend,
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
                                text = "總待收回款項 (TOTAL DEBT)",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currencyFormatter.format(totalDebt),
                                color = MidnightBlue,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            )
                        }

                        // Icon Box
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(VibrantOrange.copy(alpha = 0.1f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward, // Placeholder for TrendingUp
                                contentDescription = null,
                                tint = VibrantOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Quick Stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "本月新增賒賬",
                        amount = currencyFormatter.format(debtThisMonth),
                        amountColor = DebtColor,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "本月已收回款",
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
                        text = "重點關注 (Top Debtors)",
                        color = MidnightBlue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "查看全部",
                        color = VibrantOrange,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { /* View All */ }
                    )
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
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color
) {
    if (dataPoints.isEmpty()) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val points = dataPoints

        val path = Path()
        val stepX = width / (points.size - 1)

        points.forEachIndexed { index, ratio ->
            // Ensure ratio is used correctly.
            // 1.0 means max debt -> top of chart? Or bottom?
            // Usually chart 0 is bottom.
            // y = height - (height * ratio)
            val x = index * stepX
            val y = height - (height * ratio) // Invert for display (0 at bottom)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevY = height - (height * points[index - 1])
                val controlX1 = prevX + stepX / 2
                val controlX2 = x - stepX / 2
                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

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
        drawPath(
            path = path,
            color = VibrantOrange, // Solid Orange Line
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // soft shadow
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .height(80.dp), // Fixed height ~28 in HTML logic (h-28 = 7rem = 112px?), adjusted visually
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp) // align with padding
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // shadow-soft
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
                    .clip(RoundedCornerShape(12.dp)) // Rounded Square
                    .background(BackgroundColor)
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debtor.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue
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
