package com.example.msch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.AppConfig
import com.example.msch.logic.CyclePredictor
import com.example.msch.services.SettingsManager
import com.example.msch.ui.components.HorizontalCalendar
import com.example.msch.ui.components.InfoRow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(
    records: List<PeriodRecord>,
    settingsManager: SettingsManager,
    onInsert: (Long) -> Unit,
    onEndPeriod: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    val now = System.currentTimeMillis()

    val dateFormatter = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    val nextDateMillis = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.predictNextCycle(records, settingsManager.defaultCycleLength)
    }
    val avgCycle = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.calculateAverageCycle(records, settingsManager.defaultCycleLength)
    }
    val currentDay = remember(records) { CyclePredictor.getCurrentDay(records) }
    val daysUntil = remember(nextDateMillis) { CyclePredictor.getDaysUntilNext(nextDateMillis) }

    val activeRecord = remember(records) {
        records.maxByOrNull { it.startDate }?.takeIf {
            it.endDate == null && (now - it.startDate) < AppConfig.AUTO_CLOSE_DAYS * AppConfig.MILLIS_IN_DAY
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        HorizontalCalendar(
            records = records,
            nextDateMillis = nextDateMillis,
            settingsManager = settingsManager,
            selectedDateMillis = selectedDate,
            onDateSelected = { selectedDate = it }
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = when {
                activeRecord != null -> stringResource(R.string.period_active)
                daysUntil > 0 -> stringResource(R.string.days_until_format, daysUntil)
                daysUntil == 0 -> stringResource(R.string.period_soon)
                else -> stringResource(R.string.period_overdue)
            },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (activeRecord == null) onInsert(selectedDate)
                else onEndPeriod(selectedDate)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (activeRecord == null)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Icon(
                imageVector = if (activeRecord == null) Icons.Default.Add else Icons.Default.Check,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))

            Text(
                text = when {
                    activeRecord != null -> stringResource(R.string.end_period_button)
                    android.text.format.DateUtils.isToday(selectedDate) -> stringResource(R.string.log_period_button)
                    else -> stringResource(R.string.log_period_on_date_format, dateFormatter.format(Date(selectedDate)))
                }
            )
        }

        Spacer(Modifier.height(48.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoRow(
                label = stringResource(R.string.current_day_label),
                value = currentDay?.let { stringResource(R.string.day_format, it) } ?: "--"
            )

            InfoRow(
                label = stringResource(R.string.expected_on),
                value = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(nextDateMillis))
            )

            InfoRow(
                label = stringResource(R.string.average_cycle_label),
                value = "$avgCycle ${stringResource(R.string.days_suffix)}"
            )
        }
    }
}