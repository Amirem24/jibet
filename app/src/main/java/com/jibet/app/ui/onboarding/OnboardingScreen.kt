@file:OptIn(ExperimentalLayoutApi::class)

package com.jibet.app.ui.onboarding

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffsetWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.AccountDraft
import com.jibet.app.ui.components.AccentButton
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.JibetLogo
import com.jibet.app.ui.components.accentGradient
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.Faint
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.InkSoft
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.TextMid
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.Fmt
import com.jibet.app.util.PRESET_ACCOUNTS
import com.jibet.app.util.PRESET_CATEGORIES
import com.jibet.app.util.PresetAccount
import com.jibet.app.util.PresetCategory
import com.jibet.app.util.SUGGESTED_EXPENSE_KEYS
import com.jibet.app.util.SUGGESTED_INCOME_KEYS
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ============================================================
   آنبوردینگ جیبت — پورت ۱:۱ از screens/onboarding.tsx نسخه وب
   مراحل: ۰ اسپلش، ۱ خوش‌آمد، ۲ حساب‌ها، ۳ دسته‌بندی‌ها، ۴ آماده
   پایان آنبوردینگ: setup + setOnboarded → MainActivity خودش UI اصلی را نشان می‌دهد
   ============================================================ */

/** تعداد مراحل (بدون اسپلش) */
private const val TOTAL_STEPS = 4

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JibetApp
    val repo = app.repository
    val settings = app.settings

    /** مرحله فعلی ۰..۴ */
    var step by mutableStateOf(0)
        private set

    /** حساب‌های انتخاب‌شده — پیش‌فرض نقد + بانک مثل وب */
    var accSel by mutableStateOf(setOf("cash", "bank"))
        private set

    /** موجودی اولیه هر حساب */
    var balances by mutableStateOf(mapOf<String, Double>())
        private set

    /** متن خام تایپ‌شده برای فیلد موجودی هر حساب */
    var balanceRaw by mutableStateOf(mapOf<String, String>())
        private set

    /** دسته‌های انتخاب‌شده — پیش‌فرض پیشنهادی‌ها مثل وب */
    var catSel by mutableStateOf((SUGGESTED_EXPENSE_KEYS + SUGGESTED_INCOME_KEYS).toSet())
        private set

    var saving by mutableStateOf(false)
        private set

    fun go(n: Int) {
        step = n.coerceIn(0, TOTAL_STEPS)
    }

    fun toggleAcc(key: String) {
        accSel = if (key in accSel) accSel - key else accSel + key
    }

    fun toggleCat(key: String) {
        catSel = if (key in catSel) catSel - key else catSel + key
    }

    /** مثل وب: متن خام نگه داشته می‌شود و عدد دوباره parse می‌شود */
    fun setBalance(key: String, raw: String) {
        balanceRaw = balanceRaw + (key to raw)
        balances = balances + (key to Fmt.parseNumber(raw))
    }

    /** بازنشانی به پیشنهادی‌ها */
    fun resetCats() {
        catSel = (SUGGESTED_EXPENSE_KEYS + SUGGESTED_INCOME_KEYS).toSet()
    }

    /** انتخاب همه دسته‌ها */
    fun selectAllCats() {
        catSel = PRESET_CATEGORIES.map { it.key }.toSet()
    }

    /** ارسال انتخاب‌ها و پایان آنبوردینگ — MainActivity با تغییر bootState سوئیچ می‌کند */
    fun finish() {
        if (saving) return
        saving = true
        val accounts = PRESET_ACCOUNTS
            .filter { it.key in accSel }
            .map { AccountDraft(key = it.key, balance = balances[it.key] ?: 0.0) }
        val categoryKeys = PRESET_CATEGORIES.filter { it.key in catSel }.map { it.key }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.setup(accounts, categoryKeys)
                settings.setOnboarded()
            } catch (e: Exception) {
                saving = false
                Toast.makeText(getApplication(), "خطا در ذخیره‌ی راه‌اندازی", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/** ویزارد راه‌اندازی اولیه — اسپلش + ۴ مرحله */
@Composable
fun OnboardingScreen() {
    val vm: OnboardingViewModel = viewModel()
    val density = LocalDensity.current
    val slidePx = remember(density) { with(density) { 56.dp.toPx().roundToInt() } }

    Column(
        Modifier
            .fillMaxSize()
            // مثل overflow-hidden ریشه وب
            .clipToBounds(),
    ) {
        if (vm.step >= 1) {
            StepHeader(step = vm.step, busy = vm.saving, onSkip = vm::finish)
        }

        // مثل AnimatePresence mode="wait" — اسلاید ±۵۶dp + فید، ۲۰۰ms
        AnimatedContent(
            targetState = vm.step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val d = if (targetState > initialState) 1 else -1
                (
                    slideInHorizontally(tween(200, easing = LinearOutSlowInEasing)) { _ -> d * slidePx } +
                        fadeIn(tween(200, easing = LinearOutSlowInEasing))
                    ) togetherWith (
                    slideOutHorizontally(tween(200, easing = LinearOutSlowInEasing)) { _ -> -d * slidePx } +
                        fadeOut(tween(200, easing = LinearOutSlowInEasing))
                    )
            },
            label = "onboardingStep",
        ) { s ->
            when (s) {
                0 -> SplashStep(onStart = { vm.go(1) })
                1 -> WelcomeStep(onNext = { vm.go(2) })
                2 -> AccountsStep(
                    selected = vm.accSel,
                    balancesRaw = vm.balanceRaw,
                    busy = vm.saving,
                    onToggle = vm::toggleAcc,
                    onBalance = vm::setBalance,
                    onNext = { vm.go(3) },
                    onBack = { vm.go(1) },
                )
                3 -> CategoriesStep(
                    selected = vm.catSel,
                    busy = vm.saving,
                    onToggle = vm::toggleCat,
                    onReset = vm::resetCats,
                    onSelectAll = vm::selectAllCats,
                    onNext = { vm.go(4) },
                    onBack = { vm.go(2) },
                )
                else -> ReadyStep(busy = vm.saving, onFinish = vm::finish)
            }
        }
    }
}

/* ---------- هدر: شمارنده مرحله + نقطه‌های progress + رد شدن ---------- */

@Composable
private fun StepHeader(step: Int, busy: Boolean, onSkip: () -> Unit) {
    val jc = LocalJibetColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "${Fmt.faDigits(step)} از ${Fmt.faDigits(TOTAL_STEPS)}",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Ink),
            modifier = Modifier.widthIn(min = 56.dp),
        )
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 1..TOTAL_STEPS) {
                val dotScale by animateFloatAsState(
                    targetValue = if (i == step) 1.35f else 1f,
                    animationSpec = tween(200),
                    label = "dotScale$i",
                )
                val dotModifier = if (i == step) {
                    // گرادیان accent مثل jb-accent-gradient وب
                    Modifier.background(accentGradient())
                } else {
                    Modifier.background(
                        if (i < step) jc.accentA.copy(alpha = 0.35f) else Color(0x1A232B3B),
                    )
                }
                Box(
                    Modifier
                        .size(8.dp)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .then(dotModifier),
                )
            }
        }
        Box(Modifier.widthIn(min = 56.dp), contentAlignment = Alignment.CenterEnd) {
            if (step == 2 || step == 3) {
                Box(
                    Modifier
                        .alpha(if (busy) 0.5f else 1f)
                        .clip(RoundedCornerShape(50))
                        .pressableScale(onClick = if (busy) null else onSkip, pressedScale = 0.96f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "رد شدن",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InkSoft),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .padding(horizontal = 8.dp)
                            .wrapContentHeight(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
}

/* ---------- مرحله ۰: اسپلش ---------- */

/** حباب‌های شناور ملایم پس‌زمینه‌ی اسپلش — حس liquid glass */
private data class Bubble(val size: Int, val durMs: Int, val color: Color)

private val BUBBLES = listOf(
    // top:-6% right:-22%
    Bubble(210, 7500, Color(0x296E7BFF)),
    // bottom:16% left:-18%
    Bubble(150, 8500, Color(0x244F8CFF)),
    // top:26% left:10%
    Bubble(96, 6500, Color(0x21F07BB4)),
    // bottom:24% right:12%
    Bubble(76, 7000, Color(0x2134C77B)),
    // top:55% right:-8%
    Bubble(120, 9000, Color(0x1A23C4D8)),
)

@Composable
private fun SplashStep(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.matchParentSize()) {
            val w = maxWidth
            val h = maxHeight
            // مختصات مطلق (ltr) معادل top/right/bottom/left وب — چپ = absoluteOffset
            val positions = listOf(
                w * 1.22f - 210.dp to h * -0.06f,
                w * -0.18f to h * 0.84f - 150.dp,
                w * 0.10f to h * 0.26f,
                w * 1.12f - 76.dp to h * 0.76f - 76.dp,
                w * 1.08f - 120.dp to h * 0.55f,
            )
            BUBBLES.forEachIndexed { i, b ->
                val (x, y) = positions[i]
                val trans = rememberInfiniteTransition(label = "bubble$i")
                val dy by trans.animateFloat(
                    initialValue = 0f,
                    targetValue = -12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(b.durMs / 2, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "bubbleDy$i",
                )
                val dx by trans.animateFloat(
                    initialValue = 0f,
                    targetValue = 6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(b.durMs / 2, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "bubbleDx$i",
                )
                Box(
                    Modifier
                        .offset {
                            IntOffset((x + dx.dp).roundToPx(), (y + dy.dp).roundToPx())
                        }
                        .size(b.size.dp)
                        .blur(26.dp, BlurredEdgeTreatment.Unbounded)
                        .background(b.color, CircleShape),
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SpringIn(fromScale = 0.8f, stiffness = 240f, damping = 0.58f) {
                JibetLogo(size = 96.dp)
            }

            FadeInUp(delayMs = 100, fromY = 10f) {
                Column(
                    Modifier.padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "جیبت",
                        style = TextStyle(
                            fontFamily = Vazir,
                            fontWeight = FontWeight.Black,
                            fontSize = 36.sp,
                            lineHeight = 45.sp,
                            color = Ink,
                        ),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "jibet",
                        style = TextStyle(
                            fontFamily = Vazir,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            letterSpacing = 0.35.em,
                            color = InkSoft,
                        ),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "مدیریت مالی، ساده‌تر از همیشه",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextMid),
                        modifier = Modifier.padding(top = 12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            FadeInUp(delayMs = 200, fromY = 12f) {
                Box(
                    Modifier
                        .padding(top = 40.dp)
                        .jibetGlass(shape = RoundedCornerShape(50))
                        .pressableScale(onClick = onStart, pressedScale = 0.97f)
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "شروع",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Ink),
                    )
                }
            }
        }
    }
}

/* ---------- مرحله ۱: خوش‌آمد + تصویرسازی ---------- */

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FadeInUp(delayMs = 0, fromY = 12f) {
                    WelcomeArt()
                }
                FadeInUp(delayMs = 80, fromY = 10f) {
                    Column(
                        Modifier.padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "به جیبت خوش اومدید",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Ink),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "هزینه‌ها و درآمدهایت را ساده و سریع ثبت کن؛ جیبت همیشه نشان می‌دهد چقدر داری و پولت کجا می‌رود.",
                            style = TextStyle(
                                fontFamily = Vazir,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                lineHeight = 28.sp,
                                color = InkSoft,
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .widthIn(max = 310.dp),
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            AccentButton(text = "شروع کنید", onClick = onNext)
        }
    }
}

/** کیف پول گرادیانی + کارت‌های شناور + سکه‌ها — پورت WelcomeArt SVG وب */
@Composable
private fun WelcomeArt() {
    val trans = rememberInfiniteTransition(label = "welcomeArt")
    // کارت صورتی: y:[0,-5,0] طی ۳.۴ ثانیه
    val pinkY by trans.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pinkY",
    )
    // کارت فیروزه‌ای: y:[0,6,0] طی ۳ ثانیه با تاخیر ۰.۴
    val tealY by trans.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(400),
        ),
        label = "tealY",
    )
    // سکه سبز: y:[0,7,0] طی ۲.۸ ثانیه با تاخیر ۰.۲
    val coin1Y by trans.animateFloat(
        initialValue = 0f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(200),
        ),
        label = "coin1Y",
    )
    // سکه نارنجی: y:[0,-6,0] طی ۳.۲ ثانیه با تاخیر ۰.۶
    val coin2Y by trans.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(600),
        ),
        label = "coin2Y",
    )

    Canvas(Modifier.size(width = 216.dp, height = 176.dp)) {
        // فضای مبدأ SVG: 220×180 — مقیاس یکنواخت به بوم
        val s = size.width / 220f

        // سایه‌ی بیضی پشت
        drawOval(
            color = Color(0x126E7BFF),
            topLeft = Offset(22f * s, 38f * s),
            size = Size(176f * s, 120f * s),
        )

        // کارت شناور بالا-چپ (چرخش -۱۲ درجه)
        val pinkGrad = Brush.linearGradient(
            colors = listOf(Color(0xFFF07BB4), Color(0xFFF5A524)),
            start = Offset(20f * s, 30f * s),
            end = Offset(104f * s, 84f * s),
        )
        rotate(degrees = -12f, pivot = Offset(62f * s, 57f * s)) {
            translate(0f, pinkY * s) {
                drawRoundRect(
                    brush = pinkGrad,
                    topLeft = Offset(20f * s, 30f * s),
                    size = Size(84f * s, 54f * s),
                    cornerRadius = CornerRadius(12f * s),
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.65f),
                    topLeft = Offset(20f * s, 50f * s),
                    size = Size(84f * s, 10f * s),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(32f * s, 41f * s),
                    size = Size(16f * s, 11f * s),
                    cornerRadius = CornerRadius(3f * s),
                )
            }
        }

        // کارت شناور پایین-راست (پشت کیف — چرخش ۹ درجه)
        val tealGrad = Brush.linearGradient(
            colors = listOf(Color(0xFF23C4D8), Color(0xFF34C77B)),
            start = Offset(128f * s, 92f * s),
            end = Offset(204f * s, 140f * s),
        )
        rotate(degrees = 9f, pivot = Offset(166f * s, 116f * s)) {
            translate(0f, tealY * s) {
                drawRoundRect(
                    brush = tealGrad,
                    topLeft = Offset(128f * s, 92f * s),
                    size = Size(76f * s, 48f * s),
                    cornerRadius = CornerRadius(12f * s),
                    alpha = 0.95f,
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = Offset(128f * s, 110f * s),
                    size = Size(76f * s, 9f * s),
                )
            }
        }

        // سکه‌ها
        translate(0f, coin1Y * s) {
            drawCircle(color = Color(0xFF34C77B), radius = 13f * s, center = Offset(178f * s, 34f * s))
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 7.5f * s,
                center = Offset(178f * s, 34f * s),
                style = Stroke(width = 2.5f * s),
            )
        }
        translate(0f, coin2Y * s) {
            drawCircle(color = Color(0xFFF5A524), radius = 9f * s, center = Offset(36f * s, 142f * s))
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 5f * s,
                center = Offset(36f * s, 142f * s),
                style = Stroke(width = 2f * s),
            )
        }

        // کیف پول
        val walletGrad = Brush.linearGradient(
            colors = listOf(Color(0xFF6E7BFF), Color(0xFF4F8CFF)),
            start = Offset(58f * s, 58f * s),
            end = Offset(176f * s, 144f * s),
        )
        drawRoundRect(
            brush = walletGrad,
            topLeft = Offset(58f * s, 58f * s),
            size = Size(118f * s, 86f * s),
            cornerRadius = CornerRadius(24f * s),
        )
        // درخشش بالای کیف — معادل path وب
        val shine = Path().apply {
            moveTo(58f * s, 94f * s)
            lineTo(58f * s, 82f * s)
            arcTo(Rect(left = 58f * s, top = 58f * s, right = 106f * s, bottom = 106f * s), 180f, 90f, false)
            lineTo(152f * s, 58f * s)
            arcTo(Rect(left = 128f * s, top = 58f * s, right = 176f * s, bottom = 106f * s), 270f, 90f, false)
            lineTo(176f * s, 94f * s)
            close()
        }
        drawPath(shine, color = Color.White.copy(alpha = 0.14f))
        drawRoundRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(148f * s, 88f * s),
            size = Size(38f * s, 28f * s),
            cornerRadius = CornerRadius(13f * s),
        )
        drawCircle(color = Color.White, radius = 6.5f * s, center = Offset(167f * s, 102f * s))

        // جرقه‌ها
        drawLine(
            color = Color(0x736E7BFF),
            start = Offset(28f * s, 98f * s),
            end = Offset(42f * s, 98f * s),
            strokeWidth = 2.5f * s,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0x736E7BFF),
            start = Offset(35f * s, 91f * s),
            end = Offset(35f * s, 105f * s),
            strokeWidth = 2.5f * s,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0x80F07BB4),
            start = Offset(196f * s, 138f * s),
            end = Offset(196f * s, 148f * s),
            strokeWidth = 2.5f * s,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0x80F07BB4),
            start = Offset(191f * s, 143f * s),
            end = Offset(201f * s, 143f * s),
            strokeWidth = 2.5f * s,
            cap = StrokeCap.Round,
        )
    }
}

