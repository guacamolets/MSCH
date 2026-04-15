package com.example.msch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.msch.data.AppDatabase
import com.example.msch.data.PeriodRecord
import com.example.msch.logic.DataSerializer
import com.example.msch.ui.components.ExportMenu
import com.example.msch.ui.screens.MainScreen
import com.example.msch.ui.theme.MSCHTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.msch.logic.DataManager
import kotlinx.coroutines.CoroutineScope

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
                scope.launch(Dispatchers.IO) {
                    dao.insertAll(records)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MSCHTheme {
                val records by dao.getAllRecords().collectAsState(initial = emptyList())

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("MSCH") },
                            actions = {
                                ExportMenu(
                                    records = records,
                                    onShare = { content, name -> dataManager.shareFile(content, name) },
                                    onImport = { importLauncher.launch("application/json") }
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    MainScreen(
                        records = records,
                        onInsert = { millis ->
                            scope.launch(Dispatchers.IO) { dao.insert(PeriodRecord(startDate = millis)) }
                        },
                        onUpdate = { record, newMillis ->
                            scope.launch(Dispatchers.IO) { dao.update(record.copy(startDate = newMillis)) }
                        },
                        onDelete = { record ->
                            scope.launch(Dispatchers.IO) { dao.delete(record) }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}