package com.jibet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jibet.app.data.BootState
import com.jibet.app.data.ThemeMode
import com.jibet.app.ui.about.AboutScreen
import com.jibet.app.ui.accounts.AccountsScreen
import com.jibet.app.ui.backup.BackupScreen
import com.jibet.app.ui.budget.BudgetScreen
import com.jibet.app.ui.categories.CategoriesScreen
import com.jibet.app.ui.components.JibetLogo
import com.jibet.app.ui.components.accentGradient
import com.jibet.app.ui.components.jibetBackdrop
import com.jibet.app.ui.goals.GoalsScreen
import com.jibet.app.ui.home.HomeScreen
import com.jibet.app.ui.more.MoreScreen
import com.jibet.app.ui.nav.JibetBottomNav
import com.jibet.app.ui.onboarding.OnboardingScreen
import com.jibet.app.ui.recurring.RecurringScreen
import com.jibet.app.ui.reports.ReportsScreen
import com.jibet.app.ui.search.SearchScreen
import com.jibet.app.ui.sheets.AddTransactionSheet
import com.jibet.app.ui.sheets.TransactionDetailSheet
import com.jibet.app.ui.theme.JibetTheme
import com.jibet.app.ui.theme.LocalJibetColors
import com.jibet.app.ui.theme.Vazir
import com.jibet.app.ui.transactions.TransactionsScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // شروع با حالت روشن؛ هم‌گام با تم کاربر در کامپوزیشن به‌روز می‌شود
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val app = application as JibetApp
        setContent {
            val accent by app.settings.accent.collectAsState(initial = "blue")
            val themeMode by app.settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // آیکون‌های نوار وضعیت/ناوبری باید با تم جابه‌جا شوند
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            JibetTheme(accentKey = accent, dark = dark) {
                // کل اپ مثل وب dir="rtl" است — مستقل از لوکال دستگاه
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
                        val boot by app.settings.bootState.collectAsState(initial = BootState.Loading)
                        // منبع بلور پس‌زمینه برای شیشه‌ی نوبار (backdrop-filter وب)
                        val hazeState = remember { HazeState() }
                        Box(Modifier.fillMaxSize().haze(hazeState).jibetBackdrop()) {
                            when (boot) {
                                BootState.Loading -> Splash()
                                BootState.Fresh -> OnboardingScreen()
                                BootState.Done -> MainApp(app, hazeState)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** اسپلش شروع — مثل Splash وب (تم‌آگاه) */
@Composable
private fun Splash() {
    val jc = LocalJibetColors.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val logoScale = remember { Animatable(0.85f) }
        LaunchedEffect(Unit) {
            logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        Box(Modifier.scale(logoScale.value)) { JibetLogo(size = 86.dp) }
        Spacer(Modifier.height(20.dp))
        Text(
            "جیبت",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = 30.sp, color = jc.ink),
        )
        Text(
            "jibet",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = jc.inkSoft, letterSpacing = 0.2.em),
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(24.dp))
        val transition = rememberInfiniteTransition(label = "bar")
        val x by transition.animateFloat(
            initialValue = -1.5f,
            targetValue = 3.5f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "sweep",
        )
        Box(
            Modifier
                .width(160.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(jc.divider)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(1f / 3f)
                    .height(6.dp)
                    .offset(x = 160.dp * x)
                    .clip(RoundedCornerShape(50))
                    .background(accentGradient(horizontal = true))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "در حال آماده‌سازی…",
            style = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = jc.inkSoft),
        )
    }
}

@Composable
private fun MainApp(app: JibetApp, hazeState: HazeState) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "home"

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // مثل main.px-4.pb-32 وب — پدینگ افقی و فضا برای نویگیشن شناور
            // FIX (UI): رزرو پایین از ۱۳۲ به ۱۱۶ رسید تا با ارتفاع واقعی نویگیشن (قرص ۶۴ + حاشیه)
            // هم‌خوان شود و محتوای اسکرول بی‌دلیل زیر یک نوار خالی بریده نشود
            Box(Modifier.weight(1f).padding(horizontal = 16.dp).padding(bottom = 116.dp)) {
                AppNavHost(nav)
            }
        }
        // نویگیشن پایین — شناور روی همه‌ی صفحات
        JibetBottomNav(
            current = current,
            onTab = { tab ->
                // کلیک روی تب فعلی: هیچ کاری نکن — صفحه نباید دوباره لود/رفرش شود
                if (tab == current) return@JibetBottomNav
                nav.navigate(tab) {
                    // الگوی رسمی تب‌های Navigation Compose: ذخیره/بازیابی وضعیت تب‌ها
                    // تا جابجایی بدون رفرش کامل و با حفظ اسکرول باشد
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onAdd = { UiBus.openAdd() },
            modifier = Modifier.align(Alignment.BottomCenter),
            hazeState = hazeState,
        )
        AddTransactionSheet()
        TransactionDetailSheet()
    }
}

@Composable
private fun AppNavHost(nav: NavHostController) {
    NavHost(
        navController = nav,
        startDestination = "home",
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(tween(180)) + slideInHorizontally(tween(200)) { -it / 20 }
        },
        exitTransition = {
            fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { it / 20 }
        },
        popEnterTransition = {
            fadeIn(tween(180)) + slideInHorizontally(tween(200)) { it / 20 }
        },
        popExitTransition = {
            fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { -it / 20 }
        },
    ) {
        composable("home") {
            HomeScreen(
                onPush = { r -> nav.navigate(r) { launchSingleTop = true } },
                onTab = { r ->
                    nav.navigate(r) {
                        popUpTo("home") { inclusive = r == "home" }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable("transactions") {
            TransactionsScreen(onPush = { r -> nav.navigate(r) { launchSingleTop = true } })
        }
        composable("search") { SearchScreen(onBack = { nav.popBackStack() }) }
        composable("reports") { ReportsScreen(onBack = { nav.popBackStack() }) }
        composable("budget") { BudgetScreen() }
        composable("more") {
            MoreScreen(
                onPush = { r -> nav.navigate(r) { launchSingleTop = true } },
                onTab = { r ->
                    nav.navigate(r) {
                        popUpTo("home") { inclusive = r == "home" }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable("accounts") { AccountsScreen(onBack = { nav.popBackStack() }) }
        composable("categories") { CategoriesScreen(onBack = { nav.popBackStack() }) }
        composable("recurring") { RecurringScreen(onBack = { nav.popBackStack() }) }
        composable("goals") { GoalsScreen(onBack = { nav.popBackStack() }) }
        composable("backup") { BackupScreen(onBack = { nav.popBackStack() }) }
        composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }
    }
}
