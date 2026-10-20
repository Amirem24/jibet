package com.jibet.app.ui.search

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.jibet.app.data.TxPage
import com.jibet.app.ui.components.AccentButton
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.JibetChip
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.TxGroupedList
import com.jibet.app.ui.components.TxSummaryLine
import com.jibet.app.ui.components.groupTxsByDay
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.InkSoft
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.TextMid
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.Fmt
import com.jibet.app.util.JDate
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/* ================= دوره‌های زمانی — مثل وب ================= */

private val PERIODS: List<Pair<String, String>> = listOf(
    "all" to "همه",
    "thisMonth" to "این ماه",
    "lastMonth" to "ماه قبل",
    "last3" to "۳ ماه اخیر",
)

/* ================= ViewModel ================= */

/** مدل نمایشی صفحه‌ی جستجو و فیلتر — پورت ۱:۱ منطق search.tsx */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = getApplication<JibetApp>().repository

    val q = MutableStateFlow("")
    val kind = MutableStateFlow<Kind?>(null)
    val accountId = MutableStateFlow(0L)
    val categoryId = MutableStateFlow(0L)
    val period = MutableStateFlow("all")
    val loading = MutableStateFlow(false)
    val applied = MutableStateFlow(false)
    val result = MutableStateFlow<TxPage?>(null)

    val accounts: StateFlow<List<AccountRow>> = repo.watchAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repo.watchCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** بازه‌ی ماه شمسی (jy, jm) به میلادی */
    private fun jMonthRange(parts: JDate.JDateParts): Pair<Long, Long> {
        val start = JDate.startOfDay(JDate.toGregorian(parts.jy, parts.jm, 1))
        val end = JDate.endOfDay(
            JDate.toGregorian(parts.jy, parts.jm, JDate.jalaliMonthLength(parts.jy, parts.jm)),
        )
        return start to end
    }

    /** بازه‌ی زمانی انتخابی → (از، تا) — ۰ یعنی بدون فیلتر */
    fun periodRange(p: String): Pair<Long, Long> {
        if (p == "all") return 0L to 0L
        val now = LocalDate.now()
        val j = JDate.toJalali(now)
        return when (p) {
            "thisMonth" -> {
                val (s, e, _) = JDate.jalaliMonthRange(now)
                s to e
            }
            "lastMonth" -> jMonthRange(JDate.addJalaliMonths(j.jy, j.jm, -1))
            else -> {
                // ۳ ماه اخیر: از ابتدای دو ماه قبل تا پایان ماه جاری
                val m2 = JDate.addJalaliMonths(j.jy, j.jm, -2)
                jMonthRange(JDate.JDateParts(m2.jy, m2.jm, 1)).first to
                    jMonthRange(JDate.JDateParts(j.jy, j.jm, 1)).second
            }
        }
    }

    /** اعمال فیلتر — حداکثر ۲۰۰ نتیجه، مثل apply وب */
    fun apply() {
        loading.value = true
        applied.value = true
        viewModelScope.launch {
            try {
                val (from, to) = periodRange(period.value)
                result.value = repo.watchTxPage(
                    q = q.value.trim(),
                    kind = kind.value,
                    accountId = accountId.value,
                    categoryId = categoryId.value,
                    from = from,
                    to = to,
                    limit = 200,
                    offset = 0,
                ).first()
            } catch (_: Exception) {
                result.value = TxPage(emptyList(), 0, 0.0, 0.0)
            } finally {
                loading.value = false
            }
        }
    }

    /** پاک کردن فیلترها — مثل reset وب */
    fun reset() {
        q.value = ""
        kind.value = null
        accountId.value = 0L
        categoryId.value = 0L
        period.value = "all"
        applied.value = false
        result.value = null
        loading.value = false
    }
}

/* ================= صفحه ================= */

