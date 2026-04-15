package com.example.msch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.data.PeriodRecord
import com.example.msch.ui.components.PeriodItem
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    records: List<PeriodRecord>,
    onUpdate: (PeriodRecord, Long) -> Unit,
    onDelete: (PeriodRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRecord by remember { mutableStateOf<PeriodRecord?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            itemsIndexed(records) { index, record ->
                val cycleLength = if (index < records.size - 1) {
                    val diff = record.startDate - records[index + 1].startDate
                    (diff / (1000 * 60 * 60 * 24)).toInt()
                } else null

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
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = record.startDate)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { onUpdate(record, it) }
                        showDatePicker = false
                        selectedRecord = null
                    }) { Text(stringResource(R.string.ok_button)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancel_button))
                    }
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