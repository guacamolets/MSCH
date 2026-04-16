package com.example.msch.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.msch.main.MainViewModel
import com.example.msch.R
import com.example.msch.data.AppDatabase
import com.example.msch.entities.PeriodRecord
import com.example.msch.services.DataManager
import com.example.msch.services.SettingsManager
import com.example.msch.ui.components.DataMenu
import com.example.msch.ui.screens.HistoryScreen
import com.example.msch.ui.screens.MainScreen
import com.example.msch.ui.screens.SettingsScreen
import com.example.msch.ui.theme.MSCHTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "period-db")
            .fallbackToDestructiveMigration()
            .build()
    }
    private val dao by lazy { db.periodDao() }
    private val dataManager by lazy { DataManager(this) }
    private val settingsManager by lazy { SettingsManager(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            dataManager.readImportFile(it) { records ->
                scope.launch(Dispatchers.IO) { dao.insertAll(records) }
            }
        }
    }

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(dao, settingsManager) as T
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        enableEdgeToEdge()

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
                val records by dao.getAllRecords().collectAsState(initial = emptyList())

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
                                    onShare = { content, name ->
                                        dataManager.shareFile(content, name)
                                    },
                                    onImport = { importLauncher.launch("application/json") }
                                )
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            val items = listOf(Screen.Main, Screen.History, Screen.Settings)
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = null) },
                                    label = { Text(stringResource(screen.titleRes)) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Main.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Main.route) {
                            MainScreen(
                                records = records,
                                settingsManager = settingsManager,
                                onInsert = { millis ->
                                    scope.launch(Dispatchers.IO) { dao.insert(PeriodRecord(startDate = millis)) }
                                }
                            )
                        }

                        composable(Screen.History.route) {
                            HistoryScreen(
                                records = records,
                                onInsert = { newMillis ->
                                    scope.launch(Dispatchers.IO) { dao.insert(PeriodRecord(startDate = newMillis)) }
                                },
                                onUpdate = { record, newMillis ->
                                    scope.launch(Dispatchers.IO) { dao.update(record.copy(startDate = newMillis)) }
                                },
                                onDelete = { record ->
                                    scope.launch(Dispatchers.IO) { dao.delete(record) }
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            val nextDate by viewModel.predictedNextDate.collectAsState()
                            SettingsScreen(
                                settingsManager = settingsManager,
                                nextDateMillis = nextDate,
                                onThemeChanged = { themeTick++ }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Period Reminders"
            val descriptionText = "Notifications about upcoming cycle"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("PERIOD_REMINDER_CH", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Main : Screen("main", R.string.nav_main, Icons.Default.Home)
    data object History : Screen("history", R.string.nav_history, Icons.Default.History)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}