package com.jibet.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalMovies
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jibet.app.data.Kind
import com.jibet.app.ui.theme.BgA
import com.jibet.app.ui.theme.BgB
import com.jibet.app.ui.theme.Faint
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.InkSoft
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.TextMid
import com.jibet.app.util.Fmt

/* ================= آیکون‌ها — نگاشت نام lucide به آیکون متریال ================= */

object JibetIcon {
    private val map: Map<String, ImageVector> = mapOf(
        "banknote" to Icons.Rounded.Payments,
        "wallet" to Icons.Rounded.AccountBalanceWallet,
        "credit-card" to Icons.Rounded.CreditCard,
        "piggy-bank" to Icons.Rounded.Savings,
        "more-horizontal" to Icons.Rounded.MoreHoriz,
        "utensils" to Icons.Rounded.Restaurant,
        "car" to Icons.Rounded.DirectionsCar,
        "home" to Icons.Rounded.Home,
        "gamepad" to Icons.Rounded.SportsEsports,
        "book" to Icons.Rounded.MenuBook,
        "shopping-bag" to Icons.Rounded.ShoppingBag,
        "receipt" to Icons.Rounded.Receipt,
        "heart" to Icons.Rounded.Favorite,
        "shirt" to Icons.Rounded.Checkroom,
        "briefcase" to Icons.Rounded.Work,
        "tag" to Icons.Rounded.Sell,
        "gift" to Icons.Rounded.Redeem,
        "trending-up" to Icons.Rounded.TrendingUp,
        "plane" to Icons.Rounded.Flight,
        "laptop" to Icons.Rounded.Laptop,
        "graduation" to Icons.Rounded.School,
        "phone" to Icons.Rounded.Smartphone,
        "popcorn" to Icons.Rounded.LocalMovies,
        "droplets" to Icons.Rounded.WaterDrop,
        "clapperboard" to Icons.Rounded.LocalMovies,
        "music" to Icons.Rounded.MusicNote,
        "bus" to Icons.Rounded.DirectionsBus,
        "baby" to Icons.Rounded.ChildCare,
        "ring" to Icons.Rounded.Diamond,
        "zap" to Icons.Rounded.Bolt,
        "wifi" to Icons.Rounded.Wifi,
        "dumbbell" to Icons.Rounded.FitnessCenter,
        "coffee" to Icons.Rounded.LocalCafe,
        "target" to Icons.Rounded.GpsFixed,
    )

    fun get(name: String): ImageVector = map[name] ?: Icons.Rounded.MoreHoriz
}

/* ================= پس‌زمینه و کارت‌ها ================= */

/** پس‌زمینه‌ی نرم جیبت — دقیقاً مثل .jibet-bg وب */
fun Modifier.jibetBackdrop(): Modifier = drawBehind {
    drawRect(Brush.verticalGradient(listOf(BgA, BgB)))
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0x246E7BFF), Color(0x006E7BFF)),
            center = Offset(size.width * 0.85f, -size.height * 0.10f),
            radius = size.width * 1.2f,
        )
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0x1A4F8CFF), Color(0x004F8CFF)),
            center = Offset(0f, 0f),
            radius = size.width,
        )
    )
}

/** کارت سفید نرم — مثل .jb-card وب */
fun Modifier.jibetCard(shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .shadow(
        elevation = 10.dp,
        shape = shape,
        ambientColor = Color(0x145B74E9),
        spotColor = Color(0x2E5B74E9),
    )
    .clip(shape)
    .background(Color.White)

/** سطوح شیشه‌ای — مثل .jb-glass وب (بلور تقریبی با گرادیان نیمه‌شفاف + هایلایت داخلی لبه بالا) */
fun Modifier.jibetGlass(shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .shadow(
        elevation = 12.dp,
        shape = shape,
        ambientColor = Color(0x145B74E9),
        spotColor = Color(0x405B74E9),
    )
    .clip(shape)
    .background(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.74f), Color.White.copy(alpha = 0.5f)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    )
    // حاشیه‌ی گرادیانی = هایلایت inset «0 1px 0 rgba(255,255,255,.7)» وب
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.70f), Color.White.copy(alpha = 0.15f))
        ),
        shape,
    )

/** افکت فشردن نرم — مثل .jb-pressable وب (scale .97 + آلفا .۹ حین فشردن) */
@Composable
fun Modifier.pressableScale(
    onClick: (() -> Unit)? = null,
    pressedScale: Float = 0.97f,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(150),
        label = "press",
    )
    val base = this
        .scale(scale)
        .alpha(if (pressed) 0.9f else 1f)
        .then(
            if (onClick != null) {
                Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
            } else {
                Modifier
            }
        )
    return base
}

