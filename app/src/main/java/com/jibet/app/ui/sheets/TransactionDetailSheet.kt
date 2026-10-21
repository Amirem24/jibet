package com.jibet.app.ui.sheets

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.UiBus
import com.jibet.app.data.AccountRow
import com.jibet.app.data.CategoryEntity
import com.jibet.app.data.Kind
import com.jibet.app.data.TxRow
import com.jibet.app.ui.components.AccentButton
import com.jibet.app.ui.components.AmountInput
import com.jibet.app.ui.components.AmountText
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.JibetField
import com.jibet.app.ui.components.JibetSheet
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.Expense
import com.jibet.app.ui.theme.ExpenseSoft
import com.jibet.app.ui.theme.Income
import com.jibet.app.ui.theme.IncomeSoft
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.InkSoft
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.TextMid
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.Fmt
import com.jibet.app.util.JDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** رنگ متن روی پس‌زمینه‌ی jb-accent-soft — color-mix(accentA 80%, #232B3B 20%) وب */
private fun accentSoftText(accent: Color): Color = lerp(accent, Color(0xFF232B3B), 0.2f)

/** شیت جزئیات تراکنش: نمایش + ویرایش + حذف دو مرحله‌ای (پورت transaction-detail.tsx) */
@Composable
fun TransactionDetailSheet() {
    val txId by UiBus.detailTxId.collectAsState()
    val id = txId
    if (id != null) {
        val vm: TransactionDetailViewModel = viewModel()
        // flow با remember(id) ثابت می‌ماند تا با هر recomposition دوباره جمع نشود
        val txFlow = remember(id) { vm.watchTx(id) }
        val tx by txFlow.collectAsState(initial = null)

        JibetSheet(onDismiss = { UiBus.closeDetail() }, title = "جزئیات تراکنش") {
            DetailBody(vm = vm, tx = tx)
        }
    }
}

/** ویومدل جزئیات تراکنش */
class TransactionDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as JibetApp).repository

    /** true حین ویرایش/حذف — دکمه‌ها را غیرفعال می‌کند */
    val busy = MutableStateFlow(false)

    val accounts: StateFlow<List<AccountRow>> =
        repo.watchAccounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        repo.watchCategories().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun watchTx(id: Long): Flow<TxRow?> = repo.watchTx(id)

    fun updateTx(
        txId: Long,
        title: String,
        amount: Double,
        categoryId: Long?,
        accountId: Long?,
        note: String,
    ) {
        if (busy.value) return
        val app = getApplication<JibetApp>()
        if (amount <= 0.0) {
            Toast.makeText(app, "مبلغ را وارد کن", Toast.LENGTH_SHORT).show()
            return
        }
        val catId = categoryId
        val accId = accountId
        if (catId == null || accId == null) {
            Toast.makeText(app, "دسته و حساب را انتخاب کن", Toast.LENGTH_SHORT).show()
            return
        }
        busy.value = true
        viewModelScope.launch {
            try {
                val entity = repo.dao.getTx(txId)
                if (entity != null) {
                    repo.updateTx(
                        entity.copy(
                            title = title.trim().ifEmpty { "تراکنش" },
                            amount = amount,
                            categoryId = catId,
                            accountId = accId,
                            note = note.trim().ifEmpty { null },
                        )
                    )
                    Toast.makeText(app, "تراکنش ویرایش شد", Toast.LENGTH_SHORT).show()
                    UiBus.closeDetail()
                } else {
                    Toast.makeText(app, "خطا در ویرایش تراکنش", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(app, "خطا در ویرایش تراکنش", Toast.LENGTH_SHORT).show()
            } finally {
                busy.value = false
            }
        }
    }

    fun delete(txId: Long) {
        if (busy.value) return
        val app = getApplication<JibetApp>()
        busy.value = true
        viewModelScope.launch {
            try {
                val entity = repo.dao.getTx(txId)
                if (entity != null) {
                    repo.deleteTx(entity)
                    Toast.makeText(app, "تراکنش حذف شد", Toast.LENGTH_SHORT).show()
                    UiBus.closeDetail()
                } else {
                    Toast.makeText(app, "خطا در حذف تراکنش", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(app, "خطا در حذف تراکنش", Toast.LENGTH_SHORT).show()
            } finally {
                busy.value = false
            }
        }
    }
}

@Composable
private fun DetailBody(vm: TransactionDetailViewModel, tx: TxRow?) {
    val busy by vm.busy.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()

    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // فرم ویرایش
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }

    // با باز/بسته شدن شیت یا تعویض تراکنش، وضعیت ریست شود
    LaunchedEffect(tx) {
        if (tx != null) {
            editing = false
            confirmDelete = false
            title = tx.title
            amountStr = amountToRawString(tx.amount)
            categoryId = tx.categoryId
            accountId = tx.accountId
            note = tx.note ?: ""
        } else {
            editing = false
            confirmDelete = false
        }
    }

    if (tx == null) return

    // بازگردانی مقادیر فرم به حالت اصلی تراکنش
    fun resetForm(t: TxRow) {
        editing = false
        confirmDelete = false
        title = t.title
        amountStr = amountToRawString(t.amount)
        categoryId = t.categoryId
        accountId = t.accountId
        note = t.note ?: ""
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        if (!editing) {
            DetailView(
                vm = vm,
                tx = tx,
                confirmDelete = confirmDelete,
                busy = busy,
                onEdit = { editing = true },
                onDeleteRequest = { confirmDelete = true },
                onCancelDelete = { confirmDelete = false },
            )
        } else {
            DetailEdit(
                vm = vm,
                tx = tx,
                accounts = accounts,
                categories = categories,
                busy = busy,
                title = title,
                onTitleChange = { title = it },
                amountStr = amountStr,
                onAmountChange = { amountStr = it },
                categoryId = categoryId,
                onCategoryChange = { categoryId = it },
                accountId = accountId,
                onAccountChange = { accountId = it },
                note = note,
                onNoteChange = { note = it },
                onCancel = { resetForm(tx) },
            )
        }
    }
}

/** مقدار عددی وب‌سان از Double — مثل String(amount) جاوااسکریپت بدون «.0» */
private fun amountToRawString(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

/** نمای نمایش تراکنش */
@Composable
private fun DetailView(
    vm: TransactionDetailViewModel,
    tx: TxRow,
    confirmDelete: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    val jc = LocalJibetColors.current
    val density = LocalDensity.current
    val slidePx = with(density) { 6.dp.roundToPx() }

    Column {
        // سرشیت: آیکون، عنوان، مبلغ، نوع
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconBubble(
                icon = tx.catIcon ?: "more-horizontal",
                color = parseColor(tx.catColor),
                size = 58.dp,
                iconSize = 26.dp,
            )
            Text(
                tx.title,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            AmountText(
                amount = tx.amount,
                kind = tx.kind,
                withSign = false,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 28.sp),
            )
            Row(
                Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KindBadge(kind = tx.kind)
                if (tx.recurringId != null) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(jc.accentA.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = null,
                            tint = accentSoftText(jc.accentA),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            "تراکنش تکراری",
                            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = accentSoftText(jc.accentA)),
                        )
                    }
                }
            }
        }

        // اطلاعات
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .jibetCard()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            InfoRow(label = "دسته") {
                IconBubble(
                    icon = tx.catIcon ?: "more-horizontal",
                    color = parseColor(tx.catColor),
                    size = 28.dp,
                    iconSize = 14.dp,
                )
                Text(
                    tx.catName ?: "بدون دسته",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink),
                )
            }
            HorizontalDivider(color = Color(0xFFF0F2F8), thickness = 1.dp)
            InfoRow(label = "حساب") {
                IconBubble(
                    icon = tx.accIcon ?: "more-horizontal",
                    color = parseColor(tx.accColor),
                    size = 28.dp,
                    iconSize = 14.dp,
                )
                Text(
                    tx.accName ?: "بدون حساب",
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink),
                )
            }
            HorizontalDivider(color = Color(0xFFF0F2F8), thickness = 1.dp)
            InfoRow(label = "تاریخ") {
                Text(
                    Fmt.faDigits(JDate.formatJalaliDate(tx.date, JDate.Style.LONG)),
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink),
                )
                Text(
                    "· ساعت " + Fmt.faDigits(JDate.timeLabel(tx.date)),
                    style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = InkSoft),
                )
            }
            if (!tx.note.isNullOrBlank()) {
                HorizontalDivider(color = Color(0xFFF0F2F8), thickness = 1.dp)
                InfoRow(label = "یادداشت") {
                    Text(
                        tx.note,
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 20.sp, color = Ink),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // دکمه‌ها — حذف با تایید دو مرحله‌ای inline
        if (!confirmDelete) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(jc.accentA.copy(alpha = 0.12f))
                        .pressableScale(onClick = onEdit)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = accentSoftText(jc.accentA),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "ویرایش",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentSoftText(jc.accentA)),
                    )
                }
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ExpenseSoft)
                        .pressableScale(onClick = onDeleteRequest)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color(0xFFD64545),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "حذف",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFD64545)),
                    )
                }
            }
        } else {
            AnimatedVisibility(
                visible = confirmDelete,
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { slidePx },
                exit = ExitTransition.None,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                ) {
                    Text(
                        "مطمئنی؟ این تراکنش حذف می‌شود",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier
                                .weight(1f)
                                .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color(0x99F26D6D), ambientColor = Color.Transparent)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Expense)
                                .alpha(if (busy) 0.6f else 1f)
                                .pressableScale(onClick = if (!busy) ({ vm.delete(tx.id) }) else null)
                                .height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                "بله، حذف کن",
                                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White),
                            )
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .jibetGlass(RoundedCornerShape(16.dp))
                                .alpha(if (busy) 0.6f else 1f)
                                .pressableScale(onClick = if (!busy) onCancelDelete else null)
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "انصراف",
                                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextMid),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** نمای ویرایش تراکنش */
