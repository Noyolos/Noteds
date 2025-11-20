package com.example.noteds.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.ui.theme.BackgroundGray
import com.example.noteds.ui.theme.TealPrimary
import com.example.noteds.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    customer: CustomerEntity,
    transactionType: String, // "DEBT" or "PAYMENT"
    onBack: () -> Unit,
    onSave: (Double, String?, Long) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    val isDebt = transactionType == "DEBT"
    val title = if (isDebt) "Add Debt" else "Record Repayment"
    val buttonText = if (isDebt) "Save Debt" else "Save Payment"
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val selectedDateText = dateFormatter.format(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis()))

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) { Text("Done", color = Color.White) }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    val date = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    if (value != null) onSave(value, note.ifBlank { null }, date)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = BackgroundGray
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Customer", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }

            InputLabel("Date")
            OutlinedTextField(
                value = selectedDateText,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = TealPrimary.copy(alpha = 0.4f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            InputLabel(if (isDebt) "Amount (RM)" else "Repayment (RM)")
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.headlineSmall,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            InputLabel(if (isDebt) "Product / Note" else "Note")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                placeholder = { Text(if (isDebt) "e.g. Rice 10kg" else "Optional note", color = TextGray) },
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
        }
    }
}

@Composable
fun InputLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextGray,
        modifier = Modifier.padding(top = 8.dp)
    )
}
