package com.jibet.app.ui.accounts

import android.app.Application
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.AccountEntity
import com.jibet.app.ui.components.AmountText
import com.jibet.app.ui.components.ConfirmDialog
import com.jibet.app.ui.components.EmojiBadge
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassCard
import com.jibet.app.ui.components.SectionTitle
import com.jibet.app.ui.components.parseHex
import com.jibet.app.util.Defaults
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class AccountsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as JibetApp).repository

    val accounts: StateFlow<List<AccountEntity>> =
        repo.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> =
        repo.totalBalance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun add(name: String, emoji: String, colorHex: String, balance: Double) {
        viewModelScope.launch { repo.addAccount(name, emoji, colorHex, balance) }
    }

    fun update(a: AccountEntity) {
        viewModelScope.launch { repo.updateAccount(a) }
    }

    fun delete(a: AccountEntity) {
        viewModelScope.launch { repo.deleteAccount(a) }
    }
}

@Composable
fun AccountsScreen(onBack: () -> Unit) {
    val vm: AccountsViewModel = viewModel()
    val accounts by vm.accounts.collectAsState()
    val total by vm.totalBalance.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AccountEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "حساب‌ها", onBack = onBack, onAdd = { showAdd = true })

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "total") {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        "مجموع موجودی",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AmountText(
                        amount = total,
                        isIncome = null,
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (accounts.isEmpty()) {
                item(key = "empty") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 110.dp)
                    ) {
                        EmptyState("💳", "حسابی ثبت نشده")
                    }
                }
            }
            items(accounts, key = { it.id }) { acc ->
                AccountRow(acc = acc, onEdit = { editing = acc })
            }
        }
    }

    if (showAdd) {
        AccountDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { name, emoji, colorHex, balance ->
                vm.add(name, emoji, colorHex, balance)
                showAdd = false
            },
            onDelete = null
        )
    }

    editing?.let { acc ->
        AccountDialog(
            initial = acc,
            onDismiss = { editing = null },
            onSave = { name, emoji, colorHex, balance ->
                vm.update(acc.copy(name = name, emoji = emoji, colorHex = colorHex, balance = balance))
                editing = null
            },
            onDelete = {
                vm.delete(acc)
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
private fun AccountRow(acc: AccountEntity, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cs.surface.copy(alpha = 0.6f),
        contentColor = cs.onSurface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiBadge(acc.emoji, acc.colorHex)
            Spacer(Modifier.width(10.dp))
            Text(
                acc.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            AmountText(
                amount = acc.balance,
                isIncome = null,
                style = MaterialTheme.typography.titleMedium
            )
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
private fun AccountDialog(
    initial: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, colorHex: String, balance: Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: Defaults.emojiPicker.first()) }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: Defaults.palette.first()) }
    var balanceText by remember {
        mutableStateOf(if (initial != null) plainNumber(initial.balance) else "")
    }
    var confirmDelete by remember { mutableStateOf(false) }

    val hint: (@Composable () -> Unit)? = if (initial != null) {
        { Text("در حالت ویرایش، موجودی مستقیماً تنظیم می‌شود") }
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "حساب جدید" else "ویرایش حساب") },
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
                    label = { Text("نام حساب") },
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
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = filterNumeric(it, allowMinus = true) },
                    label = { Text("موجودی") },
                    supportingText = hint,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (initial != null && onDelete != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("حذف حساب") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), emoji, colorHex, parseNum(balanceText)) },
                enabled = name.isNotBlank()
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )

    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = "حذف حساب",
            text = "همه تراکنش‌های این حساب هم حذف می‌شوند!",
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
