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
import com.example.msch.ui.components.PeriodItem
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    records: List<PeriodRecord>,
    onInsert: (Long) -> Unit,
    onUpdate: (PeriodRecord, Long) -> Unit,
    onDelete: (PeriodRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRecord by remember { mutableStateOf<PeriodRecord?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isAddingNewRecord by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isAddingNewRecord = true
                    showDatePicker = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
            ) {
                val sortedRecords = records.sortedByDescending { it.startDate }

                itemsIndexed(sortedRecords) { index, record ->
                    val cycleLength = if (index < sortedRecords.size - 1) {
                        val diff = record.startDate - sortedRecords[index + 1].startDate
                        (diff / (1000 * 60 * 60 * 24)).toInt()
                    } else null

                    PeriodItem(
                        record = record,
                        cycleLength = cycleLength,
                        onClick = {
                            selectedRecord = record
                            isAddingNewRecord = false
                        }
                    )

                    if (index < sortedRecords.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialDate = if (isAddingNewRecord) System.currentTimeMillis() else selectedRecord?.startDate
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                if (isAddingNewRecord) isAddingNewRecord = false
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        if (isAddingNewRecord) {
                            onInsert(millis)
                        } else {
                            selectedRecord?.let { onUpdate(it, millis) }
                        }
                    }
                    showDatePicker = false
                    isAddingNewRecord = false
                    selectedRecord = null
                }) { Text(stringResource(R.string.ok_button)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    isAddingNewRecord = false
                }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    selectedRecord?.let { record ->
        if (!showDatePicker) {
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
}