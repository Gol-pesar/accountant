package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import com.example.data.local.PersonEntity
import com.example.data.local.TransactionEntity
import com.example.domain.JalaliCalendar
import com.example.ui.components.CategoryIcons
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.AccountingUiState
import com.example.ui.viewmodel.PersonLedgerSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    state: AccountingUiState,
    onSavePerson: (PersonEntity) -> Unit,
    onDeletePerson: (PersonEntity) -> Unit,
    onSettlePerson: (PersonLedgerSummary, String) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val showRial = state.preferences.showInRial
    var editingPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var inspectingSummary by remember { mutableStateOf<PersonLedgerSummary?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "اشخاص و دفتر حساب",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = {
                        editingPerson = null
                        showAddDialog = true
                    },
                    modifier = Modifier.testTag("add_person_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("شخص جدید")
                }
            }
        }

        // Summary Receivables & Payables Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("جمع مطالبات (طلب شما)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = JalaliCalendar.formatCurrency(state.totalReceivableToman, showRial),
                            style = MaterialTheme.typography.titleMedium,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("جمع بدهی‌های شما", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = JalaliCalendar.formatCurrency(state.totalPayableToman, showRial),
                            style = MaterialTheme.typography.titleMedium,
                            color = ExpenseCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (state.peopleSummaries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.People,
                    title = "هنوز شخصی تعریف نشده است",
                    subtitle = "نام دوستان، همکاران یا اعضای خانواده را ثبت کنید تا طلب و بدهی هر شخص به صورت خودکار محاسبه شود.",
                    actionLabel = "افزودن اولین شخص",
                    onAction = {
                        editingPerson = null
                        showAddDialog = true
                    }
                )
            }
        } else {
            items(state.peopleSummaries, key = { it.person.id }) { summary ->
                PersonSummaryCard(
                    summary = summary,
                    showInRial = showRial,
                    onClick = { inspectingSummary = summary }
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showAddDialog) {
        PersonEditorDialog(
            initial = editingPerson,
            onDismiss = { showAddDialog = false },
            onSave = { p ->
                onSavePerson(p)
                showAddDialog = false
            }
        )
    }

    val currentInspect = inspectingSummary?.let { ins ->
        state.peopleSummaries.firstOrNull { it.person.id == ins.person.id }
    }
    if (currentInspect != null) {
        val catMap = state.categories.associateBy { it.id }
        val personTxs = state.transactions.filter { it.personId == currentInspect.person.id }
        ModalBottomSheet(
            onDismissRequest = { inspectingSummary = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(currentInspect.person.avatarColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentInspect.person.name.take(1),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentInspect.person.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentInspect.person.relationNote.isNotBlank()) {
                            Text(
                                text = currentInspect.person.relationNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = {
                        editingPerson = currentInspect.person
                        inspectingSummary = null
                        showAddDialog = true
                    }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "ویرایش شخص")
                    }
                    IconButton(onClick = {
                        onDeletePerson(currentInspect.person)
                        inspectingSummary = null
                    }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "حذف شخص", tint = ExpenseCrimson)
                    }
                }

                if (currentInspect.person.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = JalaliCalendar.toPersianDigits(currentInspect.person.phone),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Balance Banner + Settle Up
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = when {
                                    currentInspect.netBalanceToman > 0L -> "بدهکار به شما (طلب شما)"
                                    currentInspect.netBalanceToman < 0L -> "طلبکار از شما (بدهی شما)"
                                    else -> "وضعیت حساب"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (currentInspect.netBalanceToman == 0L) {
                                    "تسویه شده"
                                } else {
                                    JalaliCalendar.formatCurrency(kotlin.math.abs(currentInspect.netBalanceToman), showRial)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    currentInspect.netBalanceToman > 0L -> IncomeGreen
                                    currentInspect.netBalanceToman < 0L -> ExpenseCrimson
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }

                        if (currentInspect.netBalanceToman != 0L) {
                            Button(
                                onClick = {
                                    val defaultBank = state.bankAccounts.firstOrNull()?.bankCode ?: "CASH"
                                    onSettlePerson(currentInspect, defaultBank)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ثبت تسویه")
                            }
                        }
                    }
                }

                Text(
                    text = "تراکنش‌های مرتبط (${JalaliCalendar.toPersianDigits(personTxs.size)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (personTxs.isEmpty()) {
                    Text(
                        text = "هنگام ثبت تراکنش جدید، می‌توانید این شخص را انتخاب کنید تا در این دفتر ثبت شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    personTxs.take(6).forEach { tx ->
                        TransactionRowCard(
                            transaction = tx,
                            category = catMap[tx.categoryId],
                            person = null,
                            showInRial = showRial,
                            onClick = {
                                inspectingSummary = null
                                onEditTransaction(tx)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonSummaryCard(
    summary: PersonLedgerSummary,
    showInRial: Boolean,
    onClick: () -> Unit
) {
    val badgeColor = when {
        summary.netBalanceToman > 0L -> IncomeGreen
        summary.netBalanceToman < 0L -> ExpenseCrimson
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = when {
        summary.netBalanceToman > 0L -> "طلب شما"
        summary.netBalanceToman < 0L -> "بدهی شما"
        else -> "تسویه"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("person_card_${summary.person.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(summary.person.avatarColorHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = summary.person.name.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.person.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = summary.person.relationNote.ifBlank {
                        "${JalaliCalendar.toPersianDigits(summary.transactionCount)} تراکنش ثبت‌شده"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor
                )
                if (summary.netBalanceToman != 0L) {
                    Text(
                        text = JalaliCalendar.formatCurrency(kotlin.math.abs(summary.netBalanceToman), showInRial),
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonEditorDialog(
    initial: PersonEntity?,
    onDismiss: () -> Unit,
    onSave: (PersonEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var note by remember { mutableStateOf(initial?.relationNote ?: "") }
    var colorHex by remember { mutableLongStateOf(initial?.avatarColorHex ?: CategoryIcons.COLOR_SWATCHES.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "افزودن شخص جدید" else "ویرایش شخص") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام و نام خانوادگی") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("person_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تماس (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("نسبت / توضیح (مثلاً: همکار، خانواده)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("رنگ آواتار", style = MaterialTheme.typography.labelMedium)
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
                    onSave(
                        PersonEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            phone = phone.trim(),
                            relationNote = note.trim(),
                            avatarColorHex = colorHex,
                            createdAtMillis = initial?.createdAtMillis ?: System.currentTimeMillis()
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_person_button")
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
