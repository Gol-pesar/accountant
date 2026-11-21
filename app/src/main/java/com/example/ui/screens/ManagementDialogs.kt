package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BankAccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomBankEntity
import com.example.data.local.SavingsGoalEntity
import com.example.domain.IranianBank
import com.example.domain.IranianBanks
import com.example.domain.JalaliCalendar
import com.example.ui.components.CategoryIcons
import com.example.ui.components.IranianBankLogo
import com.example.ui.theme.ExpenseCrimson

@Composable
fun CategoryEditorDialog(
    initial: CategoryEntity?,
    showInRial: Boolean,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit,
    onDelete: ((CategoryEntity) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var isIncome by remember { mutableStateOf(initial?.isIncome ?: false) }
    var iconKey by remember { mutableStateOf(initial?.iconKey ?: CategoryIcons.OPTIONS.first().key) }
    var colorHex by remember { mutableLongStateOf(initial?.colorHex ?: CategoryIcons.COLOR_SWATCHES.first()) }
    var budgetInput by remember {
        mutableStateOf(
            initial?.monthlyBudgetToman?.takeIf { it > 0L }?.let {
                if (showInRial) (it * 10L).toString() else it.toString()
            } ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (initial == null) "دسته‌بندی جدید" else "ویرایش دسته‌بندی")
                if (initial != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initial) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "حذف دسته‌بندی", tint = ExpenseCrimson)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("هزینه") }
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("درآمد") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دسته‌بندی") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                if (!isIncome) {
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { raw ->
                            budgetInput = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(12)
                        },
                        label = { Text("سقف بودجه ماهانه (${if (showInRial) "ریال" else "تومان"} - اختیاری)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("آیکون", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CategoryIcons.OPTIONS, key = { it.key }) { opt ->
                        val selected = iconKey == opt.key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(colorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) BorderStroke(1.5.dp, Color(colorHex)) else null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { iconKey = opt.key }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = opt.label,
                                    tint = if (selected) Color(colorHex) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text("رنگ", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CategoryIcons.COLOR_SWATCHES) { swatch ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(swatch))
                                .then(
                                    if (colorHex == swatch) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { colorHex = swatch }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawBudget = budgetInput.toLongOrNull() ?: 0L
                    val budgetToman = if (showInRial) rawBudget / 10L else rawBudget
                    onSave(
                        CategoryEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            isIncome = isIncome,
                            iconKey = iconKey,
                            colorHex = colorHex,
                            monthlyBudgetToman = if (isIncome) 0L else budgetToman
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_category_button")
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun BankAccountEditorDialog(
    initial: BankAccountEntity?,
    allBanks: List<IranianBank> = IranianBanks.allBanks(),
    customBanks: List<IranianBank> = IranianBanks.customBanks(),
    showInRial: Boolean,
    openScannerImmediately: Boolean = false,
    onRequestAddCustomBank: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (BankAccountEntity) -> Unit,
    onDelete: ((BankAccountEntity) -> Unit)? = null
) {
    var bankCode by remember { mutableStateOf(initial?.bankCode ?: "MELLAT") }
    var cardNumber by remember { mutableStateOf(initial?.cardNumber ?: "") }
    var accountTitle by remember { mutableStateOf(initial?.accountTitle ?: "") }
    var balanceInput by remember {
        mutableStateOf(
            initial?.balanceToman?.let { if (showInRial) (it * 10L).toString() else it.toString() } ?: "0"
        )
    }
    var showCardScanner by remember { mutableStateOf(openScannerImmediately) }

    val selectedBank = IranianBanks.findByCode(bankCode, customBanks)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (initial == null || initial.id == 0L) "افزودن کارت بانکی" else "ویرایش کارت بانکی")
                if (initial != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initial) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "حذف کارت", tint = ExpenseCrimson)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Detected Bank Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IranianBankLogo(bank = selectedBank, size = 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = selectedBank.persianName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "کارت را اسکن یا شماره آن را وارد کنید تا بانک خودکار تشخیص داده شود",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Scan Bank Card with Camera Button
                FilledTonalButton(
                    onClick = { showCardScanner = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("scan_bank_card_camera_button")
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("اسکن کارت بانکی با دوربین", fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { raw ->
                        val digits = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(16)
                        cardNumber = digits
                        val detected = IranianBanks.detectByCardNumber(digits, customBanks)
                        if (detected != null) {
                            bankCode = detected.code
                        }
                    },
                    label = { Text("شماره کارت ۱۶ رقمی (اختیاری)") },
                    placeholder = { Text("مثلاً 610433...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_card_number_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("انتخاب بانک", style = MaterialTheme.typography.labelMedium)
                    if (onRequestAddCustomBank != null) {
                        TextButton(
                            onClick = onRequestAddCustomBank,
                            modifier = Modifier.testTag("inline_add_custom_bank_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("بانک جدید", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allBanks, key = { it.code }) { bank ->
                        val selected = bank.code == bankCode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { bankCode = bank.code }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IranianBankLogo(bank = bank, size = 22.dp, circular = true)
                                Spacer(Modifier.width(6.dp))
                                Text(bank.shortName, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = accountTitle,
                    onValueChange = { accountTitle = it },
                    label = { Text("عنوان کارت (مثلاً: کارت حقوق، پس‌انداز)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = { raw ->
                        balanceInput = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(13)
                    },
                    label = { Text("موجودی فعلی (${if (showInRial) "ریال" else "تومان"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawBal = balanceInput.toLongOrNull() ?: 0L
                    val balToman = if (showInRial) rawBal / 10L else rawBal
                    onSave(
                        BankAccountEntity(
                            id = initial?.id ?: 0L,
                            bankCode = bankCode,
                            cardNumber = cardNumber.trim(),
                            accountTitle = accountTitle.trim(),
                            balanceToman = balToman,
                            isDefault = initial?.isDefault ?: false
                        )
                    )
                },
                modifier = Modifier.testTag("save_bank_card_button")
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )

    if (showCardScanner) {
        BankCardScannerDialog(
            customBanks = customBanks,
            onDismiss = { showCardScanner = false },
            onCardScanned = { scanned ->
                cardNumber = scanned.cardNumber
                if (scanned.detectedBank != null) {
                    bankCode = scanned.detectedBank.code
                }
                if (accountTitle.isBlank() && scanned.expiryDate != null) {
                    accountTitle = "انقضا ${JalaliCalendar.toPersianDigits(scanned.expiryDate)}"
                }
                showCardScanner = false
            }
        )
    }
}

@Composable
fun CustomBankEditorDialog(
    initial: CustomBankEntity? = null,
    onDismiss: () -> Unit,
    onSave: (CustomBankEntity) -> Unit,
    onDelete: ((CustomBankEntity) -> Unit)? = null
) {
    var persianName by remember { mutableStateOf(initial?.persianName ?: "") }
    var shortName by remember { mutableStateOf(initial?.shortName ?: "") }
    var cardPrefix by remember { mutableStateOf(initial?.cardPrefix ?: "") }
    var smsKeyword by remember { mutableStateOf(initial?.smsKeyword ?: "") }
    var colorHex by remember { mutableLongStateOf(initial?.primaryColorHex ?: 0xFF0B4F4A) }

    val previewBank = remember(persianName, shortName, colorHex) {
        CustomBankEntity(
            code = initial?.code ?: "CUSTOM_PREVIEW",
            persianName = persianName.ifBlank { "بانک دلخواه" },
            shortName = shortName.ifBlank { persianName.ifBlank { "بانک" } },
            primaryColorHex = colorHex
        ).toIranianBank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (initial == null) "افزودن بانک جدید" else "ویرایش بانک دلخواه")
                if (initial != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initial) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "حذف بانک", tint = ExpenseCrimson)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IranianBankLogo(bank = previewBank, size = 44.dp)
                    Column {
                        Text(
                            text = previewBank.persianName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "پیش‌نمایش لوگوی بانک شما در تراکنش‌ها و پیامک‌ها",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = persianName,
                    onValueChange = { persianName = it },
                    label = { Text("نام کامل بانک یا موسسه") },
                    placeholder = { Text("مثلاً: بانک ایران زمین / ویپاد / نئوبانک") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_bank_name_input")
                )

                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("نام کوتاه (اختیاری)") },
                    placeholder = { Text("مثلاً: ایران‌زمین") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cardPrefix,
                    onValueChange = { raw ->
                        cardPrefix = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() || it == ',' }.take(20)
                    },
                    label = { Text("پیش‌شماره ۶ رقمی کارت (اختیاری)") },
                    placeholder = { Text("مثلاً: 505785") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = smsKeyword,
                    onValueChange = { smsKeyword = it },
                    label = { Text("کلمه کلیدی پیامک بانک (اختیاری)") },
                    placeholder = { Text("مثلاً: ایران زمین، izbank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("رنگ برند بانک", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CategoryIcons.COLOR_SWATCHES) { swatch ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(swatch))
                                .then(
                                    if (colorHex == swatch) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { colorHex = swatch }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = persianName.trim()
                    val trimmedShort = shortName.trim().ifBlank { trimmedName.take(12) }
                    val generatedCode = initial?.code ?: "CUSTOM_${System.currentTimeMillis()}"
                    onSave(
                        CustomBankEntity(
                            id = initial?.id ?: 0L,
                            code = generatedCode,
                            persianName = trimmedName,
                            shortName = trimmedShort,
                            primaryColorHex = colorHex,
                            cardPrefix = cardPrefix.trim(),
                            smsKeyword = smsKeyword.trim()
                        )
                    )
                },
                enabled = persianName.isNotBlank(),
                modifier = Modifier.testTag("save_custom_bank_button")
            ) {
                Text("ذخیره بانک")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun SavingsGoalDialog(
    initial: SavingsGoalEntity?,
    showInRial: Boolean,
    onDismiss: () -> Unit,
    onSave: (SavingsGoalEntity) -> Unit,
    onDelete: ((SavingsGoalEntity) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var targetInput by remember {
        mutableStateOf(
            initial?.targetAmountToman?.let { if (showInRial) (it * 10L).toString() else it.toString() } ?: ""
        )
    }
    var savedInput by remember {
        mutableStateOf(
            initial?.savedAmountToman?.let { if (showInRial) (it * 10L).toString() else it.toString() } ?: "0"
        )
    }
    var colorHex by remember { mutableLongStateOf(initial?.colorHex ?: 0xFF059669) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (initial == null) "هدف پس‌انداز جدید" else "ویرایش قلک")
                if (initial != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initial) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "حذف هدف", tint = ExpenseCrimson)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان هدف (مثلاً: سفر، صندوق اضطراری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { raw ->
                        targetInput = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(13)
                    },
                    label = { Text("مبلغ هدف (${if (showInRial) "ریال" else "تومان"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = savedInput,
                    onValueChange = { raw ->
                        savedInput = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(13)
                    },
                    label = { Text("مبلغ پس‌انداز شده تا الان") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CategoryIcons.COLOR_SWATCHES) { swatch ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(swatch))
                                .then(
                                    if (colorHex == swatch) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { colorHex = swatch }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val rawTarget = targetInput.toLongOrNull() ?: 0L
            val targetToman = if (showInRial) rawTarget / 10L else rawTarget
            val rawSaved = savedInput.toLongOrNull() ?: 0L
            val savedToman = if (showInRial) rawSaved / 10L else rawSaved
            Button(
                onClick = {
                    onSave(
                        SavingsGoalEntity(
                            id = initial?.id ?: 0L,
                            title = title.trim(),
                            targetAmountToman = targetToman,
                            savedAmountToman = savedToman,
                            colorHex = colorHex,
                            createdAtMillis = initial?.createdAtMillis ?: System.currentTimeMillis()
                        )
                    )
                },
                enabled = title.isNotBlank() && targetToman > 0L
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
