package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.local.TransactionEntity
import com.example.domain.IranianBanks
import com.example.domain.JalaliCalendar
import com.example.domain.JalaliYearMonth
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IranianBankLogo
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.AccountingUiState
import com.example.ui.viewmodel.TxFilterType

@Composable
fun TransactionsScreen(
    state: AccountingUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToday: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(TxFilterType.ALL) }
    var filterBankCode by remember { mutableStateOf<String?>(null) }
    var showAllMonths by remember { mutableStateOf(false) }

    val showRial = state.preferences.showInRial
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val peopleMap = remember(state.people) { state.people.associateBy { it.id } }

    val baseList = if (showAllMonths) state.transactions else state.monthTransactions
    val filtered = remember(baseList, searchQuery, filterType, filterBankCode, catMap, peopleMap) {
        val q = searchQuery.trim().lowercase()
        baseList.filter { tx ->
            val typeOk = when (filterType) {
                TxFilterType.ALL -> true
                TxFilterType.EXPENSE -> !tx.isIncome
                TxFilterType.INCOME -> tx.isIncome
            }
            val bankOk = filterBankCode == null || tx.bankCode == filterBankCode
            val queryOk = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.note.lowercase().contains(q) ||
                (catMap[tx.categoryId]?.name?.lowercase()?.contains(q) == true) ||
                (tx.personId?.let { peopleMap[it]?.name?.lowercase()?.contains(q) } == true) ||
                IranianBanks.findByCode(tx.bankCode).persianName.lowercase().contains(q)
            typeOk && bankOk && queryOk
        }
    }

    val usedBanks = remember(baseList) {
        baseList.map { it.bankCode }.distinct().map { IranianBanks.findByCode(it) }
    }

    val filteredIncome = filtered.filter { it.isIncome }.sumOf { it.amountToman }
    val filteredExpense = filtered.filter { !it.isIncome }.sumOf { it.amountToman }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "دفتر تراکنش‌ها",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                FilterChip(
                    selected = showAllMonths,
                    onClick = { showAllMonths = !showAllMonths },
                    label = { Text(if (showAllMonths) "همه ماه‌ها" else "فقط این ماه") }
                )
            }
        }

        if (!showAllMonths) {
            item {
                MonthSelectorBar(
                    month = state.selectedMonth,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onResetToday = onResetToday
                )
            }
        }

        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جستجو در عنوان، دسته‌بندی، بانک یا شخص...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "پاک کردن جستجو")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input")
            )
        }

        // Type & Bank Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterType == TxFilterType.ALL,
                        onClick = { filterType = TxFilterType.ALL },
                        label = { Text("همه (${JalaliCalendar.toPersianDigits(baseList.size)})") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == TxFilterType.EXPENSE,
                        onClick = { filterType = TxFilterType.EXPENSE },
                        label = { Text("هزینه‌ها") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == TxFilterType.INCOME,
                        onClick = { filterType = TxFilterType.INCOME },
                        label = { Text("درآمدها") }
                    )
                }
                items(usedBanks, key = { it.code }) { bank ->
                    FilterChip(
                        selected = filterBankCode == bank.code,
                        onClick = {
                            filterBankCode = if (filterBankCode == bank.code) null else bank.code
                        },
                        leadingIcon = {
                            IranianBankLogo(bank = bank, size = 18.dp, circular = true)
                        },
                        label = { Text(bank.shortName) }
                    )
                }
            }
        }

        // Summary Bar
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("جمع درآمد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = JalaliCalendar.formatCurrency(filteredIncome, showRial),
                            style = MaterialTheme.typography.labelLarge,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("جمع هزینه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = JalaliCalendar.formatCurrency(filteredExpense, showRial),
                            style = MaterialTheme.typography.labelLarge,
                            color = ExpenseCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.ReceiptLong,
                    title = if (showAllMonths) "تراکنشی یافت نشد" else "در ${state.selectedMonth.format()} تراکنشی ثبت نشده است",
                    subtitle = "فیلترها را تغییر دهید یا یک تراکنش جدید ثبت کنید.",
                    actionLabel = "ثبت تراکنش جدید",
                    onAction = onAddTransaction
                )
            }
        } else if (showAllMonths) {
            val groupedByMonth = filtered.groupBy { tx ->
                val d = JalaliCalendar.fromEpochMillis(tx.timestampMillis)
                JalaliYearMonth(d.year, d.month)
            }.entries.sortedWith(
                compareByDescending<Map.Entry<JalaliYearMonth, List<TransactionEntity>>> { it.key.year }
                    .thenByDescending { it.key.month }
            )

            groupedByMonth.forEach { (ym, monthList) ->
                val mIncome = monthList.filter { it.isIncome }.sumOf { it.amountToman }
                val mExpense = monthList.filter { !it.isIncome }.sumOf { it.amountToman }
                item(key = "month_header_${ym.year}_${ym.month}") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = ym.format(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${JalaliCalendar.toPersianDigits(monthList.size)} تراکنش",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (mIncome > 0L) {
                                    Text(
                                        text = "+${JalaliCalendar.formatCompactCurrency(mIncome, showRial)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                                if (mExpense > 0L) {
                                    Text(
                                        text = "-${JalaliCalendar.formatCompactCurrency(mExpense, showRial)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseCrimson
                                    )
                                }
                            }
                        }
                    }
                }
                items(monthList, key = { it.id }) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        category = catMap[tx.categoryId],
                        person = tx.personId?.let { peopleMap[it] },
                        showInRial = showRial,
                        onClick = { onEditTransaction(tx) }
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
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
