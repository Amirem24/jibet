@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jibet.app.ui.goals

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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.GoalEntity
import com.jibet.app.ui.components.AccentButton
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassIconButton
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.JibetChip
import com.jibet.app.ui.components.JibetField
import com.jibet.app.ui.components.JibetKeypad
import com.jibet.app.ui.components.JibetProgress
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
import com.jibet.app.util.GOAL_COLORS
import com.jibet.app.util.GOAL_ICONS
import com.jibet.app.util.JDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** مهلت‌های سریع: ۳ ماه بعد، ۶ ماه بعد، ۱ سال بعد (رشته شمسی «jy/mm/dd») */
private data class DeadlineOption(val months: Int, val label: String)

private val DEADLINE_OPTIONS = listOf(
    DeadlineOption(3, "۳ ماه بعد"),
    DeadlineOption(6, "۶ ماه بعد"),
    DeadlineOption(12, "۱ سال بعد"),
)

/** مثل deadlineAfterMonths وب: از امروز، n ماه شمسی بعد با مهار روز به طول ماه */
private fun deadlineAfterMonths(n: Int): String {
    val t = JDate.toJalali(LocalDate.now())
    val y2 = JDate.addJalaliMonths(t.jy, t.jm, n)
    val d = minOf(t.jd, JDate.jalaliMonthLength(y2.jy, y2.jm))
    return "${y2.jy}/${y2.jm.toString().padStart(2, '0')}/${d.toString().padStart(2, '0')}"
}

private fun Double.toEditableAmount(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

class GoalsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as JibetApp).repository

    val goals = repo.watchGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun addGoal(title: String, target: Double, saved: Double, deadline: String?, colorHex: String, icon: String) {
        repo.addGoal(title, target, saved, deadline, colorHex, icon)
        toast("هدف ساخته شد", "$title · ${Fmt.money(target)} تومان")
    }

    suspend fun updateGoal(g: GoalEntity) {
        repo.updateGoal(g)
        toast("هدف ویرایش شد", "${g.title} · ${Fmt.money(g.targetAmount)} تومان")
    }

    suspend fun deleteGoal(g: GoalEntity) {
        repo.deleteGoal(g)
        toast("هدف حذف شد", g.title)
    }

    suspend fun deposit(goalId: Long, delta: Double) {
        val g = goals.value.firstOrNull { it.id == goalId } ?: return
        repo.depositGoal(goalId, delta)
        toast("پس‌انداز انجام شد", "${Fmt.money(delta)} تومان برای «${g.title}»")
        // عبور از هدف — تبریک جدا مثل وب
        if (g.savedAmount < g.targetAmount && g.savedAmount + delta >= g.targetAmount) {
            toast("تبریک! هدف تکمیل شد 🎉", g.title)
        }
    }

    private fun toast(msg: String, desc: String) {
        Toast.makeText(getApplication(), "$msg\n$desc", Toast.LENGTH_SHORT).show()
    }
}

/** هدف‌های پس‌انداز */
@Composable
fun GoalsScreen(onBack: () -> Unit) {
    val vm: GoalsViewModel = viewModel()
    val goals by vm.goals.collectAsState()

    var formOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GoalEntity?>(null) }
    var depositGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var armedId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    // خنثی‌سازی تأیید حذف بعد از چند ثانیه
    LaunchedEffect(armedId) {
        if (armedId != null) {
            delay(4000)
            armedId = null
        }
    }

    val openAdd = {
        editing = null
        formOpen = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "هدف‌های پس‌انداز",
            onBack = onBack,
            action = {
                GlassIconButton(onClick = openAdd) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "افزودن هدف",
                        tint = TextMid,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )

        if (goals.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .jibetCard()
                    .fadeUp(delayMs = 0, distance = 8.dp),
            ) {
                EmptyState(
                    title = "هنوز هدفی نساختی",
                    description = "برای سفر، لپ‌تاپ یا هر رویای دیگر، قدم‌به‌قدم پس‌انداز کن.",
                    icon = Icons.Rounded.GpsFixed,
                )
                Box(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                    AccentButton(
                        text = "هدف اولت را بساز",
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                goals.forEachIndexed { index, g ->
                    GoalCard(
                        goal = g,
                        index = index,
                        armed = armedId == g.id,
                        onArm = { armedId = g.id },
                        onDisarm = { armedId = null },
                        onDelete = {
                            armedId = null
                            scope.launch { vm.deleteGoal(g) }
                        },
                        onEdit = {
                            editing = g
                            formOpen = true
                        },
                        onDeposit = { depositGoal = g },
                    )
                }
            }
        }
    }

    if (formOpen) {
        GoalFormSheet(editing = editing, onClose = { formOpen = false }, vm = vm)
    }
    depositGoal?.let { g ->
        DepositSheet(goal = g, onClose = { depositGoal = null }, vm = vm)
    }
}

