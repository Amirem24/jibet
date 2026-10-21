package com.jibet.app.ui.reports

import android.app.Application
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
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
import com.jibet.app.data.Kind
import com.jibet.app.data.ReportBucket
import com.jibet.app.data.ReportCategory
import com.jibet.app.data.ReportData
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.JibetChip
import com.jibet.app.ui.components.JibetSegmented
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.Fmt
import com.jibet.app.util.JDate
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/* ================= ViewModel — مثل منطق screens/reports.tsx وب ================= */

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as JibetApp).repository

    /** دوره: «month» / «week» / «year» */
    val period = MutableStateFlow("month")

    /** نوع گزارش: هزینه یا درآمد */
    val kind = MutableStateFlow(Kind.EXPENSE)

    /** تاریخ مرجع — به‌صورت epoch millis (شروع روز) */
    val refMillis = MutableStateFlow(JDate.millis(LocalDate.now()))

    /**
     * داده‌ی گزارش — واکنش‌گرا مثل وب؛ با هر تغییر دوره/بازه/نوع ابتدا null
     * (اسکلت لودینگ) و بعد داده‌ی تازه منتشر می‌شود.
     */
    val report: StateFlow<ReportData?> =
        combine(period, refMillis, kind) { p, r, k -> Triple(p, r, k) }
            .flatMapLatest { (p, r, k) ->
                flow<ReportData?> {
                    emit(null)
                    emitAll(repo.watchReport(p, r, k))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPeriod(value: String) {
        period.value = value
    }

    fun setKind(value: Kind) {
        kind.value = value
    }

    /** جابه‌جایی به دوره‌ی قبل/بعد — مثل shift وب */
    fun shift(dir: Int) {
        val ref = JDate.localDate(refMillis.value)
        val next = when (period.value) {
            "month" -> {
                val j = JDate.toJalali(ref)
                val n = JDate.addJalaliMonths(j.jy, j.jm, dir)
                JDate.toGregorian(n.jy, n.jm, 1)
            }
            "year" -> {
                val j = JDate.toJalali(ref)
                JDate.toGregorian(j.jy + dir, j.jm, 1)
            }
            else -> ref.plusDays(7L * dir)
        }
        refMillis.value = JDate.millis(next)
    }

    /** لیبل وسط ناوبری: «مهر ۱۴۰۴» / «۱۲ تا ۱۸ مهر» / «سال ۱۴۰۴» */
    fun periodLabel(period: String, refMillis: Long): String = when (period) {
        "month" -> {
            val j = JDate.toJalali(JDate.localDate(refMillis))
            "${JDate.J_MONTHS[j.jm - 1]} ${Fmt.faDigits(j.jy)}"
        }
        "year" -> {
            val j = JDate.toJalali(JDate.localDate(refMillis))
            "سال ${Fmt.faDigits(j.jy)}"
        }
        else -> {
            val (weekStart, weekEnd) = JDate.persianWeekRange(JDate.localDate(refMillis))
            val s = Fmt.faDigits(JDate.formatJalaliDate(weekStart, JDate.Style.SHORT))
            val e = Fmt.faDigits(JDate.formatJalaliDate(weekEnd, JDate.Style.SHORT))
            val sMonth = s.split(" ").drop(1).joinToString(" ")
            val eMonth = e.split(" ").drop(1).joinToString(" ")
            if (sMonth == eMonth) "${s.split(" ").first()} تا $e" else "$s تا $e"
        }
    }
}

/* ================= صفحه‌ی گزارش‌ها ================= */

/** قطعه‌ی دونات */
private data class DonutSlice(val color: String, val pct: Int)

/** صفحه‌ی گزارش‌ها — پورت ۱:۱ از screens/reports.tsx نسخه وب */
@Composable
fun ReportsScreen(onBack: () -> Unit) {
    // وب با dir=rtl رندر می‌شود — برای یکسانی کامل، چیدمان این صفحه راست‌به‌چپ است
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val vm: ReportsViewModel = viewModel()
        val period by vm.period.collectAsState()
        val kind by vm.kind.collectAsState()
        val refMillis by vm.refMillis.collectAsState()
        val report by vm.report.collectAsState()
        val jc = LocalJibetColors.current
        val label = vm.periodLabel(period, refMillis)
        val dataKey = "$period-$refMillis-$kind"

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(title = "گزارش‌ها", onBack = onBack)

            JibetSegmented(
                value = period,
                options = listOf("month" to "ماهانه", "week" to "هفتگی", "year" to "سالانه"),
                onChange = { vm.setPeriod(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            // ناوبری دوره
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .jibetGlass(shape = RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavCircleButton(icon = Icons.Rounded.ChevronRight, desc = "دوره قبل") {
                    vm.shift(-1)
                }
                AnimatedContent(
                    targetState = label,
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                    transitionSpec = {
                        (slideInVertically(tween(150)) { it / 4 } + fadeIn(tween(150))) togetherWith
                            (slideOutVertically(tween(150)) { -it / 4 } + fadeOut(tween(150)))
                    },
                    label = "periodLabel",
                ) { text ->
                    Text(
                        text,
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = jc.ink),
                        maxLines = 1,
                    )
                }
                NavCircleButton(icon = Icons.Rounded.ChevronLeft, desc = "دوره بعد") {
                    vm.shift(1)
                }
            }

            // نوع گزارش
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                JibetChip(
                    label = "هزینه",
                    active = kind == Kind.EXPENSE,
                    onClick = { vm.setKind(Kind.EXPENSE) },
                    color = jc.expense,
                )
                JibetChip(
                    label = "درآمد",
                    active = kind == Kind.INCOME,
                    onClick = { vm.setKind(Kind.INCOME) },
                    color = jc.income,
                )
            }

            // محتوا — هنگام بارگذاری اسکلت و بعد کارت‌ها
            val data = report
            if (data == null) {
                ShimmerCard(height = 288.dp, modifier = Modifier.padding(top = 16.dp))
            } else {
                val total = if (kind == Kind.EXPENSE) data.totalExpense else data.totalIncome
                val enter = remember(dataKey) { Animatable(0f) }
                LaunchedEffect(dataKey) {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // دونات + راهنمای دسته‌ها — دونات دقیقاً وسط کارت
                    Box(Modifier.fillMaxWidth().jibetCard().padding(20.dp)) {
                        if (total > 0) {
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Donut(
                                    slices = data.byCategory.map { DonutSlice(color = it.color, pct = it.pct) },
                                    total = total,
                                    kind = kind,
                                )
                                CategoryLegend(categories = data.byCategory)
                            }
                        } else {
                            EmptyState(
                                icon = Icons.Rounded.PieChart,
                                title = "در این دوره تراکنشی نبوده",
                                description = "برای دیدن گزارش، اول یک تراکنش ثبت کنید.",
                            )
                        }
                    }

                    // روند
                    if (total > 0) {
                        TrendCard(buckets = data.trend, period = period, kind = kind, label = label)
                    }

                    // جمع‌بندی دوره
                    SummaryCard(totalIncome = data.totalIncome, totalExpense = data.totalExpense)
                }
            }
        }
    }
}

