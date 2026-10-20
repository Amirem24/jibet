package com.jibet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jibet.app.ui.accounts.AccountsScreen
import com.jibet.app.ui.add.AddTransactionScreen
import com.jibet.app.ui.categories.CategoriesScreen
import com.jibet.app.ui.components.LocalCurrency
import com.jibet.app.ui.goals.GoalsScreen
import com.jibet.app.ui.home.HomeScreen
import com.jibet.app.ui.recurring.RecurringScreen
import com.jibet.app.ui.settings.SettingsScreen
import com.jibet.app.ui.setup.SetupWizard
import com.jibet.app.ui.stats.StatsScreen
import com.jibet.app.ui.theme.JibetTheme
import com.jibet.app.ui.transactions.TransactionsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as JibetApp
        setContent {
            val themeSetting by app.settings.theme.collectAsState(initial = "system")
            val dark = when (themeSetting) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            JibetTheme(darkTheme = dark) {
                val currency by app.settings.currency.collectAsState(initial = "تومان")
                CompositionLocalProvider(
                    LocalCurrency provides currency,
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        val setupDone by app.settings.setupDone.collectAsState(initial = null)
                        when (setupDone) {
                            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("جیبت", style = MaterialTheme.typography.headlineMedium)
                            }
                            false -> SetupWizard(onFinished = {})
                            else -> MainScaffold(app)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(app: JibetApp) {
    // Generate transactions for due recurring items on each app start.
    LaunchedEffect(Unit) { app.repository.processDueRecurring() }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val showBar = route in setOf("home", "txs", "stats", "settings")

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBar) {
                JibetBottomBar(current = route) { dest ->
                    if (dest == "add") {
                        nav.navigate("add")
                    } else {
                        nav.navigate(dest) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        }
    ) { pad ->
        JibetBackdrop(Modifier.fillMaxSize().padding(pad)) {
            NavHost(navController = nav, startDestination = "home") {
                composable("home") {
                    HomeScreen(onNavigate = { dest -> nav.navigate(dest) })
                }
                composable("txs") {
                    TransactionsScreen(
                        onBack = { nav.popBackStack() },
                        onEditTx = { id -> nav.navigate("add?txId=$id") }
                    )
                }
                composable(
                    route = "add?txId={txId}",
                    arguments = listOf(navArgument("txId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    })
                ) { entry ->
                    AddTransactionScreen(
                        txId = entry.arguments?.getLong("txId") ?: -1L,
                        onDone = { nav.popBackStack() }
                    )
                }
                composable("stats") {
                    StatsScreen(onBack = { nav.popBackStack() })
                }
                composable("settings") {
                    SettingsScreen(
                        onNavigate = { dest -> nav.navigate(dest) },
                        onOpenWizard = { nav.navigate("wizard") }
                    )
                }
                composable("categories") { CategoriesScreen(onBack = { nav.popBackStack() }) }
                composable("accounts") { AccountsScreen(onBack = { nav.popBackStack() }) }
                composable("goals") { GoalsScreen(onBack = { nav.popBackStack() }) }
                composable("recurring") { RecurringScreen(onBack = { nav.popBackStack() }) }
                composable("wizard") { SetupWizard(onFinished = { nav.popBackStack() }) }
            }
        }
    }
}

/** Soft colorful blobs behind glassy UI. */
@Composable
private fun JibetBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tealAlpha = if (dark) 0.22f else 0.16f
    val amberAlpha = if (dark) 0.14f else 0.20f
    Box(modifier.background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .offset(x = (-70).dp, y = (-110).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF2A9D8F).copy(alpha = tealAlpha), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-90).dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF84A98C).copy(alpha = amberAlpha), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = 180.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFE9C46A).copy(alpha = amberAlpha), Color.Transparent)
                    )
                )
        )
        content()
    }
}

@Composable
private fun JibetBottomBar(current: String, onSelect: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            Triple("home", "خانه", Icons.Outlined.Home),
            Triple("txs", "تراکنش‌ها", Icons.Outlined.Receipt),
            Triple("add", "افزودن", Icons.Default.AddCircle),
            Triple("stats", "نمودار", Icons.Outlined.BarChart),
            Triple("settings", "تنظیمات", Icons.Outlined.Settings)
        )
        items.forEach { (dest, label, icon) ->
            val selected = current == dest
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(dest) },
                icon = {
                    if (dest == "add") {
                        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    } else {
                        Icon(icon, contentDescription = label)
                    }
                },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
