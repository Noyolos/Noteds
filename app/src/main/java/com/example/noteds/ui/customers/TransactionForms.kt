package com.example.noteds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    customer: CustomerEntity,
    transactionType: String, // "DEBT" or "PAYMENT"
    onBack: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("19/11/2025") } // Mock date

    // UI Text Configuration
    val isDebt = transactionType == "DEBT"
    val title = if (isDebt) "记赊账" else "登记还钱"
    val buttonText = if (isDebt) "保存赊账" else "保存还款"

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
                    if (value != null) onSave(value, note)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(25.dp)
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
            // Customer Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = TealBackground.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealLight)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = TealLight,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(customer.name.take(1), color = TealDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("顾客", fontSize = 12.sp, color = TextGray)
                        Text(customer.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(customer.phone, fontSize = 14.sp, color = TextGray)
                    }
                }
            }

            // Date Input
            InputLabel("日期")
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            // Amount Input
            InputLabel(if(isDebt) "赊账金额 (RM) *" else "还款金额 (RM) *")
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.headlineSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            // Note/Product Input
            InputLabel(if(isDebt) "商品 / 备注" else "备注")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text(if(isDebt) "例如: Milo 2kg, 白米 10kg" else "添加备注信息 (可选)") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            // Payment Method (Only for Payment)
            if (!isDebt) {
                InputLabel("付款方式")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("现金", color = TealPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                // Mock other options
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                    Icon(Icons.Outlined.Circle, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("转账", color = Color.Gray)
                }
            }
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