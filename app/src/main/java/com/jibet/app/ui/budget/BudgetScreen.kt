package com.jibet.app.ui.budget

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.BudgetEntity
import com.jibet.app.data.BudgetRow
import com.jibet.app.data.CategoryEntity
import com.jibet.app.data.Kind
import com.jibet.app.ui.components.AccentButton
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassIconButton
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.JibetKeypad
import com.jibet.app.ui.components.JibetProgress
import com.jibet.app.ui.components.JibetSheet
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.ui.theme.budgetBarColor
import com.jibet.app.util.Fmt
import com.jibet.app.util.JDate
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/* ================= ViewModel — مثل منطق screens/budget.tsx وب ================= */

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as JibetApp).repository

    /** ماه شمسی انتخاب‌شده */
    private val today = JDate.toJalali(LocalDate.now())
    val jy = MutableStateFlow(today.jy)
    val jm = MutableStateFlow(today.jm)

    /**
     * بودجه‌های ماه جاری با مصرف‌شده — null یعنی در حال بارگذاری
     * (با هر تغییر ماه ابتدا null منتشر می‌شود، مثل وب).
     */
    val budgets: StateFlow<List<BudgetRow>?> =
        combine(jy, jm) { y, m -> y to m }
            .flatMapLatest { (y, m) ->
                flow<List<BudgetRow>?> {
                    emit(null)
                    emitAll(repo.watchBudgets(y, m))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** دسته‌های هزینه */
    val expenseCats: StateFlow<List<CategoryEntity>> =
        repo.watchCategories()
            .map { cats -> cats.filter { it.kind == Kind.EXPENSE } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** «مهر ۱۴۰۴» */
    fun monthLabel(): String = "${JDate.J_MONTHS[jm.value - 1]} ${Fmt.faDigits(jy.value)}"

    fun shiftMonth(dir: Int) {
        val n = JDate.addJalaliMonths(jy.value, jm.value, dir)
        jy.value = n.jy
        jm.value = n.jm
    }

    /** ثبت/به‌روزرسانی بودجه + توست موفقیت (مثل save وب) */
    fun saveBudget(categoryId: Long, amount: Double, isEdit: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.addBudget(categoryId, jy.value, jm.value, amount)
            val title = if (isEdit) "بودجه به‌روزرسانی شد" else "بودجه ثبت شد"
            Toast.makeText(
                getApplication(),
                "$title\n${monthLabel()}",
                Toast.LENGTH_SHORT,
            ).show()
            onDone()
        }
    }

    /** حذف بودجه + توست (مثل doDelete وب) */
    fun deleteBudget(id: Long, onDone: () -> Unit) {
        val row = budgets.value?.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repo.deleteBudget(
                BudgetEntity(
                    id = row.id,
                    categoryId = row.categoryId,
                    jYear = row.jYear,
                    jMonth = row.jMonth,
                    amount = row.amount,
                )
            )
            Toast.makeText(getApplication(), "بودجه حذف شد", Toast.LENGTH_SHORT).show()
            onDone()
        }
    }
}

/* ================= صفحه‌ی بودجه‌بندی ================= */

