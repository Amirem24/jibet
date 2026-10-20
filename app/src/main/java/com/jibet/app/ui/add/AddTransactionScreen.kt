package com.jibet.app.ui.add

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.AccountEntity
import com.jibet.app.data.CategoryEntity
import com.jibet.app.data.TransactionEntity
import com.jibet.app.ui.components.ConfirmDialog
import com.jibet.app.ui.components.EmojiBadge
import com.jibet.app.ui.components.GlassCard
import com.jibet.app.ui.components.LocalCurrency
import com.jibet.app.ui.components.SectionTitle
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Add / edit transaction screen.
 * txId >= 0 -> edit mode (prefills from repo); txId == -1L -> new transaction.
 */
@Composable
fun AddTransactionScreen(txId: Long, onDone: () -> Unit) {
    val vm: AddViewModel = viewModel()
    val cs = MaterialTheme.colorScheme
    val jc = LocalJibetColors.current
    val currency = LocalCurrency.current

    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val amountDigits by vm.amountDigits.collectAsState()
    val isIncome by vm.isIncome.collectAsState()
    val selectedCategoryId by vm.selectedCategoryId.collectAsState()
    val selectedAccountId by vm.selectedAccountId.collectAsState()
    val note by vm.note.collectAsState()
    val ts by vm.ts.collectAsState()
    val saving by vm.saving.collectAsState()

    val editing = txId >= 0L
    val amountValue = (amountDigits.toLongOrNull() ?: 0L).toDouble()
    val valid = amountValue > 0.0 && selectedCategoryId != null && selectedAccountId != null

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showInvalid by remember { mutableStateOf(false) }

    // Auto-select first matching category when the list/type changes (keeps preloaded edit choice).
    val visibleCategories = remember(categories, isIncome) { categories.filter { it.isIncome == isIncome } }
    LaunchedEffect(visibleCategories) {
        if (visibleCategories.isNotEmpty() && visibleCategories.none { it.id == selectedCategoryId }) {
            vm.selectCategory(visibleCategories.first().id)
        }
    }
    // Auto-select first account; keeps the loaded tx's account in edit mode.
    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && accounts.none { it.id == selectedAccountId }) {
            vm.selectAccount(accounts.first().id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = if (editing) "ویرایش تراکنش" else "تراکنش جدید",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.weight(1f))
            if (editing) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "حذف",
                        tint = cs.error
                    )
                }
            }
        }

        // 2. Type toggle
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeChip(
                label = "هزینه",
                selected = !isIncome,
                color = jc.expense,
                onClick = { vm.setType(false) }
            )
            TypeChip(
                label = "درآمد",
                selected = isIncome,
                color = jc.income,
                onClick = { vm.setType(true) }
            )
        }

        // 3. Amount display
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Text(
                text = Fmt.money(amountValue),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = currency,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // 4. Numeric keypad (classic calculator order; Persian digits, Latin digits tracked internally)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9')
                ).forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowKeys.forEach { d ->
                            KeypadButton(label = Fmt.fa(d.toString()), onPress = { vm.appendDigit(d) })
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KeypadButton(label = "۰۰۰", onPress = { vm.appendThousand() })
                    KeypadButton(label = Fmt.fa("0"), onPress = { vm.appendDigit('0') })
                    KeypadIconButton(icon = Icons.AutoMirrored.Filled.Backspace, desc = "پاک کردن") {
                        vm.backspace()
                    }
                }
            }
        }

        // 5. Category
        SectionTitle("دسته")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visibleCategories, key = { it.id }) { cat ->
                CategoryChip(
                    cat = cat,
                    selected = cat.id == selectedCategoryId,
                    onClick = { vm.selectCategory(cat.id) }
                )
            }
        }

        // 6. Account
        SectionTitle("حساب")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts, key = { it.id }) { acc ->
                AccountChip(
                    acc = acc,
                    selected = acc.id == selectedAccountId,
                    onClick = { vm.selectAccount(acc.id) }
                )
            }
        }

        // 7. Note + date
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = note,
                onValueChange = { vm.setNote(it) },
                label = { Text("یادداشت (اختیاری)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "انتخاب تاریخ")
                }
                Text(
                    text = Fmt.date(ts),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant
                )
            }
        }

        // 8. Save
        Button(
            onClick = {
                if (valid) {
                    showInvalid = false
                    vm.save(onDone)
                } else {
                    showInvalid = true
                }
            },
            enabled = !saving,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (editing) "ذخیره تغییرات" else "ثبت تراکنش")
        }
        AnimatedVisibility(visible = showInvalid && !valid) {
            Text(
                text = "مبلغ یا دسته نامعتبر است",
                color = cs.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "حذف تراکنش",
            text = "این تراکنش حذف شود؟ موجودی حساب اصلاح می‌شود.",
            onConfirm = { vm.delete(onDone) },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = Fmt.tsToDatePicker(ts))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setDate(state.selectedDateMillis?.let { Fmt.datePickerToTs(it, ts) } ?: ts)
                    showDatePicker = false
                }) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("انصراف") }
            }
        ) {
            DatePicker(state)
        }
    }
}

/** Soft big type chip (expense / income). */
@Composable
private fun RowScope.TypeChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) color.copy(alpha = 0.15f) else cs.surfaceVariant.copy(alpha = 0.4f))
            .border(1.5.dp, if (selected) color else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) color else cs.onSurfaceVariant
        )
    }
}

