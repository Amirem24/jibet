package com.jibet.app.ui.home

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.jibet.app.JibetApp
import com.jibet.app.UiBus
import com.jibet.app.data.AccountRow
import com.jibet.app.data.Kind
import com.jibet.app.data.Summary
import com.jibet.app.data.TxRow
import com.jibet.app.ui.components.AmountText
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassIconButton
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.accentGradient
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.Fmt
import com.jibet.app.util.JDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** ویومدل خانه — جریان‌های مخزن با مقادیر اولیه مثل قرارداد */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as JibetApp).repository

    val accounts: StateFlow<List<AccountRow>> = repo.watchAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalBalance: StateFlow<Double> = repo.watchTotalBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val summary: StateFlow<Summary> = repo.watchSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Summary(0.0, 0.0, 0.0, 0.0))

    /** آخرین تراکنش‌ها — null تا نخستین انتشار (اسکلتون بارگذاری) */
    val recentTx: StateFlow<List<TxRow>?> = repo.watchTxPage(
        q = "",
        kind = null,
        accountId = 0L,
        categoryId = 0L,
        from = 0L,
        to = 0L,
        limit = 6,
        offset = 0,
    )
        .map { it.items }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** ورود نرم هر بخش — مثل FadeIn وب (opacity + y، ۲۲۰ms easeOut) با حفظ فضا در لی‌اوت */
@Composable
private fun FadeIn(delayMs: Int, modifier: Modifier = Modifier, offsetY: Dp = 10.dp, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(durationMillis = 220, easing = LinearOutSlowInEasing))
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = offsetY.toPx() * (1f - progress.value)
        }
    ) { content() }
}

/** عدد موجودی با شمارش نرم ۵۵۰ms — مثل CountUpMoney وب */
@Composable
private fun CountUpMoney(value: Double, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(value) {
        if (value != 0.0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 550, easing = LinearOutSlowInEasing))
        }
    }
    Text(
        Fmt.money(value * progress.value),
        style = TextStyle(
            fontFamily = Vazir,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            color = Color.White,
            fontFeatureSettings = "tnum",
        ),
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(onPush: (String) -> Unit, onTab: (String) -> Unit) {
    val vm: HomeViewModel = viewModel()
    val accounts by vm.accounts.collectAsState()
    val totalBalance by vm.totalBalance.collectAsState()
    val summary by vm.summary.collectAsState()
    val recent by vm.recentTx.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(onPush = onPush)

        FadeIn(0) { BalanceCard(totalBalance = totalBalance) }

        if (accounts.isNotEmpty()) {
            FadeIn(40) { AccountsCard(accounts = accounts, onOpen = { onPush("accounts") }) }
        }

        FadeIn(80) { TodayCard(summary = summary) }

        FadeIn(120) { QuickActions(onTab = onTab, onPush = onPush) }

        FadeIn(160) { RecentCard(recent = recent, onTab = onTab) }
    }
}

/* ================= هدر ================= */

@Composable
private fun Header(onPush: (String) -> Unit) {
    val jc = LocalJibetColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "جیبت",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 32.sp, color = jc.ink),
            )
            Text(
                "خلاصه‌ی امروز",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassIconButton(onClick = { onPush("search") }) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "جستجو",
                    tint = jc.textMid,
                    modifier = Modifier.size(19.dp),
                )
            }
            GlassIconButton(onClick = { onPush("reports") }) {
                Icon(
                    Icons.Rounded.BarChart,
                    contentDescription = "گزارش‌ها",
                    tint = jc.textMid,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

/* ================= موجودی کل ================= */

@Composable
private fun BalanceCard(totalBalance: Double) {
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color(0x8C5B74E9),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(26.dp))
            .background(accentGradient())
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f))
                ),
                RoundedCornerShape(26.dp),
            )
            .padding(20.dp),
    ) {
        // حباب‌های نور تزئینی — مثل blur-2xl وب
        Box(
            Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = (-40).dp, y = (-56).dp)
                .size(160.dp)
                .blur(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
        )
        Box(
            Modifier
                .align(AbsoluteAlignment.BottomRight)
                .absoluteOffset(x = 24.dp, y = 64.dp)
                .size(176.dp)
                .blur(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "موجودی کل",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f)),
                )
                Row(Modifier.padding(top = 10.dp)) {
                    CountUpMoney(value = totalBalance, modifier = Modifier.alignByBaseline())
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "تومان",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f)),
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
                        ),
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AccountBalanceWallet,
                    contentDescription = "کیف پول",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

/* ================= حساب‌ها ================= */

@Composable
private fun AccountsCard(accounts: List<AccountRow>, onOpen: () -> Unit) {
    val jc = LocalJibetColors.current
    Column(Modifier.jibetCard().padding(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .pressableScale(onClick = onOpen)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "حساب‌ها",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.inkSoft),
            )
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = jc.faint,
                modifier = Modifier.size(17.dp),
            )
        }
        accounts.forEachIndexed { index, a ->
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(jc.divider)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressableScale(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBubble(icon = a.icon, color = parseColor(a.colorHex))
                Text(
                    a.name,
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = jc.ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Fmt.money(a.balance),
                        style = TextStyle(
                            fontFamily = Vazir,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = jc.ink,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "تومان",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = jc.inkSoft),
                    )
                }
            }
        }
    }
}

/* ================= امروز ================= */

