@file:OptIn(ExperimentalLayoutApi::class)

package com.jibet.app.ui.categories

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Sell
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.CategoryEntity
import com.jibet.app.data.Kind
import com.jibet.app.ui.components.AccentButton
import com.jibet.app.ui.components.EmptyState
import com.jibet.app.ui.components.IconBubble
import com.jibet.app.ui.components.JibetField
import com.jibet.app.ui.components.JibetSegmented
import com.jibet.app.ui.components.JibetSheet
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.SectionLabel
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.jibetGlass
import com.jibet.app.ui.components.jibetInputShape
import com.jibet.app.ui.components.parseColor
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.Expense
import com.jibet.app.ui.theme.Faint
import com.jibet.app.ui.theme.Ink
import com.jibet.app.ui.theme.InkSoft
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.util.CATEGORY_COLORS
import com.jibet.app.util.CATEGORY_ICONS
import com.jibet.app.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/* ================= ViewModel ================= */

class CategoriesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as JibetApp).repository

    val categories: StateFlow<List<CategoryEntity>> =
        repo.watchCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var kind by mutableStateOf(Kind.EXPENSE)
        private set
    var sheetOpen by mutableStateOf(false)
        private set
    var editing by mutableStateOf<CategoryEntity?>(null)
        private set
    var name by mutableStateOf("")
        private set
    var color by mutableStateOf(CATEGORY_COLORS[0])
        private set
    var icon by mutableStateOf("tag")
        private set
    var saving by mutableStateOf(false)
        private set
    var confirmDelete by mutableStateOf(false)
        private set

    fun setKind(v: Kind) {
        kind = v
    }

    fun setName(v: String) {
        if (v.length <= 40) name = v
    }

    fun setColor(c: String) {
        color = c
    }

    fun setIcon(i: String) {
        icon = i
    }

    fun openAdd() {
        editing = null
        name = ""
        color = if (kind == Kind.INCOME) CATEGORY_COLORS[0] else CATEGORY_COLORS[1]
        icon = "tag"
        confirmDelete = false
        sheetOpen = true
    }

    fun openEdit(c: CategoryEntity) {
        editing = c
        name = c.name
        color = c.colorHex
        icon = c.icon
        confirmDelete = false
        sheetOpen = true
    }

    fun close() {
        sheetOpen = false
        confirmDelete = false
    }

    fun save() {
        val n = name.trim()
        if (n.isEmpty() || saving) return
        val ctx = getApplication<JibetApp>()
        val e = editing
        viewModelScope.launch {
            saving = true
            try {
                if (e != null) {
                    // ویرایش فقط نام/رنگ/آیکون را عوض می‌کند — مثل PUT وب
                    repo.updateCategory(e.copy(name = n, colorHex = color, icon = icon))
                } else {
                    repo.addCategory(n, kind, color, icon)
                }
                Toast.makeText(
                    ctx,
                    (if (e != null) "دسته ویرایش شد" else "دسته جدید اضافه شد") + "\n" + n,
                    Toast.LENGTH_SHORT,
                ).show()
                close()
            } catch (ex: Exception) {
                Toast.makeText(ctx, ex.message ?: "خطا در ذخیره‌ی دسته", Toast.LENGTH_SHORT).show()
            } finally {
                saving = false
            }
        }
    }

    fun delete() {
        val e = editing ?: return
        if (!confirmDelete) {
            confirmDelete = true
            return
        }
        val ctx = getApplication<JibetApp>()
        viewModelScope.launch {
            try {
                repo.deleteCategory(e)
                Toast.makeText(ctx, "دسته حذف شد\n${e.name}", Toast.LENGTH_SHORT).show()
                close()
            } catch (ex: IllegalStateException) {
                Toast.makeText(ctx, ex.message ?: "خطا در حذف دسته", Toast.LENGTH_SHORT).show()
                confirmDelete = false
            }
        }
    }
}

/* ================= صفحه ================= */