@Composable
private fun DetailEdit(
    vm: TransactionDetailViewModel,
    tx: TxRow,
    accounts: List<AccountRow>,
    categories: List<CategoryEntity>,
    busy: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    amountStr: String,
    onAmountChange: (String) -> Unit,
    categoryId: Long?,
    onCategoryChange: (Long?) -> Unit,
    accountId: Long?,
    onAccountChange: (Long?) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val cats = categories.filter { it.kind == tx.kind }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        JibetField(label = "عنوان") {
            SheetTextInput(
                value = title,
                onValueChange = onTitleChange,
                placeholder = "مثلاً ناهار",
            )
        }

        JibetField(label = "مبلغ (تومان)") {
            // فرمت‌شده + مکان‌نمای پایدار (الگوی BalanceField آنبوردینگ — LTR مثل وب داخل AmountInput است)
            AmountInput(
                value = amountStr,
                onValueChange = onAmountChange,
                placeholder = "۰",
            )
        }

        // دسته — فقط هم‌نوع
        Column {
            Text(
                "دسته‌بندی",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextMid),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (cats.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "دسته‌ای موجود نیست",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, color = InkSoft),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cats.forEach { c ->
                        PickChip(
                            label = c.name,
                            colorHex = c.colorHex,
                            icon = c.icon,
                            active = c.id == categoryId,
                            onClick = { onCategoryChange(c.id) },
                        )
                    }
                }
            }
        }

        // حساب
        Column {
            Text(
                "حساب",
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextMid),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                accounts.forEach { a ->
                    PickChip(
                        label = a.name,
                        colorHex = a.colorHex,
                        icon = a.icon,
                        active = a.id == accountId,
                        onClick = { onAccountChange(a.id) },
                    )
                }
            }
        }

        JibetField(label = "یادداشت (اختیاری)") {
            SheetTextInput(
                value = note,
                onValueChange = onNoteChange,
                placeholder = "یادداشت…",
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .jibetGlass(RoundedCornerShape(16.dp))
                    .alpha(if (busy) 0.6f else 1f)
                    .pressableScale(onClick = if (!busy) onCancel else null)
                    .height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        tint = TextMid,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "انصراف",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextMid),
                    )
                }
            }
            AccentButton(
                text = "",
                onClick = {
                    vm.updateTx(
                        txId = tx.id,
                        title = title,
                        amount = Fmt.parseNumber(amountStr),
                        categoryId = categoryId,
                        accountId = accountId,
                        note = note,
                    )
                },
                enabled = !busy,
                leading = {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                },
                modifier = Modifier.weight(1.7f),
            )
        }
    }
}

