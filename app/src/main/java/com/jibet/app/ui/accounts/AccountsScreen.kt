@file:OptIn(ExperimentalLayoutApi::class)

package com.jibet.app.ui.accounts

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.AccountEntity
import com.jibet.app.data.AccountKind
import com.jibet.app.data.AccountRow
import com.jibet.app.ui.components.AccentButton
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassIconButton
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.JibetField
import com.jibet.app.ui.components.JibetSheet
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.SectionLabel
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.jibetInputShape
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.Expense
import com.jibet.app.ui.theme.Faint
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.InkSoft
import com.jibet.app.ui.theme.JibetColors
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.TextMid
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.ACCOUNT_ICONS
import com.jibet.app.util.ACCOUNT_KIND_LABELS
import com.jibet.app.util.CATEGORY_COLORS
import com.jibet.app.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/* ================= ViewModel ================= */

class AccountsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as JibetApp).repository

    val accounts: StateFlow<List<AccountRow>> =
        repo.watchAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var sheetOpen by mutableStateOf(false)
        private set
    var editing by mutableStateOf<AccountRow?>(null)
        private set
    var name by mutableStateOf("")
        private set
    var kind by mutableStateOf(AccountKind.CASH)
        private set
    /** متن نمایشی فرمت‌شده‌ی موجودی (مثل ورودی فرمت‌دار وب) */
    var balanceDisplay by mutableStateOf("")
        private set
    var color by mutableStateOf(CATEGORY_COLORS[1])
        private set
    var icon by mutableStateOf("wallet")
        private set
    var saving by mutableStateOf(false)
        private set
    var confirmDelete by mutableStateOf(false)
        private set

    fun setName(v: String) {
        if (v.length <= 40) name = v
    }

    fun setKind(v: AccountKind) {
        kind = v
    }

    fun setColor(c: String) {
        color = c
    }

    fun setIcon(i: String) {
        icon = i
    }

    fun setBalance(raw: String) {
        balanceDisplay = if (raw.none { it.isDigit() }) "" else Fmt.money(Fmt.parseNumber(raw))
    }

    fun openAdd() {
        editing = null
        name = ""
        kind = AccountKind.CASH
        balanceDisplay = ""
        color = CATEGORY_COLORS[1]
        icon = "wallet"
        confirmDelete = false
        sheetOpen = true
    }

    fun openEdit(a: AccountRow) {
        editing = a
        name = a.name
        kind = a.kind
        balanceDisplay = Fmt.money(a.initialBalance)
        color = a.colorHex
        icon = a.icon
        confirmDelete = false
        sheetOpen = true
    }

    fun close() {
        sheetOpen = false
        confirmDelete = false
    }

    fun save() {
        val n = name.trim()
        if (n.isEmpty() || saving) return
        val ctx = getApplication<JibetApp>()
        val e = editing
        val initialBalance = Fmt.parseNumber(balanceDisplay)
        viewModelScope.launch {
            saving = true
            try {
                if (e != null) {
                    repo.updateAccount(
                        AccountEntity(
                            id = e.id,
                            name = n,
                            kind = kind,
                            initialBalance = initialBalance,
                            colorHex = color,
                            icon = icon,
                            isDefault = e.isDefault,
                            sortOrder = e.sortOrder,
                        ),
                    )
                } else {
                    repo.addAccount(n, kind, initialBalance, color, icon)
                }
                Toast.makeText(
                    ctx,
                    (if (e != null) "حساب ویرایش شد" else "حساب جدید اضافه شد") + "\n" + n,
                    Toast.LENGTH_SHORT,
                ).show()
                close()
            } catch (ex: Exception) {
                Toast.makeText(ctx, ex.message ?: "خطا در ذخیره‌ی حساب", Toast.LENGTH_SHORT).show()
            } finally {
                saving = false
            }
        }
    }

    fun delete() {
        val e = editing ?: return
        if (!confirmDelete) {
            confirmDelete = true
            return
        }
        val ctx = getApplication<JibetApp>()
        viewModelScope.launch {
            try {
                repo.deleteAccount(
                    AccountEntity(
                        id = e.id,
                        name = e.name,
                        kind = e.kind,
                        initialBalance = e.initialBalance,
                        colorHex = e.colorHex,
                        icon = e.icon,
                        isDefault = e.isDefault,
                        sortOrder = e.sortOrder,
                    ),
                )
                Toast.makeText(ctx, "حساب حذف شد\n${e.name}", Toast.LENGTH_SHORT).show()
                close()
            } catch (ex: IllegalStateException) {
                Toast.makeText(ctx, ex.message ?: "خطا در حذف حساب", Toast.LENGTH_SHORT).show()
                confirmDelete = false
            }
        }
    }
}

