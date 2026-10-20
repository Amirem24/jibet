@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jibet.app.ui.recurring

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.CategoryEntity
import com.jibet.app.data.Frequency
import com.jibet.app.data.Kind
import com.jibet.app.data.RecurringEntity
import com.jibet.app.data.RecurringRow
import com.jibet.app.ui.components.AccentButton
import com.jibet.app.ui.components.AmountText
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassIconButton
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.JibetChip
import com.jibet.app.ui.components.JibetField
import com.jibet.app.ui.components.JibetKeypad
import com.jibet.app.ui.components.JibetSegmented
import com.jibet.app.ui.components.JibetSheet
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.SectionLabel
import com.jibet.app.ui.components.accentGradient
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetInputShape
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.ExpenseSoft
import com.jibet.app.ui.theme.Faint
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.InkSoft
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.TextMid
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.Fmt
import com.jibet.app.util.FREQUENCY_LABELS
import com.jibet.app.util.JDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private val FREQUENCIES = listOf(Frequency.DAILY, Frequency.WEEKLY, Frequency.MONTHLY, Frequency.YEARLY)

/** شروع سریع: امروز، فردا، ۳ روز بعد، هفته بعد، ماه بعد */
private data class StartOption(val offset: Int, val label: String)

private val START_OPTIONS = listOf(
    StartOption(0, "امروز"),
    StartOption(1, "فردا"),
    StartOption(3, "۳ روز بعد"),
    StartOption(7, "هفته بعد"),
    StartOption(30, "ماه بعد"),
)

private fun dateFromOffset(n: Int): LocalDate = LocalDate.now().plusDays(n.toLong())

/** برابری روز شمسی — مثل sameJDay وب */
private fun sameJDay(millis: Long, date: LocalDate): Boolean =
    JDate.formatJalaliDate(millis, JDate.Style.NUMERIC) == JDate.formatJalaliDate(date, JDate.Style.NUMERIC)

private fun Double.toEditableAmount(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

class RecurringViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as JibetApp).repository

    val recurrings = repo.watchRecurrings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repo.watchCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts = repo.watchAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun RecurringRow.toEntity(): RecurringEntity = RecurringEntity(
        id = id,
        title = title,
        amount = amount,
        kind = kind,
        accountId = accountId,
        categoryId = categoryId,
        frequency = frequency,
        nextDate = nextDate,
        isActive = isActive,
    )

    suspend fun toggle(r: RecurringRow) {
        repo.toggleRecurring(r.toEntity())
        toast(if (r.isActive) "غیرفعال شد" else "فعال شد", r.title)
    }

    suspend fun save(
        existing: RecurringRow?,
        title: String,
        amount: Double,
        kind: Kind,
        accountId: Long,
        categoryId: Long,
        frequency: Frequency,
        nextDate: Long,
    ) {
        val freqLabel = FREQUENCY_LABELS[frequency.name] ?: frequency.name
        if (existing != null) {
            repo.updateRecurring(
                existing.toEntity().copy(
                    title = title.trim(),
                    amount = amount,
                    kind = kind,
                    accountId = accountId,
                    categoryId = categoryId,
                    frequency = frequency,
                    nextDate = nextDate,
                )
            )
            toast("ویرایش شد", "${Fmt.money(amount)} تومان · $freqLabel")
        } else {
            repo.addRecurring(title.trim(), amount, kind, accountId, categoryId, frequency, nextDate)
            toast("تراکنش تکراری اضافه شد", "${Fmt.money(amount)} تومان · $freqLabel")
        }
    }

    suspend fun delete(r: RecurringRow) {
        repo.deleteRecurring(r.toEntity())
        toast("حذف شد", r.title)
    }

    private fun toast(msg: String, desc: String) {
        Toast.makeText(getApplication(), "$msg\n$desc", Toast.LENGTH_SHORT).show()
    }
}

