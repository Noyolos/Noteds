package com.example.noteds.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.noteds.ui.components.AppScreenHeader
import com.example.noteds.ui.i18n.LocalAppLanguage
import com.example.noteds.ui.i18n.pick
import com.example.noteds.ui.theme.BackgroundColor
import com.example.noteds.ui.theme.CardSurface
import com.example.noteds.ui.theme.DebtColor
import com.example.noteds.ui.theme.FunctionalRed
import com.example.noteds.ui.theme.MidnightBlue
import com.example.noteds.ui.theme.TextSecondary
import com.example.noteds.ui.theme.TextWhite

@Composable
fun AddCustomerScreen(
    customerViewModel: CustomerViewModel,
    parentId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val language = LocalAppLanguage.current
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var initialAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var initialAmountError by remember { mutableStateOf<String?>(null) }
    val profilePhotos = remember { mutableStateListOf<String?>(null, null, null) }
    val passportPhotos = remember { mutableStateListOf<String?>(null, null, null) }

    fun validatePhone(input: String): Boolean {
        val normalized = input.trim()
        val pattern = Regex("^[0-9+\\-\\s]{6,20}")
        return normalized.isEmpty() || pattern.matches(normalized)
    }

    fun validate(): Boolean {
        var valid = true
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            nameError = language.pick("姓名必填", "Name is required")
            valid = false
        }
        val trimmedPhone = phone.trim()
        if (trimmedPhone.isNotEmpty() && !validatePhone(trimmedPhone)) {
            phoneError = language.pick("电话号码格式不正确", "Invalid phone number format")
            valid = false
        }
        if (initialAmount.isNotBlank()) {
            val value = initialAmount.toDoubleOrNull()
            if (value == null || value <= 0) {
                initialAmountError = language.pick("金额必须大于 0", "Amount must be greater than 0")
                valid = false
            }
        }
        return valid
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = {
                    nameError = null
                    phoneError = null
                    initialAmountError = null
                    if (validate()) {
                        customerViewModel.addCustomer(
                            code = code,
                            name = name,
                            phone = phone,
                            note = note,
                            profilePhotoUris = profilePhotos.toList(),
                            passportPhotoUris = passportPhotos.toList(),
                            initialDebtAmount = initialAmount.toDoubleOrNull(),
                            initialDebtNote = language.pick("初始欠款", "Initial debt"),
                            initialDebtDate = System.currentTimeMillis(),
                            repaymentDate = null,
                            parentId = parentId
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
                Text(language.pick("确认建立档案", "Create customer"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                title = language.pick("新增客户", "Add Customer"),
                subtitle = language.pick("建立客户资料与首笔欠款", "Create a new customer profile"),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    isError = phoneError != null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MidnightBlue)
                )
                if (phoneError != null) {
                    Text(text = phoneError!!, color = FunctionalRed, style = MaterialTheme.typography.bodyMedium)
                }
            }

            PhotoGrid(
                title = language.pick("证件照片", "Document photos"),
                subtitle = language.pick("身份证 / 护照等，最多 3 张。", "ID card, passport, or other documents, up to 3 photos."),
                photoUris = passportPhotos,
                onPhotoChanged = { index, uri -> passportPhotos[index] = uri }
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(language.pick("初始赊账 (选填)", "Initial debt (optional)"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)

                OutlinedTextField(
                    value = initialAmount,
                    onValueChange = {
                        initialAmount = it
                        initialAmountError = null
                    },
                    placeholder = { Text(language.pick("欠款金额 (RM)", "Debt amount (RM)")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = DebtColor.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    isError = initialAmountError != null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = DebtColor, fontWeight = FontWeight.Bold)
                )
                if (initialAmountError != null) {
                    Text(text = initialAmountError!!, color = FunctionalRed, style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(language.pick("购买了什么？(备注)", "What was purchased? (Note)")) },
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
            }
        }
    }
}
