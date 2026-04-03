@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.noteds.ui.customers

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.noteds.data.entity.LedgerEntryEntity
import com.example.noteds.data.model.TransactionType
import com.example.noteds.ui.components.AppScreenHeader
import com.example.noteds.ui.components.FullScreenImageDialog
import com.example.noteds.ui.components.rememberThumbnailImageRequest
import com.example.noteds.ui.i18n.LocalAppLanguage
import com.example.noteds.ui.i18n.pick
import com.example.noteds.ui.i18n.rememberAppDateFormatter
import com.example.noteds.ui.i18n.rememberCurrencyFormatter
import com.example.noteds.ui.theme.BackgroundColor
import com.example.noteds.ui.theme.CardSurface
import com.example.noteds.ui.theme.DebtColor
import com.example.noteds.ui.theme.FunctionalRed
import com.example.noteds.ui.theme.MidnightBlue
import com.example.noteds.ui.theme.PaymentColor
import com.example.noteds.ui.theme.TextPrimary
import com.example.noteds.ui.theme.TextSecondary
import com.example.noteds.ui.theme.TextWhite
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun CustomerDetailScreen(
    customerId: Long,
    customerViewModel: CustomerViewModel,
    onClose: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val language = LocalAppLanguage.current
    val customers by customerViewModel.customersWithBalance.collectAsStateWithLifecycle()
    val selected = remember(customers, customerId) {
        customers.firstOrNull { it.customer.id == customerId }
    }
    val currencyFormatter = rememberCurrencyFormatter()
    val dateFormatter = rememberAppDateFormatter()

    val transactions by customerViewModel.getTransactionsForCustomer(customerId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showTransactionForm by remember { mutableStateOf<TransactionType?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var entryForEdit by remember { mutableStateOf<LedgerEntryEntity?>(null) }
    var entryPendingDelete by remember { mutableStateOf<LedgerEntryEntity?>(null) }
    var fullScreenPhoto by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    if (selected == null) {
        onClose()
        return
    }

    val profilePhotos = remember(selected.customer) {
        listOf(
            selected.customer.profilePhotoUri,
            selected.customer.profilePhotoUri2,
            selected.customer.profilePhotoUri3
        ).filterNotNull()
    }
    val passportPhotos = remember(selected.customer) {
        listOf(
            selected.customer.passportPhotoUri,
            selected.customer.passportPhotoUri2,
            selected.customer.passportPhotoUri3,
            selected.customer.idCardPhotoUri
        ).filterNotNull()
    }
    val allPhotos = remember(profilePhotos, passportPhotos) {
        (profilePhotos + passportPhotos).filter { it.isNotBlank() }
    }
    val primaryPhoto = profilePhotos.firstOrNull()

    val lastPaymentDaysText = remember(transactions) {
        val lastPayment = transactions
            .filter { it.type.uppercase() == TransactionType.PAYMENT.dbValue }
            .maxByOrNull { it.timestamp }
        lastPayment?.let {
            val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.timestamp).coerceAtLeast(0)
            language.pick("${days}天前", "$days days ago")
        } ?: language.pick("暂无记录", "No payment yet")
    }

    if (showTransactionForm != null) {
        TransactionFormScreen(
            customer = selected.customer,
            transactionType = showTransactionForm!!,
            onBack = { showTransactionForm = null },
            onSave = { amount, note, timestamp ->
                customerViewModel.addLedgerEntry(customerId, showTransactionForm!!, amount, note, timestamp)
                showTransactionForm = null
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(CardSurface)
                    .navigationBarsPadding()
                    .shadow(elevation = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { showTransactionForm = TransactionType.DEBT },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DebtColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(language.pick("记一笔", "Add debt"), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showTransactionForm = TransactionType.PAYMENT },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(language.pick("收回款", "Add payment"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = BackgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                AppScreenHeader(
                    title = language.pick("客户详情", "Customer Details"),
                    subtitle = selected.customer.name,
                    onBack = onClose,
                    actions = {
                        IconButton(
                            onClick = { onEditClick(customerId) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = language.pick("编辑", "Edit"), tint = MidnightBlue)
                        }
                    }
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (primaryPhoto != null) {
                        val primaryPhotoRequest = rememberThumbnailImageRequest(primaryPhoto, 120.dp)
                        AsyncImage(
                            model = primaryPhotoRequest,
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(4.dp, Color.White, RoundedCornerShape(24.dp))
                                .clickable { fullScreenPhoto = primaryPhoto },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(BackgroundColor)
                                .border(4.dp, Color.White, RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selected.customer.name.take(1).uppercase(),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(selected.customer.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (selected.customer.code.isNotBlank()) {
                        Text(
                            text = language.pick("编号: ${selected.customer.code}", "Code: ${selected.customer.code}"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (selected.customer.phone.isNotBlank()) {
                        Text(selected.customer.phone, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.linearGradient(colors = listOf(MidnightBlue, Color(0xFF534BAE))))
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                language.pick("当前欠款", "Current balance"),
                                color = TextWhite.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currencyFormatter.format(selected.balance),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = TextWhite.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                language.pick("最近一次回款: $lastPaymentDaysText", "Last payment: $lastPaymentDaysText"),
                                color = TextWhite.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (allPhotos.isNotEmpty()) {
                item {
                    Text(
                        text = language.pick("照片画廊", "Photo gallery"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = allPhotos, key = { it }, contentType = { "photo" }) { photo ->
                            val galleryPhotoRequest = rememberThumbnailImageRequest(photo, 120.dp)
                            AsyncImage(
                                model = galleryPhotoRequest,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { fullScreenPhoto = photo },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = language.pick("流水记录", "Transactions"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            items(
                items = transactions,
                key = { it.id },
                contentType = { "transaction" }
            ) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onLongPress = { entryForEdit = transaction }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = FunctionalRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(language.pick("删除客户档案", "Delete customer"))
                }
            }
        }
    }

    entryForEdit?.let { entry ->
        EditTransactionDialog(
            entry = entry,
            onDismiss = { entryForEdit = null },
            onSave = {
                customerViewModel.updateLedgerEntry(it)
                entryForEdit = null
            },
            onDelete = {
                entryForEdit = null
                entryPendingDelete = entry
            }
        )
    }

    entryPendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text(language.pick("删除这笔交易？", "Delete this transaction?")) },
            text = { Text(language.pick("这会移除当前交易记录。", "This removes the selected transaction.")) },
            confirmButton = {
                Button(
                    onClick = {
                        customerViewModel.deleteLedgerEntry(entry.id)
                        entryPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FunctionalRed)
                ) { Text(language.pick("删除", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) {
                    Text(language.pick("取消", "Cancel"))
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(language.pick("确定要删除吗？", "Delete this customer?")) },
            text = {
                Text(
                    language.pick(
                        "此操作将删除该客户资料，但交易记录仍会保留以避免历史资料遗失。",
                        "This disables the customer record, while keeping transactions for history."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        customerViewModel.deleteCustomer(selected.customer)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FunctionalRed)
                ) { Text(language.pick("确认删除", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(language.pick("取消", "Cancel"))
                }
            }
        )
    }

    fullScreenPhoto?.let { uri ->
        FullScreenImageDialog(
            photoUri = uri,
            onDismiss = { fullScreenPhoto = null },
            onDownload = {
                customerViewModel.saveImageToGallery(uri) { success ->
                    Toast.makeText(
                        context,
                        if (success) {
                            language.pick("图片已保存到相册", "Photo saved to gallery")
                        } else {
                            language.pick("保存失败，请稍后再试", "Saving failed, please try again")
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

@Composable
fun TransactionItem(
    transaction: LedgerEntryEntity,
    onLongPress: () -> Unit = {}
) {
    val language = LocalAppLanguage.current
    val isDebt = transaction.type == TransactionType.DEBT.dbValue
    val dateFormatter = rememberAppDateFormatter()
    val currencyFormatter = rememberCurrencyFormatter()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDebt) DebtColor.copy(alpha = 0.1f) else PaymentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDebt) Icons.Default.Add else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = if (isDebt) DebtColor else PaymentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note ?: if (isDebt) language.pick("赊账", "Debt") else language.pick("还款", "Payment"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${dateFormatter.format(Date(transaction.timestamp))} · ${if (isDebt) language.pick("赊账", "Debt") else language.pick("还款", "Payment")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "${if (isDebt) "+" else "-"} ${currencyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDebt) DebtColor else PaymentColor
            )
        }
    }
}

@Composable
fun EditTransactionDialog(
    entry: LedgerEntryEntity,
    onDismiss: () -> Unit,
    onSave: (LedgerEntryEntity) -> Unit,
    onDelete: () -> Unit
) {
    val language = LocalAppLanguage.current
    var amountText by remember { mutableStateOf(entry.amount.toString()) }
    var noteText by remember { mutableStateOf(entry.note.orEmpty()) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = entry.timestamp)
    val dateFormatter = rememberAppDateFormatter()
    val selectedDate = datePickerState.selectedDateMillis ?: entry.timestamp
    val selectedDateLabel = remember(selectedDate, dateFormatter) { dateFormatter.format(Date(selectedDate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.pick("编辑交易", "Edit transaction")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = { Text(language.pick("金额", "Amount")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amountError != null
                )
                if (amountError != null) {
                    Text(amountError!!, color = FunctionalRed, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(language.pick("备注", "Note")) }
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDateLabel)
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = FunctionalRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(language.pick("删除这笔", "Delete entry"))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = language.pick("金额必须大于 0", "Amount must be greater than 0")
                        return@Button
                    }
                    onSave(entry.copy(amount = amount, note = noteText.ifBlank { null }, timestamp = selectedDate))
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (entry.type == TransactionType.DEBT.dbValue) DebtColor else PaymentColor)
            ) {
                Text(language.pick("保存", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("取消", "Cancel"))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(language.pick("确认", "Confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(language.pick("取消", "Cancel"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
