package com.example.msch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.AppConfig
import com.example.msch.ui.components.PeriodItem
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    records: List<PeriodRecord>,
    onInsert: (Long) -> Unit,
    onUpdate: (PeriodRecord) -> Unit,
    onDelete: (PeriodRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRecord by remember { mutableStateOf<PeriodRecord?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isAddingNewRecord by remember { mutableStateOf(false) }

    val sortedRecords = remember(records) { records.sortedByDescending { it.startDate } }

    Box(modifier = modifier.fillMaxSize()) {
        if (sortedRecords.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(sortedRecords) { index, record ->
                    val cycleLength = remember(sortedRecords) {
                        if (index < sortedRecords.size - 1) {
                            val diff = record.startDate - sortedRecords[index + 1].startDate
                            (diff / AppConfig.MILLIS_IN_DAY).toInt()
                        } else null
                    }

                    val periodDuration = remember(record) {
                        record.endDate?.let {
                            val diff = it - record.startDate
                            (diff / AppConfig.MILLIS_IN_DAY).toInt() + 1
                        }
                    }

                    PeriodItem(
                        record = record,
                        cycleLength = cycleLength,
                        periodDuration = periodDuration,
                        onClick = {
                            selectedRecord = record
                            isAddingNewRecord = false
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                isAddingNewRecord = true
                showDatePicker = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }

    if (showDatePicker) {
        HistoryDatePicker(
            isAdding = isAddingNewRecord,
            record = selectedRecord,
            onDismiss = {
                showDatePicker = false
                isAddingNewRecord = false
            },
            onConfirm = { startMillis, endMillis ->
                if (isAddingNewRecord) {
                    onInsert(startMillis)
                    // viewModel.addRecordWithEnd(startMillis, endMillis)
                } else {
                    selectedRecord?.let { record ->
                        val updatedRecord = record.copy(
                            startDate = startMillis,
                            endDate = endMillis
                        )
                        onUpdate(updatedRecord)
                    }
                }
                showDatePicker = false
                isAddingNewRecord = false
                selectedRecord = null
            }
        )
    }

    selectedRecord?.let { record ->
        if (!showDatePicker) {
            RecordActionDialog(
                record = record,
                onDelete = {
                    onDelete(record)
                    selectedRecord = null
                },
                onEdit = { showDatePicker = true },
                onDismiss = { selectedRecord = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDatePicker(
    isAdding: Boolean,
    record: PeriodRecord?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long?) -> Unit
) {
    val datePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = record?.startDate ?: System.currentTimeMillis(),
        initialSelectedEndDateMillis = record?.endDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val start = datePickerState.selectedStartDateMillis
                val end = datePickerState.selectedEndDateMillis
                if (start != null) {
                    onConfirm(start, end)
                }
            }) { Text(stringResource(R.string.ok_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    ) {
        DateRangePicker(
            state = datePickerState,
            title = { Text(stringResource(R.string.select_period_range), modifier = Modifier.padding(16.dp)) },
            showModeToggle = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RecordActionDialog(record: PeriodRecord, onDelete: () -> Unit, onEdit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_record_title)) },
        text = {
            val formattedDate = android.text.format.DateFormat
                .getDateFormat(LocalContext.current)
                .format(Date(record.startDate))
            Text(text = stringResource(R.string.selected_date, formattedDate))
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.delete_button), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onEdit) { Text(stringResource(R.string.edit_button)) }
        }
    )
}