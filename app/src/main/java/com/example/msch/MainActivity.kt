package com.example.msch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.msch.data.AppDatabase
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.DataManager
import com.example.msch.logic.SettingsManager
import com.example.msch.ui.components.DataMenu
import com.example.msch.ui.screens.HistoryScreen
import com.example.msch.ui.screens.MainScreen
import com.example.msch.ui.screens.SettingsScreen
import com.example.msch.ui.theme.MSCHTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "period-db")
            .fallbackToDestructiveMigration()
            .build()
    }
    private val dao by lazy { db.periodDao() }
    private val dataManager by lazy { DataManager(this) }
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MSCHTheme {
                val navController = rememberNavController()
                val settingsManager = remember { SettingsManager(this) }
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
                                    scope.launch(Dispatchers.IO) {
                                        dao.insert(PeriodRecord(startDate = newMillis))
                                    }
                                },
                                onUpdate = { record, newMillis ->
                                    scope.launch(Dispatchers.IO) {
                                        dao.update(record.copy(startDate = newMillis))
                                    }
                                },
                                onDelete = { record ->
                                    scope.launch(Dispatchers.IO) {
                                        dao.delete(record)
                                    }
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                settingsManager = settingsManager
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Main : Screen("main", R.string.nav_main, Icons.Default.Home)
    data object History : Screen("history", R.string.nav_history, Icons.Default.History)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}