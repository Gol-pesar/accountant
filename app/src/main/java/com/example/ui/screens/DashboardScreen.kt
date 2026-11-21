package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TransactionEntity
import com.example.domain.JalaliCalendar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IranianBankCardTile
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.AmberBright
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.AccountingUiState

@Composable
fun DashboardScreen(
    state: AccountingUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToday: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onOpenSms: () -> Unit,
    onOpenPeople: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestDriveBackup: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onToggleCurrency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showRial = state.preferences.showInRial
    val catMap = state.categories.associateBy { it.id }
    val peopleMap = state.people.associateBy { it.id }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting & Currency Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "حسابدار من",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = JalaliCalendar.today().formatFull(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(onClick = onToggleCurrency)
                        .testTag("currency_toggle_pill")
                ) {
                    Text(
                        text = if (showRial) "واحد: ریال" else "واحد: تومان",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Jalali Month Switcher — strictly separates each month's data
        item {
            MonthSelectorBar(
                month = state.selectedMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onResetToday = onResetToday
            )
        }

        // Hero Vault Balance Card with Generated Emerald-Gold Girih Art
        item {
            HeroBalanceVaultCard(
                totalBalanceToman = state.totalNetBalanceToman,
                monthName = state.selectedMonth.format(),
                monthShortName = state.selectedMonth.monthName,
                monthIncomeToman = state.monthIncomeToman,
                monthExpenseToman = state.monthExpenseToman,
                showInRial = showRial
            )
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Rounded.ArrowUpward,
                    label = "ثبت هزینه",
                    tint = ExpenseCrimson,
                    onClick = onAddExpense,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_expense")
                )
                QuickActionTile(
                    icon = Icons.Rounded.ArrowDownward,
                    label = "ثبت درآمد",
                    tint = IncomeGreen,
                    onClick = onAddIncome,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_income")
                )
                QuickActionTile(
                    icon = Icons.Rounded.Sms,
                    label = "پیامک بانکی",
                    tint = MaterialTheme.colorScheme.primary,
                    badgeCount = state.pendingSms.size,
                    onClick = onOpenSms,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_open_sms")
                )
                QuickActionTile(
                    icon = Icons.Rounded.CloudUpload,
                    label = "بک‌آپ درایو",
                    tint = AmberBright,
                    onClick = onRequestDriveBackup,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_drive_backup")
                )
            }
        }

        // Pending Bank SMS Notification Banner
        if (state.pendingSms.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenSms)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sms,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${JalaliCalendar.toPersianDigits(state.pendingSms.size)} تراکنش جدید در انتظار تعیین جزئیات",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "برای مشخص کردن عنوان، دسته‌بندی، شخص و توضیحات هر تراکنش لمس کنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Bank Accounts Carousel
        item {
            SectionHeader(
                title = "کارت‌ها و حساب‌های بانکی",
                actionLabel = "مدیریت کارت‌ها",
                onAction = onOpenSettings
            )
            Spacer(Modifier.height(8.dp))
            if (state.bankAccounts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenSettings)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "افزودن کارت‌های بانکی ایران",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "تشخیص خودکار نام و لوگوی ۲۲ بانک ایرانی از روی شماره کارت",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "افزودن کارت",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.bankAccounts, key = { it.id }) { acc ->
                        IranianBankCardTile(
                            account = acc,
                            showInRial = showRial,
                            onClick = onOpenSettings
                        )
                    }
                }
            }
        }

        // People Debt/Credit Summary Strip (if any balances exist)
        if (state.totalReceivableToman > 0L || state.totalPayableToman > 0L) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenPeople)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("حساب اشخاص", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "طلب: ${JalaliCalendar.formatCurrency(state.totalReceivableToman, showRial)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "بدهی: ${JalaliCalendar.formatCurrency(state.totalPayableToman, showRial)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ExpenseCrimson,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Savings Goals Preview (قلک هوشمند)
        if (state.savingsGoals.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "قلک و اهداف پس‌انداز",
                    actionLabel = "مدیریت",
                    onAction = onOpenSettings
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.savingsGoals, key = { it.id }) { goal ->
                        val ratio = if (goal.targetAmountToman > 0L) {
                            (goal.savedAmountToman.toFloat() / goal.targetAmountToman.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .width(204.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClick = onOpenSettings)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Savings,
                                        contentDescription = null,
                                        tint = Color(goal.colorHex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(99.dp)),
                                    color = Color(goal.colorHex)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${JalaliCalendar.toPersianDigits((ratio * 100).toInt())}٪ • ${
                                        JalaliCalendar.formatCompactCurrency(goal.savedAmountToman, showRial)
                                    } از ${JalaliCalendar.formatCompactCurrency(goal.targetAmountToman, showRial)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Month Transactions (Strictly separated by Jalali month)
        item {
            SectionHeader(
                title = "تراکنش‌های ${state.selectedMonth.format()}",
                actionLabel = if (state.monthTransactions.isNotEmpty()) "مشاهده همه" else null,
                onAction = onOpenTransactions
            )
        }

        if (state.monthTransactions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "در ${state.selectedMonth.format()} تراکنشی ثبت نشده است",
                    subtitle = "تراکنش‌های هر ماه به‌صورت کاملاً جداگانه ثبت و محاسبه می‌شوند. برای ثبت تراکنش جدید یا مشاهده ماه‌های دیگر اقدام کنید.",
                    actionLabel = "ثبت تراکنش جدید",
                    onAction = onAddExpense
                )
            }
        } else {
            items(state.monthTransactions.take(6), key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    category = catMap[tx.categoryId],
                    person = tx.personId?.let { peopleMap[it] },
                    showInRial = showRial,
                    onClick = { onEditTransaction(tx) }
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun HeroBalanceVaultCard(
    totalBalanceToman: Long,
    monthName: String,
    monthShortName: String,
    monthIncomeToman: Long,
    monthExpenseToman: Long,
    showInRial: Boolean
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(206.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_vault_pattern_1795249799524),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                EmeraldDeep.copy(alpha = 0.52f),
                                Color(0xFF042F2E).copy(alpha = 0.86f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "موجودی کل حساب‌ها",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = JalaliCalendar.formatCurrency(totalBalanceToman, showInRial),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroStatPill(
                        label = "درآمد $monthShortName",
                        amount = JalaliCalendar.formatCurrency(monthIncomeToman, showInRial),
                        dotColor = EmeraldBright,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = "هزینه $monthShortName",
                        amount = JalaliCalendar.formatCurrency(monthExpenseToman, showInRial),
                        dotColor = Color(0xFFFCA5A5),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    label: String,
    amount: String,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.13f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(ExpenseCrimson),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = JalaliCalendar.toPersianDigits(badgeCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
