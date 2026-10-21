package com.jibet.app.ui.about

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jibet.app.BuildConfig
import com.jibet.app.ui.components.JibetLogo
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.theme.IncomeSoft
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.InkSoft
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.TextMid
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ============================================================
   درباره جیبت — پورت ۱:۱ از screens/about.tsx نسخه وب
   ============================================================ */

/** ویژگی‌های جیبت — رشته‌ها عیناً از وب */
private val FEATURES = listOf(
    "کاملاً آفلاین و خصوصی",
    "بودجه‌بندی ماهانه",
    "هدف‌های پس‌انداز",
    "پشتیبان‌گیری محلی",
    "خروجی Excel و CSV",
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val jc = LocalJibetColors.current
    // .jb-accent-soft وب: پس‌زمینه = ۱۲٪ accentA روی سفید، متن = ۸۰٪ accentA + ۲۰٪ ink
    val softBg = lerp(Color.White, jc.accentA, 0.12f)
    val softFg = lerp(Ink, jc.accentA, 0.8f)
    // نسخه با ارقام فارسی و جداکننده «٫» — از BuildConfig تا همیشه با نسخه‌ی واقعی یکی باشد
    val version = Fmt.faDigits(BuildConfig.VERSION_NAME).replace('.', '٫')

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(title = "درباره جیبت", onBack = onBack)

        // لوگو، نسخه و تگ‌لاین — ورود اسکیل ۰.۹۲→۱ + فید
        FadeScaleIn(Modifier.padding(top = 20.dp)) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                JibetLogo(size = 72.dp)
                Text(
                    "جیبت",
                    style = TextStyle(
                        fontFamily = Vazir,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = (-0.025).em,
                        color = Ink,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "نسخه $version",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = softFg),
                    modifier = Modifier
                        .background(softBg, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
                Text(
                    "مدیریت مالی، ساده‌تر از همیشه",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = InkSoft),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // ویژگی‌ها
        FadeUp(delayMs = 80, modifier = Modifier.padding(top = 28.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .jibetCard()
                    .padding(20.dp),
            ) {
                Text(
                    "ویژگی‌های جیبت",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink),
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FEATURES.forEach { f ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(IncomeSoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = jc.income,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                            Text(
                                f,
                                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, color = TextMid),
                            )
                        }
                    }
                }
            }
        }

        // نصب روی گوشی
        FadeUp(delayMs = 140, modifier = Modifier.padding(top = 12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .jibetGlass(shape = RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(softBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = softFg,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "نصب روی گوشی",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink),
                    )
                    Text(
                        "این نسخه بومی اندروید جیبت است؛ همه‌ی داده‌ها فقط روی همین گوشی ذخیره می‌شود.",
                        style = TextStyle(
                            fontFamily = Vazir,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            lineHeight = 24.sp,
                            color = InkSoft,
                        ),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        // پانوشت
        FadeUp(delayMs = 200) {
            Text(
                "ساخته‌شده با دقت برای مدیریت آرام مالی",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = InkSoft),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
            )
        }
    }
}

/** ورود فید + اسکیل (۰.۹۲→۱، ۲۵۰ms) — مثل بلاک لوگوی وب */
@Composable
private fun FadeScaleIn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val s = remember { Animatable(0.92f) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { s.animateTo(1f, tween(250, easing = LinearOutSlowInEasing)) }
        launch { a.animateTo(1f, tween(250, easing = LinearOutSlowInEasing)) }
    }
    Box(modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = a.value }) {
        content()
    }
}

/** ورود فید-بالا (y ۱۲dp→۰، ۲۲۰ms، با تاخیر پلکانی) — مثل fadeUp وب */
@Composable
private fun FadeUp(
    delayMs: Long,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val a = remember { Animatable(0f) }
    val y = remember { Animatable(12f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        launch { a.animateTo(1f, tween(220, easing = LinearOutSlowInEasing)) }
        launch { y.animateTo(0f, tween(220, easing = LinearOutSlowInEasing)) }
    }
    Box(modifier.graphicsLayer { alpha = a.value; translationY = y.value.dp.toPx() }) {
        content()
    }
}
