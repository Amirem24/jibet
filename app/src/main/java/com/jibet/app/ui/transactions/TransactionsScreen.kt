package com.jibet.app.ui.transactions

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.util.Defaults
import com.jibet.app.util.Fmt
import com.jibet.app.util.Ranges
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class TypeFilter { ALL, EXPENSE, INCOME }
enum class PeriodFilter { ALL, TODAY, WEEK, MONTH }

/** One rendered row of the transaction list. */
data class TxRow(
    val tx: TransactionEntity,
    val categoryName: String?,
    val categoryEmoji: String,
    val categoryColor: String,
    val accountName: String
)

/**
 * Full transaction history with search + type / period / category / account filters,
 * grouped by local date with sticky headers.
 */
@Composable
fun TransactionsScreen(onBack: () -> Unit, onEditTx: (Long) -> Unit) {
    val vm: TransactionsViewModel = viewModel()
    val cs = MaterialTheme.colorScheme
    val jc = LocalJibetColors.current

    val query by vm.query.collectAsState()
    val type by vm.type.collectAsState()
    val period by vm.period.collectAsState()
    val categoryId by vm.categoryId.collectAsState()
    val accountId by vm.accountId.collectAsState()
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val filtered by vm.filtered.collectAsState()
    val totalCount by vm.totalCount.collectAsState()

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    // Group filtered transactions by local date (list is already DESC by timestamp).
    val groups = remember(filtered) {
        filtered
            .groupBy { Instant.ofEpochMilli(it.tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }
            .toList()
            .sortedByDescending { it.first }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text("تراکنش‌ها", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text(
                text = Fmt.faNum(filtered.size.toLong()) + " مورد",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }

        // 2. Search
        OutlinedTextField(
            value = query,
            onValueChange = { vm.query.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("جستجو در یادداشت‌ها…") }
        )

        // 3. Filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = type == TypeFilter.ALL,
                onClick = { vm.type.value = TypeFilter.ALL },
                label = { Text("همه") }
            )
            FilterChip(
                selected = type == TypeFilter.EXPENSE,
                onClick = { vm.type.value = TypeFilter.EXPENSE },
                label = { Text("هزینه") }
            )
            FilterChip(
                selected = type == TypeFilter.INCOME,
                onClick = { vm.type.value = TypeFilter.INCOME },
                label = { Text("درآمد") }
            )
            FilterChip(
                selected = period == PeriodFilter.ALL,
                onClick = { vm.period.value = PeriodFilter.ALL },
                label = { Text("همه") }
            )
            FilterChip(
                selected = period == PeriodFilter.TODAY,
                onClick = { vm.period.value = PeriodFilter.TODAY },
                label = { Text("امروز") }
            )
            FilterChip(
                selected = period == PeriodFilter.WEEK,
                onClick = { vm.period.value = PeriodFilter.WEEK },
                label = { Text("این هفته") }
            )
            FilterChip(
                selected = period == PeriodFilter.MONTH,
                onClick = { vm.period.value = PeriodFilter.MONTH },
                label = { Text("این ماه") }
            )
            Box {
                FilterChip(
                    selected = categoryId != null,
                    onClick = { showCategoryMenu = true },
                    label = {
                        val name = categories.firstOrNull { it.id == categoryId }?.name
                        Text(if (categoryId != null && name != null) "دسته: " + name else "دسته")
                    }
                )
                DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("بدون فیلتر") },
                        onClick = {
                            vm.categoryId.value = null
                            showCategoryMenu = false
                        }
                    )
                    categories.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.emoji + " " + c.name) },
                            onClick = {
                                vm.categoryId.value = c.id
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }
            Box {
                FilterChip(
                    selected = accountId != null,
                    onClick = { showAccountMenu = true },
                    label = {
                        val name = accounts.firstOrNull { it.id == accountId }?.name
                        Text(if (accountId != null && name != null) "حساب: " + name else "حساب")
                    }
                )
                DropdownMenu(expanded = showAccountMenu, onDismissRequest = { showAccountMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("بدون فیلتر") },
                        onClick = {
                            vm.accountId.value = null
                            showAccountMenu = false
                        }
                    )
                    accounts.forEach { a ->
                        DropdownMenuItem(
                            text = { Text(a.emoji + " " + a.name) },
                            onClick = {
                                vm.accountId.value = a.id
                                showAccountMenu = false
                            }
                        )
                    }
                }
            }
        }

        // 4. Result list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
        ) {
            if (totalCount == 0) {
                item {
                    EmptyState(
                        emoji = "🧾",
                        title = "هنوز تراکنشی ثبت نشده",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 72.dp)
                    )
                }
            } else if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🔍",
                        title = "تراکنشی پیدا نشد",
                        subtitle = "فیلترها را تغییر دهید",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 72.dp)
                    )
                }
            } else {
                groups.forEach { (day, rows) ->
                    stickyHeader(key = "h" + day.toEpochDay()) {
                        val net = rows.sumOf { if (it.tx.isIncome) it.tx.amount else -it.tx.amount }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cs.surface.copy(alpha = 0.92f))
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Fmt.dayLabel(rows.first().tx.timestamp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = Fmt.money(net),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (net >= 0) jc.income else jc.expense
                            )
                        }
                    }
                    items(rows.size, key = { rows[it].tx.id }) { index ->
                        val row = rows[index]
                        TxRowItem(row = row, onClick = { onEditTx(row.tx.id) })
                    }
                }
            }
        }
    }
}