/** صفحه‌ی بودجه‌بندی — پورت ۱:۱ از screens/budget.tsx نسخه وب */
@Composable
fun BudgetScreen() {
    // وب با dir=rtl رندر می‌شود — برای یکسانی کامل، چیدمان این صفحه راست‌به‌چپ است
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val vm: BudgetViewModel = viewModel()
        val jy by vm.jy.collectAsState()
        val jm by vm.jm.collectAsState()
        val budgets by vm.budgets.collectAsState()
        val expenseCats by vm.expenseCats.collectAsState()
        val jc = LocalJibetColors.current

        var sheetOpen by remember { mutableStateOf(false) }
        var editing by remember { mutableStateOf<BudgetRow?>(null) }
        var confirmId by remember { mutableStateOf<Long?>(null) }
        var busy by remember { mutableStateOf(false) }

        val items = budgets
        val list = items ?: emptyList()
        val totalAmount = list.sumOf { it.amount }
        val totalSpent = list.sumOf { it.spent }
        val remaining = maxOf(0.0, totalAmount - totalSpent)
        val totalPct = if (totalAmount > 0) (totalSpent / totalAmount * 100.0).toFloat() else 0f
        val monthLabel = "${JDate.J_MONTHS[jm - 1]} ${Fmt.faDigits(jy)}"

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(
                title = "بودجه‌بندی",
                action = {
                    GlassIconButton(
                        onClick = {
                            editing = null
                            sheetOpen = true
                        },
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "افزودن بودجه",
                            tint = jc.textMid,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )

            // انتخابگر ماه
            Row(
                Modifier
                    .fillMaxWidth()
                    .jibetGlass(shape = RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavCircleButton(icon = Icons.Rounded.ChevronRight, desc = "ماه قبل") {
                    confirmId = null
                    vm.shiftMonth(-1)
                }
                AnimatedContent(
                    targetState = monthLabel,
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                    transitionSpec = {
                        (slideInVertically(tween(150)) { it / 4 } + fadeIn(tween(150))) togetherWith
                            (slideOutVertically(tween(150)) { -it / 4 } + fadeOut(tween(150)))
                    },
                    label = "monthLabel",
                ) { text ->
                    Text(
                        text,
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = jc.ink),
                        maxLines = 1,
                    )
                }
                NavCircleButton(icon = Icons.Rounded.ChevronLeft, desc = "ماه بعد") {
                    confirmId = null
                    vm.shiftMonth(1)
                }
            }

            // محتوا — هنگام بارگذاری اسکلت و بعد کارت‌ها
            if (items == null) {
                ShimmerCard(height = 224.dp, modifier = Modifier.padding(top = 16.dp))
            } else {
                val enter = remember(jy, jm) { Animatable(0f) }
                LaunchedEffect(jy, jm) {
                    enter.animateTo(1f, tween(220, easing = EaseOut))
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .graphicsLayer {
                            alpha = enter.value
                            translationY = 10.dp.toPx() * (1f - enter.value)
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        list.isEmpty() && expenseCats.isEmpty() -> {
                            Box(Modifier.fillMaxWidth().jibetCard()) {
                                EmptyState(
                                    icon = Icons.Rounded.Wallet,
                                    title = "دسته‌ی هزینه‌ای ندارید",
                                    description = "برای بودجه‌بندی، اول از بخش «بیشتر → دسته‌ها» یک دسته‌ی هزینه بسازید.",
                                )
                            }
                        }
                        list.isEmpty() -> {
                            Box(Modifier.fillMaxWidth().jibetCard()) {
                                Column {
                                    EmptyState(
                                        icon = Icons.Rounded.Wallet,
                                        title = "بودجه‌ای تعیین نشده",
                                        description = "برای هر دسته سقف هزینه‌ی ماهانه بگذارید تا خرجتان کنترل شود.",
                                    )
                                    AccentButton(
                                        text = "تعیین اولین بودجه",
                                        onClick = {
                                            editing = null
                                            sheetOpen = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 32.dp)
                                            .padding(bottom = 24.dp),
                                    )
                                }
                            }
                        }
                        else -> {
                            // کارت کل بودجه
                            TotalCard(
                                count = list.size,
                                totalAmount = totalAmount,
                                totalSpent = totalSpent,
                                remaining = remaining,
                                totalPct = totalPct,
                            )
                            // کارت هر دسته
                            list.forEachIndexed { index, item ->
                                BudgetCard(
                                    item = item,
                                    index = index,
                                    confirming = confirmId == item.id,
                                    busy = busy,
                                    onEdit = {
                                        editing = item
                                        sheetOpen = true
                                    },
                                    onAskDelete = { confirmId = item.id },
                                    onConfirmDelete = {
                                        if (!busy) {
                                            busy = true
                                            vm.deleteBudget(item.id) {
                                                busy = false
                                                confirmId = null
                                            }
                                        }
                                    },
                                    onCancelDelete = { confirmId = null },
                                )
                            }
                        }
                    }
                }
            }
        }

        // شیت افزودن/ویرایش بودجه
        if (sheetOpen) {
            BudgetSheet(
                editing = editing,
                jy = jy,
                jm = jm,
                cats = expenseCats,
                existingIds = list.map { it.categoryId }.toSet(),
                onClose = { sheetOpen = false },
                onSave = { categoryId, amount, isEdit, onDone ->
                    vm.saveBudget(categoryId, amount, isEdit, onDone)
                },
            )
        }
    }
}

/* ================= کارت کل بودجه ================= */