@Composable
private fun TodayCard(summary: Summary) {
    val jc = LocalJibetColors.current
    Column(Modifier.jibetCard().padding(16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "امروز",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.inkSoft),
            )
            Text(
                Fmt.faDigits(JDate.formatJalaliDate(System.currentTimeMillis(), JDate.Style.SHORT)),
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = jc.faint),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayStat(kind = Kind.EXPENSE, amount = summary.todayExpense, modifier = Modifier.weight(1f))
            DayStat(kind = Kind.INCOME, amount = summary.todayIncome, modifier = Modifier.weight(1f))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(1.dp)
                .background(jc.divider),
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "این ماه",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MonthStat(amount = summary.monthIncome, isIncome = true)
                MonthStat(amount = summary.monthExpense, isIncome = false)
            }
        }
    }
}

@Composable
private fun DayStat(kind: Kind, amount: Double, modifier: Modifier = Modifier) {
    val jc = LocalJibetColors.current
    val isIncome = kind == Kind.INCOME
    val color = if (isIncome) jc.income else jc.expense
    val soft = if (isIncome) jc.incomeSoft else jc.expenseSoft
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(soft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isIncome) Icons.Rounded.Add else Icons.Rounded.Remove,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Text(
                if (isIncome) "درآمد" else "هزینه",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = jc.inkSoft),
            )
            Row(Modifier.padding(top = 2.dp)) {
                Text(
                    Fmt.money(amount),
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = color),
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "تومان",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = jc.inkSoft),
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Composable
private fun MonthStat(amount: Double, isIncome: Boolean) {
    val jc = LocalJibetColors.current
    val color = if (isIncome) jc.income else jc.expense
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            if (isIncome) Icons.Rounded.Add else Icons.Rounded.Remove,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(11.dp),
        )
        Text(
            Fmt.money(amount),
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color),
        )
    }
}

/* ================= اکشن‌های سریع ================= */

@Composable
private fun QuickActions(onTab: (String) -> Unit, onPush: (String) -> Unit) {
    val jc = LocalJibetColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickAction(
            label = "هزینه",
            icon = Icons.Rounded.Remove,
            color = jc.expense,
            bg = jc.expenseSoft,
            onTap = { UiBus.openAdd(Kind.EXPENSE) },
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            label = "درآمد",
            icon = Icons.Rounded.Add,
            color = jc.income,
            bg = jc.incomeSoft,
            onTap = { UiBus.openAdd(Kind.INCOME) },
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            label = "بودجه",
            icon = Icons.Rounded.PieChart,
            color = jc.accentA,
            bg = jc.accentA.copy(alpha = 0.12f),
            onTap = { onTab("budget") },
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            label = "هدف‌ها",
            icon = Icons.Rounded.GpsFixed,
            color = jc.accentB,
            bg = jc.accentB.copy(alpha = 0.13f),
            onTap = { onPush("goals") },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    color: Color,
    bg: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .jibetGlass(shape = RoundedCornerShape(20.dp))
            .pressableScale(onClick = onTap, pressedScale = 0.94f, role = Role.Button)
            .height(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            label,
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/* ================= آخرین تراکنش‌ها ================= */

@Composable
private fun RecentCard(recent: List<TxRow>?, onTab: (String) -> Unit) {
    val jc = LocalJibetColors.current
    Column(Modifier.jibetCard().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "آخرین تراکنش‌ها",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = jc.ink),
            )
            Row(
                Modifier.pressableScale(onClick = { onTab("transactions") }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "همه",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = jc.accentA),
                )
                Icon(
                    Icons.Rounded.ChevronLeft,
                    contentDescription = null,
                    tint = jc.accentA,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        val recentList = recent
        when {
            recentList == null -> RecentSkeleton()
            recentList.isEmpty() -> {
                Column(Modifier.padding(bottom = 8.dp)) {
                    EmptyState(title = "هنوز تراکنشی ثبت نشده")
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            Modifier
                                .shadow(
                                    12.dp,
                                    CircleShape,
                                    spotColor = Color(0x995B74E9),
                                    ambientColor = Color.Transparent,
                                )
                                .clip(CircleShape)
                                .background(accentGradient())
                                .pressableScale(onClick = { UiBus.openAdd() })
                                .padding(horizontal = 24.dp)
                                .height(44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                "ثبت اولین تراکنش",
                                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White),
                            )
                        }
                    }
                }
            }
            else -> {
                Column(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)) {
                    recentList.forEachIndexed { index, tx ->
                        if (index > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(jc.divider)
                            )
                        }
                        // ورود پله‌ای ردیف‌ها — ۳۰ms فاصله مثل staggerChildren وب
                        FadeIn(delayMs = index * 30) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressableScale(onClick = { UiBus.openDetail(tx.id) })
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IconBubble(
                                    icon = tx.catIcon ?: "more-horizontal",
                                    color = parseColor(tx.catColor),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        tx.title,
                                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = jc.ink),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${tx.accName ?: "حساب"} · ${Fmt.faDigits(JDate.formatJalaliDate(tx.date, JDate.Style.SHORT))}",
                                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                AmountText(
                                    amount = tx.amount,
                                    kind = tx.kind,
                                    suffix = "",
                                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** اسکلتون بارگذاری — ۳ ردیف با حباب و میله‌های اسکلت */
@Composable
private fun RecentSkeleton() {
    val jc = LocalJibetColors.current
    Column(
        Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(jc.skeleton)
                )
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .width(112.dp)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(jc.skeleton)
                    )
                    Box(
                        Modifier
                            .width(80.dp)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(jc.skeleton)
                    )
                }
                Box(
                    Modifier
                        .width(64.dp)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(jc.skeleton)
                )
            }
        }
    }
}
