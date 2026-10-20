package com.jibet.app.ui.categories

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.CategoryEntity
import com.jibet.app.ui.components.ConfirmDialog
import com.jibet.app.ui.components.EmojiBadge
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.ProgressBarSoft
import com.jibet.app.ui.components.SectionTitle
import com.jibet.app.ui.components.parseHex
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.util.Defaults
import com.jibet.app.util.Fmt
import com.jibet.app.util.Ranges
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class CategoriesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as JibetApp).repository

    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** مجموع مصرف هر دسته در ماه جاری (شناسه دسته -> مبلغ) */
    var monthSums by mutableStateOf<Map<Long, Double>>(emptyMap())
        private set

    init {
        refreshMonthSums()
    }

    fun refreshMonthSums() {
        viewModelScope.launch {
            val (from, to) = Ranges.thisMonth()
            monthSums = repo.categorySums(from, to, isIncome = false)
                .associate { it.categoryId to it.total }
        }
    }

    fun add(name: String, emoji: String, colorHex: String, isIncome: Boolean, budget: Double) {
        viewModelScope.launch { repo.addCategory(name, emoji, colorHex, isIncome, budget) }
    }

    fun update(c: CategoryEntity) {
        viewModelScope.launch { repo.updateCategory(c) }
    }

    fun delete(c: CategoryEntity) {
        viewModelScope.launch { repo.deleteCategory(c) }
    }
}

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val vm: CategoriesViewModel = viewModel()
    val categories by vm.categories.collectAsState()
    val sums = vm.monthSums
    val jc = LocalJibetColors.current

    var showIncome by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "دسته‌بندی‌ها", onBack = onBack, onAdd = { showAdd = true })

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showIncome,
                onClick = { showIncome = false },
                label = { Text("هزینه‌ها") }
            )
            FilterChip(
                selected = showIncome,
                onClick = { showIncome = true },
                label = { Text("درآمدها") }
            )
        }

        val visible = categories.filter { it.isIncome == showIncome }
        if (visible.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState("🗂", "دسته‌ای نیست", "با دکمه + اضافه کنید")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(visible, key = { it.id }) { cat ->
                    CategoryRow(
                        cat = cat,
                        spent = sums[cat.id] ?: 0.0,
                        expenseColor = jc.expense,
                        onEdit = { editing = cat }
                    )
                }
            }
        }
    }

    if (showAdd) {
        CategoryDialog(
            initial = null,
            expense = !showIncome,
            onDismiss = { showAdd = false },
            onSave = { name, emoji, colorHex, budget ->
                vm.add(name, emoji, colorHex, !showIncome, budget)
                showAdd = false
            },
            onDelete = null
        )
    }

    editing?.let { cat ->
        CategoryDialog(
            initial = cat,
            expense = cat.isIncome,
            onDismiss = { editing = null },
            onSave = { name, emoji, colorHex, budget ->
                vm.update(cat.copy(name = name, emoji = emoji, colorHex = colorHex, budget = budget))
                editing = null
            },
            onDelete = {
                vm.delete(cat)
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
private fun CategoryRow(cat: CategoryEntity, spent: Double, expenseColor: Color, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cs.surface.copy(alpha = 0.6f),
        contentColor = cs.onSurface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiBadge(cat.emoji, cat.colorHex)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(cat.name, style = MaterialTheme.typography.titleMedium)
                if (!cat.isIncome && cat.budget > 0.0) {
                    Text(
                        "بودجه ماهانه: " + Fmt.money(cat.budget),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            if (!cat.isIncome && cat.budget > 0.0) {
                Column(
                    Modifier.width(100.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "مصرف: " + Fmt.money(spent),
                        style = MaterialTheme.typography.labelSmall,
                        color = expenseColor
                    )
                    Spacer(Modifier.height(4.dp))
                    val raw = if (cat.budget > 0) (spent / cat.budget).toFloat() else 0f
                    val progress by animateFloatAsState(
                        targetValue = raw.coerceIn(0f, 1f),
                        label = "budgetProgress"
                    )
                    ProgressBarSoft(progress, expenseColor, Modifier.width(90.dp))
                    if (spent >= cat.budget) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "بیش از بودجه!",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.error
                        )
                    }
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "ویرایش",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    initial: CategoryEntity?,
    expense: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, colorHex: String, budget: Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: Defaults.emojiPicker.first()) }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: Defaults.palette.first()) }
    var budgetText by remember {
        mutableStateOf(if (initial != null && initial.budget > 0.0) plainNumber(initial.budget) else "")
    }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "دسته جدید" else "ویرایش دسته") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دسته") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                SectionTitle("ایموجی")
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Defaults.emojiPicker) { e ->
                        EmojiOption(emoji = e, selected = e == emoji, onClick = { emoji = e })
                    }
                }
                SectionTitle("رنگ")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Defaults.palette.forEach { hex ->
                        ColorOption(hex = hex, selected = hex == colorHex, onClick = { colorHex = hex })
                    }
                }
                if (expense) {
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = filterNumeric(it) },
                        label = { Text("بودجه ماهانه (۰ = بدون بودجه)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (initial != null && onDelete != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("حذف دسته") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name.trim(), emoji, colorHex, parseNum(budgetText).coerceAtLeast(0.0))
                },
                enabled = name.isNotBlank()
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )

    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = "حذف دسته",
            text = "تراکنش‌های قبلی این دسته باقی می‌مانند.",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun EmojiOption(emoji: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) cs.primaryContainer else cs.surfaceVariant.copy(alpha = 0.55f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) cs.primary else cs.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 18.sp)
    }
}

@Composable
private fun ColorOption(hex: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(parseHex(hex))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) cs.onSurface else cs.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
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