/* ---------- مرحله ۲: حساب‌ها ---------- */

@Composable
private fun AccountsStep(
    selected: Set<String>,
    balancesRaw: Map<String, String>,
    busy: Boolean,
    onToggle: (String) -> Unit,
    onBalance: (String, String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
        ) {
            Text(
                "حساب‌های خود را اضافه کنید",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "حساب‌هایی که با آن‌ها پول خرج و دریافت می‌کنی را انتخاب کن و موجودی اولیه‌شان را وارد کن.",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 24.sp, color = InkSoft),
                modifier = Modifier.padding(top = 4.dp),
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PRESET_ACCOUNTS.forEach { a ->
                    AccountCard(
                        account = a,
                        selected = a.key in selected,
                        rawBalance = balancesRaw[a.key] ?: "",
                        onToggle = { onToggle(a.key) },
                        onBalance = { raw -> onBalance(a.key, raw) },
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier
                    .alpha(if (busy) 0.5f else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .pressableScale(onClick = if (busy) null else onBack)
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "بازگشت",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkSoft),
                )
            }
            AccentButton(
                text = "بعدی",
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = !busy && selected.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: PresetAccount,
    selected: Boolean,
    rawBalance: String,
    onToggle: () -> Unit,
    onBalance: (String) -> Unit,
) {
    val jc = LocalJibetColors.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (selected) 12.dp else 8.dp,
                shape = shape,
                spotColor = if (selected) jc.accentA.copy(alpha = 0.28f) else Color(0x295B74E9),
                ambientColor = Color.Transparent,
            )
            .clip(shape)
            .background(Color.White)
            .then(if (selected) Modifier.border(2.dp, jc.accentA, shape) else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .pressableScale(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                IconBubble(icon = account.icon, color = parseColor(account.color), size = 44.dp)
                // بج تیک — بالا-چپ حباب مثل وب
                androidx.compose.animation.AnimatedVisibility(
                    visible = selected,
                    enter = scaleIn(spring(dampingRatio = 0.54f, stiffness = 500f)),
                    exit = scaleOut(spring(dampingRatio = 0.54f, stiffness = 500f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .absoluteOffset(x = (-6).dp, y = (-6).dp),
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .background(accentGradient(), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    account.name,
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) {
                    Text(
                        "انتخاب شده",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = jc.accentA),
                    )
                }
            }
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = if (selected) InkSoft else Faint,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(
            visible = selected,
            enter = expandVertically(tween(200, easing = LinearOutSlowInEasing)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(200)),
        ) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                Text(
                    "موجودی اولیه",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextMid),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                BalanceField(
                    raw = rawBalance,
                    onValueChange = onBalance,
                    accName = account.name,
                )
            }
        }
    }
}

/** فیلد موجودی اولیه — استایل inputClass وب + پسوند «تومان» داخل فیلد */
@Composable
private fun BalanceField(
    raw: String,
    onValueChange: (String) -> Unit,
    accName: String,
) {
    val jc = LocalJibetColors.current
    val display = if (raw.isEmpty()) "" else Fmt.money(Fmt.parseNumber(raw))
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.85f))
            .border(1.dp, InkSoft.copy(alpha = 0.18f), shape),
    ) {
        BasicTextField(
            value = display,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Ink),
            cursorBrush = SolidColor(jc.accentA),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(start = 16.dp, end = 64.dp)
                .semantics { contentDescription = "موجودی اولیه $accName" },
        )
        if (display.isEmpty()) {
            Text(
                "مثلاً " + Fmt.money(500000.0),
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Faint),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
            )
        }
        Text(
            "تومان",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
        )
    }
}