/** صفحه‌ی جستجو و فیلتر پیشرفته — نتیجه با همان لیست گروه‌بندی روز */
@Composable
fun SearchScreen(onBack: () -> Unit) {
    val vm: SearchViewModel = viewModel()
    val jc = LocalJibetColors.current
    val q by vm.q.collectAsState()
    val kind by vm.kind.collectAsState()
    val accountId by vm.accountId.collectAsState()
    val categoryId by vm.categoryId.collectAsState()
    val period by vm.period.collectAsState()
    val loading by vm.loading.collectAsState()
    val applied by vm.applied.collectAsState()
    val result by vm.result.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()

    val range = remember(period) { vm.periodRange(period) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(title = "جستجو و فیلتر", onBack = onBack)

        // جستجوی متنی
        SearchField(
            value = q,
            onValueChange = { vm.q.value = it },
            placeholder = "جستجو در عنوان و یادداشت...",
            a11yLabel = "متن جستجو",
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // نوع
        FilterSectionLabel("نوع تراکنش")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JibetChip(label = "همه", active = kind == null, onClick = { vm.kind.value = null })
            JibetChip(
                label = "هزینه",
                active = kind == Kind.EXPENSE,
                color = jc.expense,
                onClick = { vm.kind.value = Kind.EXPENSE },
            )
            JibetChip(
                label = "درآمد",
                active = kind == Kind.INCOME,
                color = jc.income,
                onClick = { vm.kind.value = Kind.INCOME },
            )
        }

        // حساب
        FilterSectionLabel("حساب")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JibetChip(label = "همه حساب‌ها", active = accountId == 0L, onClick = { vm.accountId.value = 0L })
            accounts.forEach { a ->
                JibetChip(
                    label = a.name,
                    active = accountId == a.id,
                    onClick = { vm.accountId.value = if (vm.accountId.value == a.id) 0L else a.id },
                    color = parseColor(a.colorHex),
                )
            }
        }

        // دسته‌بندی
        FilterSectionLabel("دسته‌بندی")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JibetChip(label = "همه دسته‌ها", active = categoryId == 0L, onClick = { vm.categoryId.value = 0L })
            categories.forEach { c ->
                JibetChip(
                    label = c.name,
                    active = categoryId == c.id,
                    onClick = { vm.categoryId.value = if (vm.categoryId.value == c.id) 0L else c.id },
                    color = parseColor(c.colorHex),
                )
            }
        }

        // بازه زمانی
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "بازه زمانی",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextMid),
                modifier = Modifier.alignByBaseline(),
            )
            if (range.first != 0L) {
                Text(
                    "${Fmt.faDigits(JDate.formatJalaliDate(range.first, JDate.Style.SHORT))} تا " +
                        Fmt.faDigits(JDate.formatJalaliDate(range.second, JDate.Style.SHORT)),
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = InkSoft),
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PERIODS.forEach { p ->
                JibetChip(
                    label = p.second,
                    active = period == p.first,
                    onClick = { vm.period.value = p.first },
                    leadingIcon = Icons.Rounded.DateRange,
                )
            }
        }

        // دکمه‌ها
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .jibetGlass(shape = RoundedCornerShape(16.dp))
                    .pressableScale(onClick = { vm.reset() }, pressedScale = 0.98f)
                    .height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "پاک کردن",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextMid),
                )
            }
            AccentButton(
                text = if (loading) "" else "اعمال فیلتر",
                onClick = { vm.apply() },
                enabled = !loading,
                modifier = Modifier.weight(1.7f),
                leading = {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.FilterList,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                },
            )
        }

        // نتایج
        Column(Modifier.padding(top = 24.dp)) {
            if (!applied) {
                EmptyState(
                    title = "جستجو و فیلتر کن",
                    description = "بر اساس متن، نوع، حساب، دسته‌بندی و بازه‌ی زمانی بین تراکنش‌ها بگرد.",
                    icon = Icons.Rounded.FilterList,
                )
            } else if (loading) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        strokeWidth = 2.dp,
                        color = InkSoft,
                    )
                    Box(Modifier.width(8.dp))
                    Text(
                        "در حال جستجو…",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InkSoft),
                    )
                }
            } else {
                val res = result
                if (res == null || res.items.isEmpty()) {
                    EmptyState(
                        title = "نتیجه‌ای یافت نشد",
                        description = "فیلترها را تغییر بده و دوباره اعمال کن.",
                        icon = Icons.Rounded.SearchOff,
                    )
                } else {
                    // ورود نرم نتیجه — مثل motion.div وب (opacity + y 200ms)
                    FadeSlideIn {
                        TxSummaryLine(income = res.income, expense = res.expense, total = res.total)
                        TxGroupedList(
                            groups = groupTxsByDay(res.items),
                            onOpen = { UiBus.openDetail(it.id) },
                        )
                        // حد نصاب ۲۰۰ نتیجه — معادل result.total > result.items.length وب
                        if (res.total > res.items.size) {
                            Text(
                                "${Fmt.faDigits(res.items.size)} مورد از ${Fmt.faDigits(res.total)} مورد نمایش داده شد — فیلتر را محدودتر کن.",
                                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = InkSoft),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ================= اجزای محلی ================= */

/** نوار جستجوی شیشه‌ای — مثل input وب (h-11 rounded-full) */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    a11yLabel: String,
) {
    Box(
        modifier
            .fillMaxWidth()
            .jibetGlass(shape = RoundedCornerShape(50))
            .height(44.dp),
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = Color(0xFFA7AEC0),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(17.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, color = Ink),
            cursorBrush = SolidColor(Ink),
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = a11yLabel }
                .padding(start = 44.dp, end = 16.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, color = Color(0xFFA7AEC0)),
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** تیتر بخش فیلتر — مثل p.mb-2.px-1 وب */
@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text,
        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextMid),
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .padding(bottom = 8.dp),
    )
}

/** ورود نرم با محو شدن + لغزش ۱۰dp — مثل motion.div وب */
@Composable
private fun FadeSlideIn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(durationMillis = 200)) }
    Box(
        modifier.graphicsLayer {
            alpha = anim.value
            translationY = (1f - anim.value) * 10.dp.toPx()
        },
    ) {
        content()
    }
}
