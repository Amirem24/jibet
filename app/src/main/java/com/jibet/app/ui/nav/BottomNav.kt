package com.jibet.app.ui.nav

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jibet.app.ui.components.JibetIcon
import com.jibet.app.ui.components.accentGradient
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    NavTab("home", "خانه", Icons.Rounded.Home),
    NavTab("transactions", "تراکنش‌ها", Icons.Rounded.ReceiptLong),
    NavTab("budget", "بودجه", Icons.Rounded.PieChart),
    NavTab("more", "بیشتر", Icons.Rounded.MoreHoriz),
)

/** نویگیشن پایین با دکمه‌ی + کامل و برجسته — مثل BottomNav نسخه وب.
 *  FIX (UI): قبلاً دکمه‌ی + فرزندِ ردیفِ clip‌شده‌ی قرص بود و به‌صورت نیم‌دایره‌ی بریده
 *  رندر می‌شد. حالا قرص دقیقاً مثل وب است: شیشه‌ای (jb-glass: گرادیان سفید نیمه‌شفاف +
 *  بوردر روشن + سایه‌ی نرم و پهن 0 18px 40px -16px rgba(35,43,59,.28))، عرض
 *  min(94%, 420px)، شعاع 26، و دکمه‌ی + خواهرِ آن و بدون clip، مرکزش دقیقاً روی
 *  لبه‌ی بالایی قرص (offset ‎-28dp = نصف قطر) با هایلایت داخلی لبه‌ی بالا مثل وب. */
@Composable
fun JibetBottomNav(
    current: String,
    onTab: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val jc = LocalJibetColors.current
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 420.dp)
                .height(64.dp)
                .shadow(
                    18.dp,
                    RoundedCornerShape(26.dp),
                    spotColor = if (jc.isDark) Color(0x66000000) else Color(0x47232B3B),
                    ambientColor = Color.Transparent,
                )
                .clip(RoundedCornerShape(26.dp))
                // شیشه‌ی وب (jb-glass): بلور واقعی پس‌زمینه (backdrop-filter: blur(22px)) با Haze
                // + tint سفید نیمه‌شفاف؛ بدون Haze (API پایین) گرادیان شیشه‌ای جایگزین می‌شود
                .then(
                    if (hazeState != null) {
                        Modifier.hazeChild(
                            state = hazeState,
                            shape = RoundedCornerShape(26.dp),
                        ) {
                            backgroundColor = jc.bgB
                            tints = listOf(HazeTint(jc.glassTop.copy(alpha = if (jc.isDark) 0.32f else 0.55f)))
                            blurRadius = 16.dp
                            noiseFactor = 0f
                        }
                    } else {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(jc.glassTop, jc.glassBottom),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                            )
                        )
                    }
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(jc.glassBorder, jc.glassBorder.copy(alpha = jc.glassBorder.alpha * 0.2f))),
                    RoundedCornerShape(26.dp),
                )
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TABS.slice(0..1).forEach { tab ->
                NavTabButton(tab = tab, active = current == tab.route, onClick = { onTab(tab.route) }, modifier = Modifier.width(68.dp))
            }
            // جای خالی دکمه‌ی مرکزی (w-16 وب)
            Spacer(Modifier.width(64.dp))
            TABS.slice(2..3).forEach { tab ->
                NavTabButton(tab = tab, active = current == tab.route, onClick = { onTab(tab.route) }, modifier = Modifier.width(68.dp))
            }
        }
        // دکمه‌ی + — دایره‌ی کامل، مرکزش روی لبه‌ی بالایی قرص (مثل نسخه وب)
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(120), label = "fab")
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
                .scale(scale)
                .shadow(
                    16.dp,
                    CircleShape,
                    spotColor = LocalJibetColors.current.accentA.copy(alpha = 0.55f),
                    ambientColor = Color.Transparent,
                )
                .clip(CircleShape)
                .background(accentGradient())
                .size(56.dp)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            // هایلایت داخلی لبه‌ی بالا — inset 0 2px 4px rgba(255,255,255,.4) وب
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.38f), Color.Transparent),
                            startY = 0f,
                            endY = 26f,
                        )
                    )
            )
            Icon(
                Icons.Rounded.Add,
                contentDescription = "ثبت تراکنش جدید",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun NavTabButton(tab: NavTab, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val jc = LocalJibetColors.current
    val color = if (active) jc.accentA else jc.iconMuted
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick, role = Role.Tab)
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Text(
            tab.label,
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
            color = color,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
