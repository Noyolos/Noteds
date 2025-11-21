package com.example.noteds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.noteds.data.model.CustomerWithBalance
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CustomerRow(
    item: CustomerWithBalance,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
    ) {
    ModernCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CustomerAvatar(
                    name = item.customer.name,
                    photoUri = item.customer.profilePhotoUri
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = item.customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.customer.code.isNotBlank()) {
                        Text(
                            text = "編號: ${'$'}{item.customer.code}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = item.customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
            val formattedBalance = remember(item.balance) {
                currencyFormatter.format(kotlin.math.abs(item.balance))
            }
            val balanceText = remember(item.balance, formattedBalance) {
                when {
                    item.balance > 0 -> "欠 $formattedBalance"
                    item.balance < 0 -> "預存 $formattedBalance"
                    else -> "已結清"
                }
            }
            val balanceColor = when {
                item.balance > 0 -> MaterialTheme.colorScheme.error
                item.balance < 0 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = balanceText,
                style = MaterialTheme.typography.titleMedium,
                color = balanceColor
            )
        }
    }
}

@Composable
private fun CustomerAvatar(name: String, photoUri: String?) {
    if (photoUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = remember(name) {
            name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifEmpty { "?" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