/** گرادیان رنگ اصلی */
@Composable
fun accentGradient(horizontal: Boolean = false): Brush {
    val c = LocalJibetColors.current
    return if (horizontal) {
        Brush.horizontalGradient(listOf(c.accentB, c.accentA))
    } else {
        Brush.linearGradient(listOf(c.accentB, c.accentA))
    }
}

/* ================= اجزای پایه ================= */

/** حباب آیکون رنگی — مثل IconBubble وب */
@Composable
fun IconBubble(
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.5f,
) {
    val isSoft = color == Color(0xFF8A93A6)
    val bg = if (isSoft) Color(0xFFF1F3F9) else color.copy(alpha = 0.11f)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = JibetIcon.get(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** لوگوی جیبت — کیف پول گرادیانی */
@Composable
fun JibetLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(size * 0.28f),
                spotColor = Color(0x8C5B74E9),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(size * 0.28f))
            .background(accentGradient())
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.1f))
                ),
                RoundedCornerShape(size * 0.28f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Wallet,
            contentDescription = "جیبت",
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

/** دکمه‌ی دایره‌ای شیشه‌ای ۴۰dp — برای هدرها */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .jibetGlass(shape = CircleShape)
            .pressableScale(onClick = onClick, pressedScale = 0.94f)
            .size(size),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** هدر صفحه با دکمه‌ی بازگشت اختیاری */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(BgA.copy(alpha = 0.95f), BgA.copy(alpha = 0f))
                    )
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) {
            GlassIconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = TextMid,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        action?.invoke(this)
    }
}

/** چیپ فیلتر */
@Composable
fun JibetChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    leadingIcon: ImageVector? = null,
) {
    val jc = LocalJibetColors.current
    Row(
        modifier
            .then(
                if (active) Modifier
                    .shadow(8.dp, RoundedCornerShape(50), spotColor = (color ?: jc.accentA).copy(alpha = 0.45f))
                    .clip(RoundedCornerShape(50))
                    .background(
                        color?.let { Brush.linearGradient(listOf(it, it)) }
                            ?: Brush.linearGradient(listOf(jc.accentB, jc.accentA))
                    )
                else Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.8f))
            )
            .pressableScale(onClick = onClick, pressedScale = 0.96f)
            .padding(horizontal = 14.dp)
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = if (active) Color.White else TextMid,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            label,
            style = TextStyle(
                fontFamily = com.jibet.app.ui.theme.Vazir,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (active) Color.White else TextMid,
            ),
        )
    }
}

