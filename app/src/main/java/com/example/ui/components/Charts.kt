package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.JalaliCalendar
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen

data class CategorySlice(
    val categoryId: Long,
    val name: String,
    val amountToman: Long,
    val colorHex: Long,
    val iconKey: String,
    val percentage: Float
)

data class MonthlyBarPoint(
    val monthLabel: String,
    val incomeToman: Long,
    val expenseToman: Long
)

data class DailyPoint(
    val dayOfMonth: Int,
    val expenseToman: Long
)

data class BudgetStatus(
    val categoryId: Long,
    val categoryName: String,
    val iconKey: String,
    val colorHex: Long,
    val spentToman: Long,
    val budgetToman: Long
) {
    val progress: Float
        get() = if (budgetToman <= 0L) 0f else (spentToman.toFloat() / budgetToman.toFloat()).coerceIn(0f, 1.5f)
    val isExceeded: Boolean get() = budgetToman > 0L && spentToman > budgetToman
}

@Composable
fun CategoryDonutChart(
    slices: List<CategorySlice>,
    totalToman: Long,
    showInRial: Boolean,
    title: String,
    modifier: Modifier = Modifier
) {
    val animProgress = remember(slices) { Animatable(0f) }
    var selectedIdx by remember(slices) { mutableIntStateOf(-1) }

    LaunchedEffect(slices) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val activeSlice = slices.getOrNull(selectedIdx)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(188.dp)) {
                val strokeWidth = 28.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)

                if (slices.isEmpty() || totalToman <= 0L) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                } else {
                    var startAngle = -90f
                    val gap = if (slices.size > 1) 3f else 0f
                    slices.forEachIndexed { index, slice ->
                        val rawSweep = (slice.percentage * 360f * animProgress.value)
                        val sweep = (rawSweep - gap).coerceAtLeast(1f)
                        val isSelected = index == selectedIdx
                        drawArc(
                            color = Color(slice.colorHex),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += rawSweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeSlice?.name ?: title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = JalaliCalendar.formatCurrency(
                        amountToman = activeSlice?.amountToman ?: totalToman,
                        showInRial = showInRial,
                        withUnit = false
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (activeSlice != null) {
                        "${JalaliCalendar.toPersianDigits((activeSlice.percentage * 100).toInt())}٪"
                    } else {
                        if (showInRial) "ریال" else "تومان"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (slices.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slices.forEachIndexed { index, slice ->
                    val isSelected = selectedIdx == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            Color(slice.colorHex).copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedIdx = if (selectedIdx == index) -1 else index
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(slice.colorHex))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = slice.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${JalaliCalendar.toPersianDigits((slice.percentage * 100).toInt())}٪",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = JalaliCalendar.formatCurrency(slice.amountToman, showInRial),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SixMonthBarChart(
    points: List<MonthlyBarPoint>,
    showInRial: Boolean,
    modifier: Modifier = Modifier
) {
    val anim = remember(points) { Animatable(0f) }
    LaunchedEffect(points) {
        anim.snapTo(0f)
        anim.animateTo(1f, animationSpec = tween(750, easing = FastOutSlowInEasing))
    }

    val maxVal = remember(points) {
        points.maxOfOrNull { maxOf(it.incomeToman, it.expenseToman) }?.coerceAtLeast(1L) ?: 1L
    }
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = IncomeGreen, label = "درآمد")
            LegendDot(color = ExpenseCrimson, label = "هزینه")
            Spacer(Modifier.weight(1f))
            Text(
                text = "حداکثر: ${JalaliCalendar.formatCompactCurrency(maxVal, showInRial)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            val h = size.height
            val w = size.width
            // Horizontal guide lines
            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(
                    color = gridColor.copy(alpha = 0.65f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (points.isEmpty()) return@Canvas
            val groupWidth = w / points.size
            val barWidth = (groupWidth * 0.26f).coerceIn(8.dp.toPx(), 18.dp.toPx())
            val barGap = 4.dp.toPx()

            points.forEachIndexed { idx, pt ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val incHeight = ((pt.incomeToman.toFloat() / maxVal.toFloat()) * (h - 8.dp.toPx()) * anim.value)
                    .coerceAtLeast(if (pt.incomeToman > 0) 4.dp.toPx() else 0f)
                val expHeight = ((pt.expenseToman.toFloat() / maxVal.toFloat()) * (h - 8.dp.toPx()) * anim.value)
                    .coerceAtLeast(if (pt.expenseToman > 0) 4.dp.toPx() else 0f)

                if (incHeight > 0f) {
                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(centerX - barWidth - barGap / 2f, h - incHeight),
                        size = Size(barWidth, incHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
                if (expHeight > 0f) {
                    drawRoundRect(
                        color = ExpenseCrimson,
                        topLeft = Offset(centerX + barGap / 2f, h - expHeight),
                        size = Size(barWidth, expHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { pt ->
                Text(
                    text = pt.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DailySpendingAreaChart(
    points: List<DailyPoint>,
    showInRial: Boolean,
    modifier: Modifier = Modifier
) {
    val anim = remember(points) { Animatable(0f) }
    LaunchedEffect(points) {
        anim.snapTo(0f)
        anim.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val maxExpense = remember(points) {
        points.maxOfOrNull { it.expenseToman }?.coerceAtLeast(1L) ?: 1L
    }
    val peakDay = remember(points) { points.maxByOrNull { it.expenseToman } }
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        if (peakDay != null && peakDay.expenseToman > 0L) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "اوج خرج: روز ${JalaliCalendar.toPersianDigits(peakDay.dayOfMonth)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = JalaliCalendar.formatCurrency(peakDay.expenseToman, showInRial),
                    style = MaterialTheme.typography.labelLarge,
                    color = ExpenseCrimson,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
        ) {
            val w = size.width
            val h = size.height
            val padTop = 10.dp.toPx()
            val padBottom = 6.dp.toPx()
            val usableH = h - padTop - padBottom

            drawLine(
                color = gridColor.copy(alpha = 0.6f),
                start = Offset(0f, h - padBottom),
                end = Offset(w, h - padBottom),
                strokeWidth = 1.dp.toPx()
            )

            if (points.size < 2) return@Canvas
            val stepX = w / (points.size - 1)
            val linePath = Path()
            val fillPath = Path()

            points.forEachIndexed { idx, pt ->
                val x = idx * stepX
                val ratio = (pt.expenseToman.toFloat() / maxExpense.toFloat()) * anim.value
                val y = padTop + usableH * (1f - ratio)
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h - padBottom)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (idx - 1) * stepX
                    val prevRatio = (points[idx - 1].expenseToman.toFloat() / maxExpense.toFloat()) * anim.value
                    val prevY = padTop + usableH * (1f - prevRatio)
                    val ctrlX = (prevX + x) / 2f
                    linePath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
                    fillPath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
                }
            }
            fillPath.lineTo(w, h - padBottom)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primary.copy(alpha = 0.32f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = primary,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("روز ۱", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("روز ۱۵", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "روز ${JalaliCalendar.toPersianDigits(points.size)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BudgetStatusRow(
    status: BudgetStatus,
    showInRial: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        status.isExceeded -> ExpenseCrimson
        status.progress >= 0.8f -> AmberGold
        else -> IncomeGreen
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(status.colorHex))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = status.categoryName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${JalaliCalendar.toPersianDigits((status.progress * 100).toInt())}٪",
                style = MaterialTheme.typography.labelMedium,
                color = barColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { status.progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "مصرف: ${JalaliCalendar.formatCurrency(status.spentToman, showInRial)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "سقف: ${JalaliCalendar.formatCurrency(status.budgetToman, showInRial)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