/** بج نوع تراکنش */
@Composable
private fun KindBadge(kind: Kind) {
    val isIncome = kind == Kind.INCOME
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isIncome) IncomeSoft else ExpenseSoft)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            if (isIncome) "درآمد" else "هزینه",
            style = TextStyle(
                fontFamily = Vazir,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isIncome) Income else Expense,
            ),
        )
    }
}

/** ردیف اطلاعات جزئیات */
@Composable
private fun InfoRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = InkSoft),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

/** چیپ انتخاب دسته/حساب با حباب آیکون — مثل Chip وب */
@Composable
private fun PickChip(label: String, colorHex: String, icon: String, active: Boolean, onClick: () -> Unit) {
    val color = parseColor(colorHex)
    Row(
        Modifier
            .then(
                if (active) {
                    Modifier.shadow(4.dp, RoundedCornerShape(50), spotColor = Color(0x1A000000), ambientColor = Color.Transparent)
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(50))
            .background(if (active) color else Color.White.copy(alpha = 0.85f))
            .pressableScale(onClick = onClick)
            .padding(horizontal = 14.dp)
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconBubble(
            icon = icon,
            color = if (active) Color.White else color,
            size = 22.dp,
            iconSize = 11.dp,
        )
        Text(
            label,
            style = TextStyle(
                fontFamily = Vazir,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = if (active) Color.White else TextMid,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