/** Category chip with emoji badge. */
@Composable
private fun CategoryChip(cat: CategoryEntity, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) cs.primaryContainer.copy(alpha = 0.5f) else cs.surfaceVariant.copy(alpha = 0.4f))
            .border(1.5.dp, if (selected) cs.primary else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiBadge(emoji = cat.emoji, colorHex = cat.colorHex, size = 28.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = cat.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) cs.onPrimaryContainer else cs.onSurface
        )
    }
}

/** Account chip with emoji badge + balance line. */
@Composable
private fun AccountChip(acc: AccountEntity, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) cs.primaryContainer.copy(alpha = 0.5f) else cs.surfaceVariant.copy(alpha = 0.4f))
            .border(1.5.dp, if (selected) cs.primary else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(emoji = acc.emoji, colorHex = acc.colorHex, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = acc.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) cs.onPrimaryContainer else cs.onSurface
            )
        }
        Text(
            text = Fmt.money(acc.balance),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant
        )
    }
}

/** Numeric keypad digit key. */
@Composable
private fun RowScope.KeypadButton(label: String, onPress: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPress()
        },
        modifier = Modifier
            .weight(1f)
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = cs.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = cs.onSurface
            )
        }
    }
}

/** Numeric keypad icon key (backspace). */
@Composable
private fun RowScope.KeypadIconButton(icon: ImageVector, desc: String, onPress: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPress()
        },
        modifier = Modifier
            .weight(1f)
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = cs.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/** ViewModel for the add/edit transaction screen. */
class AddViewModel(app: Application, private val state: SavedStateHandle) : AndroidViewModel(app) {

    private val repo = (app as JibetApp).repository

    // Route arg (query param "txId" of the "add?txId={txId}" destination); -1L = new transaction.
    private val txId: Long = ((state.get<Any>("txId") as? Number)?.toLong()) ?: -1L

    val categories: StateFlow<List<CategoryEntity>> = repo.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val accounts: StateFlow<List<AccountEntity>> = repo.accounts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _amountDigits = MutableStateFlow("")
    val amountDigits: StateFlow<String> = _amountDigits.asStateFlow()
    private val _isIncome = MutableStateFlow(false)
    val isIncome: StateFlow<Boolean> = _isIncome.asStateFlow()
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()
    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId.asStateFlow()
    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()
    private val _ts = MutableStateFlow(System.currentTimeMillis())
    val ts: StateFlow<Long> = _ts.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private var editingTx: TransactionEntity? = null

    init {
        if (txId >= 0L) {
            viewModelScope.launch {
                val t = repo.getTransaction(txId)
                if (t != null) {
                    editingTx = t
                    _amountDigits.value = t.amount.toLong().toString()
                    _isIncome.value = t.isIncome
                    _selectedCategoryId.value = t.categoryId
                    _selectedAccountId.value = t.accountId
                    _note.value = t.note
                    _ts.value = t.timestamp
                }
            }
        }
    }

    fun appendDigit(d: Char) {
        val cur = _amountDigits.value
        if (cur.isEmpty() || cur == "0") {
            if (d != '0') _amountDigits.value = d.toString()
        } else if (cur.length < MAX_DIGITS) {
            _amountDigits.value = cur + d
        }
    }

    fun appendThousand() {
        val cur = _amountDigits.value
        if (cur.isEmpty() || cur == "0" || cur.length + 3 > MAX_DIGITS) return
        _amountDigits.value = cur + "000"
    }

    fun backspace() {
        val cur = _amountDigits.value
        if (cur.isNotEmpty()) _amountDigits.value = cur.dropLast(1)
    }

    fun setType(income: Boolean) {
        _isIncome.value = income
    }

    fun selectCategory(id: Long) {
        _selectedCategoryId.value = id
    }

    fun selectAccount(id: Long) {
        _selectedAccountId.value = id
    }

    fun setNote(value: String) {
        _note.value = value
    }

    fun setDate(newTs: Long) {
        _ts.value = newTs
    }

    fun save(onDone: () -> Unit) {
        if (_saving.value) return
        val amount = amountValue()
        val catId = _selectedCategoryId.value ?: return
        val accId = _selectedAccountId.value ?: return
        if (amount <= 0.0) return
        viewModelScope.launch {
            _saving.value = true
            try {
                val e = editingTx
                if (e != null) {
                    repo.updateTransaction(
                        e.copy(
                            amount = amount,
                            isIncome = _isIncome.value,
                            categoryId = catId,
                            accountId = accId,
                            note = _note.value.trim(),
                            timestamp = _ts.value
                        )
                    )
                } else {
                    repo.addTransaction(
                        amount = amount,
                        isIncome = _isIncome.value,
                        categoryId = catId,
                        accountId = accId,
                        note = _note.value.trim(),
                        timestamp = _ts.value
                    )
                }
            } finally {
                _saving.value = false
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val e = editingTx
        if (e == null) {
            onDone()
            return
        }
        viewModelScope.launch {
            repo.deleteTransaction(e)
            onDone()
        }
    }

    private fun amountValue(): Double = (_amountDigits.value.toLongOrNull() ?: 0L).toDouble()

    private companion object {
        const val MAX_DIGITS = 12
    }
}
