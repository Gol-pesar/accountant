package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.PendingSmsEntity
import com.example.data.local.PersonEntity
import com.example.domain.IranianBanks
import com.example.domain.JalaliCalendar
import com.example.domain.ParsedBankSms
import com.example.domain.ParsedTxType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IranianBankLogo
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.AccountingUiState

@Composable
fun SmsAnalyzerScreen(
    state: AccountingUiState,
    manualSmsText: String,
    manualParsedResult: ParsedBankSms?,
    smsPermissionDenied: Boolean,
    initialOpenPendingSmsId: Long?,
    onConsumedInitialPendingSmsId: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onScanInbox: () -> Unit,
    onSyncBalancesOnly: () -> Unit,
    onApprovePendingSms: (PendingSmsEntity, Long?, String, Long?, Int, String) -> Unit,
    onDismissPendingSms: (Long) -> Unit,
    onManualSmsTextChange: (String) -> Unit,
    onCommitManualSms: (ParsedBankSms, Long?, String, Long?, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val showRial = state.preferences.showInRial
    val hasBaseline = state.preferences.smsBaselineTimestampMillis > 0L
    var detailPendingSms by remember { mutableStateOf<PendingSmsEntity?>(null) }

    // Automatically open the transaction detail dialog when user taps a bank SMS notification
    LaunchedEffect(initialOpenPendingSmsId, state.pendingSms) {
        val targetId = initialOpenPendingSmsId ?: return@LaunchedEffect
        if (targetId > 0L) {
            val matched = state.pendingSms.firstOrNull { it.id == targetId }
            if (matched != null) {
                detailPendingSms = matched
                onConsumedInitialPendingSmsId()
            }
        } else {
            onConsumedInitialPendingSmsId()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "تحلیلگر هوشمند پیامک‌های بانکی",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "در اولین اتصال فقط تراز و موجودی فعلی حساب‌ها ثبت می‌شود؛ سپس پس از هر تراکنش جدید، شما جزئیات آن را تعیین می‌کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // SMS Permission / Balance Baseline & New SMS Scanner Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.hasSmsPermission) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.hasSmsPermission) Icons.Rounded.Sms else Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    !state.hasSmsPermission -> "اتصال به صندوق پیامک گوشی"
                                    !hasBaseline -> "آماده ثبت موجودی تراز اولیه"
                                    else -> "دسترسی پیامک فعال است"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    !state.hasSmsPermission -> "برای ثبت موجودی تراز اولیه و دریافت پیامک‌های جدید بانکی به صورت کاملاً آفلاین، مجوز پیامک را تأیید کنید."
                                    !hasBaseline -> "با زدن دکمه زیر، فقط آخرین موجودی هر بانک ثبت می‌شود و تراکنش‌های قدیمی گذشته وارد دفتر نمی‌شوند."
                                    else -> "تراز اولیه ثبت شده است. از این پس فقط پیامک‌های جدید بررسی شده و برای تعیین جزئیات (عنوان، دسته‌بندی، شخص و یادداشت) به شما نمایش داده می‌شوند."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (!state.hasSmsPermission) {
                        Button(
                            onClick = onRequestSmsPermission,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("request_sms_permission_button")
                        ) {
                            Text("اعطای دسترسی خواندن پیامک")
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("open_sms_app_settings_button")
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("باز کردن تنظیمات مجوز برنامه")
                        }

                        if (smsPermissionDenied) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "راهنمای فعال‌سازی در اندروید ۱۳ به بالا:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "۱) دکمه «باز کردن تنظیمات مجوز برنامه» را بزنید.\n۲) در بالای صفحه، آیکون سه نقطه (⋮) را لمس کرده و گزینه «مجاز کردن تنظیمات محدودشده / Allow restricted settings» را بزنید.\n۳) سپس وارد بخش «مجوزها / Permissions» ← «پیامک / SMS» شده و آن را روی مجاز (Allow) بگذارید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onScanInbox,
                            enabled = !state.isScanningSms,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("scan_sms_inbox_button")
                        ) {
                            if (state.isScanningSms) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("در حال بررسی پیامک‌ها...")
                            } else {
                                Icon(
                                    imageVector = if (hasBaseline) Icons.Rounded.Refresh else Icons.Rounded.AccountBalance,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (hasBaseline) {
                                        "بررسی پیامک‌های بانکی جدید"
                                    } else {
                                        "ثبت موجودی تراز اولیه از پیامک‌ها"
                                    }
                                )
                            }
                        }

                        if (hasBaseline) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onSyncBalancesOnly,
                                enabled = !state.isScanningSms,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("sync_balances_only_button")
                            ) {
                                Icon(Icons.Rounded.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("به‌روزرسانی فقط موجودی تراز حساب‌ها", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Instant Bank SMS Notification Permission Prompt Card
        if (!state.hasNotificationPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "فعال‌سازی اعلان فوری تراکنش‌های بانکی",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "با اعطای دسترسی اعلانات، هر پیامک بانکی که دریافت شود بلافاصله در نوار اعلانات نمایش داده می‌شود تا همان لحظه شرح آن را تعیین کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Button(
                            onClick = onRequestNotificationPermission,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("sms_request_notification_permission_button")
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("اعطای دسترسی اعلانات")
                        }
                    }
                }
            }
        }

        // Pending Detected Bank SMS Queue (Mandatory User Detail Entry)
        item {
            Text(
                text = "تراکنش‌های جدید در انتظار تعیین جزئیات (${JalaliCalendar.toPersianDigits(state.pendingSms.size)})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.pendingSms.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Sms,
                    title = "تراکنش جدیدی در انتظار تعیین جزئیات نیست",
                    subtitle = if (hasBaseline) {
                        "به محض دریافت پیامک بانکی جدید، در این بخش نمایش داده می‌شود تا عنوان، دسته‌بندی، شخص و یادداشت آن را مشخص کنید."
                    } else {
                        "ابتدا دکمه «ثبت موجودی تراز اولیه از پیامک‌ها» را بزنید تا فقط موجودی فعلی حساب‌های شما ثبت شود."
                    }
                )
            }
        } else {
            items(state.pendingSms, key = { it.id }) { sms ->
                PendingSmsCard(
                    sms = sms,
                    showInRial = showRial,
                    onOpenDetails = { detailPendingSms = sms },
                    onDismiss = { onDismissPendingSms(sms.id) }
                )
            }
        }

        // Manual SMS Text Paste & Instant Analyzer Card
        item {
            ManualSmsAnalyzerCard(
                text = manualSmsText,
                parsed = manualParsedResult,
                categories = state.categories,
                showInRial = showRial,
                onTextChange = onManualSmsTextChange,
                onCommit = onCommitManualSms
            )
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    val activePending = detailPendingSms
    if (activePending != null) {
        PendingSmsDetailDialog(
            sms = activePending,
            categories = state.categories,
            people = state.people,
            showInRial = showRial,
            onDismiss = { detailPendingSms = null },
            onConfirm = { categoryId, title, personId, personImpact, note ->
                onApprovePendingSms(activePending, categoryId, title, personId, personImpact, note)
                detailPendingSms = null
            }
        )
    }
}