/** تراکنش‌های تکراری (اجاره، حقوق، اشتراک‌ها) */
@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val vm: RecurringViewModel = viewModel()
    val recurrings by vm.recurrings.collectAsState()
    val scope = rememberCoroutineScope()

    var sheetOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringRow?>(null) }

    val openAdd = {
        editing = null
        sheetOpen = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "تراکنش‌های تکراری",
            onBack = onBack,
            action = {
                GlassIconButton(onClick = openAdd) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "افزودن تراکنش تکراری",
                        tint = TextMid,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )

        Row(
            Modifier.padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Rounded.Repeat,
                contentDescription = null,
                tint = InkSoft,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "این تراکنش‌ها خودکار در سررسید ثبت می‌شوند.",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = InkSoft),
            )
        }

        if (recurrings.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .jibetCard()
                    .fadeUp(delayMs = 0, distance = 8.dp),
            ) {
                EmptyState(
                    title = "تراکنش تکراری نداری",
                    description = "اجاره، حقوق و اشتراک‌ها را یک بار ثبت کن تا جیبت خودش پیگیری‌شان کند.",
                    icon = Icons.Rounded.Repeat,
                )
                Box(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                    AccentButton(
                        text = "افزودن تراکنش تکراری",
                        onClick = openAdd,
                        modifier = Modifier.fillMaxWidth(),
                        leading = {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recurrings.forEachIndexed { index, r ->
                    RecurringRowItem(
                        r = r,
                        index = index,
                        onEdit = {
                            editing = r
                            sheetOpen = true
                        },
                        onToggle = { scope.launch { vm.toggle(r) } },
                    )
                }
            }
        }
    }

    if (sheetOpen) {
        RecurringSheet(editing = editing, vm = vm, onClose = { sheetOpen = false })
    }
}

/** ردیف یک تراکنش تکراری */
@Composable
private fun RecurringRowItem(r: RecurringRow, index: Int, onEdit: () -> Unit, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .fadeUp(delayMs = minOf(index * 40, 200), distance = 10.dp)
            .jibetCard()
            .alpha(if (r.isActive) 1f else 0.6f)
            .pressableScale(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBubble(
            icon = r.catIcon ?: "more-horizontal",
            color = parseColor(r.catColor),
        )
        Column(Modifier.weight(1f)) {
            Text(
                r.title,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = InkSoft,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    "${FREQUENCY_LABELS[r.frequency.name] ?: r.frequency.name} · سررسید بعدی: " +
                        Fmt.faDigits(JDate.formatJalaliDate(r.nextDate, JDate.Style.SHORT)),
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = InkSoft),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AmountText(
                amount = r.amount,
                kind = r.kind,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
            )
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Switch(
                    checked = r.isActive,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.scale(0.8f),
                )
            }
        }
    }
}

