package com.jibet.app.ui.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.exportBackupJson
import com.jibet.app.data.exportCsv
import com.jibet.app.data.restoreBackup
import com.jibet.app.ui.components.ScreenHeader
import com.jibet.app.ui.components.jibetCard
import com.jibet.app.ui.components.pressableScale
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* ================= ViewModel ================= */

class BackupViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as JibetApp).repository
    private val settings = (app as JibetApp).settings

    private var restoring by mutableStateOf(false)

    /** ذخیره‌ی فایل پشتیبان JSON روی URI انتخاب‌شده با SAF */
    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val json = repo.exportBackupJson(settings)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: throw IOException("output stream is null")
                }
                Toast.makeText(context, "پشتیبان ذخیره شد ✅", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در ذخیره‌ی پشتیبان", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** ذخیره‌ی خروجی CSV روی URI انتخاب‌شده با SAF */
    fun exportCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val csv = repo.exportCsv()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: throw IOException("output stream is null")
                }
                Toast.makeText(context, "خروجی CSV آماده شد ✅", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در ساخت خروجی CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** خواندن فایل پشتیبان و بازیابی کامل داده‌ها */
    fun restore(context: Context, uri: Uri) {
        if (restoring) return
        viewModelScope.launch {
            restoring = true
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: ""
                }
                repo.restoreBackup(
                    text,
                    settings,
                    snapshotFile = java.io.File(context.filesDir, "jibet-auto-pre-restore.json"),
                )
                Toast.makeText(context, "بازیابی شد ✅", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                // «فایل پشتیبان معتبر نیست» — از restoreBackup
                Toast.makeText(context, e.message ?: "فایل پشتیبان معتبر نیست", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "بازیابی ناموفق بود", Toast.LENGTH_SHORT).show()
            } finally {
                restoring = false
            }
        }
    }
}

/* ================= صفحه ================= */

/** صفحه‌ی پشتیبان‌گیری و خروجی — پورت ۱:۱ از screens/backup.tsx وب
 *  انتخاب فایل با SAF خودش تأیید است؛ بنر هشدار بالای کارت بازیابی می‌ماند. */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val vm: BackupViewModel = viewModel()
    val jc = LocalJibetColors.current
    val context = LocalContext.current

    val jsonSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) vm.exportBackup(context, uri)
    }
    val csvSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) vm.exportCsv(context, uri)
    }
    val jsonOpener = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) vm.restore(context, uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(title = "پشتیبان‌گیری و خروجی", onBack = onBack)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // پشتیبان‌گیری محلی
            FadeUp(0, durationMillis = 180) {
                ActionRow(
                    title = "پشتیبان‌گیری محلی",
                    subtitle = "دانلود فایل پشتیبان JSON از همه‌ی داده‌ها",
                    color = Color(0xFF5B7FFF),
                    icon = Icons.Rounded.FileDownload,
                    iconSize = 22.dp,
                    onClick = { jsonSaver.launch("jibet-backup-${System.currentTimeMillis()}.json") },
                )
            }

            // خروجی اکسل
            FadeUp(50, durationMillis = 180) {
                ActionRow(
                    title = "خروجی Excel / CSV",
                    subtitle = "دریافت فایل قابل استفاده در اکسل",
                    color = Color(0xFF2FBF83),
                    icon = Icons.Rounded.TableChart,
                    iconSize = 22.dp,
                    onClick = { csvSaver.launch("jibet-transactions-${System.currentTimeMillis()}.csv") },
                )
            }

            // هشدار بازیابی
            FadeUp(100, durationMillis = 180) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(jc.expenseSoft)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = jc.expense,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "بازیابی، داده‌های فعلی را کامل جایگزین می‌کند.",
                        style = TextStyle(
                            fontFamily = Vazir,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            lineHeight = 20.sp,
                            color = jc.expense,
                        ),
                    )
                }
            }

            // بازیابی از پشتیبان
            FadeUp(150, durationMillis = 180) {
                ActionRow(
                    title = "بازیابی از پشتیبان",
                    subtitle = "انتخاب فایل پشتیبان JSON جیبت",
                    color = Color(0xFFF5A524),
                    icon = Icons.Rounded.FileUpload,
                    iconSize = 22.dp,
                    onClick = { jsonOpener.launch(arrayOf("application/json")) },
                )
            }

            // راهنمای آفلاین
            FadeUp(200, durationMillis = 180) {
                Row(
                    Modifier
                        .jibetCard()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    BackupBubble(
                        color = Color(0xFF8A93A6),
                        size = 44.dp,
                        icon = Icons.Rounded.VerifiedUser,
                        iconSize = 20.dp,
                    )
                    Text(
                        buildAnnotatedString {
                            append("جیبت کاملاً محلی است؛ داده‌ها فقط روی همین دستگاه می‌مانند. ")
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = jc.ink)) {
                                append("هر ماه یک پشتیبان بگیر.")
                            }
                        },
                        style = TextStyle(
                            fontFamily = Vazir,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 24.sp,
                            color = jc.textMid,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/* ================= اجزای صفحه ================= */

/** ردیف کارتی قابل لمس با حباب آیکون رنگی — مثل ردیف‌های backup.tsx وب */
@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    val jc = LocalJibetColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(onClick = onClick)
            .jibetCard()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackupBubble(color = color, size = 48.dp, icon = icon, iconSize = iconSize)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = jc.ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Rounded.ChevronLeft,
            contentDescription = null,
            tint = jc.faint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** حباب آیکون با پس‌زمینه‌ی نرم — هم‌سبک Bubble وب (رنگ با آلفای ۱۱٪) */
@Composable
private fun BackupBubble(color: Color, size: Dp, icon: ImageVector, iconSize: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.11f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** ورود نرم با پرش به بالا — مثل motion.div وب */
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