@Composable
private fun TotalCard(
    count: Int,
    totalAmount: Double,
    totalSpent: Double,
    remaining: Double,
    totalPct: Float,
) {
    val jc = LocalJibetColors.current
    Column(Modifier.fillMaxWidth().jibetCard().padding(20.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "کل بودجه",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.inkSoft),
            )
            Text(
                "${Fmt.faDigits(count)} دسته",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = jc.inkSoft, fontFeatureSettings = "tnum"),
            )
        }
        Row(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                Fmt.money(totalAmount),
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 32.sp, color = jc.ink, fontFeatureSettings = "tnum"),
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                "تومان",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = jc.inkSoft),
                modifier = Modifier.alignByBaseline(),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.weight(1f)) {
                JibetProgress(pct = totalPct, color = jc.accentA)
            }
            Text(
                Fmt.percent(totalPct.toDouble()),
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp, color = jc.budgetBarColor(totalPct), fontFeatureSettings = "tnum"),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "هزینه‌شده ",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
                )
                Text(
                    Fmt.money(totalSpent),
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = jc.ink, fontFeatureSettings = "tnum"),
                )
                Text(
                    " تومان",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "باقی‌مانده ",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
                )
                Text(
                    Fmt.money(remaining),
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = jc.ink, fontFeatureSettings = "tnum"),
                )
                Text(
                    " تومان",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
                )
            }
        }
    }
}

/* ================= کارت هر دسته ================= */

