package com.jibet.app.ui.goals

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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.GoalEntity
import com.jibet.app.ui.components.AmountText
import com.jibet.app.ui.components.ConfirmDialog
import com.jibet.app.ui.components.EmojiBadge
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassCard
import com.jibet.app.ui.components.ProgressBarSoft
import com.jibet.app.ui.components.SectionTitle
import com.jibet.app.ui.components.parseHex
import com.jibet.app.util.Defaults
import com.jibet.app.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class GoalsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as JibetApp).repository

    val goals: StateFlow<List<GoalEntity>> =
        repo.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, target: Double, emoji: String, colorHex: String, deadline: Long?) {
        viewModelScope.launch { repo.addGoal(title, target, emoji, colorHex, deadline) }
    }

    fun update(g: GoalEntity) {
        viewModelScope.launch { repo.updateGoal(g) }
    }

    fun delete(g: GoalEntity) {
        viewModelScope.launch { repo.deleteGoal(g) }
    }

    fun contribute(id: Long, amount: Double) {
        viewModelScope.launch { repo.contributeGoal(id, amount) }
    }
}

@Composable
fun GoalsScreen(onBack: () -> Unit) {
    val vm: GoalsViewModel = viewModel()
    val goals by vm.goals.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GoalEntity?>(null) }
    var depositFor by remember { mutableStateOf<GoalEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "اهداف پس‌انداز", onBack = onBack, onAdd = { showAdd = true })

        if (goals.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState("🎯", "هدفی تعریف نشده", "مثلاً سفر، گوشی جدید، اضطراری…")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onDeposit = { depositFor = goal },
                        onEdit = { editing = goal },
                        onDelete = { vm.delete(goal) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        GoalDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { title, target, emoji, colorHex, deadline ->
                vm.add(title, target, emoji, colorHex, deadline)
                showAdd = false
            },
            onDelete = null
        )
    }

    editing?.let { goal ->
        GoalDialog(
            initial = goal,
            onDismiss = { editing = null },
            onSave = { title, target, emoji, colorHex, deadline ->
                vm.update(
                    goal.copy(
                        title = title,
                        target = target,
                        emoji = emoji,
                        colorHex = colorHex,
                        deadline = deadline
                    )
                )
                editing = null
            },
            onDelete = {
                vm.delete(goal)
                editing = null
            }
        )
    }

    depositFor?.let { goal ->
        DepositDialog(
            goal = goal,
            onDismiss = { depositFor = null },
            onSubmit = { amount ->
                vm.contribute(goal.id, amount)
                depositFor = null
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
private fun GoalCard(
    goal: GoalEntity,
    onDeposit: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(goal.emoji, goal.colorHex, size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (goal.done) {
                        Text(" ✅", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AmountText(
                        amount = goal.saved,
                        isIncome = true,
                        style = MaterialTheme.typography.titleSmall,
                        withCurrency = false
                    )
                    Text(
                        " از ",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant
                    )
                    AmountText(
                        amount = goal.target,
                        isIncome = null,
                        style = MaterialTheme.typography.titleSmall,
                        withCurrency = false
                    )
                }
                Spacer(Modifier.height(6.dp))
                val raw = if (goal.target > 0) (goal.saved / goal.target).toFloat() else 0f
                val progress by animateFloatAsState(
                    targetValue = raw.coerceIn(0f, 1f),
                    label = "goalProgress"
                )
                ProgressBarSoft(progress, parseHex(goal.colorHex), Modifier.fillMaxWidth())
                goal.deadline?.let { dl ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "تا " + Fmt.date(dl),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onDeposit,
                shape = RoundedCornerShape(12.dp),
                color = cs.primary.copy(alpha = 0.12f),
                contentColor = cs.primary
            ) {
                Text(
                    "افزودن پول +",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "ویرایش",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "حذف",
                    modifier = Modifier.size(18.dp),
                    tint = cs.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun DepositDialog(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onSubmit: (Double) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val amount = parseNum(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن به «${goal.title}»") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = filterNumeric(it, allowMinus = true) },
                    label = { Text("مبلغ") },
                    placeholder = { Text("مثبت واریز / منفی برداشت") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(amount); onDismiss() },
                enabled = amount != 0.0
            ) { Text("ثبت") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun GoalDialog(
    initial: GoalEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, target: Double, emoji: String, colorHex: String, deadline: Long?) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var targetText by remember {
        mutableStateOf(if (initial != null && initial.target > 0.0) plainNumber(initial.target) else "")
    }
    var emoji by remember { mutableStateOf(initial?.emoji ?: Defaults.goalEmojis.first()) }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: Defaults.palette.first()) }
    var deadline by remember { mutableStateOf(initial?.deadline) }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val target = parseNum(targetText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "هدف جدید" else "ویرایش هدف") },
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
                    label = { Text("عنوان هدف") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = filterNumeric(it) },
                    label = { Text("مبلغ هدف") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                SectionTitle("ایموجی")
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Defaults.goalEmojis) { e ->
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
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dl = deadline
                    Text(
                        if (dl == null) "بدون موعد" else Fmt.date(dl),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "انتخاب تاریخ")
                    }
                    if (dl != null) {
                        TextButton(onClick = { deadline = null }) { Text("حذف موعد") }
                    }
                }
                if (initial != null && onDelete != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("حذف هدف") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), target, emoji, colorHex, deadline) },
                enabled = title.isNotBlank() && target > 0.0
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )

    if (showPicker) {
        JibetDatePicker(
            initialTs = deadline,
            onPicked = { deadline = it },
            onDismiss = { showPicker = false }
        )
    }

    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = "حذف هدف",
            text = "پول جمع‌شده برای این هدف هم پاک می‌شود.",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun JibetDatePicker(initialTs: Long?, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialTs?.let { Fmt.tsToDatePicker(it) }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = state.selectedDateMillis
                    if (picked != null) {
                        onPicked(Fmt.datePickerToTs(picked, initialTs ?: System.currentTimeMillis()))
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
