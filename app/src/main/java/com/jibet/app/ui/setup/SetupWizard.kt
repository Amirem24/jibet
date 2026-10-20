@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jibet.app.ui.setup

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.ui.components.SectionTitle
import com.jibet.app.util.Defaults
import kotlinx.coroutines.launch

/** Local draft of one account row in the wizard (page 2). */
data class DraftAccount(val emoji: String, val name: String, val balance: String)

/** Locally-added category chip in the wizard (page 3). */
data class CustomCat(val name: String, val emoji: String, val colorHex: String, val isIncome: Boolean)

class WizardViewModel(app: Application) : AndroidViewModel(app) {

    var saving by mutableStateOf(false)
        private set

    /**
     * Persists the wizard result: selected default categories, custom categories
     * and account drafts, then marks setup as done.
     */
    fun finish(
        accounts: List<DraftAccount>,
        expenses: List<Triple<String, String, String>>,
        incomes: List<Triple<String, String, String>>,
        customs: List<CustomCat>,
        onDone: () -> Unit
    ) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            val appCtx = getApplication() as JibetApp
            expenses.forEach { (name, emoji, hex) ->
                appCtx.repository.addCategory(name, emoji, hex, false)
            }
            incomes.forEach { (name, emoji, hex) ->
                appCtx.repository.addCategory(name, emoji, hex, true)
            }
            customs.forEach { c ->
                appCtx.repository.addCategory(c.name, c.emoji, c.colorHex, c.isIncome)
            }
            accounts.forEachIndexed { index, a ->
                val name = a.name.trim()
                if (name.isNotEmpty()) {
                    val hex = Defaults.accountSuggestions
                        .firstOrNull { it.name == name && it.emoji == a.emoji }
                        ?.colorHex
                        ?: Defaults.palette[index % Defaults.palette.size]
                    appCtx.repository.addAccount(name, a.emoji, hex, a.balance.parseBalance())
                }
            }
            appCtx.settings.setSetupDone(true)
            saving = false
            onDone()
        }
    }
}

/** Accepts latin AND persian digits / separators; falls back to 0.0. */
private fun String.parseBalance(): Double {
    val normalized = buildString {
        for (ch in this@parseBalance) {
            when (ch) {
                '۰' -> append('0')
                '۱' -> append('1')
                '۲' -> append('2')
                '۳' -> append('3')
                '۴' -> append('4')
                '۵' -> append('5')
                '۶' -> append('6')
                '۷' -> append('7')
                '۸' -> append('8')
                '۹' -> append('9')
                '٫', ',' -> append('.')
                else -> append(ch)
            }
        }
    }
    return normalized.toDoubleOrNull() ?: 0.0
}

@Composable
fun SetupWizard(onFinished: () -> Unit) {
    val vm: WizardViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    val accounts = remember { mutableStateListOf(DraftAccount("💵", "نقدی", "0")) }
    var selectedExpenses by remember {
        mutableStateOf(Defaults.expenseCategories.map { it.name }.toSet())
    }
    var selectedIncomes by remember {
        mutableStateOf(Defaults.incomeCategories.map { it.name }.toSet())
    }
    val customCats = remember { mutableStateListOf<CustomCat>() }
    var customIncome by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var pickingEmojiIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { i -> Dot(selected = pagerState.currentPage == i) }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> WelcomePage(
                    onStart = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
                1 -> AccountsPage(
                    accounts = accounts,
                    onPickEmoji = { pickingEmojiIndex = it },
                    onNext = { scope.launch { pagerState.animateScrollToPage(2) } }
                )
                else -> CategoriesPage(
                    selectedExpenses = selectedExpenses,
                    onToggleExpense = { name ->
                        selectedExpenses =
                            if (name in selectedExpenses) selectedExpenses - name
                            else selectedExpenses + name
                    },
                    selectedIncomes = selectedIncomes,
                    onToggleIncome = { name ->
                        selectedIncomes =
                            if (name in selectedIncomes) selectedIncomes - name
                            else selectedIncomes + name
                    },
                    customCats = customCats,
                    onRemoveCustom = { customCats.remove(it) },
                    customName = customName,
                    onCustomName = { customName = it },
                    customIncome = customIncome,
                    onCustomIncome = { customIncome = it },
                    onAddCustom = {
                        val name = customName.trim()
                        if (name.isNotEmpty()) {
                            customCats.add(
                                CustomCat(
                                    name = name,
                                    emoji = "🏷️",
                                    colorHex = Defaults.palette[customCats.size % Defaults.palette.size],
                                    isIncome = customIncome
                                )
                            )
                            customName = ""
                        }
                    },
                    saving = vm.saving,
                    onFinish = {
                        val expenses = Defaults.expenseCategories
                            .filter { it.name in selectedExpenses }
                            .map { Triple(it.name, it.emoji, it.colorHex) }
                        val incomes = Defaults.incomeCategories
                            .filter { it.name in selectedIncomes }
                            .map { Triple(it.name, it.emoji, it.colorHex) }
                        vm.finish(
                            accounts = accounts.toList(),
                            expenses = expenses,
                            incomes = incomes,
                            customs = customCats.toList(),
                            onDone = onFinished
                        )
                    }
                )
            }
        }
    }

    pickingEmojiIndex?.let { idx ->
        if (idx < accounts.size) {
            EmojiPickerDialog(
                onPick = { e ->
                    accounts[idx] = accounts[idx].copy(emoji = e)
                    pickingEmojiIndex = null
                },
                onDismiss = { pickingEmojiIndex = null }
            )
        }
    }
}

