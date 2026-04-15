package com.example.msch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.data.PeriodRecord
import com.example.msch.logic.CyclePredictor
import com.example.msch.logic.SettingsManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    records: List<PeriodRecord>,
    settingsManager: SettingsManager,
    onInsert: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDatePicker by remember { mutableStateOf(false) }

    val nextDateMillis = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.predictNextCycle(records, settingsManager.defaultCycleLength)
    }
    val avgCycle = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.calculateAverage(records, settingsManager.defaultCycleLength)
    }

    if (showAddDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showAddDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onInsert(it) }
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
                Text(
                    text = stringResource(R.string.next_period_label),
                    style = MaterialTheme.typography.labelMedium
                )
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
            onClick = { showAddDatePicker = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(stringResource(R.string.log_period_button))
        }
    }
}