/** کارت یک هدف */
@Composable
private fun GoalCard(
    goal: GoalEntity,
    index: Int,
    armed: Boolean,
    onArm: () -> Unit,
    onDisarm: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDeposit: () -> Unit,
) {
    val color = parseColor(goal.colorHex)
    val pct = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount) * 100.0 else 0.0
    val showPct = pct.coerceIn(0.0, 100.0)
    val remaining = maxOf(0.0, goal.targetAmount - goal.savedAmount)

    Column(
        Modifier
            .fillMaxWidth()
            .fadeUp(delayMs = minOf(index * 50, 200), distance = 10.dp)
            .jibetCard()
            .padding(16.dp),
    ) {
        // هدر: آیکون، عنوان، ویرایش/حذف
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(icon = goal.icon, color = color, size = 48.dp, iconSize = 22.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    goal.title,
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "هدف: ${Fmt.money(goal.targetAmount)} تومان",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (armed) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // انصراف
                    Box(
                        Modifier
                            .shadow(2.dp, CircleShape, spotColor = Color(0x1A232B3B), ambientColor = Color.Transparent)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                            .pressableScale(onClick = onDisarm)
                            .size(44.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "انصراف",
                            tint = InkSoft,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    // تأیید حذف
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(LocalJibetColors.current.expense)
                            .pressableScale(onClick = onDelete)
                            .padding(horizontal = 14.dp)
                            .height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            "تأیید حذف",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White),
                        )
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallCircleButton(onClick = onEdit) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "ویرایش ${goal.title}",
                            tint = InkSoft,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    SmallCircleButton(onClick = onArm) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "حذف ${goal.title}",
                            tint = InkSoft,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }

        // نوار پیشرفت + درصد وسط
        Box(Modifier.padding(top = 14.dp)) {
            JibetProgress(showPct.toFloat(), color = color, height = 14.dp)
            Text(
                Fmt.percent(showPct),
                style = TextStyle(
                    fontFamily = Vazir,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    color = if (showPct >= 48) Color.White else TextMid,
                ),
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // اعداد
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("پس‌انداز شده ", style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft))
                Text(Fmt.money(goal.savedAmount), style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Ink))
                Text(" تومان", style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("باقی‌مانده ", style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft))
                Text(Fmt.money(remaining), style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = color))
                Text(" تومان", style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft))
            }
        }

        // مهلت + دکمه پس‌انداز
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (goal.deadline != null) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = InkSoft,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "تا ${Fmt.faDigits(goal.deadline!!)}",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft),
                    )
                } else {
                    Icon(
                        Icons.Rounded.GpsFixed,
                        contentDescription = null,
                        tint = InkSoft,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "بدون مهلت",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft),
                    )
                }
            }
            Row(
                Modifier
                    .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = Color(0x8C5B74E9), ambientColor = Color.Transparent)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentGradient())
                    .pressableScale(onClick = onDeposit)
                    .padding(horizontal = 16.dp)
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    "پس‌انداز",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White),
                )
            }
        }
    }
}