// ---------- Page 1: welcome ----------

@Composable
private fun WelcomePage(onStart: () -> Unit) {
    val features = listOf(
        "⚡" to "ثبت سریع با کیبورد اختصاصی",
        "🎯" to "اهداف پس‌انداز",
        "📊" to "نمودارهای ساده روز/هفته/ماه",
        "🔒" to "همه‌چیز روی گوشی خودت"
    )
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text("💰", fontSize = 72.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            "به جیبت خوش آمدید",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "هزینه‌هایت را ساده و سریع مدیریت کن — کاملاً آفلاین و خصوصی.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(30.dp))
        features.forEach { (emoji, text) ->
            FeatureRow(emoji, text)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("شروع", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun FeatureRow(emoji: String, text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ---------- Page 2: accounts ----------

@Composable
private fun AccountsPage(
    accounts: SnapshotStateList<DraftAccount>,
    onPickEmoji: (Int) -> Unit,
    onNext: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            "حساب‌های اولیه",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "مثلاً نقدی، کارت بانکی، کیف پول…",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            accounts.forEachIndexed { index, acc ->
                AccountDraftRow(
                    account = acc,
                    canDelete = accounts.size > 1,
                    onEmoji = { onPickEmoji(index) },
                    onName = { accounts[index] = accounts[index].copy(name = it) },
                    onBalance = { accounts[index] = accounts[index].copy(balance = it) },
                    onDelete = { accounts.removeAt(index) }
                )
            }

            Spacer(Modifier.height(4.dp))
            SectionTitle("پیشنهاد سریع")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Defaults.accountSuggestions.forEach { s ->
                    val added = accounts.any { it.name.trim() == s.name }
                    Surface(
                        onClick = { if (!added) accounts.add(DraftAccount(s.emoji, s.name, "0")) },
                        enabled = !added,
                        shape = RoundedCornerShape(14.dp),
                        color = if (added) cs.surfaceVariant.copy(alpha = 0.4f) else cs.primaryContainer
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(s.emoji, fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                s.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (added) cs.onSurfaceVariant else cs.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Button(
            onClick = onNext,
            enabled = accounts.any { it.name.isNotBlank() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("ادامه", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AccountDraftRow(
    account: DraftAccount,
    canDelete: Boolean,
    onEmoji: () -> Unit,
    onName: (String) -> Unit,
    onBalance: (String) -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onEmoji,
                    shape = RoundedCornerShape(14.dp),
                    color = cs.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f))
                ) {
                    Text(
                        account.emoji,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = account.name,
                    onValueChange = onName,
                    label = { Text("نام حساب") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "حذف حساب",
                            tint = cs.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = account.balance,
                onValueChange = onBalance,
                label = { Text("موجودی اولیه (۰ هم باشد)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------- Page 3: categories ----------

@Composable
private fun CategoriesPage(
    selectedExpenses: Set<String>,
    onToggleExpense: (String) -> Unit,
    selectedIncomes: Set<String>,
    onToggleIncome: (String) -> Unit,
    customCats: List<CustomCat>,
    onRemoveCustom: (CustomCat) -> Unit,
    customName: String,
    onCustomName: (String) -> Unit,
    customIncome: Boolean,
    onCustomIncome: (Boolean) -> Unit,
    onAddCustom: () -> Unit,
    saving: Boolean,
    onFinish: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            "دسته‌بندی‌های پیش‌فرض",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "هر کدام را نخواهی خاموشش کن؛ بعداً هم می‌تونی اضافه کنی.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("هزینه‌ها")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Defaults.expenseCategories.forEach { c ->
                    ToggleChip(c.emoji, c.name, c.name in selectedExpenses) {
                        onToggleExpense(c.name)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("درآمدها")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Defaults.incomeCategories.forEach { c ->
                    ToggleChip(c.emoji, c.name, c.name in selectedIncomes) {
                        onToggleIncome(c.name)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("دسته‌های جدید")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip("💸", "هزینه", !customIncome) { onCustomIncome(false) }
                ToggleChip("💰", "درآمد", customIncome) { onCustomIncome(true) }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = onCustomName,
                    label = { Text("دسته جدید") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onAddCustom, enabled = customName.isNotBlank()) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "افزودن دسته",
                        tint = if (customName.isNotBlank()) cs.primary else cs.onSurfaceVariant
                    )
                }
            }
            if (customCats.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customCats.forEach { c ->
                        CustomCatChip(c) { onRemoveCustom(c) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = onFinish,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = cs.onPrimary
                )
            } else {
                Text("پایان و شروع ✨", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ToggleChip(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (selected) cs.primaryContainer else cs.surfaceVariant.copy(alpha = 0.4f),
        label = "chipBg"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, if (selected) cs.primary else Color.Transparent)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 15.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomCatChip(c: CustomCat, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${c.emoji} ${c.name}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "حذف " + c.name,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ---------- Shared bits ----------

@Composable
private fun Dot(selected: Boolean) {
    val cs = MaterialTheme.colorScheme
    val dotSize by animateDpAsState(
        targetValue = if (selected) 10.dp else 7.dp,
        label = "dotSize"
    )
    val color by animateColorAsState(
        targetValue = if (selected) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.35f),
        label = "dotColor"
    )
    Box(
        Modifier
            .padding(horizontal = 3.dp)
            .size(dotSize)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun EmojiPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ایموجی") },
        text = {
            FlowRow(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Defaults.emojiPicker.forEach { e ->
                    Surface(
                        onClick = { onPick(e) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(e, fontSize = 22.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}
