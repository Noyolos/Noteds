package com.example.noteds.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    customer: CustomerEntity,
    transactionType: String, // "DEBT" or "PAYMENT"
    onBack: () -> Unit,
    onSave: (Double, String, Long) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") } // CASH or TRANSFER

    var smartInput by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val selectedDate = datePickerState.selectedDateMillis?.let {
        dateFormatter.format(Date(it))
    } ?: dateFormatter.format(Date())

    val isDebt = transactionType == "DEBT"
    val primaryColor = if (isDebt) DebtColor else PaymentColor
    val title = if (isDebt) "新增賒賬" else "登記還款"
    val buttonText = if (isDebt) "確認保存" else "確認保存"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    val timestamp = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val finalNote = if (!isDebt) "$note ($paymentMethod)" else note
                    if (value != null) onSave(value, finalNote, timestamp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        containerColor = CardSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Customer Card (Small)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundColor, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(customer.name.take(1).uppercase(), color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("交易對象", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(customer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MidnightBlue)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Smart Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = VibrantOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI 智慧輸入", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = VibrantOrange)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = smartInput,
                        onValueChange = { smartInput = it },
                        placeholder = { Text("試試輸入：三包米 45塊", style = MaterialTheme.typography.bodySmall, color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibrantOrange,
                            unfocusedBorderColor = VibrantOrange.copy(alpha = 0.2f),
                            focusedContainerColor = VibrantOrange.copy(alpha = 0.05f),
                            unfocusedContainerColor = VibrantOrange.copy(alpha = 0.05f)
                        ),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isParsing = true
                            // Mock parsing logic
                            isParsing = false
                        },
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = smartInput.isNotBlank() && !isParsing
                    ) {
                        Text(if(isParsing) "..." else "AI 填單", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Large Amount Input
            Text("金額 (RM)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
            TextField(
                value = amount,
                onValueChange = { amount = it },
                textStyle = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = primaryColor
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = {
                    Text("0.00",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Note & Date
            Text("備註 / 商品", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(if (isDebt) "例如：5包米，2箱水..." else "例如：現金還款...", color = TextSecondary.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BackgroundColor,
                    unfocusedContainerColor = BackgroundColor,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            // Payment Method Toggle (Only for Payment)
            if (!isDebt) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaymentMethodOption(
                        label = "現金",
                        selected = paymentMethod == "CASH",
                        color = primaryColor,
                        onClick = { paymentMethod = "CASH" },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentMethodOption(
                        label = "轉賬",
                        selected = paymentMethod == "TRANSFER",
                        color = primaryColor,
                        onClick = { paymentMethod = "TRANSFER" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Confirm", color = primaryColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun PaymentMethodOption(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) color.copy(alpha = 0.1f) else BackgroundColor
    val borderColor = if (selected) color else Color.Transparent

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Default.Check, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                color = if (selected) color else TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