/* ================= دونات ================= */

/** دونات — هر دسته یک قطعه با انیمیشن کشیده‌شدن (مثل Donut وب) */
@Composable
private fun Donut(
    slices: List<DonutSlice>,
    total: Double,
    kind: Kind,
) {
    val jc = LocalJibetColors.current
    // هندسه‌ی دونات مثل وب (r=54/stroke=22 در ۱۴۰) با مقیاس ۱٫۲۶× — بزرگ‌تر برای خوانایی موبایل
    val circumference = 2f * PI.toFloat() * 68f
    val minSliceDeg = 360f / circumference // ۱ واحد SVG ≈ ۱٫۰۶ درجه
    val gapDeg = if (slices.size > 1) 3f else 0f
    val signature = remember(slices) { slices.joinToString("|") { "${it.pct}-${it.color}" } }

    // ورود: چرخش ۹۸−→۹۰− + بزرگ‌نمایی ۰٫۹→۱ + شفافیت ۰→۱
    val rotation = remember { Animatable(-98f) }
    val scale = remember { Animatable(0.9f) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { rotation.animateTo(-90f, tween(350, easing = EaseOut)) }
        launch { scale.animateTo(1f, tween(350, easing = EaseOut)) }
        launch { reveal.animateTo(1f, tween(350, easing = EaseOut)) }
    }

    // هر قطعه با تاخیر ۶۰ms × اندیس از صفر کشیده می‌شود
    val sweeps = remember(signature) { List(slices.size) { Animatable(0f) } }
    LaunchedEffect(signature) {
        sweeps.forEachIndexed { index, anim ->
            launch {
                anim.snapTo(0f)
                anim.animateTo(1f, tween(300, delayMillis = 60 * index, easing = EaseOut))
            }
        }
    }

    Box(Modifier.size(176.dp)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation.value
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = reveal.value
                },
        ) {
            val radius = 68.dp.toPx()
            val stroke = 28.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)
            // خط راهنما — rgba(138,146,166,0.10)
            drawCircle(
                color = jc.track,
                radius = radius,
                center = center,
                style = Stroke(width = stroke),
            )
            var cumulative = 0f
            slices.forEachIndexed { index, slice ->
                val len = maxOf(slice.pct / 100f * 360f - gapDeg, minSliceDeg)
                val start = cumulative / 100f * 360f + gapDeg / 2f
                drawArc(
                    color = parseColor(slice.color),
                    startAngle = -90f + start,
                    sweepAngle = len * sweeps[index].value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                cumulative += slice.pct
            }
        }
        // وسط دونات
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "مجموع ${if (kind == Kind.EXPENSE) "هزینه‌ها" else "درآمدها"}",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, color = jc.inkSoft),
            )
            Text(
                Fmt.money(total),
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = 19.sp, lineHeight = 24.sp, color = jc.ink, fontFeatureSettings = "tnum"),
            )
            Text(
                "تومان",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = jc.inkSoft),
            )
        }
    }
}

