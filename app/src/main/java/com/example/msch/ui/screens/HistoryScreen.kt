package com.example.msch.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Delete
import com.example.msch.R
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.AppConfig
import com.example.msch.logic.CyclePredictor.calculateAverageCycle
import com.example.msch.logic.CyclePredictor.getOvulationDate
import com.example.msch.services.SettingsManager
import com.example.msch.ui.components.PeriodItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    records: List<PeriodRecord>,
    settingsManager: SettingsManager,
    onInsert: (Long) -> Unit,
    onUpdate: (PeriodRecord) -> Unit,
    onDelete: (PeriodRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRecord by remember { mutableStateOf<PeriodRecord?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isAddingNewRecord by remember { mutableStateOf(false) }
    var itemToDeleteId by remember { mutableStateOf<Long?>(null) }
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
                itemsIndexed(items = sortedRecords, key = { _, record -> record.id }) { index, record ->
                    val cycleLength = remember(sortedRecords) {
                        if (index > 0) {
                            val diff = sortedRecords[index - 1].startDate - record.startDate
                            (diff / AppConfig.MILLIS_IN_DAY).toInt()
                        } else {
                            val diff = System.currentTimeMillis() - record.startDate
                            (diff / AppConfig.MILLIS_IN_DAY).toInt() + 1
                        }
                    }

                    val periodLength = remember(record) {
                        record.endDate?.let {
                            val diff = it - record.startDate
                            (diff / AppConfig.MILLIS_IN_DAY).toInt() + 1
                        }
                    }

                    val nextCycleStart = remember(sortedRecords, record) {
                        if (index > 0) {
                            sortedRecords[index - 1].startDate
                        } else {
                            val avg = calculateAverageCycle(records, settingsManager.defaultCycleLength)
                            record.startDate + (avg * AppConfig.MILLIS_IN_DAY)
                        }
                    }

                    val ovulationDay = remember(record, nextCycleStart) {
                        ((getOvulationDate(nextCycleStart) - record.startDate) / AppConfig.MILLIS_IN_DAY).toInt()
                    }

                    val currentRecordId = record.id.toLong()
                    Box(modifier = Modifier.fillMaxWidth()) {
                        PeriodItem(
                            record = record,
                            cycleLength = cycleLength,
                            periodLength = periodLength,
                            ovulationDay = ovulationDay,
                            onClick = {
                                if (itemToDeleteId != null) {
                                    itemToDeleteId = null
                                } else {
                                    selectedRecord = record
                                    isAddingNewRecord = false
                                    showDatePicker = true
                                }
                            },
                            onLongClick = {
                                itemToDeleteId = currentRecordId
                            }
                        )

                        if (itemToDeleteId == currentRecordId) {
                            Surface(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(vertical = 4.dp)
                                    .clickable { itemToDeleteId = null },
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                onDelete(record)
                                                itemToDeleteId = null
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            )
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                selectedRecord = null
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