@Composable
private fun BudgetCard(
    item: BudgetRow,
    index: Int,
    confirming: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onAskDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    val jc = LocalJibetColors.current
    val pct = if (item.amount > 0) (item.spent / item.amount * 100.0).toFloat() else 0f
    val barColor = jc.budgetBarColor(pct)
    val appear = remember(item.id) { Animatable(0f) }
    LaunchedEffect(item.id) {
        appear.animateTo(1f, tween(200, delayMillis = minOf(index * 40, 240), easing = EaseOut))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .jibetCard()
            .graphicsLayer {
                alpha = appear.value
                translationY = 8.dp.toPx() * (1f - appear.value)
            }
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(
                icon = item.catIcon ?: "more-horizontal",
                color = parseColor(item.catColor),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    item.catName ?: "دسته",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = jc.ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Fmt.money(item.spent),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft, fontFeatureSettings = "tnum"),
                        maxLines = 1,
                    )
                    Text(
                        " / ${Fmt.money(item.amount)}",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft, fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        modifier = Modifier.alpha(0.7f),
                    )
                    Text(
                        " تومان",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
                        maxLines = 1,
                    )
                }
            }
            if (confirming) {
                // تأیید دومرحله‌ای حذف — مثل وب
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "حذف؟",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = jc.inkSoft),
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(jc.expense)
                            .alpha(if (busy) 0.5f else 1f)
                            .pressableScale(onClick = if (busy) null else onConfirmDelete, pressedScale = 0.95f, role = Role.Button)
                            .padding(horizontal = 10.dp)
                            .height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "بله",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White),
                        )
                    }
                    Box(
                        Modifier
                            .shadow(2.dp, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .background(jc.inputBg)
                            .pressableScale(onClick = onCancelDelete, pressedScale = 0.95f, role = Role.Button)
                            .padding(horizontal = 10.dp)
                            .height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "خیر",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = jc.inkSoft),
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        Fmt.percent(pct.toDouble()),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = barColor, fontFeatureSettings = "tnum"),
                    )
                    Box(
                        Modifier
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(jc.inputBg)
                            .pressableScale(onClick = onEdit, pressedScale = 0.94f, role = Role.Button)
                            .size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "ویرایش بودجه",
                            tint = jc.inkSoft,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Box(
                        Modifier
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(jc.inputBg)
                            .pressableScale(onClick = onAskDelete, pressedScale = 0.94f, role = Role.Button)
                            .size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "حذف بودجه",
                            tint = jc.inkSoft,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
        JibetProgress(
            pct = pct,
            color = barColor,
            height = 7.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

/* ================= شیت افزودن/ویرایش بودجه ================= */

/** مقدار خام مبلغ برای کیپد — مثل String(amount) وب */
private fun rawAmount(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()

@Composable
private fun BudgetSheet(
    editing: BudgetRow?,
    jy: Int,
    jm: Int,
    cats: List<CategoryEntity>,
    existingIds: Set<Long>,
    onClose: () -> Unit,
    onSave: (categoryId: Long, amount: Double, isEdit: Boolean, onDone: () -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val jc = LocalJibetColors.current

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() }
    }

    // بازنشانی وضعیت هنگام بازشدن شیت — شیت فقط هنگام باز بودن در composition است
    var amount by remember { mutableStateOf(editing?.let { rawAmount(it.amount) } ?: "") }
    var catId by remember { mutableStateOf(editing?.categoryId) }
    var saving by remember { mutableStateOf(false) }

    // در حالت افزودن فقط دسته‌های بدون بودجه؛ در ویرایش همه با تیکِ دارای‌بودجه
    val available = if (editing != null) cats else cats.filter { it.id !in existingIds }
    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val canSave = catId != null && amount.isNotEmpty() && amountValue > 0.0 && !saving

    JibetSheet(
        onDismiss = { close() },
        sheetState = sheetState,
        title = if (editing != null) "ویرایش بودجه" else "افزودن بودجه",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "برای ${JDate.J_MONTHS[jm - 1]} ${Fmt.faDigits(jy)}",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
            )

            // نمایش مبلغ
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
                    "مبلغ بودجه",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.inkSoft),
                )
                // مثل dir=ltr وب: عدد سمت چپ و «تومان» سمت راست آن
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (amount.isEmpty()) "۰" else Fmt.money(amountValue),
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = if (amount.isEmpty()) jc.faint else jc.ink, fontFeatureSettings = "tnum"),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "تومان",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = jc.inkSoft),
                        )
                    }
                }
            }

            // انتخاب دسته
            Column {
                Text(
                    "دسته‌بندی (هزینه)",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.textMid),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                when {
                    cats.isEmpty() -> SheetHint("اول از بخش «دسته‌ها» یک دسته‌ی هزینه بسازید")
                    available.isEmpty() -> SheetHint("همه‌ی دسته‌های هزینه در این ماه بودجه دارند")
                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        available.chunked(3).forEach { rowCats ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowCats.forEach { category ->
                                    CategoryCell(
                                        category = category,
                                        active = category.id == catId,
                                        hasBudget = existingIds.contains(category.id),
                                        onClick = { catId = category.id },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(3 - rowCats.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            // کیپد مبلغ
            JibetKeypad(value = amount, onChange = { amount = it })

            AccentButton(
                text = if (saving) "در حال ذخیره…" else if (editing != null) "ذخیره‌ی تغییرات" else "ثبت بودجه",
                onClick = {
                    val id = catId ?: return@AccentButton
                    saving = true
                    onSave(id, amountValue, editing != null) {
                        saving = false
                        close()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
            )
        }
    }
}

/** راهنمای داخل شیت — bg-white/70 rounded-2xl مثل وب */
@Composable
private fun SheetHint(text: String) {
    val jc = LocalJibetColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(jc.cardSoft)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = jc.inkSoft),
            textAlign = TextAlign.Center,
        )
    }
}

/** خانه‌ی انتخاب دسته — grid-cols-3 مثل وب */
@Composable
private fun CategoryCell(
    category: CategoryEntity,
    active: Boolean,
    hasBudget: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val jc = LocalJibetColors.current
    val color = parseColor(category.colorHex)
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(
                    if (active) {
                        Modifier
                            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = color.copy(alpha = 0.8f), ambientColor = Color.Transparent)
                            .clip(RoundedCornerShape(16.dp))
                            .background(color)
                    } else {
                        Modifier
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(jc.inputBg)
                    }
                )
                .pressableScale(onClick = onClick, pressedScale = 0.96f)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconBubble(
                icon = category.icon,
                color = if (active) Color.White else color,
                size = 34.dp,
                iconSize = 17.dp,
            )
            Text(
                category.name,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, lineHeight = 16.sp, color = if (active) Color.White else jc.textMid),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (hasBudget) {
            // بج دارای‌بودجه — چپِ فیزیکی مثل left-1.5 وب
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(jc.income),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "دارای بودجه",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

/* ================= اجزای مشترک محلی ================= */

/** دکمه‌ی دایره‌ای ناوبری ماه — h-9 w-9 rounded-xl bg-white/80 مثل وب */
@Composable
private fun NavCircleButton(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
) {
    val jc = LocalJibetColors.current
    Box(
        Modifier
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(jc.cardSoft)
            .pressableScale(onClick = onClick, pressedScale = 0.95f, role = Role.Button)
            .size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = jc.textMid,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** اسکلت لودینگ — کارت سفید با آلفای تپنده (مثل animate-pulse وب) */
@Composable
private fun ShimmerCard(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val jc = LocalJibetColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    Box(modifier.fillMaxWidth().height(height).jibetCard()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(jc.skeleton.copy(alpha = alpha)),
        )
    }
}
