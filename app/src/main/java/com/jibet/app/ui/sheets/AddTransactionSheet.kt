package com.jibet.app.ui.sheets

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.UiBus
import com.jibet.app.data.AccountRow
import com.jibet.app.data.CategoryEntity
import com.jibet.app.data.Kind
import com.jibet.app.ui.components.CategoryGridCell
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.JibetAmountCard
import com.jibet.app.ui.components.JibetField
import com.jibet.app.ui.components.JibetKeypad
import com.jibet.app.ui.components.JibetSegmented
import com.jibet.app.ui.components.JibetSheet
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetInputShape
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.Fmt
import com.jibet.app.util.JDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val QUICK_AMOUNTS = listOf(50_000, 100_000, 500_000)

/** شیت ثبت سریع تراکنش — با یک دست، کیبورد عددی فارسی (پورت add-transaction.tsx) */
@Composable
fun AddTransactionSheet() {
    val vm: AddTransactionViewModel = viewModel()
    val open by UiBus.addOpen.collectAsState()
    val initialKind by UiBus.addKind.collectAsState()

    if (open) {
        JibetSheet(onDismiss = { UiBus.closeAdd() }, title = "ثبت تراکنش") {
            // با هر باز شدن/تعویض نوع، فرم از نو mount می‌شود (مثل key={kind} وب)
            key(initialKind) {
                AddForm(vm = vm, initialKind = initialKind)
            }
        }
    }
}

/** ویومدل ثبت تراکنش */
class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as JibetApp).repository

    /** true حین ذخیره — دکمه را غیرفعال و آیکون را پالس می‌کند */
    val saving = MutableStateFlow(false)

    val accounts: StateFlow<List<AccountRow>> =
        repo.watchAccounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        repo.watchCategories().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun save(
        amount: Double,
        kind: Kind,
        categoryId: Long?,
        accountId: Long?,
        dateOffset: Int,
        note: String,
    ) {
        if (saving.value) return
        val catId = categoryId ?: return
        val accId = accountId ?: return
        val app = getApplication<JibetApp>()
        saving.value = true
        viewModelScope.launch {
            try {
                val trimmed = note.trim()
                val title = trimmed.ifEmpty {
                    categories.value.firstOrNull { it.id == catId }?.name ?: "تراکنش"
                }
                // حفظ ساعت فعلی روز اما با روز انتخاب‌شده (امروز/دیروز)
                val now = LocalDateTime.now()
                val date = now.toLocalDate().minusDays(dateOffset.toLong())
                    .atTime(now.toLocalTime())
                    .atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                repo.addTx(amount, kind, title, trimmed.ifEmpty { null }, date, accId, catId)
                val msg = (if (kind == Kind.EXPENSE) "هزینه ثبت شد" else "درآمد ثبت شد") +
                    "\n" + Fmt.money(amount) + " تومان"
                Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
                saving.value = false
                UiBus.closeAdd()
            } catch (e: Exception) {
                Toast.makeText(app, "خطا در ثبت", Toast.LENGTH_SHORT).show()
                saving.value = false
            }
        }
    }
}