/** صفحه‌ی دسته‌بندی‌ها — پورت ۱:۱ از screens/categories.tsx وب */
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val vm: CategoriesViewModel = viewModel()
    val jc = LocalJibetColors.current
    val all by vm.categories.collectAsState()
    val categories = all.filter { it.kind == vm.kind }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "دسته‌بندی‌ها",
            onBack = onBack,
            subtitle = "${Fmt.faDigits(categories.size)} دسته",
        )

        FadeUp(0) {
            JibetSegmented(
                value = vm.kind,
                options = listOf(Kind.EXPENSE to "هزینه", Kind.INCOME to "درآمد"),
                onChange = { vm.setKind(it) },
                activeColor = if (vm.kind == Kind.EXPENSE) jc.expense else jc.income,
            )
        }

        // دکمه دسته جدید
        FadeUp(40, Modifier.padding(top = 12.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(jc.accentA.copy(alpha = 0.12f))
                    .pressableScale(onClick = vm::openAdd)
                    .height(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = jc.accentA,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "دسته جدید",
                        style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = jc.accentA),
                    )
                }
            }
        }

        // لیست دسته‌ها
        FadeUp(80, Modifier.padding(top = 12.dp)) {
            if (categories.isEmpty()) {
                Box(Modifier.jibetCard()) {
                    EmptyState(
                        title = "دسته‌ای در این بخش نیست",
                        description = "با دکمه‌ی «دسته جدید» اولین دسته را بسازید.",
                        icon = Icons.Rounded.Sell,
                    )
                }
            } else {
                Column(
                    Modifier
                        .jibetGlass()
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    categories.forEach { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pressableScale(onClick = { vm.openEdit(c) })
                                .clip(RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconBubble(icon = c.icon, color = parseColor(c.colorHex))
                            Text(
                                c.name,
                                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.Rounded.ChevronLeft,
                                contentDescription = null,
                                tint = Faint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    CategorySheet(vm)
}

/* ================= شیت افزودن/ویرایش دسته ================= */

@Composable
private fun CategorySheet(vm: CategoriesViewModel) {
    if (!vm.sheetOpen) return
    val jc = LocalJibetColors.current
    JibetSheet(
        onDismiss = vm::close,
        title = if (vm.editing != null) "ویرایش دسته" else "دسته جدید",
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            JibetField(label = "نام دسته") {
                NameInput(
                    value = vm.name,
                    onChange = vm::setName,
                    placeholder = "مثلاً قهوه",
                    accent = jc.accentA,
                )
            }

            Column {
                SectionLabel("رنگ")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CATEGORY_COLORS.forEach { c ->
                        ColorSwatch(hex = c, selected = vm.color == c, onClick = { vm.setColor(c) })
                    }
                }
            }

            Column {
                SectionLabel("آیکون")
                // شبکه‌ی ۵ ستونه — مثل grid grid-cols-5 وب
                FlowRow(
                    maxItemsInEachRow = 5,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CATEGORY_ICONS.forEach { ic ->
                        IconTile(icon = ic, colorHex = vm.color, active = vm.icon == ic, onClick = { vm.setIcon(ic) })
                    }
                }
            }

            AccentButton(
                text = if (vm.saving) "در حال ذخیره…" else if (vm.editing != null) "ذخیره‌ی تغییرات" else "افزودن دسته",
                onClick = vm::save,
                enabled = vm.name.isNotBlank() && !vm.saving,
            )

            if (vm.editing != null) {
                DeleteButton(label = "حذف دسته", armed = vm.confirmDelete, onClick = vm::delete)
            }
        }
    }
}

/* ================= اجزای فرم ================= */

/** ورودی متنی استاندارد — مثل inputClass وب */
@Composable
private fun NameInput(value: String, onChange: (String) -> Unit, placeholder: String, accent: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .jibetInputShape()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Faint),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Ink),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** دایره‌ی رنگ — فعال با حلقه‌ی ۲dp جوهری + فاصله‌ی ۲dp سفید (ring-offset وب) */
@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .pressableScale(onClick = onClick, pressedScale = 0.92f)
            .size(if (selected) 48.dp else 44.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.background(Color.White) else Modifier)
            .then(if (selected) Modifier.border(2.dp, Ink.copy(alpha = 0.3f), CircleShape) else Modifier)
            .padding(if (selected) 2.dp else 0.dp)
            .clip(CircleShape)
            .background(parseColor(hex)),
    )
}

/** کاشی آیکون ۵۲dp — فعال با پس‌زمینه‌ی رنگ انتخابی */
@Composable
private fun IconTile(icon: String, colorHex: String, active: Boolean, onClick: () -> Unit) {
    val color = parseColor(colorHex)
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .pressableScale(onClick = onClick, pressedScale = 0.94f)
            .then(
                if (active) {
                    Modifier
                } else {
                    Modifier.shadow(4.dp, shape, spotColor = Color(0x1A5B74E9), ambientColor = Color.Transparent)
                },
            )
            .size(52.dp)
            .clip(shape)
            .background(if (active) color else Color.White.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        IconBubble(icon = icon, color = if (active) Color.White else color, size = 40.dp)
    }
}

/** دکمه‌ی حذف دومرحله‌ای — مثل دکمه‌ی حذف وب */
@Composable
private fun DeleteButton(label: String, armed: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (armed) {
                    Modifier
                        .shadow(10.dp, shape, spotColor = Color(0x8CF26D6D), ambientColor = Color.Transparent)
                        .clip(shape)
                        .background(Brush.linearGradient(listOf(Color(0xFFF28080), Expense)))
                } else {
                    Modifier
                        .clip(shape)
                        .background(Expense.copy(alpha = 0.10f))
                },
            )
            .pressableScale(onClick = onClick)
            .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = null,
                tint = if (armed) Color.White else Expense,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (armed) "مطمئنی؟ حذف کن" else label,
                style = TextStyle(
                    fontFamily = Vazir,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (armed) Color.White else Expense,
                ),
            )
        }
    }
}

/** ورود نرم با پرش به بالا — مثل motion.div fadeUp وب */
@Composable
private fun FadeUp(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    durationMillis: Int = 220,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis, delayMillis, EaseOut),
        label = "fadeUpAlpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (shown) 0.dp else 12.dp,
        animationSpec = tween(durationMillis, delayMillis, EaseOut),
        label = "fadeUpY",
    )
    LaunchedEffect(Unit) {
        shown = true
    }
    Box(
        modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY.toPx()
        },
    ) {
        content()
    }
}
