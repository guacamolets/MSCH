package com.example.msch.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.data.PeriodRecord
import com.example.msch.logic.CyclePredictor
import com.example.msch.logic.SettingsManager
import com.example.msch.ui.components.PeriodItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    records: List<PeriodRecord>,
    settingsManager: SettingsManager,
    onInsert: (Long) -> Unit,
    onUpdate: (PeriodRecord, Long) -> Unit,
    onDelete: (PeriodRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRecord by remember { mutableStateOf<PeriodRecord?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddDatePicker by remember { mutableStateOf(false) }

    val nextDateMillis = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.predictNextCycle(records, settingsManager.defaultCycleLength)
    }
    val avgCycle = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.calculateAverage(records, settingsManager.defaultCycleLength)
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
                        onInsert(selectedMillis)
                    }
                    showAddDatePicker = false
                }) {
                    Text(stringResource(R.string.ok_button))
                }
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(R.string.average_cycle_label)}: $avgCycle ${stringResource(R.string.days_suffix)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
            itemsIndexed(records) { index, record ->
                val cycleLength = if (index < records.size - 1) {
                    val nextRecord = records[index + 1]
                    val diff = record.startDate - nextRecord.startDate
                    (diff / (1000 * 60 * 60 * 24)).toInt()
                } else {
                    null
                }

                PeriodItem(
                    record = record,
                    cycleLength = cycleLength,
                    onClick = { selectedRecord = record }
                )
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
                        onUpdate(record, newDate)
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
                    onDelete(record)
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