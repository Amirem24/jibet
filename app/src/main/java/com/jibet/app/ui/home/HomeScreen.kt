package com.jibet.app.ui.home

import android.app.Application
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.AccountEntity
import com.jibet.app.data.CategoryEntity
import com.jibet.app.data.TransactionEntity
import com.jibet.app.ui.components.AmountText
import com.jibet.app.ui.components.EmojiBadge
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassCard
import com.jibet.app.ui.components.LocalCurrency
import com.jibet.app.ui.components.SectionTitle
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.util.Fmt
import com.jibet.app.util.Jalali
import com.jibet.app.util.Ranges
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as JibetApp).repository

    val totalBalance = repo.totalBalance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val transactions = repo.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accounts = repo.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** income to expense for the current Jalali month — reactive to new transactions */
    val monthSums = repo.transactions
        .map { list ->
            val (from, to) = Ranges.thisMonth()
            var income = 0.0
            var expense = 0.0
            list.forEach { tx ->
                if (tx.timestamp in from..to) {
                    if (tx.isIncome) income += tx.amount else expense += tx.amount
                }
            }
            income to expense
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0 to 0.0)
}

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val vm: HomeViewModel = viewModel()
    val totalBalance by vm.totalBalance.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val monthSums by vm.monthSums.collectAsState()
    val jc = LocalJibetColors.current

    val recent = remember(transactions, categories, accounts) {
        transactions.take(6).mapNotNull { tx ->
            val acc = accounts.firstOrNull { it.id == tx.accountId } ?: return@mapNotNull null
            val cat = categories.firstOrNull { it.id == tx.categoryId }
            Triple(tx, cat, acc)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val p = remember { Jalali.parts(System.currentTimeMillis()) }
            Column {
                Text(
                    text = "سلام 👋",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = "جیبت", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Fmt.faNum(p[0].toLong()) + " " + Fmt.MONTHS[p[1] - 1],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            GlassCard(corner = 28.dp) {
                Text(
                    text = "موجودی کل",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                AmountText(amount = totalBalance, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonthStatChip(
                        label = "درآمد این ماه",
                        amount = monthSums.first,
                        tint = jc.income,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                    MonthStatChip(
                        label = "هزینه این ماه",
                        amount = monthSums.second,
                        tint = jc.expense,
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f)
                    )
                }
                TextButton(onClick = { onNavigate("stats") }) {
                    Text("نمودار جزئیات ←")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("➕", "ثبت", Modifier.weight(1f)) { onNavigate("add") }
                QuickAction("🎯", "اهداف", Modifier.weight(1f)) { onNavigate("goals") }
                QuickAction("💳", "حساب‌ها", Modifier.weight(1f)) { onNavigate("accounts") }
                QuickAction("🗂", "دسته‌ها", Modifier.weight(1f)) { onNavigate("categories") }
            }
        }
        item {
            Column {
                SectionTitle("۷ روز اخیر")
                Spacer(Modifier.height(10.dp))
                GlassCard {
                    WeekExpenseChart(transactions)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("تراکنش‌های اخیر")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onNavigate("txs") }) { Text("همه ←") }
            }
        }
        item {
            Crossfade(
                targetState = recent.isEmpty(),
                animationSpec = tween(300),
                label = "recentState"
            ) { empty ->
                if (empty) {
                    EmptyState(
                        emoji = "🧾",
                        title = "هنوز تراکنشی ثبت نشده",
                        subtitle = "برای شروع، دکمه افزودن را بزنید"
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        recent.forEach { (tx, cat, acc) ->
                            RecentTxRow(tx = tx, cat = cat, acc = acc, onOpen = { onNavigate("txs") })
                        }
                    }
                }
            }
        }
        item {
            TextButton(onClick = { onNavigate("recurring") }, modifier = Modifier.fillMaxWidth()) {
                Text("🔁 دوره‌ای", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MonthStatChip(
    label: String,
    amount: Double,
    tint: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(16.dp), color = tint.copy(alpha = 0.10f), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AmountText(
                    amount = amount,
                    withCurrency = false,
                    style = MaterialTheme.typography.titleSmall,
                    color = tint
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Mini bar chart of daily expenses over the last 7 days (RTL flow: today on the left). */
@Composable
private fun WeekExpenseChart(transactions: List<TransactionEntity>) {
    val jc = LocalJibetColors.current
    val days = remember(transactions) {
        val dates = Ranges.lastNDates(7)
        val sums = HashMap<LocalDate, Double>()
        dates.forEach { sums[it] = 0.0 }
        transactions.forEach { tx ->
            if (!tx.isIncome) {
                val d = Instant.ofEpochMilli(tx.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                if (sums.containsKey(d)) sums[d] = (sums[d] ?: 0.0) + tx.amount
            }
        }
        dates.map { it to (sums[it] ?: 0.0) }
    }
    val maxV = (days.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    val animatedMax by animateFloatAsState(
        targetValue = maxV.toFloat(),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "weekMax"
    )
    Column {
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val slot = size.width / days.size
            val barW = (slot * 0.42f).coerceIn(6.dp.toPx(), 20.dp.toPx())
            days.forEachIndexed { i, (date, v) ->
                if (v > 0.0 && animatedMax > 0f) {
                    val slotLeft = size.width - (i + 1) * slot
                    val cx = slotLeft + slot / 2f
                    val h = ((v.toFloat() / animatedMax) * size.height)
                        .coerceIn(4.dp.toPx(), size.height)
                    val color = if (i == days.lastIndex) jc.income else jc.expense.copy(alpha = 0.85f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(cx - barW / 2f, size.height - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEach { (date, _) ->
                Text(
                    text = Fmt.weekdayShort(date.dayOfWeek),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RecentTxRow(
    tx: TransactionEntity,
    cat: CategoryEntity?,
    acc: AccountEntity,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (cat != null) EmojiBadge(emoji = cat.emoji, colorHex = cat.colorHex)
        else EmojiBadge(emoji = "🏷", colorHex = "#6F7975")
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cat?.name ?: "بدون دسته",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = acc.name + " • " + (Fmt.relativeDay(tx.timestamp) ?: Fmt.date(tx.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (tx.note.isNotBlank()) {
                Text(
                    text = tx.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            AmountText(
                amount = tx.amount,
                isIncome = tx.isIncome,
                withCurrency = false,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = LocalCurrency.current,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