/** فرم ثبت (با هر باز شدن از نو mount می‌شود) */
@Composable
private fun AddForm(vm: AddTransactionViewModel, initialKind: Kind) {
    val jc = LocalJibetColors.current
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()
    val saving by vm.saving.collectAsState()

    var txKind by remember { mutableStateOf(initialKind) }
    var amountStr by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var dateOffset by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    // سوییچ کیپد اپ ↔ کیبورد سیستم — کنار فیلد مبلغ
    var useSystemKeyboard by rememberSaveable { mutableStateOf(false) }

    // حساب پیش‌فرض: حساب isDefault یا اولین حساب (مقدار اولیه‌ی useState وب)
    LaunchedEffect(accounts) {
        if (accountId == null && accounts.isNotEmpty()) {
            accountId = (accounts.firstOrNull { it.isDefault } ?: accounts.first()).id
        }
    }

    val amount = Fmt.parseNumber(amountStr)
    val canSave = amountStr.isNotEmpty() && amount > 0 && categoryId != null && accountId != null
    val cats = categories.filter { it.kind == txKind }

    Column(
        Modifier
            .fillMaxWidth(),
        // اسکرول با خود شیت است (JibetSheet) — اسکرول تو در تو حذف شد
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        JibetSegmented(
            value = txKind,
            options = listOf(Kind.EXPENSE to "هزینه", Kind.INCOME to "درآمد"),
            onChange = { kind ->
                txKind = kind
                categoryId = null
            },
            activeColor = if (txKind == Kind.EXPENSE) jc.expense else jc.income,
        )

        // نمایش مبلغ + دکمه‌ی سوییچ کیپد اپ/کیبورد سیستم
        JibetAmountCard(
            label = "مبلغ",
            amount = amountStr,
            onAmountChange = { amountStr = it },
            systemKeyboard = useSystemKeyboard,
            onToggleKeyboard = { useSystemKeyboard = !useSystemKeyboard },
            bigFontSize = 24.sp,
        )

        // مبالغ سریع
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_AMOUNTS.forEach { q ->
                Box(
                    Modifier
                        .weight(1f)
                        .shadow(2.dp, RoundedCornerShape(50), spotColor = Color(0x0D232B3B), ambientColor = Color.Transparent)
                        .clip(RoundedCornerShape(50))
                        .background(jc.cardSoft)
                        .pressableScale(onClick = { amountStr = q.toString() })
                        .height(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        Fmt.money(q.toLong()),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = jc.textMid),
                    )
                }
            }
        }

        // کیپد عددی فارسی — چسبیده به مبلغ (نه تهِ شیت)؛ در حالت کیبورد سیستم مخفی می‌شود
        if (!useSystemKeyboard) {
            JibetKeypad(value = amountStr, onChange = { amountStr = it })
        }

        // دسته‌بندی
        Column {
            Text(
                "دسته‌بندی",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.textMid),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (cats.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(jc.cardSoft)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "دسته‌ای برای این نوع وجود ندارد",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = jc.inkSoft),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.chunked(3).forEach { rowCats ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowCats.forEach { c ->
                                CategoryGridCell(
                                    name = c.name,
                                    icon = c.icon,
                                    colorHex = c.colorHex,
                                    active = c.id == categoryId,
                                    onClick = { categoryId = c.id },
                                )
                            }
                            repeat(3 - rowCats.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // حساب — وقتی هیچ حسابی نیست، کارت شیشه‌ای راهنما به‌جای ردیف چیپ‌ها
        Column {
            Text(
                "حساب",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.textMid),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (accounts.isEmpty()) {
                val ctx = LocalContext.current
                Box(
                    Modifier
                        .fillMaxWidth()
                        .jibetCard(RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = jc.inkSoft,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            "هیچ حسابی نداری — ابتدا یک حساب بساز",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.ink),
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(jc.accentB, jc.accentA)))
                                .pressableScale(onClick = {
                                    Toast.makeText(ctx, "از صفحهٔ حساب‌ها یک حساب اضافه کنید", Toast.LENGTH_SHORT).show()
                                })
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "ساخت حساب",
                                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White),
                            )
                        }
                    }
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    accounts.forEach { a ->
                        AccountChip(
                            account = a,
                            active = a.id == accountId,
                            onClick = { accountId = a.id },
                        )
                    }
                }
            }
        }

        // تاریخ
        Column {
            Text(
                "تاریخ",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.textMid),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 1).forEach { o ->
                    DateChip(
                        offset = o,
                        active = dateOffset == o,
                        onClick = { dateOffset = o },
                    )
                }
            }
        }

        // یادداشت
        JibetField(label = "یادداشت (اختیاری)") {
            SheetTextInput(
                value = note,
                onValueChange = { note = it },
                placeholder = "مثلاً ناهار با بچه‌ها",
            )
        }

        // دکمه ثبت
        SaveButton(
            kind = txKind,
            canSave = canSave,
            saving = saving,
            onClick = {
                vm.save(
                    amount = Fmt.parseNumber(amountStr),
                    kind = txKind,
                    categoryId = categoryId,
                    accountId = accountId,
                    dateOffset = dateOffset,
                    note = note,
                )
            },
        )
    }
}

