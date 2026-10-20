package com.jibet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jibet.app.data.TxRow
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.InkSoft
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.Fmt
import com.jibet.app.util.JDate

/** گروه یک روز برای لیست گروه‌بندی‌شده */
data class TxDayGroup(val key: String, val label: String, val net: Double, val items: List<TxRow>)

/** گروه‌بندی تراکنش‌ها بر اساس روز (میلادی-محلی) با برچسب شمسی — مثل groupTxsByDay وب */
fun groupTxsByDay(items: List<TxRow>): List<TxDayGroup> {
    val map = LinkedHashMap<String, TxDayGroup>()
    for (tx in items) {
        val d = JDate.localDate(tx.date)
        val key = "${d.year}-${d.monthValue}-${d.dayOfMonth}"
        val delta = if (tx.kind == com.jibet.app.data.Kind.INCOME) tx.amount else -tx.amount
        val existing = map[key]
        map[key] = if (existing == null) {
            TxDayGroup(key, JDate.dayGroupLabel(tx.date), delta, listOf(tx))
        } else {
            existing.copy(net = existing.net + delta, items = existing.items + tx)
        }
    }
    return map.values.toList()
}

/** ردیف تراکنش — مثل ListRow وب */
@Composable
fun TxRowItem(
    tx: TxRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleOverride: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .pressableScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBubble(
            icon = tx.catIcon ?: "more-horizontal",
            color = parseColor(tx.catColor),
        )
        Column(Modifier.weight(1f)) {
            Text(
                tx.title,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                subtitleOverride
                    ?: "${tx.catName ?: "بدون دسته"} · ${tx.accName ?: "بدون حساب"}",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        AmountText(amount = tx.amount, kind = tx.kind, style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
    }
}

/** تبدیل رشته hex به Color (بهینه برای رشته‌های #RRGGBB) */
fun parseColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF8A93A6)
    return try {
        val s = hex.removePrefix("#")
        Color(android.graphics.Color.parseColor("#$s"))
    } catch (_: Exception) {
        Color(0xFF8A93A6)
    }
}

/** بج خالص روز */
@Composable
fun NetBadge(net: Double, modifier: Modifier = Modifier) {
    val jc = LocalJibetColors.current
    if (net == 0.0) {
        Text(
            "۰ تومان",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFB4BAC8)),
            modifier = modifier,
        )
    } else {
        val isIncome = net > 0
        Text(
            (if (isIncome) "+" else "") + Fmt.money(net),
            style = TextStyle(
                fontFamily = Vazir,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = if (isIncome) jc.income else jc.expense,
            ),
            modifier = modifier,
        )
    }
}

/** خلاصه‌ی فیلتر فعلی: درآمد / هزینه / تعداد */
@Composable
fun TxSummaryLine(income: Double, expense: Double, total: Int, modifier: Modifier = Modifier) {
    val jc = LocalJibetColors.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "درآمد: ${Fmt.money(income)} تومان",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = jc.income),
            )
            Text(
                "هزینه: ${Fmt.money(expense)} تومان",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = jc.expense),
            )
        }
        Text(
            "${Fmt.faDigits(total)} مورد",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = InkSoft),
        )
    }
}

/** لیست گروه‌بندی‌شده بر اساس روز — مشترک بین صفحه‌ی تراکنش‌ها و جستجو */
@Composable
fun TxGroupedList(
    groups: List<TxDayGroup>,
    onOpen: (TxRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        groups.forEach { g ->
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Fmt.faDigits(g.label),
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = InkSoft),
                    )
                    NetBadge(net = g.net)
                }
                Column(
                    Modifier
                        .jibetCard(shape = RoundedCornerShape(24.dp))
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                ) {
                    g.items.forEach { tx ->
                        TxRowItem(tx = tx, onClick = { onOpen(tx) })
                    }
                }
            }
        }
    }
}