/** Soft transaction row: category badge, names, amount. */
@Composable
private fun TxRowItem(row: TxRow, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiBadge(emoji = row.categoryEmoji, colorHex = row.categoryColor, size = 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.categoryName ?: "بدون دسته",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = buildString {
                append(row.accountName)
                if (row.tx.note.isNotBlank()) {
                    append(" • ")
                    append(row.tx.note)
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AmountText(
            amount = row.tx.amount,
            isIncome = row.tx.isIncome,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

/** ViewModel: combines repo flows with active filters into a filtered row list. */
class TransactionsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as JibetApp).repository

    val query = MutableStateFlow("")
    val type = MutableStateFlow(TypeFilter.ALL)
    val period = MutableStateFlow(PeriodFilter.ALL)
    val categoryId = MutableStateFlow<Long?>(null)
    val accountId = MutableStateFlow<Long?>(null)

    val categories: StateFlow<List<CategoryEntity>> = repo.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val accounts: StateFlow<List<AccountEntity>> = repo.accounts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val totalCount: StateFlow<Int> = repo.transactions
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private data class Filters(
        val query: String,
        val type: TypeFilter,
        val period: PeriodFilter,
        val categoryId: Long?,
        val accountId: Long?
    )

    private val filters: Flow<Filters> = combine(query, type, period, categoryId, accountId) { q, t, p, c, a ->
        Filters(q, t, p, c, a)
    }

    val filtered: StateFlow<List<TxRow>> = combine(
        repo.transactions, repo.categories, repo.accounts, filters
    ) { txs, cats, accs, f ->
        val catById = cats.associateBy { it.id }
        val accById = accs.associateBy { it.id }
        val range: Pair<Long, Long>? = when (f.period) {
            PeriodFilter.ALL -> null
            PeriodFilter.TODAY -> Ranges.today()
            PeriodFilter.WEEK -> Ranges.thisWeek()
            PeriodFilter.MONTH -> Ranges.thisMonth()
        }
        val from = range?.first ?: 0L
        val to = range?.second ?: Long.MAX_VALUE
        val q = f.query.trim()
        txs.filter { tx ->
            val cat = catById[tx.categoryId]
            val acc = accById[tx.accountId]
            val typeOk = when (f.type) {
                TypeFilter.ALL -> true
                TypeFilter.EXPENSE -> !tx.isIncome
                TypeFilter.INCOME -> tx.isIncome
            }
            val periodOk = tx.timestamp in from..to
            val categoryOk = f.categoryId == null || tx.categoryId == f.categoryId
            val accountOk = f.accountId == null || tx.accountId == f.accountId
            val queryOk = q.isEmpty() ||
                tx.note.contains(q, ignoreCase = true) ||
                (cat?.name ?: "").contains(q, ignoreCase = true) ||
                (acc?.name ?: "").contains(q, ignoreCase = true)
            typeOk && periodOk && categoryOk && accountOk && queryOk
        }.map { tx ->
            val cat = catById[tx.categoryId]
            val acc = accById[tx.accountId]
            TxRow(
                tx = tx,
                categoryName = cat?.name,
                categoryEmoji = cat?.emoji ?: "🧾",
                categoryColor = cat?.colorHex ?: Defaults.palette.first(),
                accountName = acc?.name ?: "—"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
