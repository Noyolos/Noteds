package com.example.noteds.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.noteds.data.model.CustomerWithBalance
import com.example.noteds.ui.components.AddCustomerDialog
import com.example.noteds.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    customerViewModel: CustomerViewModel,
    modifier: Modifier = Modifier,
    onCustomerClick: (CustomerWithBalance) -> Unit = {}
) {
    val customers by customerViewModel.customersWithBalance.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    // 搜尋文字狀態
    var searchQuery by remember { mutableStateOf("") }

    // 過濾邏輯
    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) {
            customers
        } else {
            customers.filter {
                it.customer.name.contains(searchQuery, ignoreCase = true) ||
                        it.customer.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("客人列表", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        containerColor = BackgroundGray
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜尋欄
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索姓名 / 电话", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredCustomers,
                    key = { it.customer.id }
                ) { item ->
                    CustomerCard(
                        item = item,
                        formatter = currencyFormatter,
                        onClick = { onCustomerClick(item) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddCustomerDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, phone, note ->
                    customerViewModel.addCustomer(name, phone, note, null, null)
                }
            )
        }
    }
}

@Composable
fun CustomerCard(
    item: CustomerWithBalance,
    formatter: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(TealLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.customer.name.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealDark
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text("Outstanding: ", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                    Text(
                        formatter.format(item.balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.balance > 0) DebtRed else PaymentGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = BackgroundGray,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.customer.phone.ifBlank { "No phone" }, color = TextGray, fontSize = 12.sp)
                        Text("View", color = TealPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextGray
            )
        }
    }
}