/* ---------- مرحله ۳: دسته‌بندی‌ها ---------- */

@Composable
private fun CategoriesStep(
    selected: Set<String>,
    busy: Boolean,
    onToggle: (String) -> Unit,
    onReset: () -> Unit,
    onSelectAll: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val jc = LocalJibetColors.current
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
        ) {
            Text(
                "دسته‌بندی‌ها را شخصی‌سازی کنید",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "دسته‌های پرکاربردت را انتخاب کن؛ بعداً هم می‌توانی از بخش دسته‌بندی‌ها دسته جدید بسازی.",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 24.sp, color = InkSoft),
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${Fmt.faDigits(selected.size)} دسته انتخاب شده",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextMid),
                    modifier = Modifier.weight(1f),
                )
                GlassPill(icon = Icons.Rounded.Refresh, label = "بازنشانی پیشنهادی", onClick = onReset)
                GlassPill(icon = Icons.Rounded.DoneAll, label = "انتخاب همه", onClick = onSelectAll)
            }

            CategorySection(
                title = "هزینه‌ها",
                dot = jc.expense,
                items = PRESET_CATEGORIES.filter { it.kind == "expense" },
                selected = selected,
                onToggle = onToggle,
            )
            CategorySection(
                title = "درآمدها",
                dot = jc.income,
                items = PRESET_CATEGORIES.filter { it.kind == "income" },
                selected = selected,
                onToggle = onToggle,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier
                    .alpha(if (busy) 0.5f else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .pressableScale(onClick = if (busy) null else onBack)
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "بازگشت",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkSoft),
                )
            }
            AccentButton(
                text = "بعدی",
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = !busy,
            )
        }
    }
}