/** شیت افزودن/ویرایش تراکنش تکراری */
@Composable
private fun RecurringSheet(editing: RecurringRow?, vm: RecurringViewModel, onClose: () -> Unit) {
    val accounts by vm.accounts.collectAsState()
    val allCategories by vm.categories.collectAsState()
    val jc = LocalJibetColors.current
    val scope = rememberCoroutineScope()

    // تطبیق سررسید فعلی با یکی از گزینه‌های سریع (در غیر این صورت «امروز»)
    val initOffset: Int = editing?.nextDate
        ?.let { nd -> START_OPTIONS.firstOrNull { o -> sameJDay(nd, dateFromOffset(o.offset)) }?.offset }
        ?: 0

    var kind by remember { mutableStateOf(editing?.kind ?: Kind.EXPENSE) }
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var amount by remember { mutableStateOf(editing?.amount?.toEditableAmount() ?: "") }
    var categoryId by remember { mutableStateOf(editing?.categoryId) }
    var accountId by remember { mutableStateOf<Long?>(editing?.accountId) }
    var frequency by remember { mutableStateOf(editing?.frequency ?: Frequency.MONTHLY) }
    var startOffset by remember { mutableStateOf(if (editing != null) initOffset else 0) }
    var saving by remember { mutableStateOf(false) }
    var armed by remember { mutableStateOf(false) }

    // انتخاب پیش‌فرض حساب مثل وب: پیش‌فرض → اولین حساب
    LaunchedEffect(accounts) {
        if (accountId == null) {
            accountId = accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id
        }
    }

    LaunchedEffect(armed) {
        if (armed) {
            delay(4000)
            armed = false
        }
    }

    val categories = allCategories.filter { it.kind == kind }
    val startDate = dateFromOffset(startOffset)
    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val canSave = title.isNotBlank() && amountValue > 0 && categoryId != null && accountId != null && !saving

    val changeKind: (Kind) -> Unit = { v ->
        kind = v
        // اگر دسته‌ی فعلی با نوع جدید نمی‌خواند، ریست کن
        categoryId = categoryId?.takeIf { id -> allCategories.any { it.id == id && it.kind == v } }
    }

    JibetSheet(
        onDismiss = onClose,
        title = if (editing != null) "ویرایش تراکنش تکراری" else "تراکنش تکراری جدید",
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // نوع
            JibetSegmented(
                value = kind,
                options = listOf(Kind.EXPENSE to "هزینه", Kind.INCOME to "درآمد"),
                onChange = { changeKind(it) },
                activeColor = if (kind == Kind.EXPENSE) jc.expense else jc.income,
            )

            // عنوان
            JibetField(label = "عنوان") {
                InputBox {
                    JibetTextInput(value = title, onValueChange = { title = it }, placeholder = "مثلاً اجاره خانه")
                }
            }

            // مبلغ
            AmountDisplayCard(label = "مبلغ هر بار", amount = amount)
            JibetKeypad(value = amount, onChange = { amount = it })

            // دسته‌بندی
            Column {
                SectionLabel("دسته‌بندی")
                if (categories.isEmpty()) {
                    Text(
                        "دسته‌ای برای این نوع وجود ندارد",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = InkSoft),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(vertical = 16.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.chunked(3).forEach { rowCats ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowCats.forEach { c ->
                                    CategoryCell(
                                        c = c,
                                        active = c.id == categoryId,
                                        onClick = { categoryId = c.id },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(3 - rowCats.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            // حساب
            Column {
                SectionLabel("حساب")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    accounts.forEach { a ->
                        AccountChip(
                            name = a.name,
                            icon = a.icon,
                            colorHex = a.colorHex,
                            active = a.id == accountId,
                            onClick = { accountId = a.id },
                        )
                    }
                }
            }

            // تناوب
            Column {
                SectionLabel("تناوب")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FREQUENCIES.forEach { f ->
                        JibetChip(
                            label = FREQUENCY_LABELS[f.name] ?: f.name,
                            active = frequency == f,
                            onClick = { frequency = f },
                        )
                    }
                }
            }

            // شروع از
            Column {
                SectionLabel("شروع از")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    START_OPTIONS.forEach { o ->
                        val active = startOffset == o.offset
                        JibetChip(
                            label = if (active) {
                                "${o.label} · ${JDate.formatJalaliDate(dateFromOffset(o.offset), JDate.Style.SHORT)}"
                            } else {
                                o.label
                            },
                            active = active,
                            onClick = { startOffset = o.offset },
                        )
                    }
                }
                Row(
                    Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = InkSoft,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        "اولین ثبت: ${Fmt.faDigits(JDate.formatJalaliDate(startDate, JDate.Style.LONG))}",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = InkSoft),
                    )
                }
            }

            // ذخیره
            GradientSubmitButton(
                text = if (saving) "در حال ذخیره…" else "ذخیره",
                saving = saving,
                enabled = canSave,
                idleIcon = Icons.Rounded.Check,
                savingIcon = Icons.Rounded.AccountBalanceWallet,
                idleIconSize = 20.dp,
                onClick = {
                    if (!canSave || saving) return@GradientSubmitButton
                    saving = true
                    scope.launch {
                        vm.save(
                            existing = editing,
                            title = title.trim(),
                            amount = amountValue,
                            kind = kind,
                            accountId = accountId!!,
                            categoryId = categoryId!!,
                            frequency = frequency,
                            nextDate = JDate.millis(startDate),
                        )
                        saving = false
                        onClose()
                    }
                },
            )

            // حذف دو مرحله‌ای
            if (editing != null) {
                TwoStepDeleteButton(
                    armed = armed,
                    label = "حذف این تراکنش تکراری",
                    armedLabel = "تأیید حذف — برگشت‌پذیر نیست",
                    onClick = {
                        if (!armed) {
                            armed = true
                        } else {
                            val target = editing
                            scope.launch {
                                vm.delete(target)
                                onClose()
                            }
                        }
                    },
                )
            }
        }
    }
}

/** سلول دسته‌بندی — گرید ۳ ستونه مثل وب */
@Composable
private fun CategoryCell(c: CategoryEntity, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cc = parseColor(c.colorHex)
    Column(
        modifier
            .shadow(
                if (active) 10.dp else 2.dp,
                RoundedCornerShape(16.dp),
                spotColor = if (active) cc.copy(alpha = 0.55f) else Color(0x1A232B3B),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) cc else Color.White.copy(alpha = 0.85f))
            .pressableScale(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconBubble(
            icon = c.icon,
            color = if (active) Color.White else cc,
            size = 34.dp,
            iconSize = 17.dp,
        )
        Text(
            c.name,
            style = TextStyle(
                fontFamily = Vazir,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = if (active) Color.White else TextMid,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** چیپ حساب افقی مثل وب */
@Composable
private fun AccountChip(name: String, icon: String, colorHex: String, active: Boolean, onClick: () -> Unit) {
    val cc = parseColor(colorHex)
    Row(
        Modifier
            .shadow(
                if (active) 8.dp else 2.dp,
                RoundedCornerShape(50),
                spotColor = if (active) cc.copy(alpha = 0.5f) else Color(0x1A232B3B),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(50))
            .background(if (active) cc else Color.White.copy(alpha = 0.85f))
            .pressableScale(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBubble(
            icon = icon,
            color = if (active) Color.White else cc,
            size = 26.dp,
            iconSize = 13.dp,
        )
        Text(
            name,
            style = TextStyle(
                fontFamily = Vazir,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = if (active) Color.White else TextMid,
            ),
            maxLines = 1,
        )
    }
}

/** دکمه گرادیانی ذخیره با حالت saving */
@Composable
private fun GradientSubmitButton(
    text: String,
    saving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    idleIcon: ImageVector,
    savingIcon: ImageVector,
    idleIconSize: Dp = 20.dp,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(16.dp), spotColor = Color(0x8C5B74E9), ambientColor = Color.Transparent)
            .clip(RoundedCornerShape(16.dp))
            .background(accentGradient())
            .alpha(if (enabled) 1f else 0.4f)
            .pressableScale(onClick = if (enabled) onClick else null)
            .height(52.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (saving) {
            PulsingIcon(savingIcon, 19.dp)
        } else {
            Icon(idleIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(idleIconSize))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White),
        )
    }
}

/** آیکون تپنده هنگام saving — مثل animate-pulse وب */
@Composable
private fun PulsingIcon(icon: ImageVector, size: Dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse-alpha",
    )
    Icon(
        icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(size).alpha(alpha),
    )
}

/** دکمه حذف دو مرحله‌ای — expense-soft → expense */
@Composable
private fun TwoStepDeleteButton(armed: Boolean, label: String, armedLabel: String, onClick: () -> Unit) {
    val jc = LocalJibetColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (armed) jc.expense else ExpenseSoft)
            .pressableScale(onClick = onClick)
            .height(44.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (armed) Icons.Rounded.Warning else Icons.Rounded.Delete,
            contentDescription = null,
            tint = if (armed) Color.White else jc.expense,
            modifier = Modifier.size(if (armed) 16.dp else 15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (armed) armedLabel else label,
            style = TextStyle(
                fontFamily = Vazir,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = if (armed) Color.White else jc.expense,
            ),
        )
    }
}

/** کارت نمایش مبلغ با برچسب — مثل کارت‌های وب با dir=ltr */
@Composable
private fun AmountDisplayCard(label: String, amount: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .jibetCard()
            .heightIn(min = 64.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InkSoft),
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (amount.isEmpty()) {
                    Text(
                        "۰",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Faint),
                    )
                } else {
                    Text(
                        Fmt.money(amount.toDoubleOrNull() ?: 0.0),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "تومان",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = InkSoft),
                )
            }
        }
    }
}

/** فیلد ورودی متنی جیبت — BasicTextField داخل باکس ۴۸dp */
@Composable
private fun JibetTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int = 40,
) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        textStyle = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Ink),
        cursorBrush = SolidColor(LocalJibetColors.current.accentA),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Faint),
                        maxLines = 1,
                    )
                }
                inner()
            }
        },
    )
}

/** باکس استاندارد فیلد — مثل inputClass وب */
@Composable
private fun InputBox(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .jibetInputShape()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

/** ورود نرم fade-up — مثل framer وب */
@Composable
private fun Modifier.fadeUp(delayMs: Int, distance: Dp): Modifier {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        anim.animateTo(1f, tween(durationMillis = 180))
    }
    return this.graphicsLayer {
        alpha = anim.value
        translationY = (1f - anim.value) * distance.toPx()
    }
}
