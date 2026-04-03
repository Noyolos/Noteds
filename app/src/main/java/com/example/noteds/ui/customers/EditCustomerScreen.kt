package com.example.noteds.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.noteds.ui.components.AppScreenHeader
import com.example.noteds.ui.i18n.LocalAppLanguage
import com.example.noteds.ui.i18n.pick
import com.example.noteds.ui.theme.BackgroundColor
import com.example.noteds.ui.theme.CardSurface
import com.example.noteds.ui.theme.FunctionalRed
import com.example.noteds.ui.theme.MidnightBlue
import com.example.noteds.ui.theme.TextSecondary
import com.example.noteds.ui.theme.TextWhite

@Composable
fun EditCustomerScreen(
    customerId: Long,
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val language = LocalAppLanguage.current
    val customers by customerViewModel.customersWithBalance.collectAsStateWithLifecycle()
    val customer = remember(customers, customerId) {
        customers.firstOrNull { it.customer.id == customerId }?.customer
    }

    if (customer == null) return

    var code by remember { mutableStateOf(customer.code) }
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var note by remember { mutableStateOf(customer.note) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    val profilePhotos = remember { mutableStateListOf(customer.profilePhotoUri, customer.profilePhotoUri2, customer.profilePhotoUri3) }
    val passportPhotos = remember { mutableStateListOf(customer.passportPhotoUri, customer.passportPhotoUri2, customer.passportPhotoUri3) }
    var legacyIdCardPhotoUri by remember { mutableStateOf(customer.idCardPhotoUri) }
    val showLegacyIdCardSection = remember(customer.idCardPhotoUri) { customer.idCardPhotoUri != null }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun validatePhone(input: String): Boolean {
        val normalized = input.trim()
        val pattern = Regex("^[0-9+\\-\\s]{6,20}")
        return normalized.isEmpty() || pattern.matches(normalized)
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = {
                    nameError = null
                    phoneError = null
                    val trimmedName = name.trim()
                    val trimmedPhone = phone.trim()
                    val isNameValid = trimmedName.isNotEmpty()
                    val isPhoneValid = validatePhone(trimmedPhone)
                    if (!isNameValid) nameError = language.pick("姓名必填", "Name is required")
                    if (!isPhoneValid) phoneError = language.pick("电话号码格式不正确", "Invalid phone number format")
                    if (isNameValid && isPhoneValid) {
                        customerViewModel.updateCustomer(
                            customerId = customerId,
                            code = code,
                            name = name,
                            phone = phone,
                            note = note,
                            profilePhotoUris = profilePhotos.toList(),
                            passportPhotoUris = passportPhotos.toList(),
                            idCardPhotoUri = legacyIdCardPhotoUri,
                            repaymentDate = customer.expectedRepaymentDate
                        )
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(language.pick("保存修改", "Save changes"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AppScreenHeader(
                title = language.pick("编辑客户", "Edit Customer"),
                subtitle = language.pick("更新资料、照片和旧档案兼容字段", "Update profile, photos, and legacy fields"),
                onBack = onBack
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            PhotoGrid(
                title = language.pick("头像照片", "Profile photos"),
                subtitle = language.pick("可拍照或从相册选择，最多 3 张。", "Take photos or choose from gallery, up to 3 photos."),
                photoUris = profilePhotos,
                onPhotoChanged = { index, uri -> profilePhotos[index] = uri }
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(language.pick("基本资料", "Basic info"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    placeholder = { Text(language.pick("客户编号", "Customer code")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = MidnightBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    placeholder = { Text(language.pick("客户姓名 *", "Customer name *")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = MidnightBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = nameError != null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MidnightBlue)
                )
                if (nameError != null) {
                    Text(text = nameError!!, color = FunctionalRed, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneError = null
                    },
                    placeholder = { Text(language.pick("电话号码", "Phone number")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = MidnightBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )
                if (phoneError != null) {
                    Text(text = phoneError!!, color = FunctionalRed, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(language.pick("备注", "Note")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = MidnightBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )
            }

            PhotoGrid(
                title = language.pick("证件照片", "Document photos"),
                subtitle = language.pick("身份证 / 护照等，最多 3 张。", "ID card, passport, or other documents, up to 3 photos."),
                photoUris = passportPhotos,
                onPhotoChanged = { index, uri -> passportPhotos[index] = uri }
            )

            if (showLegacyIdCardSection || legacyIdCardPhotoUri != null) {
                CustomerPhotoPicker(
                    title = language.pick("旧版身份证照片", "Legacy ID card photo"),
                    subtitle = language.pick("保留旧资料相容用，可单独更换或移除。", "Kept for older records. You can replace or remove it separately."),
                    currentPhotoUri = legacyIdCardPhotoUri,
                    onPhotoSelected = { legacyIdCardPhotoUri = it },
                    onPhotoCleared = { legacyIdCardPhotoUri = null }
                )
            }

            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FunctionalRed),
                border = BorderStroke(1.dp, FunctionalRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(language.pick("删除此客户档案", "Delete this customer"), fontWeight = FontWeight.Bold)
            }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(language.pick("确定要删除吗？", "Delete this customer?")) },
                text = {
                    Text(
                        language.pick(
                            "此操作将永久删除 ${customer.name} 及其所有交易记录。",
                            "This permanently deletes ${customer.name} and all related transactions."
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            customerViewModel.deleteCustomer(customer)
                            onSaved()
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
    }
}