/** دکمه‌ی شیشه‌ای کوچک بالای لیست دسته‌ها */
@Composable
private fun GlassPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .jibetGlass(shape = RoundedCornerShape(50))
            .pressableScale(onClick = onClick, pressedScale = 0.96f)
            .heightIn(min = 40.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextMid,
            modifier = Modifier.size(13.dp),
        )
        Text(
            label,
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextMid),
        )
    }
}

@Composable
private fun CategorySection(
    title: String,
    dot: Color,
    items: List<PresetCategory>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    val selCount = items.count { it.key in selected }
    Column(Modifier.padding(top = 16.dp)) {
        Row(
            Modifier.padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
            Text(
                title,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Ink),
            )
            Text(
                "${Fmt.faDigits(selCount)} از ${Fmt.faDigits(items.size)}",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = InkSoft),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { c ->
                CategoryChip(
                    name = c.name,
                    icon = c.icon,
                    color = parseColor(c.color),
                    selected = c.key in selected,
                    onToggle = { onToggle(c.key) },
                )
            }
        }
    }
}

/** چیپ دسته — مثل دکمه‌های flex-wrap وب */
@Composable
private fun CategoryChip(
    name: String,
    icon: String,
    color: Color,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.82f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) color.copy(alpha = 0.35f) else InkSoft.copy(alpha = 0.16f),
                shape = shape,
            )
            .pressableScale(onClick = onToggle, pressedScale = 0.97f)
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBubble(icon = icon, color = color, size = 28.dp, iconSize = 14.dp)
        Text(
            name,
            style = TextStyle(
                fontFamily = Vazir,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (selected) Ink else TextMid,
            ),
        )
    }
}