/** راهنمای دسته‌ها زیر دونات */
@Composable
private fun CategoryLegend(categories: List<ReportCategory>) {
    val jc = LocalJibetColors.current
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(jc.divider),
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            categories.forEach { category ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(parseColor(category.color)),
                    )
                    IconBubble(
                        icon = category.icon,
                        color = parseColor(category.color),
                        size = 30.dp,
                        iconSize = 15.dp,
                    )
                    Text(
                        category.name,
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = jc.ink),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        Fmt.money(category.total),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = jc.ink, fontFeatureSettings = "tnum"),
                    )
                    Text(
                        Fmt.percent(category.pct.toDouble()),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = jc.inkSoft, fontFeatureSettings = "tnum"),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(44.dp),
                    )
                }
            }
        }
    }
}

/* ================= روند ================= */

/** نمودار میله‌ای روند هزینه/درآمد در دوره */
@Composable
private fun TrendCard(
    buckets: List<ReportBucket>,
    period: String,
    kind: Kind,
    label: String,
) {
    val jc = LocalJibetColors.current
    val barColor = if (kind == Kind.EXPENSE) jc.expense else jc.income
    val values = buckets.map { if (kind == Kind.EXPENSE) it.expense else it.income }
    val max = maxOf(values.maxOrNull() ?: 1.0, 1.0)

    fun labelFor(index: Int): String {
        val bucket = buckets[index]
        if (period == "week") return bucket.label
        if (period == "year") return bucket.label.take(2) // دو حرف اول ماه
        val day = bucket.key.split("-").getOrNull(2)?.toIntOrNull() ?: 0
        return if (day % 5 == 0 || index == buckets.lastIndex) Fmt.faDigits(day) else ""
    }

    Box(Modifier.fillMaxWidth().jibetCard().padding(20.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "روند ${if (kind == Kind.EXPENSE) "هزینه" else "درآمد"}",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = jc.ink),
                )
                Text(
                    label,
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = jc.inkSoft),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                buckets.forEachIndexed { index, bucket ->
                    val value = values[index]
                    val barHeight = maxOf(4, (value / max * 90.0).roundToInt()).dp
                    val grow = remember(bucket.key) { Animatable(0f) }
                    LaunchedEffect(bucket.key) {
                        grow.animateTo(1f, tween(280, delayMillis = minOf(index * 15, 200), easing = EaseOut))
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                Modifier
                                    .widthCap(18.dp)
                                    .height(barHeight)
                                    .graphicsLayer {
                                        scaleY = grow.value
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                        alpha = if (value > 0) 1f else 0.3f
                                    }
                                    .clip(RoundedCornerShape(50))
                                    .background(barColor),
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                labelFor(index).ifEmpty { "\u200C" },
                                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 9.sp, lineHeight = 12.sp, color = jc.inkSoft),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ================= جمع‌بندی دوره ================= */

@Composable
private fun SummaryCard(totalIncome: Double, totalExpense: Double) {
    val jc = LocalJibetColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .jibetCard()
            .height(IntrinsicSize.Min)
            .padding(16.dp),
    ) {
        // درآمد دوره
        Row(
            Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(jc.incomeSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = jc.income,
                    modifier = Modifier.size(17.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "درآمد دوره",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = jc.inkSoft),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Fmt.money(totalIncome),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, color = jc.ink, fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "تومان",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = jc.inkSoft),
                    )
                }
            }
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(jc.divider),
        )
        // هزینه دوره
        Row(
            Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(jc.expenseSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Remove,
                    contentDescription = null,
                    tint = jc.expense,
                    modifier = Modifier.size(17.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "هزینه دوره",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = jc.inkSoft),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Fmt.money(totalExpense),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, color = jc.ink, fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "تومان",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = jc.inkSoft),
                    )
                }
            }
        }
    }
}

/* ================= اجزای مشترک محلی ================= */

/** دکمه‌ی دایره‌ای ناوبری دوره — h-9 w-9 rounded-xl bg-white/80 مثل وب */
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

/** عرض = min(عرض در دسترس، سقف) — معادل w-full max-w-[18px] وب */
private fun Modifier.widthCap(cap: Dp): Modifier = layout { measurable, constraints ->
    val width = minOf(constraints.maxWidth, cap.roundToPx()).coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(width, placeable.height) { placeable.placeRelative(0, 0) }
}
