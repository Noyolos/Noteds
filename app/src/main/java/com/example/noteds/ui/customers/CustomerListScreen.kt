package com.example.noteds.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.noteds.data.entity.CustomerEntity
import com.example.noteds.data.model.CustomerWithBalance
import com.example.noteds.ui.components.AppScreenHeader
import com.example.noteds.ui.components.rememberThumbnailImageRequest
import com.example.noteds.ui.i18n.LocalAppLanguage
import com.example.noteds.ui.i18n.pick
import com.example.noteds.ui.i18n.rememberCurrencyFormatter
import com.example.noteds.ui.theme.BackgroundColor
import com.example.noteds.ui.theme.CardSurface
import com.example.noteds.ui.theme.DebtColor
import com.example.noteds.ui.theme.FunctionalRed
import com.example.noteds.ui.theme.MidnightBlue
import com.example.noteds.ui.theme.PaymentColor
import com.example.noteds.ui.theme.TextSecondary
import com.example.noteds.ui.theme.TextWhite
import com.example.noteds.ui.theme.VibrantOrange
import kotlin.math.abs

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
    val language = LocalAppLanguage.current
    val customersFlow = remember(parentId) { customerViewModel.getCustomers(parentId) }
    val customers by customersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var groupHead by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var pendingDelete by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var pendingMove by remember { mutableStateOf<CustomerWithBalance?>(null) }

    LaunchedEffect(parentId) {
        groupHead = if (parentId != null) customerViewModel.getCustomer(parentId) else null
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

    val titleText = if (parentId == null) {
        language.pick("客户名单", "Customers")
    } else {
        groupHead?.name ?: language.pick("载入中...", "Loading...")
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                customerViewModel.createFolder(name, parentId)
                showCreateFolderDialog = false
            }
        )
    }

    selectedItem?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            title = { Text(language.pick("操作选项", "Actions"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedItem = null
                            pendingMove = item
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(language.pick("移动到文件夹", "Move to folder"))
                    }

                    OutlinedButton(
                        onClick = {
                            selectedItem = null
                            pendingDelete = item
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FunctionalRed),
                        border = BorderStroke(1.dp, FunctionalRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(language.pick("删除", "Delete"))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedItem = null }) {
                    Text(language.pick("取消", "Cancel"))
                }
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    language.pick(
                        "删除${if (item.customer.isGroup) "文件夹" else "客户"}",
                        "Delete ${if (item.customer.isGroup) "folder" else "customer"}"
                    )
                )
            },
            text = {
                Text(
                    if (item.customer.isGroup) {
                        language.pick(
                            "确定要删除文件夹“${item.customer.name}”吗？里面的客户也会一起删除。",
                            "Delete folder \"${item.customer.name}\"? Customers inside it will also be deleted."
                        )
                    } else {
                        language.pick(
                            "确定要删除“${item.customer.name}”吗？",
                            "Delete \"${item.customer.name}\"?"
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customerViewModel.deleteCustomer(item.customer)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FunctionalRed)
                ) {
                    Text(language.pick("删除", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(language.pick("取消", "Cancel"))
                }
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    pendingMove?.let { item ->
        val allFolders by customerViewModel.allFolders.collectAsStateWithLifecycle()
        val availableFolders = remember(allFolders, item.customer.id, item.customer.isGroup) {
            val blockedIds = mutableSetOf(item.customer.id)
            if (item.customer.isGroup) {
                var changed = true
                while (changed) {
                    changed = false
                    allFolders.forEach { folder ->
                        if (folder.parentId in blockedIds && blockedIds.add(folder.id)) {
                            changed = true
                        }
                    }
                }
            }
            allFolders.filter { it.id !in blockedIds }
        }

        MoveToFolderDialog(
            allFolders = availableFolders,
            currentParentId = parentId,
            onDismiss = { pendingMove = null },
            onFolderSelected = { targetFolderId ->
                customerViewModel.moveCustomer(item.customer, targetFolderId)
                pendingMove = null
            }
        )
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = VibrantOrange,
                contentColor = TextWhite,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = language.pick("新增客户", "Add customer"),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppScreenHeader(
                title = titleText,
                subtitle = language.pick("长按客户可移动或删除", "Long press a customer for more actions"),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = language.pick("新建文件夹", "New folder"),
                            tint = VibrantOrange
                        )
                    }
                }
            )

            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            language.pick("搜索姓名或电话...", "Search name or phone..."),
                            color = TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MidnightBlue)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardSurface),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredCustomers,
                    key = { it.customer.id },
                    contentType = { if (it.customer.isGroup) "folder" else "customer" }
                ) { item ->
                    CustomerCard(
                        item = item,
                        onClick = { onCustomerClick(item) },
                        onLongPress = { selectedItem = item }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val language = LocalAppLanguage.current
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                language.pick("新建文件夹", "New Folder"),
                color = MidnightBlue,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text(language.pick("文件夹名称", "Folder name")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName) },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
            ) {
                Text(language.pick("建立", "Create"), color = TextWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("取消", "Cancel"), color = TextSecondary)
            }
        },
        containerColor = CardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun MoveToFolderDialog(
    allFolders: List<CustomerEntity>,
    currentParentId: Long?,
    onDismiss: () -> Unit,
    onFolderSelected: (Long?) -> Unit
) {
    val language = LocalAppLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                language.pick("移动到...", "Move to..."),
                fontWeight = FontWeight.Bold,
                color = MidnightBlue
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentParentId != null) {
                    item {
                        FolderItem(
                            name = language.pick("根目录（最外层）", "Root folder"),
                            onClick = { onFolderSelected(null) }
                        )
                    }
                }

                items(items = allFolders, key = { it.id }) { folder ->
                    if (folder.id != currentParentId) {
                        FolderItem(name = folder.name, onClick = { onFolderSelected(folder.id) })
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("取消", "Cancel"))
            }
        },
        containerColor = CardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun FolderItem(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            tint = VibrantOrange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = name, style = MaterialTheme.typography.bodyLarge, color = MidnightBlue)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerCard(
    item: CustomerWithBalance,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val language = LocalAppLanguage.current
    val formatter = rememberCurrencyFormatter()
    val avatarRequest = rememberThumbnailImageRequest(item.customer.profilePhotoUri, 48.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.customer.isGroup) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(VibrantOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = language.pick("文件夹", "Folder"),
                        tint = VibrantOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (!item.customer.profilePhotoUri.isNullOrBlank()) {
                AsyncImage(
                    model = avatarRequest,
                    contentDescription = language.pick("头像", "Avatar"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MidnightBlue.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.customer.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MidnightBlue
                    )
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
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.customer.isGroup) {
                        Text(
                            text = language.pick("文件夹", "Folder"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.6f)
                        )
                        val balanceText = when {
                            item.balance > 0 -> language.pick(
                                "总欠 ${formatter.format(item.balance)}",
                                "Debt ${formatter.format(item.balance)}"
                            )
                            item.balance < 0 -> language.pick(
                                "总存 ${formatter.format(abs(item.balance))}",
                                "Credit ${formatter.format(abs(item.balance))}"
                            )
                            else -> language.pick("无欠款", "No balance")
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
                    } else {
                        Text(
                            text = item.customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        val balanceText = when {
                            item.balance > 0 -> language.pick(
                                "欠 ${formatter.format(item.balance)}",
                                "Debt ${formatter.format(item.balance)}"
                            )
                            item.balance < 0 -> language.pick(
                                "存 ${formatter.format(abs(item.balance))}",
                                "Credit ${formatter.format(abs(item.balance))}"
                            )
                            else -> language.pick("已结清", "Settled")
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
