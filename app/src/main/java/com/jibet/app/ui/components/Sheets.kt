package com.jibet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir

/** شیت پایین صفحه با استایل شیشه‌ای — پایه‌ی همه‌ی فرم‌ها و جزئیات (مثل Sheet.tsx وب) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JibetSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val jc = LocalJibetColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Unspecified,
        scrimColor = jc.scrim,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        // سطح opaque — محتوای صفحه‌ی زیرین نباید از داخل شیت دیده شود
        // سقف ارتفاع ۹۰٪ صفحه — شیت هرگز full-page نمی‌شود (مثل max-h وب، ولی جمع‌وجورتر برای موبایل)
        val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.90f).dp
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .jibetSheetSurface(shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 32.dp),
        ) {
            // دستگیره
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .background(jc.faint.copy(alpha = 0.35f), RoundedCornerShape(50)),
            )
            if (title != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = jc.ink),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(jc.cardSoft, RoundedCornerShape(50)),
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "بستن",
                            tint = jc.inkSoft,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            content()
        }
    }
}