/* ================= صفحه ================= */

/** صفحه‌ی حساب‌ها — پورت ۱:۱ از screens/accounts.tsx وب */
@Composable
fun AccountsScreen(onBack: () -> Unit) {
    val vm: AccountsViewModel = viewModel()
    val jc = LocalJibetColors.current
    val accounts by vm.accounts.collectAsState()
    val total = accounts.sumOf { it.balance }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "حساب‌ها",
            onBack = onBack,
            action = {
                GlassIconButton(onClick = vm::openAdd) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "افزودن حساب",
                        tint = TextMid,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )

        // کارت مجموع موجودی
        FadeUp(0) {
            Column(
                Modifier
                    .jibetCard()
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    "مجموع موجودی",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InkSoft),
                )
                Row(Modifier.padding(top = 4.dp)) {
                    Text(
                        Fmt.money(total),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = 26.sp, color = Ink),
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "تومان",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkSoft),
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        // لیست حساب‌ها
        FadeUp(50, Modifier.padding(top = 12.dp)) {
            if (accounts.isEmpty()) {
                Box(Modifier.jibetCard()) {
                    EmptyState(
                        title = "هنوز حسابی ندارید",
                        description = "با دکمه‌ی + اولین حساب را بسازید تا موجودی‌ها اینجا دیده شوند.",
                    )
                }
            } else {
                Column(
                    Modifier
                        .jibetGlass()
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    accounts.forEach { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pressableScale(onClick = { vm.openEdit(a) })
                                .clip(RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconBubble(icon = a.icon, color = parseColor(a.colorHex))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    a.name,
                                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    ACCOUNT_KIND_LABELS[a.kind.name] ?: "",
                                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    Fmt.money(a.balance),
                                    style = TextStyle(
                                        fontFamily = Vazir,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (a.balance < 0) jc.expense else Ink,
                                    ),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "تومان",
                                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, color = InkSoft),
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Rounded.ChevronLeft,
                                    contentDescription = null,
                                    tint = Faint,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    AccountSheet(vm, jc)
}

/* ================= شیت افزودن/ویرایش حساب ================= */

@Composable
private fun AccountSheet(vm: AccountsViewModel, jc: JibetColors) {
    if (!vm.sheetOpen) return
    JibetSheet(
        onDismiss = vm::close,
        title = if (vm.editing != null) "ویرایش حساب" else "حساب جدید",
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            JibetField(label = "نام حساب") {
                NameInput(
                    value = vm.name,
                    onChange = vm::setName,
                    placeholder = "مثلاً کارت بانکی ملت",
                    accent = jc.accentA,
                )
            }

            Column {
                SectionLabel("نوع حساب")
                KindChips(selected = vm.kind, onSelect = vm::setKind, jc)
            }

            JibetField(label = "موجودی اولیه") {
                BalanceInput(value = vm.balanceDisplay, onChange = vm::setBalance, accent = jc.accentA)
                Text(
                    Fmt.money(Fmt.parseNumber(vm.balanceDisplay)) + " تومان",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 11.5.sp, color = InkSoft),
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
                )
            }

            Column {
                SectionLabel("رنگ")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CATEGORY_COLORS.forEach { c ->
                        ColorSwatch(hex = c, selected = vm.color == c, onClick = { vm.setColor(c) })
                    }
                }
            }

            Column {
                SectionLabel("آیکون")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ACCOUNT_ICONS.forEach { ic ->
                        IconTile(icon = ic, colorHex = vm.color, active = vm.icon == ic, onClick = { vm.setIcon(ic) })
                    }
                }
            }

            AccentButton(
                text = if (vm.editing != null) "ذخیره‌ی تغییرات" else "افزودن حساب",
                onClick = vm::save,
                enabled = vm.name.isNotBlank() && !vm.saving,
            )

            if (vm.editing != null) {
                DeleteButton(label = "حذف حساب", armed = vm.confirmDelete, onClick = vm::delete)
            }
        }
    }
}

/* ================= اجزای فرم ================= */

