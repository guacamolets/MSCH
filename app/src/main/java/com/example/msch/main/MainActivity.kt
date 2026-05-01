package com.example.msch.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.room.Room
import com.example.msch.R
import com.example.msch.data.AppDatabase
import com.example.msch.services.DataManager
import com.example.msch.services.SettingsManager
import com.example.msch.ui.components.DataMenu
import com.example.msch.ui.screens.*
import com.example.msch.ui.theme.MSCHTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "period-db")
            .fallbackToDestructiveMigration()
            .build()
    }
    private val settingsManager by lazy { SettingsManager(this) }
    private val dataManager by lazy { DataManager(this) }

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(db.periodDao(), settingsManager) as T
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { dataManager.readImportFile(it) { records -> viewModel.importRecords(records) } }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        enableEdgeToEdge()

        lifecycleScope.launch {
            viewModel.sanitizeRecords()
        }

        setContent {
            var themeTick by remember { mutableIntStateOf(0) }
            val darkTheme = remember(themeTick) {
                when (settingsManager.appTheme) {
                    1 -> false
                    2 -> true
                    else -> null
                }
            } ?: isSystemInDarkTheme()

            MSCHTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val records by viewModel.records.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            actions = {
                                DataMenu(
                                    records = records,
                                    onShare = { c, n -> dataManager.shareFile(c, n) },
                                    onImportFile = { importLauncher.launch("*/*") },
                                    onImportText = { rawText -> viewModel.importRawText(rawText) }
                                )
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            listOf(Screen.Main, Screen.History, Screen.Settings).forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, null) },
                                    label = { Text(stringResource(screen.titleRes)) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(
                        navController = navController,
                        viewModel = viewModel,
                        settingsManager = settingsManager,
                        padding = innerPadding,
                        onThemeChanged = { themeTick++ }
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "PERIOD_REMINDER_CH", "Period Reminders", NotificationManager.IMPORTANCE_DEFAULT
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
}

@Composable
fun AppNavigation(
    navController: androidx.navigation.NavHostController,
    viewModel: MainViewModel,
    settingsManager: SettingsManager,
    padding: PaddingValues,
    onThemeChanged: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val nextDate by viewModel.predictedNextDate.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = Modifier.padding(padding)
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                records = records,
                settingsManager = settingsManager,
                onInsert = { viewModel.addRecord(it) },
                onEndPeriod = { viewModel.endPeriod(it) }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                records = records,
                onInsert = { viewModel.addRecord(it) },
                onUpdate = { viewModel.updateRecord(it) },
                onDelete = { viewModel.deleteRecord(it) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(settingsManager, nextDate, onThemeChanged, onDeleteAll = {viewModel.clearAllHistory()})
        }
    }
}

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Main : Screen("main", R.string.nav_main, Icons.Default.Home)
    object History : Screen("history", R.string.nav_history, Icons.Default.History)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}