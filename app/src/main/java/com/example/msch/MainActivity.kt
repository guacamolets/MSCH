package com.example.msch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.msch.ui.theme.MSCHTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "period-db"
        ).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MSCHTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        dao = db.periodDao(),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dao: PeriodDao, modifier: Modifier = Modifier) {
    val records by dao.getAllRecords().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedRecord by remember { mutableStateOf<PeriodRecord?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val nextDateMillis = remember(records) {
        CyclePredictor.predictNextCycle(records)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Next cycle starts")
                val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                Text(
                    text = sdf.format(Date(nextDateMillis)),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val newRecord = PeriodRecord(startDate = System.currentTimeMillis())
                    dao.insert(newRecord)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Track period")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "History:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(records) { record ->
                PeriodItem(
                    record = record,
                    onClick = { selectedRecord = record })
            }
        }
    }

    selectedRecord?.let { record ->
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = record.startDate
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val newDate = datePickerState.selectedDateMillis ?: record.startDate
                        scope.launch(Dispatchers.IO) {
                            dao.update(record.copy(startDate = newDate))
                        }
                        showDatePicker = false
                        selectedRecord = null
                    }) { Text("ОК") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = { selectedRecord = null },
            text = { Text("Date: ${SimpleDateFormat("dd.MM.yyyy").format(Date(record.startDate))}") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) { dao.delete(record) }
                    selectedRecord = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Edit")
                }
            }
        )
    }
}

@Composable
fun PeriodItem(record: PeriodRecord, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val dateString = sdf.format(Date(record.startDate))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }
    ) {
        Text(
            text = dateString,
            modifier = Modifier.padding(16.dp)
        )
    }
}