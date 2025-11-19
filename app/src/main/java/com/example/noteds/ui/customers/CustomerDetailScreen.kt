package com.example.noteds.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.noteds.ui.components.FullScreenImageDialog

@Composable
fun CustomerDetailScreen(
    customerId: Long,
    customerViewModel: CustomerViewModel,
    onClose: () -> Unit
) {
    val customers by customerViewModel.customersWithBalance.collectAsState()
    val selected = customers.firstOrNull { it.customer.id == customerId }

    LaunchedEffect(customers) {
        if (customers.isNotEmpty() && selected == null) {
            onClose()
        }
    }

    selected ?: return

    var fullScreenPhoto by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = selected.customer.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = selected.customer.phone,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selected.customer.note.isNotBlank()) {
                Text(
                    text = selected.customer.note,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            CustomerPhotoPicker(
                title = "Profile photo",
                subtitle = "Add or update the customer's avatar",
                currentPhotoUri = selected.customer.profilePhotoUri,
                onPhotoSelected = { customerViewModel.updateProfilePhoto(customerId, it) },
                onViewPhoto = { fullScreenPhoto = it }
            )

            CustomerPhotoPicker(
                title = "ID Card photo",
                subtitle = "Capture or select an ID card image",
                currentPhotoUri = selected.customer.idCardPhotoUri,
                onPhotoSelected = { customerViewModel.updateIdCardPhoto(customerId, it) },
                onViewPhoto = { fullScreenPhoto = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "Close")
            }
        }
    }

    fullScreenPhoto?.let { uri ->
        FullScreenImageDialog(
            photoUri = uri,
            onDismiss = { fullScreenPhoto = null }
        )
    }
}