/** دکمه دایره‌ای کوچک white/70 — مثل وب */
@Composable
private fun SmallCircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .shadow(2.dp, CircleShape, spotColor = Color(0x1A232B3B), ambientColor = Color.Transparent)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.7f))
            .pressableScale(onClick = onClick)
            .size(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** شیت ثبت پس‌انداز روی یک هدف */
@Composable
private fun DepositSheet(goal: GoalEntity, onClose: () -> Unit, vm: GoalsViewModel) {
    var amount by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val color = parseColor(goal.colorHex)
    val pct = if (goal.targetAmount > 0) ((goal.savedAmount / goal.targetAmount) * 100.0).coerceIn(0.0, 100.0) else 0.0

    JibetSheet(onDismiss = onClose, title = "پس‌انداز برای «${goal.title}»") {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // وضعیت فعلی
            Column(
                Modifier
                    .fillMaxWidth()
                    .jibetCard()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("پس‌انداز شده: ", style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = InkSoft))
                        Text(Fmt.money(goal.savedAmount), style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = Ink))
                        Text(" تومان", style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = InkSoft))
                    }
                    Text(
                        Fmt.percent(pct),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = color),
                    )
                }
                Box(Modifier.padding(top = 10.dp)) {
                    JibetProgress(pct.toFloat(), color = color, height = 8.dp)
                }
                Text(
                    "هدف: ${Fmt.money(goal.targetAmount)} تومان",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = InkSoft),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // مبلغ جدید
            AmountDisplayCard(label = "مبلغ پس‌انداز", amount = amount)
            JibetKeypad(value = amount, onChange = { amount = it })

            GradientSubmitButton(
                text = if (saving) "در حال ثبت…" else "ثبت پس‌انداز",
                saving = saving,
                enabled = amount.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0 && !saving,
                idleIcon = Icons.Rounded.AccountBalanceWallet,
                savingIcon = Icons.Rounded.Savings,
                idleIconSize = 19.dp,
                onClick = {
                    if (saving) return@GradientSubmitButton
                    val delta = amount.toDoubleOrNull() ?: 0.0
                    if (delta <= 0) return@GradientSubmitButton
                    saving = true
                    scope.launch {
                        vm.deposit(goal.id, delta)
                        saving = false
                        onClose()
                    }
                },
            )
        }
    }
}

/** شیت افزودن/ویرایش هدف */
@Composable
private fun GoalFormSheet(editing: GoalEntity?, onClose: () -> Unit, vm: GoalsViewModel) {
    JibetSheet(
        onDismiss = onClose,
        title = if (editing != null) "ویرایش هدف" else "هدف جدید",
    ) {
        GoalForm(editing = editing, onClose = onClose, vm = vm)
    }
}

