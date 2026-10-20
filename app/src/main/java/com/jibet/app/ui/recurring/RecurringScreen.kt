package com.jibet.app.ui.recurring

import android.app.Application
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.AccountEntity
import com.jibet.app.data.CategoryEntity
import com.jibet.app.data.RecurFreq
import com.jibet.app.data.RecurringEntity
import com.jibet.app.ui.components.AmountText
import com.jibet.app.ui.components.ConfirmDialog
import com.jibet.app.ui.components.EmojiBadge
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.SectionTitle
import com.jibet.app.util.Defaults
import com.jibet.app.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class RecurringViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as JibetApp).repository

    val recurring: StateFlow<List<RecurringEntity>> =
        repo.recurring.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> =
        repo.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(
        title: String,
        amount: Double,
        isIncome: Boolean,
        categoryId: Long,
        accountId: Long,
        freq: RecurFreq,
        firstRun: Long
    ) {
        viewModelScope.launch {
            repo.addRecurring(title, amount, isIncome, categoryId, accountId, freq, firstRun)
        }
    }

    fun update(r: RecurringEntity) {
        viewModelScope.launch { repo.updateRecurring(r) }
    }

    fun delete(r: RecurringEntity) {
        viewModelScope.launch { repo.deleteRecurring(r) }
    }

    fun toggle(r: RecurringEntity, active: Boolean) {
        viewModelScope.launch { repo.toggleRecurring(r, active) }
    }
}

@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val vm: RecurringViewModel = viewModel()
    val recurring by vm.recurring.collectAsState()
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "تراکنش‌های دوره‌ای", onBack = onBack, onAdd = { showAdd = true })

        Text(
            "سررسیدشده‌ها هنگام باز کردن برنامه خودکار ثبت می‌شوند",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        if (recurring.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState("🔁", "دوره‌ای ندارید", "اجاره، قسط، حقوق…")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recurring, key = { it.id }) { r ->
                    RecurringRow(
                        r = r,
                        categories = categories,
                        onToggle = { vm.toggle(r, it) },
                        onEdit = { editing = r },
                        onDelete = { vm.delete(r) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        RecurringDialog(
            initial = null,
            categories = categories,
            accounts = accounts,
            onDismiss = { showAdd = false },
            onSave = { title, amount, isIncome, catId, accId, freq, firstRun ->
                vm.add(title, amount, isIncome, catId, accId, freq, firstRun)
                showAdd = false
            },
            onDelete = null
        )
    }

    editing?.let { r ->
        RecurringDialog(
            initial = r,
            categories = categories,
            accounts = accounts,
            onDismiss = { editing = null },
            onSave = { title, amount, isIncome, catId, accId, freq, firstRun ->
                vm.update(
                    r.copy(
                        title = title,
                        amount = amount,
                        isIncome = isIncome,
                        categoryId = catId,
                        accountId = accId,
                        freq = freq.name,
                        nextRun = firstRun
                    )
                )
                editing = null
            },
            onDelete = {
                vm.delete(r)
                editing = null
            }
        )
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = "افزودن")
        }
    }
}