/* ---------- مرحله ۴: آماده ---------- */

@Composable
private fun ReadyStep(busy: Boolean, onFinish: () -> Unit) {
    val jc = LocalJibetColors.current
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
                    SpringIn(fromScale = 0.4f, stiffness = 240f, damping = 0.55f) {
                        Box(
                            Modifier
                                .size(128.dp)
                                .clip(CircleShape)
                                .background(jc.income.copy(alpha = 0.13f)),
                        )
                    }
                    SpringIn(delayMs = 80, fromScale = 0.4f, stiffness = 320f, damping = 0.42f) {
                        Box(
                            Modifier
                                .size(80.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = CircleShape,
                                    spotColor = jc.income.copy(alpha = 0.55f),
                                    ambientColor = Color.Transparent,
                                )
                                .clip(CircleShape)
                                .background(jc.income),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp),
                            )
                        }
                    }
                }

                FadeInUp(delayMs = 150, fromY = 10f) {
                    Column(
                        Modifier.padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "آماده‌اید!",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Ink),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "حساب‌ها و دسته‌بندی‌هایت ساخته شد؛ حالا می‌توانی ثبت هزینه و درآمد را شروع کنی.",
                            style = TextStyle(
                                fontFamily = Vazir,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                lineHeight = 28.sp,
                                color = InkSoft,
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .widthIn(max = 310.dp),
                        )
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            AccentButton(
                text = if (busy) "در حال آماده‌سازی…" else "شروع کنید",
                onClick = onFinish,
                enabled = !busy,
                leading = if (busy) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

/* ---------- انیمیشن‌های ورود مشترک ---------- */

/** ورود فنری (اسپرینگ) با اسکیل + آلفا — مثل motion.spring فریمر */
@Composable
private fun SpringIn(
    delayMs: Long = 0,
    fromScale: Float,
    stiffness: Float,
    damping: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val s = remember { Animatable(fromScale) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        launch {
            s.animateTo(1f, spring(dampingRatio = damping, stiffness = stiffness))
        }
        launch {
            a.animateTo(1f, spring(dampingRatio = damping, stiffness = stiffness))
        }
    }
    Box(modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = a.value }) {
        content()
    }
}

/** ورود محو-بالا (y + آلفا، ۲۵۰ms) — مثل motion.div های وب */
@Composable
private fun FadeInUp(
    delayMs: Long,
    fromY: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val a = remember { Animatable(0f) }
    val y = remember { Animatable(fromY) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        launch {
            a.animateTo(1f, tween(250, easing = LinearOutSlowInEasing))
        }
        launch {
            y.animateTo(0f, tween(250, easing = LinearOutSlowInEasing))
        }
    }
    Box(modifier.graphicsLayer { alpha = a.value; translationY = y.value.dp.toPx() }) {
        content()
    }
}
