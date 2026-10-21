package com.jibet.app.ui.more

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.ThemeMode
import com.jibet.app.ui.components.JibetLogo
import com.jibet.app.ui.components.JibetSegmented
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.ACCENT_PRESETS
import com.jibet.app.util.Fmt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ویومدل صفحه‌ی بیشتر — رنگ برنامه، موجودی کل و دموی داده‌ها */
class MoreViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as JibetApp
    private val repo = app.repository

    val accent: StateFlow<String> = app.settings.accent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "blue")

    /** حالت نمایش برنامه — سیستمی/روشن/تاریک */
    val themeMode: StateFlow<ThemeMode> = app.settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val totalBalance: StateFlow<Double> = repo.watchTotalBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** تایید دو مرحله‌ای — بعد از ۳.۵ ثانیه خودکار ریست می‌شود (مثل وب) */
    var confirmLoad by mutableStateOf(false)
        private set
    var confirmClear by mutableStateOf(false)
        private set

    /** null | "load" | "clear" */
    var busy by mutableStateOf<String?>(null)
        private set

    private var loadConfirmJob: Job? = null
    private var clearConfirmJob: Job? = null

    fun setAccent(key: String) {
        viewModelScope.launch { app.settings.setAccent(key) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { app.settings.setThemeMode(mode) }
    }

    fun loadSample() {
        if (busy != null) return
        if (!confirmLoad) {
            confirmLoad = true
            loadConfirmJob?.cancel()
            loadConfirmJob = viewModelScope.launch {
                delay(3_500)
                confirmLoad = false
            }
            return
        }
        busy = "load"
        viewModelScope.launch {
            val error = try {
                repo.loadSampleData()
            } catch (_: Exception) {
                "خطا در بارگذاری داده‌ی نمونه"
            }
            busy = null
            if (error == null) {
                confirmLoad = false
                Toast.makeText(getApplication(), "داده‌ی نمونه بارگذاری شد", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearData() {
        if (busy != null) return
        if (!confirmClear) {
            confirmClear = true
            clearConfirmJob?.cancel()
            clearConfirmJob = viewModelScope.launch {
                delay(3_500)
                confirmClear = false
            }
            return
        }
        busy = "clear"
        viewModelScope.launch {
            val error = try {
                repo.clearData()
                null
            } catch (_: Exception) {
                "خطا در پاک‌سازی داده‌ها"
            }
            busy = null
            if (error == null) {
                confirmClear = false
                Toast.makeText(getApplication(), "تراکنش‌ها پاک‌سازی شد", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/** ورود نرم کارت‌ها — مثل fadeUp وب (opacity + y ۱۲dp، ۲۲۰ms easeOut) */
@Composable
private fun FadeUp(delayMs: Int, modifier: Modifier = Modifier, offsetY: Dp = 12.dp, content: @Composable () -> Unit) {
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

/** تیتر بخش — مثل SectionTitle وب (mt-6 mb-2 px-1) */
@Composable
private fun SectionTitle(text: String) {
    val jc = LocalJibetColors.current
    Text(
        text,
        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = jc.inkSoft),
        modifier = Modifier
            .padding(top = 24.dp, bottom = 8.dp)
            .padding(start = 4.dp, end = 4.dp),
    )
}

/** حلقه‌ی انتخاب رنگ فعال — مثل ring-2 + ring-offset-2 وب (بدون تغییر لی‌اوت) */
@Composable
private fun Modifier.accentRing(active: Boolean): Modifier {
    if (!active) return this
    val jc = LocalJibetColors.current
    return this.drawBehind {
        val stroke = 2.dp.toPx()
        val r = size.minDimension / 2f
        // باند رنگ پس‌زمینه (offset) سپس باند مرکبی ۳۰٪
        drawCircle(jc.bgA, radius = r + stroke / 2f, style = Stroke(stroke))
        drawCircle(jc.ink.copy(alpha = 0.3f), radius = r + stroke * 1.5f, style = Stroke(stroke))
    }
}

@Composable
fun MoreScreen(onPush: (String) -> Unit, onTab: (String) -> Unit) {
    val vm: MoreViewModel = viewModel()
    val accent by vm.accent.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val totalBalance by vm.totalBalance.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(title = "بیشتر")

        FadeUp(0) { IntroCard(totalBalance = totalBalance) }

        FadeUp(50, Modifier.padding(top = 12.dp)) {
            HubList(onPush = onPush, onTab = onTab)
        }

        SectionTitle("ظاهر")
        FadeUp(100) {
            AccentCard(accent = accent, onSelect = { vm.setAccent(it) })
        }
        FadeUp(120) {
            ThemeCard(mode = themeMode, onSelect = { vm.setThemeMode(it) })
        }

        SectionTitle("داده‌ها (دمو)")
        FadeUp(150) { DataCard(vm = vm) }
    }
}

/* ================= کارت معرفی ================= */

@Composable
private fun IntroCard(totalBalance: Double) {
    val jc = LocalJibetColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .jibetGlass(RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        JibetLogo(size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(
                "جیبت",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = 16.sp, color = jc.ink),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = jc.inkSoft)) { append("موجودی کل: ") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = jc.ink)) { append(Fmt.money(totalBalance)) }
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = jc.inkSoft)) { append(" تومان") }
                },
                style = TextStyle(fontFamily = Vazir, fontSize = 12.5.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/* ================= لیست هاب ================= */

@Composable
private fun HubList(onPush: (String) -> Unit, onTab: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .jibetGlass(RoundedCornerShape(24.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HubRow(title = "حساب‌ها", colorHex = "#5B7FFF", icon = Icons.Rounded.CreditCard) { onPush("accounts") }
        HubRow(title = "دسته‌بندی‌ها", colorHex = "#F5A524", icon = Icons.Rounded.Sell) { onPush("categories") }
        HubRow(title = "بودجه‌بندی", colorHex = "#B07BFF", icon = Icons.Rounded.GpsFixed) { onTab("budget") }
        HubRow(title = "تراکنش‌های تکراری", colorHex = "#23C4D8", icon = Icons.Rounded.Repeat) { onPush("recurring") }
        HubRow(title = "هدف‌های پس‌انداز", colorHex = "#F07BB4", icon = Icons.Rounded.Redeem) { onPush("goals") }
        HubRow(title = "گزارش‌ها", colorHex = "#34C77B", icon = Icons.Rounded.TrendingUp) { onPush("reports") }
        HubRow(title = "جستجو و فیلتر", colorHex = "#4ECDC4", icon = Icons.Rounded.Search) { onPush("search") }
        HubRow(title = "پشتیبان‌گیری و خروجی", colorHex = "#FFB020", icon = Icons.Rounded.Payments) { onPush("backup") }
        HubRow(title = "درباره جیبت", colorHex = "#F26D6D", icon = Icons.Rounded.Favorite) { onPush("about") }
    }
}

@Composable
private fun HubRow(title: String, colorHex: String, icon: ImageVector, onClick: () -> Unit) {
    val jc = LocalJibetColors.current
    val color = parseColor(colorHex)
    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            title,
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = jc.ink),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Rounded.ChevronLeft,
            contentDescription = null,
            tint = jc.faint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/* ================= ظاهر ================= */

@Composable
private fun AccentCard(accent: String, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val jc = LocalJibetColors.current
    Column(Modifier.jibetCard().padding(16.dp)) {
        Text(
            "رنگ برنامه",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = jc.ink),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ACCENT_PRESETS.forEach { p ->
                val active = accent == p.key
                Box(
                    Modifier
                        .size(44.dp)
                        .accentRing(active)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(parseColor(p.b), parseColor(p.a))))
                        .pressableScale(onClick = {
                            onSelect(p.key)
                            Toast.makeText(context, "رنگ برنامه تغییر کرد", Toast.LENGTH_SHORT).show()
                        })
                        .semantics { contentDescription = "رنگ ${p.name}" },
                )
            }
        }
    }
}

/** کارت حالت نمایش — سیستمی/روشن/تاریک، هم‌زبان بصری با کارت رنگ */
@Composable
private fun ThemeCard(mode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val jc = LocalJibetColors.current
    Column(Modifier.jibetCard().padding(16.dp)) {
        Text(
            "حالت نمایش",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = jc.ink),
        )
        JibetSegmented(
            value = mode,
            options = listOf(
                ThemeMode.SYSTEM to "سیستمی",
                ThemeMode.LIGHT to "روشن",
                ThemeMode.DARK to "تاریک",
            ),
            onChange = { onSelect(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            activeColor = jc.accentA,
        )
        Text(
            "تاریک: پس‌زمینه‌ی تیره برای شب",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = jc.inkSoft),
            modifier = Modifier
                .padding(top = 10.dp)
                .padding(start = 4.dp, end = 4.dp),
        )
    }
}

/* ================= داده‌ها (دمو) ================= */

@Composable
private fun DataCard(vm: MoreViewModel) {
    val jc = LocalJibetColors.current

    Column(
        Modifier.jibetCard().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // بارگذاری داده‌ی نمونه
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .then(
                        if (vm.confirmLoad) {
                            Modifier
                                .shadow(
                                    10.dp,
                                    RoundedCornerShape(16.dp),
                                    spotColor = Color(0x8C5B74E9),
                                    ambientColor = Color.Transparent,
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(jc.accentB, jc.accentA)))
                        } else {
                            Modifier
                        }
                    )
                    .pressableScale(onClick = { vm.loadSample() })
                    .alpha(if (vm.busy != null) 0.5f else 1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    vm.busy == "load" -> DemoButtonLabel(text = "در حال بارگذاری…", color = Color.White, pill = false)
                    vm.confirmLoad -> DemoButtonLabel(text = "مطمئنی؟ بارگذاری کن", color = Color.White, pill = false)
                    else -> DemoButtonLabel(text = "بارگذاری داده‌ی نمونه", color = jc.accentA, pill = true)
                }
            }
            Text(
                "چند تراکنش، بودجه و هدف آزمایشی ساخته می‌شود؛ فقط وقتی هیچ تراکنشی نداشته باشید کار می‌کند.",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 20.sp, color = jc.inkSoft),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .padding(start = 4.dp, end = 4.dp),
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(jc.ink.copy(alpha = 0.05f))
        )

        // پاک‌سازی تراکنش‌ها
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .then(
                        if (vm.confirmClear) {
                            Modifier
                                .shadow(
                                    10.dp,
                                    RoundedCornerShape(16.dp),
                                    spotColor = Color(0x8CF26D6D),
                                    ambientColor = Color.Transparent,
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFF28080), jc.expense)))
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(jc.expense.copy(alpha = 0.10f))
                        }
                    )
                    .pressableScale(onClick = { vm.clearData() })
                    .alpha(if (vm.busy != null) 0.5f else 1f),
                contentAlignment = Alignment.Center,
            ) {
                val label = when {
                    vm.busy == "clear" -> "در حال پاک‌سازی…"
                    vm.confirmClear -> "مطمئنی؟ پاک کن"
                    else -> "پاک‌سازی تراکنش‌ها"
                }
                Text(
                    label,
                    style = TextStyle(
                        fontFamily = Vazir,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (vm.confirmClear) Color.White else jc.expense,
                    ),
                )
            }
            Text(
                "همه‌ی تراکنش‌ها، بودجه‌ها، هدف‌ها و موارد تکراری حذف می‌شوند و قابل بازگشت نیستند.",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 20.sp, color = jc.inkSoft),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .padding(start = 4.dp, end = 4.dp),
            )
        }
    }
}

/** برچسب دکمه‌ی دمو — حالت idle داخل قرص نرم accent ۱۲٪ مثل jb-accent-soft وب */
@Composable
private fun DemoButtonLabel(text: String, color: Color, pill: Boolean) {
    val jc = LocalJibetColors.current
    Text(
        text,
        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = color),
        modifier = if (pill) {
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(jc.accentA.copy(alpha = 0.12f))
        } else {
            Modifier
        },
    )
}
