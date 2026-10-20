@file:OptIn(ExperimentalCoroutinesApi::class)

package com.jibet.app.ui.transactions

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.UiBus
import com.jibet.app.data.AccountRow
import com.jibet.app.data.Kind
import com.jibet.app.data.TxPage
import com.jibet.app.data.TxRow
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.GlassIconButton
import com.jibet.app.ui.components.JibetChip
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.TxGroupedList
import com.jibet.app.ui.components.TxSummaryLine
import com.jibet.app.ui.components.groupTxsByDay
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.TextMid
import com.jibet.app.ui.theme.Vazir
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/* ================= ViewModel ================= */

/** مدل‌ نمایشی صفحه‌ی تراکنش‌ها — پورت ۱:۱ منطق transactions.tsx */
class TransactionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = getApplication<JibetApp>().repository

    /** متن خام جستجو */
    val qInput = MutableStateFlow("")

    /** جستجوی دیبانس‌شده (۳۵۰ms مثل وب) */
    private val _q = MutableStateFlow("")
    val q: StateFlow<String> = _q.asStateFlow()

    /** null = همه */
    val kind = MutableStateFlow<Kind?>(null)

    /** 0 = همه حساب‌ها */
    val accountId = MutableStateFlow(0L)

    val offset = MutableStateFlow(0)
    val loadingMore = MutableStateFlow(false)
    val loading = MutableStateFlow(true)

    /** آیتم‌های انباشته‌شده — معادل setItems وب */
    private val _loaded = MutableStateFlow<List<TxRow>>(emptyList())
    val items: StateFlow<List<TxRow>> = _loaded.asStateFlow()

    val accounts: StateFlow<List<AccountRow>> = repo.watchAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * صفحه‌ی فعلی — با تغییر فیلترها و هر تغییر داده‌ی دیتابیس دوباره منتشر می‌شود
     * (جریان‌های Room خودکار واکنش‌گرا هستند؛ معادل refetch وب روی تغییر استور)
     */
    val page: StateFlow<TxPage> = combine(_q, kind, accountId, offset) { q, k, acc, off ->
        PageParams(q, k, acc, off)
    }
        .flatMapLatest { p ->
            repo.watchTxPage(
                q = p.q,
                kind = p.kind,
                accountId = p.accountId,
                categoryId = 0,
                from = 0,
                to = 0,
                limit = PAGE,
                offset = p.offset,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxPage(emptyList(), 0, 0.0, 0.0))

    init {
        // دیبانس جستجو — ۳۵۰ms بعد از آخرین تایپ
        viewModelScope.launch {
            qInput.collectLatest { input ->
                delay(350)
                _q.value = input.trim()
            }
        }
        // ریست صفحه روی تغییر فیلترها — مثل setOffset(0) وب
        viewModelScope.launch {
            combine(_q, kind, accountId) { _, _, _ -> Unit }
                .drop(1)
                .collect {
                    offset.value = 0
                    loading.value = true
                }
        }
        // انباشت آیتم‌ها — offset=0 جایگزین لیست، offset>0 افزودن بدون تکرار
        viewModelScope.launch {
            page.collect { p ->
                if (offset.value == 0) {
                    _loaded.value = p.items
                } else {
                    val seen = _loaded.value.mapTo(HashSet()) { it.id }
                    _loaded.value = _loaded.value + p.items.filter { it.id !in seen }
                }
                if (loadingMore.value) loadingMore.value = false
                loading.value = false
            }
        }
    }

    /** نمایش بیشتر — مثل loadMore وب */
    fun loadMore() {
        if (loadingMore.value) return
        if (_loaded.value.size >= page.value.total) return
        loadingMore.value = true
        offset.value += PAGE
    }

    private data class PageParams(val q: String, val kind: Kind?, val accountId: Long, val offset: Int)

    private companion object {
        const val PAGE = 50
    }
}

/* ================= صفحه ================= */

/** صفحه‌ی تراکنش‌ها: جستجو، فیلتر نوع/حساب، لیست گروه‌بندی روز + نمایش بیشتر */
@Composable
fun TransactionsScreen(onPush: (String) -> Unit) {
    val vm: TransactionsViewModel = viewModel()
    val jc = LocalJibetColors.current
    val qInput by vm.qInput.collectAsState()
    val kind by vm.kind.collectAsState()
    val accountId by vm.accountId.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val page by vm.page.collectAsState()
    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "تراکنش‌ها",
            action = {
                GlassIconButton(onClick = { onPush("search") }) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = "جستجو و فیلتر",
                        tint = TextMid,
                        modifier = Modifier.size(19.dp),
                    )
                }
            },
        )

        // نوار جستجو
        SearchField(
            value = qInput,
            onValueChange = { vm.qInput.value = it },
            placeholder = "جستجو...",
            a11yLabel = "جستجوی تراکنش",
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // چیپ‌های نوع
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
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

        // چیپ‌های حساب
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
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

        TxSummaryLine(income = page.income, expense = page.expense, total = page.total)

        // محتوا
        if (loading && items.isEmpty()) {
            SkeletonList()
        } else if (items.isEmpty()) {
            EmptyState(
                title = "تراکنشی یافت نشد",
                description = "با دکمه‌ی + پایین صفحه اولین تراکنش را ثبت کن یا فیلترها را تغییر بده.",
                icon = Icons.Rounded.AccountBalanceWallet,
            )
        } else {
            // ورود نرم لیست — مثل motion.div وب (opacity 180ms)
            val fade = remember { Animatable(0f) }
            LaunchedEffect(Unit) { fade.animateTo(1f, tween(durationMillis = 180)) }
            Column(Modifier.alpha(fade.value)) {
                TxGroupedList(
                    groups = groupTxsByDay(items),
                    onOpen = { UiBus.openDetail(it.id) },
                )

                if (page.total > items.size) {
                    val moreAction: (() -> Unit)? = if (loadingMore) null else ({ vm.loadMore() })
                    Row(
                        Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .alpha(if (loadingMore) 0.6f else 1f)
                            .jibetGlass(shape = RoundedCornerShape(16.dp))
                            .pressableScale(onClick = moreAction, pressedScale = 0.98f)
                            .height(44.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (loadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TextMid,
                            )
                        } else {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextMid,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            if (loadingMore) "در حال بارگذاری…" else "نمایش بیشتر",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextMid),
                        )
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

/** مستطیل پالس‌دار اسکلتون */
@Composable
private fun PulsingBox(
    width: Dp,
    height: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Box(
        modifier
            .alpha(alpha)
            .background(color, shape)
            .width(width)
            .height(height),
    )
}

/** اسکلتون بارگذاری اولیه — مثل SkeletonList وب (۵ ردیف) */
@Composable
private fun SkeletonList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(5) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .jibetCard()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PulsingBox(width = 40.dp, height = 40.dp, color = Color(0xFFE4E8F4), shape = CircleShape)
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PulsingBox(width = 112.dp, height = 12.dp, color = Color(0xFFE4E8F4))
                    PulsingBox(width = 64.dp, height = 10.dp, color = Color(0xFFECEFF7))
                }
                PulsingBox(width = 64.dp, height = 12.dp, color = Color(0xFFE4E8F4))
            }
        }
    }
}
