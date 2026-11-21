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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.domain.JalaliCalendar
import com.example.ui.components.BudgetStatusRow
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.DailySpendingAreaChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.SixMonthBarChart
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.AccountingUiState

@Composable
fun AnalyticsScreen(
    state: AccountingUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToday: () -> Unit,
    onShareMonthlyReport: () -> Unit,
    onManageCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showIncomeDonut by remember { mutableStateOf(false) }
    val showRial = state.preferences.showInRial
    val netMonth = state.monthIncomeToman - state.monthExpenseToman
    val savingsRate = if (state.monthIncomeToman > 0L) {
        ((netMonth.toFloat() / state.monthIncomeToman.toFloat()) * 100f).toInt().coerceIn(-999, 100)
    } else 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "نمودارها و تحلیل ماهانه",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = onShareMonthlyReport,
                    modifier = Modifier.testTag("share_report_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("گزارش")
                }
            }
        }

        item {
            MonthSelectorBar(
                month = state.selectedMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onResetToday = onResetToday
            )
        }

        // KPI Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    label = "تراز خالص ماه",
                    value = JalaliCalendar.formatCurrency(netMonth, showRial),
                    valueColor = if (netMonth >= 0L) IncomeGreen else ExpenseCrimson,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "نرخ پس‌انداز",
                    value = "${JalaliCalendar.toPersianDigits(savingsRate)}٪",
                    valueColor = if (savingsRate >= 0) MaterialTheme.colorScheme.primary else ExpenseCrimson,
                    modifier = Modifier.weight(0.75f)
                )
            }
        }

        // Donut Chart Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سهم دسته‌بندی‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !showIncomeDonut,
                                onClick = { showIncomeDonut = false },
                                label = { Text("هزینه") }
                            )
                            FilterChip(
                                selected = showIncomeDonut,
                                onClick = { showIncomeDonut = true },
                                label = { Text("درآمد") }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val slices = if (showIncomeDonut) state.incomeSlices else state.expenseSlices
                    val total = if (showIncomeDonut) state.monthIncomeToman else state.monthExpenseToman
                    if (slices.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Rounded.PieChart,
                            title = "داده‌ای برای این ماه ثبت نشده",
                            subtitle = "با ثبت هزینه‌ها و درآمدها، نمودار تفکیک دسته‌بندی به صورت زنده نمایش داده می‌شود."
                        )
                    } else {
                        CategoryDonutChart(
                            slices = slices,
                            totalToman = total,
                            showInRial = showRial,
                            title = if (showIncomeDonut) "جمع درآمد" else "جمع هزینه"
                        )
                    }
                }
            }
        }

        // 6-Month Bar Comparison Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مقایسه ۶ ماهه درآمد و هزینه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    SixMonthBarChart(
                        points = state.sixMonthSeries,
                        showInRial = showRial
                    )
                }
            }
        }

        // Daily Spending Area Trend Chart
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "روند هزینه‌های روزانه (${state.selectedMonth.monthName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    DailySpendingAreaChart(
                        points = state.dailySpendingSeries,
                        showInRial = showRial
                    )
                }
            }
        }

        // Monthly Category Budgets
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "کنترل بودجه ماهانه دسته‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(onClick = onManageCategories) {
                            Text("تنظیم بودجه")
                        }
                    }

                    if (state.budgetStatuses.isEmpty()) {
                        Text(
                            text = "هنوز سقف بودجه‌ای برای دسته‌بندی‌ها تعیین نکرده‌اید. از بخش «تنظیمات ← دسته‌بندی‌ها» می‌توانید برای هر دسته بودجه ماهانه تعریف کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.budgetStatuses.forEach { bs ->
                            BudgetStatusRow(status = bs, showInRial = showRial)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}
