package com.example.noteds.ui.customers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // 修复：使用 AutoMirrored
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.model.CustomerWithBalance
import com.example.noteds.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    customerViewModel: CustomerViewModel,
    modifier: Modifier = Modifier,
    parentId: Long? = null,
    onBack: (() -> Unit)? = null,
    onCustomerClick: (CustomerWithBalance) -> Unit = {},
    onAddCustomerClick: () -> Unit
) {
    val customersFlow = remember(parentId) { customerViewModel.getCustomers(parentId) }
    val customers by customersFlow.collectAsState(initial = emptyList())
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    var searchQuery by remember { mutableStateOf("") }
    var groupHead by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(parentId) {
        if (parentId != null) {
            groupHead = customerViewModel.getCustomer(parentId)
        }
    }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) {
            customers
        } else {
            customers.filter {
                it.customer.name.contains(searchQuery, ignoreCase = true) ||
                        it.customer.phone.contains(searchQuery)
            }
        }
    }

    val titleText = if (parentId == null) "客戶名錄" else (groupHead?.name ?: "載入中...")

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                customerViewModel.createFolder(name, parentId)
                showCreateFolderDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightBlue)
                    .statusBarsPadding()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            ) {
                // Top Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            // 修复：使用 AutoMirrored
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // 新建文件夹按钮
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = VibrantOrange
                        )
                    }
                }

                // Search Bar
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜尋姓名或電話...", color = TextWhite.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextWhite) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null, tint = TextWhite) } }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = TextWhite
                        ),
                        singleLine = true
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = VibrantOrange,
                contentColor = TextWhite,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = BackgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = filteredCustomers, key = { it.customer.id }) { item ->
                CustomerCard(
                    item = item,
                    formatter = currencyFormatter,
                    onClick = { onCustomerClick(item) },
                    onDelete = { customerViewModel.deleteCustomer(item.customer) }
                )
            }
        }
    }
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夾", color = MidnightBlue, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("文件夾名稱") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName) },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
            ) { Text("建立", color = TextWhite) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = CardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerCard(
    item: CustomerWithBalance,
    formatter: NumberFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("刪除${if(item.customer.isGroup) "文件夾" else "客戶"}") },
            text = {
                Text(
                    if (item.customer.isGroup) "確定要刪除文件夾「${item.customer.name}」嗎？\n內部的客戶將會全部被刪除。"
                    else "確定要刪除「${item.customer.name}」嗎？"
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = FunctionalRed)) {
                    Text("刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.customer.isGroup) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(VibrantOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = VibrantOrange, modifier = Modifier.size(24.dp))
                }
            } else if (!item.customer.profilePhotoUri.isNullOrBlank()) {
                AsyncImage(
                    model = item.customer.profilePhotoUri, contentDescription = "Avatar",
                    contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MidnightBlue.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item.customer.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MidnightBlue)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MidnightBlue
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.customer.isGroup) {
                        Text(
                            text = "文件夾",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.5f)
                        )
                        val balance = item.balance
                        val balanceText = when {
                            balance > 0 -> "總欠 ${formatter.format(balance)}"
                            balance < 0 -> "總存 ${formatter.format(kotlin.math.abs(balance))}"
                            else -> "無欠款"
                        }
                        val balanceColor = when {
                            balance > 0 -> DebtColor
                            balance < 0 -> PaymentColor
                            else -> TextSecondary
                        }
                        Text(
                            text = balanceText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = balanceColor,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = item.customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        val balanceText = when {
                            item.balance > 0 -> "欠 ${formatter.format(item.balance)}"
                            item.balance < 0 -> "預存 ${formatter.format(kotlin.math.abs(item.balance))}"
                            else -> "已結清"
                        }
                        val balanceColor = when {
                            item.balance > 0 -> DebtColor
                            item.balance < 0 -> PaymentColor
                            else -> TextSecondary
                        }
                        Text(
                            text = balanceText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = balanceColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}