@Composable
private fun PendingSmsCard(
    sms: PendingSmsEntity,
    showInRial: Boolean,
    onOpenDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    val bank = IranianBanks.findByCode(sms.bankCode)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IranianBankLogo(bank = bank, size = 42.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bank.persianName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = JalaliCalendar.fromEpochMillis(sms.timestampMillis).formatFull(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (sms.isIncome) "+" else "-"}${
                            JalaliCalendar.formatCurrency(sms.amountToman, showInRial)
                        }",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (sms.isIncome) IncomeGreen else ExpenseCrimson
                    )
                    if (sms.remainingBalanceToman != null) {
                        Text(
                            text = "مانده: ${JalaliCalendar.formatCompactCurrency(sms.remainingBalanceToman, showInRial)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = sms.rawBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                    maxLines = 3
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.36f)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("رد کردن")
                }
                Button(
                    onClick = onOpenDetails,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.64f)
                        .testTag("complete_sms_details_${sms.id}")
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تعیین جزئیات و ثبت")
                }
            }
        }
    }
}

@Composable
private fun PendingSmsDetailDialog(
    sms: PendingSmsEntity,
    categories: List<CategoryEntity>,
    people: List<PersonEntity>,
    showInRial: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long?, title: String, personId: Long?, personImpact: Int, note: String) -> Unit
) {
    val bank = IranianBanks.findByCode(sms.bankCode)
    val matchingCats = remember(categories, sms.isIncome) {
        categories.filter { it.isIncome == sms.isIncome }
    }
    var selectedCatId by remember(sms.id) {
        mutableStateOf(
            matchingCats.firstOrNull { it.name == sms.suggestedCategoryName }?.id
                ?: matchingCats.firstOrNull()?.id
        )
    }
    var title by remember(sms.id) { mutableStateOf("") }
    var selectedPersonId by remember(sms.id) { mutableStateOf<Long?>(null) }
    var personImpact by remember(sms.id) { mutableIntStateOf(0) }
    var note by remember(sms.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IranianBankLogo(bank = bank, size = 34.dp, circular = true)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "تکمیل جزئیات تراکنش",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${bank.shortName} • ${if (sms.isIncome) "+" else "-"}${
                            JalaliCalendar.formatCurrency(sms.amountToman, showInRial)
                        }",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sms.isIncome) IncomeGreen else ExpenseCrimson,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = JalaliCalendar.fromEpochMillis(sms.timestampMillis).formatFull(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = sms.rawBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان تراکنش (بابت چه چیزی بود؟)") },
                    placeholder = { Text("مثلاً: خرید سوپرمارکت، کرایه اسنپ، واریز حقوق...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_sms_title_input")
                )

                Text("دسته‌بندی", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(matchingCats, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = cat.id },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (people.isNotEmpty()) {
                    Text("شخص مرتبط (اختیاری)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedPersonId == null,
                                onClick = {
                                    selectedPersonId = null
                                    personImpact = 0
                                },
                                label = { Text("بدون شخص", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        items(people, key = { it.id }) { person ->
                            FilterChip(
                                selected = selectedPersonId == person.id,
                                onClick = { selectedPersonId = person.id },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(person.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    if (selectedPersonId != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = personImpact == 0,
                                onClick = { personImpact = 0 },
                                label = { Text("فقط برچسب", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = personImpact == +1,
                                onClick = { personImpact = +1 },
                                label = { Text("طلب من (+)", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = personImpact == -1,
                                onClick = { personImpact = -1 },
                                label = { Text("بدهی من (-)", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات / یادداشت (اختیاری)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_sms_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim().ifBlank {
                        matchingCats.firstOrNull { it.id == selectedCatId }?.name
                            ?: "${bank.shortName} - ${if (sms.isIncome) "واریز" else "برداشت"}"
                    }
                    onConfirm(selectedCatId, finalTitle, selectedPersonId, personImpact, note.trim())
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_pending_sms_detail_button")
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("ثبت نهایی تراکنش")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun ManualSmsAnalyzerCard(
    text: String,
    parsed: ParsedBankSms?,
    categories: List<CategoryEntity>,
    showInRial: Boolean,
    onTextChange: (String) -> Unit,
    onCommit: (ParsedBankSms, Long?, String, Long?, Int, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "تحلیلگر دستی متن پیامک بانکی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "متن هر پیامک بانکی ایرانی را اینجا کپی کنید تا بانک، نوع تراکنش، مبلغ و مانده استخراج شده و جزئیات آن را ثبت کنید:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text("مثال: بانک ملت - برداشت مبلغ 2,500,000 ریال از کارت 610433****1234 مانده 18,400,000 ریال")
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_sms_input")
            )

            if (parsed != null) {
                val isIncome = parsed.type == ParsedTxType.INCOME
                val matchingCats = categories.filter { it.isIncome == isIncome }
                var selectedCatId by remember(parsed.smsHash) {
                    mutableStateOf(
                        matchingCats.firstOrNull { it.name == parsed.suggestedCategoryName }?.id
                            ?: matchingCats.firstOrNull()?.id
                    )
                }
                var customTitle by remember(parsed.smsHash) { mutableStateOf("") }
                var customNote by remember(parsed.smsHash) { mutableStateOf("") }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IranianBankLogo(bank = parsed.bank, size = 40.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = parsed.bank.persianName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isIncome) "واریز (درآمد)" else "برداشت (هزینه)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isIncome) IncomeGreen else ExpenseCrimson,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = JalaliCalendar.formatCurrency(parsed.amountToman, showInRial),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) IncomeGreen else ExpenseCrimson
                            )
                        }

                        if (parsed.remainingBalanceToman != null) {
                            Text(
                                text = "مانده تشخیص داده‌شده: ${JalaliCalendar.formatCurrency(parsed.remainingBalanceToman, showInRial)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("عنوان تراکنش (بابت چه چیزی؟)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(matchingCats, key = { it.id }) { cat ->
                                FilterChip(
                                    selected = selectedCatId == cat.id,
                                    onClick = { selectedCatId = cat.id },
                                    label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customNote,
                            onValueChange = { customNote = it },
                            label = { Text("توضیحات (اختیاری)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val catName = matchingCats.firstOrNull { it.id == selectedCatId }?.name
                                val txTitle = customTitle.trim().ifBlank {
                                    catName ?: "${parsed.bank.shortName} - ${if (isIncome) "واریز" else "برداشت"}"
                                }
                                onCommit(parsed, selectedCatId, txTitle, null, 0, customNote.trim())
                            },
                            enabled = customTitle.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("commit_manual_sms_button")
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("ثبت در دفتر تراکنش‌ها")
                        }
                    }
                }
            } else if (text.isNotBlank()) {
                Text(
                    text = "الگوی مبلغ یا نوع تراکنش بانکی در متن شناسایی نشد. مطمئن شوید کلماتی مثل برداشت/واریز و مبلغ در پیامک وجود دارد.",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExpenseCrimson
                )
            }
        }
    }
}
