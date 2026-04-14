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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    var showAddDatePicker by remember { mutableStateOf(false) }

    val nextDateMillis = remember(records) {
        CyclePredictor.predictNextCycle(records)
    }

    if (showAddDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showAddDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        scope.launch(Dispatchers.IO) {
                            dao.insert(PeriodRecord(startDate = selectedMillis))
                        }
                    }
                    showAddDatePicker = false
                }) { Text(stringResource(R.string.ok_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDatePicker = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                Text(stringResource(R.string.next_period_label))
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
            onClick = { showAddDatePicker = true },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.log_period_button))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.history_title),
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
                    }) { Text(stringResource(R.string.ok_button)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_button)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = { selectedRecord = null },
            title = { Text(stringResource(R.string.manage_record_title)) },
            text = {
                val formattedDate = android.text.format.DateFormat
                    .getDateFormat(LocalContext.current)
                    .format(Date(record.startDate))

                Text(text = stringResource(R.string.selected_date, formattedDate))
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) { dao.delete(record) }
                    selectedRecord = null
                }) {
                    Text(
                        text = stringResource(R.string.delete_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.edit_button))
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