@Composable
private fun RecurringRow(
    r: RecurringEntity,
    categories: List<CategoryEntity>,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val cat = categories.firstOrNull { it.id == r.categoryId }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cs.surface.copy(alpha = 0.6f),
        contentColor = cs.onSurface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cat != null) {
                    EmojiBadge(cat.emoji, cat.colorHex)
                } else {
                    EmojiBadge("🔁", Defaults.palette[0])
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            r.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = cs.secondaryContainer.copy(alpha = 0.5f),
                            contentColor = cs.onSecondaryContainer
                        ) {
                            Text(
                                Defaults.freqLabel(r.freq),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "سررسید بعدی: " + Fmt.date(r.nextRun),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    AmountText(
                        amount = r.amount,
                        isIncome = r.isIncome,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Switch(
                        checked = r.active,
                        onCheckedChange = onToggle,
                        modifier = Modifier.scale(0.8f)
                    )
                    Text(
                        if (r.active) "فعال" else "غیرفعال",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "ویرایش",
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "حذف",
                        modifier = Modifier.size(17.dp),
                        tint = cs.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringDialog(
    initial: RecurringEntity?,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        isIncome: Boolean,
        categoryId: Long,
        accountId: Long,
        freq: RecurFreq,
        firstRun: Long
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amountText by remember {
        mutableStateOf(if (initial != null && initial.amount > 0.0) plainNumber(initial.amount) else "")
    }
    var income by remember { mutableStateOf(initial?.isIncome ?: false) }
    var catId by remember { mutableStateOf(initial?.categoryId ?: -1L) }
    var accId by remember { mutableStateOf(initial?.accountId ?: -1L) }
    var freq by remember {
        mutableStateOf(
            if (initial != null) {
                try {
                    RecurFreq.valueOf(initial.freq)
                } catch (e: Exception) {
                    RecurFreq.MONTHLY
                }
            } else {
                RecurFreq.MONTHLY
            }
        )
    }
    var firstRun by remember { mutableStateOf(initial?.nextRun ?: System.currentTimeMillis()) }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val amount = parseNum(amountText)
    val cats = categories.filter { it.isIncome == income }
    // انتخاب خودکار اولین دسته/حساب وقتی انتخاب فعلی معتبر نیست
    val effectiveCatId = if (cats.any { it.id == catId }) catId else cats.firstOrNull()?.id ?: -1L
    val effectiveAccId = if (accounts.any { it.id == accId }) accId else accounts.firstOrNull()?.id ?: -1L
    val valid = title.isNotBlank() && amount > 0.0 && effectiveCatId > 0L && effectiveAccId > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "دوره‌ای جدید" else "ویرایش دوره‌ای") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان مثلاً اجاره خانه") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = filterNumeric(it) },
                    label = { Text("مبلغ") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !income,
                        onClick = { income = false },
                        label = { Text("هزینه") }
                    )
                    FilterChip(
                        selected = income,
                        onClick = { income = true },
                        label = { Text("درآمد") }
                    )
                }
                SectionTitle("دسته‌بندی")
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cats, key = { it.id }) { c ->
                        FilterChip(
                            selected = effectiveCatId == c.id,
                            onClick = { catId = c.id },
                            label = { Text(c.emoji + " " + c.name) }
                        )
                    }
                }
                SectionTitle("حساب")
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts, key = { it.id }) { a ->
                        FilterChip(
                            selected = effectiveAccId == a.id,
                            onClick = { accId = a.id },
                            label = { Text(a.emoji + " " + a.name) }
                        )
                    }
                }
                SectionTitle("تکرار")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = freq == RecurFreq.DAILY,
                        onClick = { freq = RecurFreq.DAILY },
                        label = { Text("روزانه") }
                    )
                    FilterChip(
                        selected = freq == RecurFreq.WEEKLY,
                        onClick = { freq = RecurFreq.WEEKLY },
                        label = { Text("هفتگی") }
                    )
                    FilterChip(
                        selected = freq == RecurFreq.MONTHLY,
                        onClick = { freq = RecurFreq.MONTHLY },
                        label = { Text("ماهانه") }
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "اولین سررسید: " + Fmt.date(firstRun),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "انتخاب تاریخ")
                    }
                }
                if (initial != null && onDelete != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("حذف") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title.trim(), amount, income, effectiveCatId, effectiveAccId, freq, firstRun)
                },
                enabled = valid
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )

    if (showPicker) {
        JibetDatePicker(
            initialTs = firstRun,
            onPicked = { firstRun = it },
            onDismiss = { showPicker = false }
        )
    }

    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = "حذف دوره‌ای",
            text = "این تراکنش دوره‌ای حذف می‌شود.",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun JibetDatePicker(initialTs: Long, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = Fmt.tsToDatePicker(initialTs)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = state.selectedDateMillis
                    if (picked != null) {
                        onPicked(Fmt.datePickerToTs(picked, initialTs))
                    }
                    onDismiss()
                }
            ) { Text("تأیید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    ) {
        DatePicker(state = state)
    }
}

private fun plainNumber(v: Double): String {
    val r = v.roundToLong()
    return if (r.toDouble() == v) r.toString() else v.toString()
}

private fun toLatinDigits(s: String): String = buildString {
    for (c in s) {
        append(
            when (c) {
                in '۰'..'۹' -> '0' + (c - '۰')
                in '٠'..'٩' -> '0' + (c - '٠')
                else -> c
            }
        )
    }
}

private fun parseNum(s: String): Double =
    toLatinDigits(s).replace(",", "").replace("٬", "").trim().toDoubleOrNull() ?: 0.0

private fun filterNumeric(input: String, allowMinus: Boolean = false): String {
    var minusSeen = false
    return buildString {
        for (c in input) {
            when {
                c.isDigit() -> append(c)
                allowMinus && c == '-' && !minusSeen && isEmpty() -> {
                    append(c)
                    minusSeen = true
                }
            }
        }
    }
}