/** چیپ انتخاب حساب */
@Composable
private fun AccountChip(account: AccountRow, active: Boolean, onClick: () -> Unit) {
    val jc = LocalJibetColors.current
    val color = parseColor(account.colorHex)
    Row(
        Modifier
            .then(
                if (active) {
                    Modifier.shadow(8.dp, RoundedCornerShape(50), spotColor = color, ambientColor = Color.Transparent)
                } else {
                    Modifier.shadow(2.dp, RoundedCornerShape(50), spotColor = Color(0x0D232B3B), ambientColor = Color.Transparent)
                }
            )
            .clip(RoundedCornerShape(50))
            .background(if (active) color else jc.inputBg)
            .pressableScale(onClick = onClick)
            // pl-3.5/pr-1.5 وب = پدینگ فیزیکی؛ در RTL استارت = راست (۶dp) و انته = چپ (۱۴dp)
            .padding(start = 6.dp, top = 6.dp, end = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBubble(
            icon = account.icon,
            color = if (active) Color.White else color,
            size = 26.dp,
            iconSize = 13.dp,
        )
        Text(
            account.name,
            style = TextStyle(
                fontFamily = Vazir,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = if (active) Color.White else jc.textMid,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** چیپ تاریخ: امروز / دیروز */
@Composable
private fun RowScope.DateChip(offset: Int, active: Boolean, onClick: () -> Unit) {
    val jc = LocalJibetColors.current
    val color = if (active) Color(0xFF3D55C8) else jc.inkSoft
    val date = LocalDate.now().minusDays(offset.toLong())
    Row(
        Modifier
            .weight(1f)
            .then(
                if (active) {
                    Modifier
                } else {
                    Modifier.shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0D232B3B), ambientColor = Color.Transparent)
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) jc.accentA.copy(alpha = 0.12f) else jc.inputBg)
            .pressableScale(onClick = onClick)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(15.dp),
        )
        Text(
            if (offset == 0) "امروز" else "دیروز",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color),
        )
        Text(
            "(${JDate.formatJalaliDate(date, JDate.Style.SHORT)})",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color.copy(alpha = 0.7f)),
        )
    }
}

/** دکمه ثبت گرادیانی بر اساس نوع */
@Composable
private fun SaveButton(kind: Kind, canSave: Boolean, saving: Boolean, onClick: () -> Unit) {
    val jc = LocalJibetColors.current
    val expense = kind == Kind.EXPENSE
    val pulse = rememberInfiniteTransition(label = "savingPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "savingPulseAlpha",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(
                14.dp,
                RoundedCornerShape(16.dp),
                spotColor = if (expense) Color(0x8CF26D6D) else Color(0x8C2FBF83),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (expense) {
                    Brush.linearGradient(listOf(Color(0xFFF28080), jc.expense))
                } else {
                    Brush.linearGradient(listOf(Color(0xFF43D19E), jc.income))
                }
            )
            .alpha(if (canSave && !saving) 1f else 0.4f)
            .pressableScale(onClick = if (canSave && !saving) onClick else null, pressedScale = 0.98f)
            .height(54.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (saving) {
                Icon(
                    Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(pulseAlpha),
                )
            } else {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
            Text(
                if (saving) "در حال ثبت…" else "ثبت تراکنش",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White),
            )
        }
    }
}

/** ورودی متنی استاندارد شیت‌ها — مثل inputClass وب */
@Composable
internal fun SheetTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLength: Int = 60,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val jc = LocalJibetColors.current
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        textStyle = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = jc.ink),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        cursorBrush = SolidColor(jc.accentA),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .jibetInputShape(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = jc.faint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            }
        },
    )
}
