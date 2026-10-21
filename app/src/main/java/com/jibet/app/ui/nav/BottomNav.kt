package com.jibet.app.ui.nav

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jibet.app.ui.components.JibetIcon
import com.jibet.app.ui.components.accentGradient
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir

data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    NavTab("home", "خانه", Icons.Rounded.Home),
    NavTab("transactions", "تراکنش‌ها", Icons.Rounded.ReceiptLong),
    NavTab("budget", "بودجه", Icons.Rounded.PieChart),
    NavTab("more", "بیشتر", Icons.Rounded.MoreHoriz),
)

/** نویگیشن پایین شیشه‌ای با دکمه‌ی شناور + — مثل BottomNav.tsx وب */
@Composable
fun JibetBottomNav(
    current: String,
    onTab: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val jc = LocalJibetColors.current
        Row(
            Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 420.dp)
                .jibetGlass(shape = RoundedCornerShape(26.dp))
                .shadow(
                    20.dp,
                    RoundedCornerShape(26.dp),
                    spotColor = if (jc.isDark) Color(0x66000000) else Color(0x47232B3B),
                    ambientColor = Color.Transparent,
                )
                .clip(RoundedCornerShape(26.dp))
                .background(Color.Transparent)
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            TABS.slice(0..1).forEach { tab ->
                NavTabButton(tab = tab, active = current == tab.route, onClick = { onTab(tab.route) })
            }

            // دکمه‌ی + — نیمه‌شناور روی لبه‌ی بالایی نویگیشن
            Box(
                Modifier
                    .width(64.dp)
                    .height(52.dp),
            ) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(120), label = "fab")
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-26).dp)
                        .scale(scale)
                        .shadow(
                            16.dp,
                            CircleShape,
                            spotColor = LocalJibetColors.current.accentA.copy(alpha = 0.6f),
                            ambientColor = Color.Transparent,
                        )
                        .clip(CircleShape)
                        .background(accentGradient())
                        .size(56.dp)
                        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "ثبت تراکنش جدید",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            TABS.slice(2..3).forEach { tab ->
                NavTabButton(tab = tab, active = current == tab.route, onClick = { onTab(tab.route) })
            }
        }
    }
}

@Composable
private fun NavTabButton(tab: NavTab, active: Boolean, onClick: () -> Unit) {
    val jc = LocalJibetColors.current
    val color = if (active) jc.accentA else jc.iconMuted
    Column(
        Modifier
            .width(68.dp)
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