/** سگمنت تب (هزینه/درآمد یا ماهانه/هفتگی/سالانه) */
@Composable
fun <T> JibetSegmented(
    value: T,
    options: List<Pair<T, String>>,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color? = null,
) {
    val jc = LocalJibetColors.current
    Row(
        modifier
            .jibetGlass(shape = RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        options.forEach { (v, label) ->
            val active = v == value
            Box(
                Modifier
                    .weight(1f)
                    .then(
                        if (active) Modifier
                            .shadow(10.dp, RoundedCornerShape(50), spotColor = jc.accentA.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(50))
                            .background(
                                activeColor?.let { Brush.linearGradient(listOf(it, it)) }
                                    ?: Brush.linearGradient(listOf(jc.accentB, jc.accentA))
                            )
                        else Modifier.clip(RoundedCornerShape(50))
                    )
                    .pressableScale(onClick = { onChange(v) }, pressedScale = 0.97f)
                    .height(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = TextStyle(
                        fontFamily = com.jibet.app.ui.theme.Vazir,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (active) Color.White else InkSoft,
                    ),
                )
            }
        }
    }
}

/** نوار پیشرفت گرد و نرم */
@Composable
fun JibetProgress(
    pct: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalJibetColors.current.accentA,
    height: Dp = 8.dp,
    track: Color = Color(0x248A92A6),
) {
    val w = pct.coerceIn(0f, 100f)
    val animated by animateFloatAsState(w / 100f, tween(500), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** متن مبلغ رنگی بر اساس نوع — مثل AmountText وب */
@Composable
fun AmountText(
    amount: Double,
    kind: Kind,
    modifier: Modifier = Modifier,
    withSign: Boolean = true,
    suffix: String = "تومان",
    style: TextStyle = TextStyle(
        fontFamily = com.jibet.app.ui.theme.Vazir,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
    ),
    suffixStyle: TextStyle = style.copy(fontSize = style.fontSize * 0.72f, fontWeight = FontWeight.Medium, color = InkSoft),
) {
    val jc = LocalJibetColors.current
    val color = if (kind == Kind.INCOME) jc.income else jc.expense
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (withSign) {
            Icon(
                if (kind == Kind.INCOME) Icons.Rounded.Add else Icons.Rounded.Remove,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(style.fontSize.value.dp * 0.95f),
            )
            Box(Modifier.width(2.dp))
        }
        Text(Fmt.money(amount), style = style.copy(color = color))
        if (suffix.isNotEmpty()) {
            Box(Modifier.width(3.dp))
            Text(suffix, style = suffixStyle.copy(color = suffixStyle.color))
        }
    }
}

/** حالت خالی */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
) {
    val jc = LocalJibetColors.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(jc.accentA.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon ?: Icons.Rounded.Inbox,
                contentDescription = null,
                tint = jc.accentA.copy(alpha = 0.8f),
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            title,
            style = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink),
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                description,
                style = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 24.sp, color = InkSoft),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** دکمه‌ی اصلی گرادیانی — مثل AccentButton وب که همیشه w-full است */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val jc = LocalJibetColors.current
    Row(
        modifier
            .fillMaxWidth()
            .shadow(
                16.dp,
                RoundedCornerShape(16.dp),
                spotColor = jc.accentA.copy(alpha = 0.55f),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(accentGradient())
            .alpha(if (enabled) 1f else 0.5f)
            .pressableScale(onClick = if (enabled) onClick else null, pressedScale = 0.98f)
            .height(52.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        if (leading != null) Box(Modifier.width(8.dp))
        Text(
            text,
            style = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White),
        )
    }
}

/** کیپد عددی فارسی — قرارداد یکسان با Keypad وب */
@Composable
fun JibetKeypad(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 12,
) {
    val press: (String) -> Unit = { key ->
        when {
            key == "del" -> onChange(value.dropLast(1))
            (key == "0" || key == "000") && value.isEmpty() -> {}
            value.length < maxLength -> onChange((value + key).take(maxLength))
        }
    }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "000", "0", "del")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowKeys.forEach { k ->
                    Box(
                        Modifier
                            .weight(1f)
                            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x405B74E9), ambientColor = Color.Transparent)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .pressableScale(onClick = { press(k) }, pressedScale = 0.95f)
                            .height(54.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (k == "del") {
                            Icon(
                                Icons.Rounded.Backspace,
                                contentDescription = "حذف",
                                tint = InkSoft,
                                modifier = Modifier.size(22.dp),
                            )
                        } else {
                            Text(
                                // «۰۰۰» باید سه صفر دیده شود، نه مثل «۰» فشرده شود
                                if (k == "000") Fmt.faDigits("000") else Fmt.money(k.toLong()),
                                style = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Ink),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** ورودی مبلغ — نمایش فرمت‌شده با جداکننده + مکان‌نمای پایدار
 *  (پورت الگوی BalanceField آنبوردینگ: متن خام بالادست نگه داشته می‌شود و
 *  تعداد رقم‌های پیش از مکان‌نما حفظ می‌شود تا کرسر با هر فرمت‌سازی نپرد) */
@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "۰",
) {
    val jc = LocalJibetColors.current
    var fieldValue by remember { mutableStateOf(TextFieldValue("")) }

    // سینک با مقدار بیرونی — فقط وقتی واقعاً بیرون عوض شده (نه بعد از هر تایپ)
    LaunchedEffect(value) {
        val needsSync =
            Fmt.parseNumber(fieldValue.text) != Fmt.parseNumber(value) ||
            fieldValue.text.isEmpty() != value.isEmpty()
        if (needsSync) {
            val formatted = if (value.isEmpty()) "" else Fmt.money(Fmt.parseNumber(value))
            fieldValue = TextFieldValue(formatted, TextRange(formatted.length))
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier
                .height(48.dp)
                .jibetInputShape(),
        ) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { input ->
                    // تعداد رقم‌های پیش از مکان‌نمای کاربر حفظ می‌شود تا پرش نکند
                    val digitsBefore = input.text.take(input.selection.start).count { it.isDigit() }
                    val formatted = if (input.text.isEmpty()) "" else Fmt.money(Fmt.parseNumber(input.text))
                    var idx = 0
                    var count = 0
                    for ((i, c) in formatted.withIndex()) {
                        if (count >= digitsBefore) break
                        if (c.isDigit()) count++
                        idx = i + 1
                    }
                    fieldValue = TextFieldValue(formatted, TextRange(idx))
                    onValueChange(input.text)
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Ink),
                cursorBrush = SolidColor(jc.accentA),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            if (fieldValue.text.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    placeholder,
                    style = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Faint),
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }
    }
}

/** فیلد ورودی استاندارد جیبت */
@Composable
fun JibetField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        Text(
            label,
            style = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextMid),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        content()
    }
}

/** استایل فیلد متنی — مثل inputClass وب */
@Composable
fun Modifier.jibetInputShape(): Modifier = this
    .clip(RoundedCornerShape(16.dp))
    .background(Color.White.copy(alpha = 0.85f))
    .border(1.dp, InkSoft.copy(alpha = 0.18f), RoundedCornerShape(16.dp))

/** تیتر بخش داخل شیت‌ها و فرم‌ها */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = TextStyle(fontFamily = com.jibet.app.ui.theme.Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextMid),
        modifier = modifier.padding(bottom = 8.dp),
    )
}
