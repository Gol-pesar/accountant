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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sms
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    onRequestSmsPermission: () -> Unit,
    onScanInbox: () -> Unit,
    onApprovePendingSms: (PendingSmsEntity, Long?, String, Long?, Int) -> Unit,
    onDismissPendingSms: (Long) -> Unit,
    onManualSmsTextChange: (String) -> Unit,
    onCommitManualSms: (ParsedBankSms, Long?, String, Long?, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val showRial = state.preferences.showInRial

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
                text = "تشخیص خودکار ۲۲ بانک ایرانی، تبدیل ریال به تومان، استخراج مانده حساب و پیشنهاد دسته‌بندی",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // SMS Permission / Inbox Scanner Card
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
                                text = if (state.hasSmsPermission) "دسترسی پیامک فعال است" else "اتصال به صندوق پیامک گوشی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.hasSmsPermission) {
                                    "تحلیل پیامک‌ها کاملاً آفلاین روی گوشی شما انجام می‌شود و به هیچ سروری ارسال نمی‌گردد."
                                } else {
                                    "برای خواندن پیامک‌های بانکی و ثبت خودکار تراکنش‌ها، مجوز دسترسی پیامک را تأیید کنید."
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
                                Text("در حال اسکن پیامک‌ها...")
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("اسکن صندوق پیامک‌های بانکی")
                            }
                        }
                    }
                }
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

        // Pending Detected Bank SMS Queue
        item {
            Text(
                text = "پیامک‌های بانکی در انتظار تأیید (${JalaliCalendar.toPersianDigits(state.pendingSms.size)})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.pendingSms.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Sms,
                    title = "پیامک جدیدی در صف نیست",
                    subtitle = "دکمه «اسکن صندوق پیامک‌های بانکی» را بزنید یا متن یک پیامک بانکی را در کادر بالا جای‌گذاری کنید."
                )
            }
        } else {
            items(state.pendingSms, key = { it.id }) { sms ->
                PendingSmsCard(
                    sms = sms,
                    categories = state.categories,
                    showInRial = showRial,
                    onApprove = onApprovePendingSms,
                    onDismiss = { onDismissPendingSms(sms.id) }
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun ManualSmsAnalyzerCard(
    text: String,
    parsed: ParsedBankSms?,
    categories: List<CategoryEntity>,
    showInRial: Boolean,
    onTextChange: (String) -> Unit,
    onCommit: (ParsedBankSms, Long?, String, Long?, Int) -> Unit
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
                text = "متن هر پیامک بانکی ایرانی را اینجا کپی کنید تا بانک، نوع تراکنش، مبلغ و مانده بلافاصله استخراج شود:",
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

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(matchingCats, key = { it.id }) { cat ->
                                FilterChip(
                                    selected = selectedCatId == cat.id,
                                    onClick = { selectedCatId = cat.id },
                                    label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val catName = matchingCats.firstOrNull { it.id == selectedCatId }?.name
                                val txTitle = catName ?: "${parsed.bank.shortName} - ${if (isIncome) "واریز" else "برداشت"}"
                                onCommit(parsed, selectedCatId, txTitle, null, 0)
                            },
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

@Composable
private fun PendingSmsCard(
    sms: PendingSmsEntity,
    categories: List<CategoryEntity>,
    showInRial: Boolean,
    onApprove: (PendingSmsEntity, Long?, String, Long?, Int) -> Unit,
    onDismiss: () -> Unit
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

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(matchingCats, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCatId == cat.id,
                        onClick = { selectedCatId = cat.id },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.4f)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("رد کردن")
                }
                Button(
                    onClick = {
                        val cat = matchingCats.firstOrNull { it.id == selectedCatId }
                        val txTitle = cat?.name ?: "${bank.shortName} - ${if (sms.isIncome) "واریز" else "برداشت"}"
                        onApprove(sms, selectedCatId, txTitle, null, 0)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.6f)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تأیید و ثبت")
                }
            }
        }
    }
}