/** ورودی متنی استاندارد — مثل inputClass وب */
@Composable
private fun NameInput(value: String, onChange: (String) -> Unit, placeholder: String, accent: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .jibetInputShape()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Faint),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Ink),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** ورودی موجودی — کیبورد عددی، نمایش فرمت‌شده، جهت چپ‌به‌راست مثل dir="ltr" وب */
@Composable
private fun BalanceInput(value: String, onChange: (String) -> Unit, accent: Color) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .jibetInputShape()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    "۰",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Faint),
                )
            }
            BasicTextField(
                value = TextFieldValue(value, TextRange(value.length)),
                onValueChange = { onChange(it.text) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Ink),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** چیپ‌های نوع حساب — ۵ گزینه با پر کردن سطر */
@Composable
private fun KindChips(selected: AccountKind, onSelect: (AccountKind) -> Unit, jc: JibetColors) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccountKind.entries.forEach { k ->
            val active = k == selected
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .then(
                        if (active) {
                            Modifier
                                .shadow(8.dp, shape, spotColor = Color(0x995B74E9), ambientColor = Color.Transparent)
                                .clip(shape)
                                .background(Brush.linearGradient(listOf(jc.accentB, jc.accentA)))
                        } else {
                            Modifier
                                .shadow(2.dp, shape, spotColor = Color(0x1A5B74E9), ambientColor = Color.Transparent)
                                .clip(shape)
                                .background(Color.White.copy(alpha = 0.85f))
                        },
                    )
                    .pressableScale(onClick = { onSelect(k) }, pressedScale = 0.96f)
                    .padding(horizontal = 16.dp)
                    .height(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    ACCOUNT_KIND_LABELS[k.name] ?: "",
                    style = TextStyle(
                        fontFamily = Vazir,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (active) Color.White else TextMid,
                    ),
                )
            }
        }
    }
}

/** دایره‌ی رنگ — فعال با حلقه‌ی ۲dp جوهری + فاصله‌ی ۲dp سفید (ring-offset وب) */
@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .pressableScale(onClick = onClick, pressedScale = 0.92f)
            .size(if (selected) 48.dp else 44.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.background(Color.White) else Modifier)
            .then(if (selected) Modifier.border(2.dp, Ink.copy(alpha = 0.3f), CircleShape) else Modifier)
            .padding(if (selected) 2.dp else 0.dp)
            .clip(CircleShape)
            .background(parseColor(hex)),
    )
}

/** کاشی آیکون ۵۲dp — فعال با پس‌زمینه‌ی رنگ انتخابی */
@Composable
private fun IconTile(icon: String, colorHex: String, active: Boolean, onClick: () -> Unit) {
    val color = parseColor(colorHex)
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .pressableScale(onClick = onClick, pressedScale = 0.94f)
            .then(
                if (active) {
                    Modifier
                } else {
                    Modifier.shadow(4.dp, shape, spotColor = Color(0x1A5B74E9), ambientColor = Color.Transparent)
                },
            )
            .size(52.dp)
            .clip(shape)
            .background(if (active) color else Color.White.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        IconBubble(icon = icon, color = if (active) Color.White else color, size = 40.dp)
    }
}

/** دکمه‌ی حذف دومرحله‌ای — مثل دکمه‌ی حذف وب */
@Composable
private fun DeleteButton(label: String, armed: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (armed) {
                    Modifier
                        .shadow(10.dp, shape, spotColor = Color(0x8CF26D6D), ambientColor = Color.Transparent)
                        .clip(shape)
                        .background(Brush.linearGradient(listOf(Color(0xFFF28080), Expense)))
                } else {
                    Modifier
                        .clip(shape)
                        .background(Expense.copy(alpha = 0.10f))
                },
            )
            .pressableScale(onClick = onClick)
            .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = null,
                tint = if (armed) Color.White else Expense,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (armed) "مطمئنی؟ حذف کن" else label,
                style = TextStyle(
                    fontFamily = Vazir,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (armed) Color.White else Expense,
                ),
            )
        }
    }
}

/** ورود نرم با پرش به بالا — مثل motion.div fadeUp وب */
@Composable
private fun FadeUp(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    durationMillis: Int = 220,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis, delayMillis, EaseOut),
        label = "fadeUpAlpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (shown) 0.dp else 12.dp,
        animationSpec = tween(durationMillis, delayMillis, EaseOut),
        label = "fadeUpY",
    )
    LaunchedEffect(Unit) {
        shown = true
    }
    Box(
        modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY.toPx()
        },
    ) {
        content()
    }
}
