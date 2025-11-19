package com.example.noteds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    } else {
        modifier.clip(RoundedCornerShape(24.dp))
    }

    Card(
        modifier = clickableModifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun TotalDebtCard(
    totalDebt: Double,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val formatted = remember(totalDebt) { currencyFormatter.format(totalDebt) }
    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(
        colorScheme.primary,
        colorScheme.secondary
    ) {
        Brush.linearGradient(
            listOf(
                colorScheme.primary,
                colorScheme.secondary
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column {
            Text(
                text = "Total Outstanding Debt",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = formatted,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
