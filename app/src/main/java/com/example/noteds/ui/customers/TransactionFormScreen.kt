@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.noteds.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.noteds.data.model.PaymentMethod
import com.example.noteds.data.model.TransactionType
import com.example.noteds.ui.components.AppScreenHeader
import com.example.noteds.ui.i18n.LocalAppLanguage
import com.example.noteds.ui.i18n.pick
import com.example.noteds.ui.i18n.rememberAppDateFormatter
import com.example.noteds.ui.theme.BackgroundColor
import com.example.noteds.ui.theme.CardSurface
import com.example.noteds.ui.theme.DebtColor
import com.example.noteds.ui.theme.FunctionalRed
import com.example.noteds.ui.theme.MidnightBlue
import com.example.noteds.ui.theme.PaymentColor
import com.example.noteds.ui.theme.TextPrimary
import com.example.noteds.ui.theme.TextSecondary
import java.util.Date

@Composable
fun TransactionFormScreen(
    customer: CustomerEntity,
    transactionType: TransactionType,
    onBack: () -> Unit,
    onSave: (Double, String, Long) -> Unit
) {
    val language = LocalAppLanguage.current
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val dateFormatter = rememberAppDateFormatter()

    val selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val selectedDate = remember(selectedDateMillis, dateFormatter) { dateFormatter.format(Date(selectedDateMillis)) }

    val isDebt = transactionType == TransactionType.DEBT
    val primaryColor = if (isDebt) DebtColor else PaymentColor
    val title = if (isDebt) {
        language.pick("新增赊账", "Add Debt")
    } else {
        language.pick("登记回款", "Record Payment")
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    val finalNote = if (!isDebt && note.isNotBlank()) {
                        "$note (${paymentMethod.dbValue})"
                    } else {
                        note
                    }
                    if (value != null && value > 0) {
                        amountError = null
                        onSave(value, finalNote, selectedDateMillis)
                    } else {
                        amountError = language.pick("金额必须大于 0", "Amount must be greater than 0")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(language.pick("确认保存", "Save"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        containerColor = CardSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(bottom = 24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppScreenHeader(
                title = title,
                subtitle = language.pick("记录一笔新的客户往来", "Record a new customer transaction"),
                onBack = onBack,
                modifier = Modifier.align(Alignment.Start)
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(24.dp))

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
                    Text(language.pick("交易对象", "Customer"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(customer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MidnightBlue)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(language.pick("金额 (RM)", "Amount (RM)"), style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
            TextField(
                value = amount,
                onValueChange = {
                    amount = it
                    amountError = null
                },
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
                    Text(
                        "0.00",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )

            if (amountError != null) {
                Text(
                    text = amountError!!,
                    color = FunctionalRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                language.pick("日期", "Date"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = selectedDate,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BackgroundColor,
                    unfocusedContainerColor = BackgroundColor
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                language.pick("备注 / 商品", "Note / Item"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = {
                    Text(
                        if (isDebt) {
                            language.pick("例如：5包米，2箱水...", "For example: rice, drinks...")
                        } else {
                            language.pick("例如：现金还款...", "For example: cash payment...")
                        },
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BackgroundColor,
                    unfocusedContainerColor = BackgroundColor,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            if (!isDebt) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodOption(
                        label = language.pick("现金", "Cash"),
                        selected = paymentMethod == PaymentMethod.CASH,
                        color = primaryColor,
                        onClick = { paymentMethod = PaymentMethod.CASH },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentMethodOption(
                        label = "TNG",
                        selected = paymentMethod == PaymentMethod.TNG,
                        color = primaryColor,
                        onClick = { paymentMethod = PaymentMethod.TNG },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentMethodOption(
                        label = language.pick("转账", "Bank"),
                        selected = paymentMethod == PaymentMethod.BANK_TRANSFER,
                        color = primaryColor,
                        onClick = { paymentMethod = PaymentMethod.BANK_TRANSFER },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(language.pick("确认", "Confirm"), color = primaryColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(language.pick("取消", "Cancel"), color = TextSecondary)
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (selected) {
                Icon(Icons.Default.Check, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = if (selected) color else TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
