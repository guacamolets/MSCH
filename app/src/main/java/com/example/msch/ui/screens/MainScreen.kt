package com.example.msch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.msch.R
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.CyclePredictor
import com.example.msch.logic.SettingsManager
import com.example.msch.ui.components.HorizontalCalendar
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
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val nextDateMillis = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.predictNextCycle(records, settingsManager.defaultCycleLength)
    }
    val avgCycle = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.calculateAverage(records, settingsManager.defaultCycleLength)
    }
    val currentDay = remember(records) {
        CyclePredictor.getCurrentDay(records)
    }
    val daysUntil = remember(nextDateMillis) {
        CyclePredictor.getDaysUntilNext(nextDateMillis)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalCalendar(
            records = records,
            nextDateMillis = nextDateMillis,
            settingsManager = settingsManager,
            selectedDateMillis = selectedDate,
            onDateSelected = { selectedDate = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = when {
                daysUntil > 0 -> stringResource(R.string.days_until_format, daysUntil)
                daysUntil == 0 -> stringResource(R.string.period_soon)
                else -> stringResource(R.string.period_overdue)
            },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onInsert(selectedDate) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))

            val isToday = android.text.format.DateUtils.isToday(selectedDate)
            Text(
                if (isToday) stringResource(R.string.log_period_button)
                else {
                    stringResource(
                        R.string.log_period_on_date_format,
                        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(selectedDate))
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoRow(
                label = stringResource(R.string.current_day_label),
                value = currentDay?.let { stringResource(R.string.day_format, it) } ?: "--"
            )

            val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
            InfoRow(
                label = stringResource(R.string.expected_on),
                value = sdf.format(Date(nextDateMillis))
            )

            InfoRow(
                label = stringResource(R.string.average_cycle_label),
                value = "$avgCycle ${stringResource(R.string.days_suffix)}"
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}