package com.jibet.app.ui.stats

import android.app.Application
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.CategorySum
import com.jibet.app.data.TransactionEntity
import com.jibet.app.ui.components.AmountText
import com.jibet.app.ui.components.EmojiBadge
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassCard
import com.jibet.app.ui.components.ProgressBarSoft
import com.jibet.app.ui.components.SectionTitle
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.util.Fmt
import com.jibet.app.util.Jalali
import com.jibet.app.util.Ranges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class StatMode { WEEK, MONTH, YEAR }

/** One chart bucket: expense + income totals plus an x-axis label. */
private data class ChartBucket(val expense: Double, val income: Double, val label: String)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as JibetApp).repository

    val mode = MutableStateFlow(StatMode.MONTH)

    val transactions = repo.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** income to expense for the selected period */
    var sums by mutableStateOf(0.0 to 0.0)
        private set

    var categoryBreakdown by mutableStateOf(emptyList<CategorySum>())
        private set

    fun setMode(m: StatMode) {
        mode.value = m
    }

    fun reload() {
        viewModelScope.launch {
            val r = when (mode.value) {
                StatMode.WEEK -> Ranges.thisWeek()
                StatMode.MONTH -> Ranges.thisMonth()
                StatMode.YEAR -> Ranges.thisYear()
            }
            sums = repo.sumsBetween(r.first, r.second)
            categoryBreakdown = repo.categorySums(r.first, r.second, false)
        }
    }
}

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val vm: StatsViewModel = viewModel()
    val mode by vm.mode.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val jc = LocalJibetColors.current

    LaunchedEffect(mode) { vm.reload() }

    val buckets = remember(transactions, mode) { buildBuckets(transactions, mode) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
                Text(text = "نمودارها", style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == StatMode.WEEK,
                    onClick = { vm.setMode(StatMode.WEEK) },
                    label = { Text("هفته") }
                )
                FilterChip(
                    selected = mode == StatMode.MONTH,
                    onClick = { vm.setMode(StatMode.MONTH) },
                    label = { Text("ماه") }
                )
                FilterChip(
                    selected = mode == StatMode.YEAR,
                    onClick = { vm.setMode(StatMode.YEAR) },
                    label = { Text("سال") }
                )
            }
        }
        item {
            GlassCard {
                GroupedBarChart(buckets = buckets)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = jc.expense, label = "هزینه")
                    LegendDot(color = jc.income, label = "درآمد")
                }
            }
        }
        item {
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "جمع درآمد",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        AmountText(
                            amount = vm.sums.first,
                            isIncome = true,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "جمع هزینه",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        AmountText(
                            amount = vm.sums.second,
                            isIncome = false,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "خالص",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        val net = vm.sums.first - vm.sums.second
                        AmountText(
                            amount = net,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (net >= 0) jc.income else jc.expense
                        )
                    }
                }
            }
        }
        item { SectionTitle("بر اساس دسته") }
        item {
            val sorted = vm.categoryBreakdown.sortedByDescending { it.total }
            Column(Modifier.animateContentSize()) {
                if (sorted.isEmpty()) {
                    EmptyState(emoji = "📊", title = "داده‌ای برای این بازه نیست")
                } else {
                    val maxT = sorted.maxOf { it.total }.coerceAtLeast(1.0)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        sorted.forEach { s ->
                            val cat = categories.firstOrNull { it.id == s.categoryId }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                EmojiBadge(
                                    emoji = cat?.emoji ?: "🏷",
                                    colorHex = cat?.colorHex ?: "#6F7975",
                                    size = 40.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat?.name ?: "بدون دسته",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    ProgressBarSoft(
                                        progress = (s.total / maxT).toFloat(),
                                        color = jc.expense
                                    )
                                }
                                AmountText(
                                    amount = s.total,
                                    isIncome = false,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Grouped expense/income bars with baseline and x labels drawn inside the canvas (RTL flow). */
@Composable
private fun GroupedBarChart(buckets: List<ChartBucket>) {
    val jc = LocalJibetColors.current
    val outline = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall
        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()
    val maxV = (buckets.maxOfOrNull { maxOf(it.expense, it.income) } ?: 0.0).coerceAtLeast(1.0)
    val animatedMax by animateFloatAsState(
        targetValue = maxV.toFloat(),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "statsMax"
    )
    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
        val n = buckets.size.coerceAtLeast(1)
        val slot = size.width / n
        val pairGap = (slot * 0.22f).coerceIn(2.dp.toPx(), 6.dp.toPx())
        val barW = ((slot - pairGap) / 2f).coerceIn(2.dp.toPx(), 20.dp.toPx())
        val baselineY = size.height - 20.dp.toPx()
        val availH = baselineY - 4.dp.toPx()
        buckets.forEachIndexed { i, b ->
            val slotLeft = size.width - (i + 1) * slot
            val cx = slotLeft + slot / 2f
            val expH = if (b.expense > 0.0 && animatedMax > 0f) {
                (b.expense.toFloat() / animatedMax * availH).coerceIn(3.dp.toPx(), availH)
            } else 0f
            val incH = if (b.income > 0.0 && animatedMax > 0f) {
                (b.income.toFloat() / animatedMax * availH).coerceIn(3.dp.toPx(), availH)
            } else 0f
            if (expH > 0f) {
                drawRoundRect(
                    color = jc.expense,
                    topLeft = Offset(cx + pairGap / 2f, baselineY - expH),
                    size = Size(barW, expH),
                    cornerRadius = CornerRadius(minOf(barW, expH) / 2f, minOf(barW, expH) / 2f)
                )
            }
            if (incH > 0f) {
                drawRoundRect(
                    color = jc.income,
                    topLeft = Offset(cx - pairGap / 2f - barW, baselineY - incH),
                    size = Size(barW, incH),
                    cornerRadius = CornerRadius(minOf(barW, incH) / 2f, minOf(barW, incH) / 2f)
                )
            }
            if (b.label.isNotEmpty()) {
                val layout = textMeasurer.measure(text = b.label, style = labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(cx - layout.size.width / 2f, baselineY + 5.dp.toPx())
                )
            }
        }
        drawLine(
            color = outline.copy(alpha = 0.3f),
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildBuckets(transactions: List<TransactionEntity>, mode: StatMode): List<ChartBucket> {
    val zone = ZoneId.systemDefault()
    return when (mode) {
        StatMode.WEEK -> {
            val dates = Ranges.lastNDates(7)
            val expense = HashMap<LocalDate, Double>()
            val income = HashMap<LocalDate, Double>()
            dates.forEach { expense[it] = 0.0; income[it] = 0.0 }
            transactions.forEach { tx ->
                val d = Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()
                if (expense.containsKey(d)) {
                    if (tx.isIncome) income[d] = (income[d] ?: 0.0) + tx.amount
                    else expense[d] = (expense[d] ?: 0.0) + tx.amount
                }
            }
            dates.map { d ->
                ChartBucket(expense[d] ?: 0.0, income[d] ?: 0.0, Fmt.weekdayShort(d.dayOfWeek))
            }
        }
        StatMode.MONTH -> {
            val r = Ranges.thisMonth()
            val currentJd = Jalali.parts(System.currentTimeMillis())[2]
            val expense = DoubleArray(currentJd)
            val income = DoubleArray(currentJd)
            transactions.forEach { tx ->
                if (tx.timestamp in r.first..r.second) {
                    val jd = Jalali.parts(tx.timestamp)[2]
                    if (jd in 1..currentJd) {
                        if (tx.isIncome) income[jd - 1] += tx.amount
                        else expense[jd - 1] += tx.amount
                    }
                }
            }
            (1..currentJd).map { d ->
                ChartBucket(
                    expense[d - 1],
                    income[d - 1],
                    if (d % 5 == 0) Fmt.faNum(d.toLong()) else ""
                )
            }
        }
        StatMode.YEAR -> {
            val r = Ranges.thisYear()
            val jy = Jalali.parts(System.currentTimeMillis())[0]
            val expense = DoubleArray(12)
            val income = DoubleArray(12)
            transactions.forEach { tx ->
                if (tx.timestamp in r.first..r.second) {
                    val p = Jalali.parts(tx.timestamp)
                    if (p[0] == jy) {
                        if (tx.isIncome) income[p[1] - 1] += tx.amount
                        else expense[p[1] - 1] += tx.amount
                    }
                }
            }
            (1..12).map { m -> ChartBucket(expense[m - 1], income[m - 1], Fmt.MONTHS[m - 1].take(2)) }
        }
    }
}
