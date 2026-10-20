package com.jibet.app.ui.settings

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jibet.app.JibetApp
import com.jibet.app.data.BackupData
import com.jibet.app.ui.components.ConfirmDialog
import com.jibet.app.ui.components.GlassCard
import com.jibet.app.ui.components.SectionTitle
import com.jibet.app.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as JibetApp).repository
    private val settings = (app as JibetApp).settings

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    val theme: StateFlow<String> =
        settings.theme.stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val currency: StateFlow<String> =
        settings.currency.stateIn(viewModelScope, SharingStarted.Eagerly, "تومان")

    /** One-shot toast message, consumed by the screen. */
    var message by mutableStateOf<String?>(null)

    private var busy = false

    fun setTheme(v: String) {
        viewModelScope.launch { settings.setTheme(v) }
    }

    fun setCurrency(v: String) {
        viewModelScope.launch { settings.setCurrency(v) }
    }

    fun exportBackup(uri: Uri) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                val data = repo.snapshot()
                val text = json.encodeToString(BackupData.serializer(), data)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                message = "پشتیبان ذخیره شد ✅"
            } catch (e: Exception) {
                message = "ذخیره پشتیبان ناموفق بود ❌"
            } finally {
                busy = false
            }
        }
    }

    fun exportCsv(uri: Uri) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                val csv = buildCsv(repo.snapshot())
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }
                message = "فایل CSV آماده شد ✅"
            } catch (e: Exception) {
                message = "ساخت فایل CSV ناموفق بود ❌"
            } finally {
                busy = false
            }
        }
    }

    fun importBackup(uri: Uri) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                } ?: ""
                val data = json.decodeFromString(BackupData.serializer(), text)
                repo.restore(data)
                message = "بازیابی شد ✅"
            } catch (e: Exception) {
                message = "فایل نامعتبر است ❌"
            } finally {
                busy = false
            }
        }
    }

    private fun buildCsv(snap: BackupData): String {
        fun esc(s: String): String =
            "\"" + s.replace("\"", "'").replace(",", "،").replace("\n", " ") + "\""

        val catNames = snap.categories.associate { it.id to it.name }
        val accNames = snap.accounts.associate { it.id to it.name }

        val sb = StringBuilder("\uFEFF")
        sb.append("تاریخ,مبلغ,نوع,دسته,حساب,یادداشت\n")
        snap.transactions.sortedBy { it.timestamp }.forEach { t ->
            sb.append(Fmt.date(t.timestamp)).append(',')
            sb.append(t.amount.toLong().toString()).append(',')
            sb.append(if (t.isIncome) "درآمد" else "هزینه").append(',')
            sb.append(esc(catNames[t.categoryId] ?: "")).append(',')
            sb.append(esc(accNames[t.accountId] ?: "")).append(',')
            sb.append(esc(t.note)).append('\n')
        }
        return sb.toString()
    }
}

@Composable
fun SettingsScreen(onNavigate: (String) -> Unit, onOpenWizard: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val context = LocalContext.current
    val theme by vm.theme.collectAsState()
    val currency by vm.currency.collectAsState()

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportBackup(uri) }

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) vm.exportCsv(uri) }

    val importJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importBackup(uri) }

    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var currencyInput by remember { mutableStateOf("") }

    LaunchedEffect(vm.message) {
        vm.message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            vm.message = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
    ) {
        Text(
            "تنظیمات",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // ---------- مدیریت ----------
        SectionTitle("مدیریت")
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            SettingsRow("حساب‌ها", leading = { LeadBadge(emoji = "💳") }) {
                onNavigate("accounts")
            }
            Spacer(Modifier.height(6.dp))
            SettingsRow("دسته‌بندی‌ها", leading = { LeadBadge(emoji = "🗂") }) {
                onNavigate("categories")
            }
            Spacer(Modifier.height(6.dp))
            SettingsRow("تراکنش‌های دوره‌ای", leading = { LeadBadge(emoji = "🔁") }) {
                onNavigate("recurring")
            }
            Spacer(Modifier.height(6.dp))
            SettingsRow("اهداف", leading = { LeadBadge(emoji = "🎯") }) {
                onNavigate("goals")
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------- پشتیبان‌گیری ----------
        SectionTitle("پشتیبان‌گیری")
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            SettingsRow("خروجی پشتیبان (JSON)", leading = { LeadBadge(icon = Icons.Outlined.Upload) }) {
                exportJson.launch("jibet-backup-" + System.currentTimeMillis() + ".json")
            }
            Spacer(Modifier.height(6.dp))
            SettingsRow("بازیابی پشتیبان", leading = { LeadBadge(icon = Icons.Outlined.Restore) }) {
                showRestoreConfirm = true
            }
            Spacer(Modifier.height(6.dp))
            SettingsRow("خروجی CSV (اکسل)", leading = { LeadBadge(icon = Icons.Outlined.TableChart) }) {
                exportCsv.launch("jibet-transactions.csv")
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------- نمایش ----------
        SectionTitle("نمایش")
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "تم",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = theme == "system",
                        onClick = { vm.setTheme("system") },
                        label = { Text("سیستم") }
                    )
                    FilterChip(
                        selected = theme == "light",
                        onClick = { vm.setTheme("light") },
                        label = { Text("روشن") }
                    )
                    FilterChip(
                        selected = theme == "dark",
                        onClick = { vm.setTheme("dark") },
                        label = { Text("تاریک") }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        currencyInput = currency
                        showCurrencyDialog = true
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "واحد پول",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    currency,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = {
                    currencyInput = currency
                    showCurrencyDialog = true
                }) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "ویرایش واحد پول",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------- درباره ----------
        SectionTitle("درباره")
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            SettingsRow(
                "نسخه " + Fmt.fa("1.0.0").replace(".", "٫"),
                leading = { LeadBadge(emoji = "ℹ️") },
                showArrow = false
            )
            Spacer(Modifier.height(6.dp))
            SettingsRow(
                "ساخته‌شده با ❤️ برای مدیریت مالی شخصی",
                leading = { LeadBadge(emoji = "💚") },
                showArrow = false
            )
            Spacer(Modifier.height(6.dp))
            SettingsRow("راه‌اندازی مجدد (ویزارد اولیه)", leading = { LeadBadge(emoji = "🔄") }) {
                onOpenWizard()
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "جیبت — همه داده‌ها فقط روی دستگاه شما",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showRestoreConfirm) {
        ConfirmDialog(
            title = "بازیابی پشتیبان",
            text = "همه داده‌های فعلی جایگزین می‌شوند. مطمئنی؟",
            confirmText = "بازیابی",
            onConfirm = { importJson.launch(arrayOf("application/json")) },
            onDismiss = { showRestoreConfirm = false }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("واحد پول") },
            text = {
                OutlinedTextField(
                    value = currencyInput,
                    onValueChange = { currencyInput = it },
                    label = { Text("مثلاً تومان / ریال / دلار") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.setCurrency(currencyInput.trim())
                        showCurrencyDialog = false
                    },
                    enabled = currencyInput.isNotBlank()
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

// ---------- Row building blocks ----------

@Composable
private fun SettingsRow(
    label: String,
    leading: @Composable () -> Unit,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (showArrow) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeadBadge(emoji: String? = null, icon: ImageVector? = null) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = 16.sp)
        } else if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
