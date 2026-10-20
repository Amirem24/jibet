package com.jibet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.util.Fmt

val LocalCurrency = staticCompositionLocalOf { "تومان" }

fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF2A9D8F)
}

/** Soft translucent "liquid glass" card. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val dark = cs.background.luminance() < 0.5f
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .background(
                brush = Brush.linearGradient(
                    listOf(cs.surface.copy(alpha = 0.88f), cs.surface.copy(alpha = 0.62f))
                ),
                shape = shape
            )
            .border(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.55f), shape)
            .clip(shape)
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Amount text with Persian digits; isIncome colors teal/coral, null = neutral. */
@Composable
fun AmountText(
    amount: Double,
    modifier: Modifier = Modifier,
    isIncome: Boolean? = null,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    withCurrency: Boolean = true,
    color: Color? = null
) {
    val jc = LocalJibetColors.current
    val currency = LocalCurrency.current
    val resolved = color ?: when (isIncome) {
        true -> jc.income
        false -> jc.expense
        null -> Color.Unspecified
    }
    Text(
        text = Fmt.money(amount) + if (withCurrency) " " + currency else "",
        style = style,
        fontWeight = FontWeight.SemiBold,
        color = resolved,
        modifier = modifier
    )
}

@Composable
fun EmojiBadge(emoji: String, colorHex: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(parseHex(colorHex).copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String = "", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 52.sp)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String = "حذف",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

/** Soft rounded progress bar (default height 8dp; caller may override via modifier). */
@Composable
fun ProgressBarSoft(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    track: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier
            .height(8.dp)
            .fillMaxWidth()
            .background(track, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .background(color, RoundedCornerShape(8.dp))
        )
    }
}