/** فرم افزودن/ویرایش هدف (با هر تغییر هدف از نو mount می‌شود) */
@Composable
private fun GoalForm(editing: GoalEntity?, onClose: () -> Unit, vm: GoalsViewModel) {
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var amount by remember { mutableStateOf(editing?.targetAmount?.toEditableAmount() ?: "") }
    var saved by remember { mutableStateOf("") } // فقط حالت افزودن: پس‌انداز اولیه
    var color by remember { mutableStateOf(editing?.colorHex ?: GOAL_COLORS[0]) }
    var icon by remember { mutableStateOf(editing?.icon ?: GOAL_ICONS[0]) }
    var deadline by remember { mutableStateOf(editing?.deadline) }
    var saving by remember { mutableStateOf(false) }
    var armed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(armed) {
        if (armed) {
            delay(4000)
            armed = false
        }
    }

    val targetAmount = amount.toDoubleOrNull() ?: 0.0
    val canSave = title.isNotBlank() && targetAmount > 0 && !saving
    val deadlineMatchesOption =
        deadline != null && DEADLINE_OPTIONS.any { deadlineAfterMonths(it.months) == deadline }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // عنوان
        JibetField(label = "عنوان هدف") {
            InputBox {
                JibetTextInput(value = title, onValueChange = { title = it }, placeholder = "مثلاً سفر شمال")
            }
        }

        // مبلغ هدف
        AmountDisplayCard(label = "مبلغ هدف", amount = amount)
        JibetKeypad(value = amount, onChange = { amount = it })

        // پس‌انداز اولیه (فقط افزودن)
        if (editing == null) {
            JibetField(label = "پس‌انداز اولیه (اختیاری)") {
                Column {
                    InputBox {
                        JibetTextInput(value = saved, onValueChange = { saved = it }, placeholder = "مثلاً ۵۰۰٬۰۰۰", maxLength = 15)
                    }
                    if (Fmt.parseNumber(saved) > 0) {
                        Text(
                            "معادل: ${Fmt.money(Fmt.parseNumber(saved))} تومان",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = InkSoft),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        // رنگ
        Column {
            SectionLabel("رنگ")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GOAL_COLORS.forEach { c ->
                    GoalColorDot(hex = c, active = color == c, onClick = { color = c })
                }
            }
        }

        // آیکون
        Column {
            SectionLabel("آیکون")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GOAL_ICONS.forEach { ic ->
                    GoalIconOption(iconName = ic, color = parseColor(color), active = icon == ic, onClick = { icon = ic })
                }
            }
        }

        // مهلت
        Column {
            SectionLabel("مهلت")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DEADLINE_OPTIONS.forEach { o ->
                    val v = deadlineAfterMonths(o.months)
                    val active = deadline == v
                    JibetChip(
                        label = if (active) "${o.label} · ${Fmt.faDigits(v)}" else o.label,
                        active = active,
                        onClick = { deadline = v },
                    )
                }
                JibetChip(label = "بدون مهلت", active = deadline == null, onClick = { deadline = null })
            }
            if (deadline != null && !deadlineMatchesOption) {
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
                        "مهلت فعلی: تا ${Fmt.faDigits(deadline!!)}",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = InkSoft),
                    )
                }
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
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (editing != null) {
                        vm.updateGoal(
                            editing.copy(
                                title = title.trim(),
                                targetAmount = amt,
                                deadline = deadline,
                                colorHex = color,
                                icon = icon,
                            )
                        )
                    } else {
                        vm.addGoal(title.trim(), amt, Fmt.parseNumber(saved), deadline, color, icon)
                    }
                    saving = false
                    onClose()
                }
            },
        )

        // حذف دو مرحله‌ای
        if (editing != null) {
            TwoStepDeleteButton(
                armed = armed,
                label = "حذف این هدف",
                armedLabel = "تأیید حذف — برگشت‌پذیر نیست",
                onClick = {
                    if (!armed) {
                        armed = true
                    } else {
                        val target = editing
                        scope.launch {
                            vm.deleteGoal(target)
                            onClose()
                        }
                    }
                },
            )
        }
    }
}

/** دایره انتخاب رنگ با رینگ فعال — مثل وب */
@Composable
private fun GoalColorDot(hex: String, active: Boolean, onClick: () -> Unit) {
    val cc = parseColor(hex)
    Box(
        Modifier
            .border(1.5.dp, if (active) cc else Color.Transparent, CircleShape)
            .padding(2.dp)
            .pressableScale(onClick = onClick)
            .then(
                if (active) Modifier
                else Modifier.shadow(6.dp, CircleShape, spotColor = cc.copy(alpha = 0.4f), ambientColor = Color.Transparent)
            )
            .clip(CircleShape)
            .background(cc)
            .border(2.dp, if (active) Color.White else Color.Transparent, CircleShape)
            .size(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "انتخاب رنگ",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** دکمه انتخاب آیکون ۴۸dp با رینگ فعال */
@Composable
private fun GoalIconOption(iconName: String, color: Color, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(if (active) Modifier.border(2.dp, color, CircleShape) else Modifier)
            .pressableScale(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        IconBubble(icon = iconName, color = color, size = 44.dp, iconSize = 20.dp)
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
