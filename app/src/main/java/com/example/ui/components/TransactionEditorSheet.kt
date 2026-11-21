package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.CategoryEntity
import com.example.data.local.PersonEntity
import com.example.data.local.TransactionEntity
import com.example.domain.IranianBank
import com.example.domain.IranianBanks
import com.example.domain.JalaliCalendar
import com.example.domain.JalaliDate
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorSheet(
    initialTx: TransactionEntity? = null,
    defaultIsIncome: Boolean = false,
    categories: List<CategoryEntity>,
    people: List<PersonEntity>,
    allBanks: List<IranianBank> = IranianBanks.allBanks(),
    showInRial: Boolean,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: ((TransactionEntity) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isIncome by remember { mutableStateOf(initialTx?.isIncome ?: defaultIsIncome) }
    var amountInput by remember {
        mutableStateOf(
            initialTx?.amountToman?.let { if (showInRial) (it * 10L).toString() else it.toString() } ?: ""
        )
    }
    var title by remember { mutableStateOf(initialTx?.title ?: "") }
    var note by remember { mutableStateOf(initialTx?.note ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf(
            initialTx?.categoryId ?: categories.firstOrNull { it.isIncome == (initialTx?.isIncome ?: defaultIsIncome) }?.id
        )
    }
    var selectedBankCode by remember { mutableStateOf(initialTx?.bankCode ?: "MELLAT") }
    var selectedPersonId by remember { mutableStateOf(initialTx?.personId) }
    var personImpact by remember { mutableIntStateOf(initialTx?.personImpact ?: 0) }
    var selectedTimestamp by remember { mutableLongStateOf(initialTx?.timestampMillis ?: System.currentTimeMillis()) }

    val filteredCategories = remember(categories, isIncome) {
        categories.filter { it.isIncome == isIncome }
    }

    val parsedAmountInput = JalaliCalendar.toLatinDigits(amountInput).filter { it.isDigit() }.toLongOrNull() ?: 0L
    val amountInToman = if (showInRial) parsedAmountInput / 10L else parsedAmountInput
    val selectedDate = JalaliCalendar.fromEpochMillis(selectedTimestamp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (initialTx == null) "ثبت تراکنش جدید" else "ویرایش تراکنش",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (initialTx != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(initialTx) },
                        modifier = Modifier.testTag("delete_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "حذف تراکنش",
                            tint = ExpenseCrimson
                        )
                    }
                }
            }

            // Expense / Income Segmented Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TypeToggleChip(
                    label = "هزینه (برداشت)",
                    selected = !isIncome,
                    activeColor = ExpenseCrimson,
                    onClick = {
                        isIncome = false
                        selectedCategoryId = categories.firstOrNull { !it.isIncome }?.id
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_expense_type")
                )
                TypeToggleChip(
                    label = "درآمد (واریز)",
                    selected = isIncome,
                    activeColor = IncomeGreen,
                    onClick = {
                        isIncome = true
                        selectedCategoryId = categories.firstOrNull { it.isIncome }?.id
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_income_type")
                )
            }

            // Amount Input
            OutlinedTextField(
                value = amountInput,
                onValueChange = { raw ->
                    amountInput = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(13)
                },
                label = { Text("مبلغ (${if (showInRial) "ریال" else "تومان"})") },
                supportingText = {
                    if (amountInToman > 0L) {
                        Text(
                            text = JalaliCalendar.formatCurrency(amountInToman, showInRial),
                            color = if (isIncome) IncomeGreen else ExpenseCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input")
            )

            // Quick amount preset chips (in Toman)
            val presets = listOf(
                50_000L to "۵۰ هزار",
                100_000L to "۱۰۰ هزار",
                500_000L to "۵۰۰ هزار",
                1_000_000L to "۱ میلیون",
                5_000_000L to "۵ میلیون"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { (tomanVal, label) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val v = if (showInRial) tomanVal * 10L else tomanVal
                            amountInput = v.toString()
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان تراکنش (اختیاری)") },
                placeholder = { Text("مثلاً: خرید سوپرمارکت یا واریز حقوق") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_title_input")
            )

            // Category Selector
            Text(
                text = "دسته‌بندی",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(filteredCategories, key = { it.id }) { cat ->
                    val selected = selectedCategoryId == cat.id
                    val catColor = Color(cat.colorHex)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (selected) BorderStroke(1.5.dp, catColor) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedCategoryId = cat.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = CategoryIcons.resolve(cat.iconKey),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) catColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Iranian Bank Picker
            Text(
                text = "بانک / کیف پول",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allBanks, key = { it.code }) { bank ->
                    val selected = selectedBankCode == bank.code
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedBankCode = bank.code }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IranianBankLogo(bank = bank, size = 24.dp, circular = true)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = bank.shortName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Person Linking (optional)
            if (people.isNotEmpty()) {
                Text(
                    text = "مرتبط با شخص (اختیاری)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedPersonId == null,
                            onClick = {
                                selectedPersonId = null
                                personImpact = 0
                            },
                            label = { Text("هیچ‌کس") }
                        )
                    }
                    items(people, key = { it.id }) { person ->
                        FilterChip(
                            selected = selectedPersonId == person.id,
                            onClick = {
                                selectedPersonId = person.id
                                if (personImpact == 0) {
                                    personImpact = if (isIncome) -1 else +1
                                }
                            },
                            label = { Text(person.name) }
                        )
                    }
                }
                if (selectedPersonId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = personImpact == 1,
                            onClick = { personImpact = 1 },
                            label = { Text("طلب من از شخص") }
                        )
                        FilterChip(
                            selected = personImpact == -1,
                            onClick = { personImpact = -1 },
                            label = { Text("بدهی من / دریافتی") }
                        )
                        FilterChip(
                            selected = personImpact == 0,
                            onClick = { personImpact = 0 },
                            label = { Text("فقط برچسب") }
                        )
                    }
                }
            }

            // Jalali Date Picker (quick day shift)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تاریخ شمسی: ${selectedDate.formatFull()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedDate == JalaliCalendar.today(),
                        onClick = { selectedTimestamp = System.currentTimeMillis() },
                        label = { Text("امروز", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { selectedTimestamp -= 86_400_000L },
                        label = { Text("روز قبل", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("یادداشت (اختیاری)") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Save Button
            Button(
                onClick = {
                    val chosenCat = filteredCategories.firstOrNull { it.id == selectedCategoryId }
                    val finalTitle = title.trim().ifEmpty {
                        chosenCat?.name ?: if (isIncome) "درآمد جدید" else "هزینه جدید"
                    }
                    onSave(
                        TransactionEntity(
                            id = initialTx?.id ?: 0L,
                            title = finalTitle,
                            amountToman = amountInToman,
                            isIncome = isIncome,
                            categoryId = selectedCategoryId,
                            bankCode = selectedBankCode,
                            personId = selectedPersonId,
                            personImpact = if (selectedPersonId != null) personImpact else 0,
                            timestampMillis = selectedTimestamp,
                            note = note.trim(),
                            fromSms = initialTx?.fromSms ?: false,
                            smsHash = initialTx?.smsHash
                        )
                    )
                },
                enabled = amountInToman > 0L,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button")
            ) {
                Text(
                    text = if (initialTx == null) "ثبت تراکنش" else "ذخیره تغییرات",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TypeToggleChip(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) activeColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) activeColor else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
