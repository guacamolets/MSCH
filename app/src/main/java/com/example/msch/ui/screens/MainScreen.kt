package com.example.msch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.AppConfig
import com.example.msch.logic.CyclePredictor
import com.example.msch.services.SettingsManager
import com.example.msch.ui.components.HorizontalCalendar
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
    val nextDateStr = remember(nextDateMillis) {
        SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(nextDateMillis))
    }
    val variations = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.getVariations(records, settingsManager.defaultCycleLength)
    }

    val currentDay = remember(records) { CyclePredictor.getCurrentDay(records) }
    val daysUntil = remember(nextDateMillis) { CyclePredictor.getDaysUntilNext(nextDateMillis) }
    val lastStats = remember(records) { CyclePredictor.getLastStats(records) }

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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (activeRecord != null) {
                Text(
                    text = stringResource(R.string.period_active),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = when {
                        daysUntil > 0 -> stringResource(R.string.period_header_expected)
                        daysUntil == 0 -> stringResource(R.string.period_soon)
                        else -> stringResource(R.string.period_overdue)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                if (daysUntil > 0) {
                    Text(
                        text = stringResource(R.string.days_count_format, daysUntil),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (activeRecord == null) onInsert(selectedDate)
                else onEndPeriod(selectedDate)
            },
            modifier = Modifier.widthIn(min = 200.dp).height(56.dp).padding(horizontal = 32.dp),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoBlock(
                primaryLabel = SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date()),
                secondaryLabel = currentDay?.let {
                    stringResource(R.string.status_subtitle_format, it, nextDateStr)
                } ?: stringResource(R.string.no_data),
                icon = Icons.Default.Event
            )

            InfoBlock(
                primaryLabel = stringResource(R.string.label_cycle),
                secondaryLabel = stringResource(
                    R.string.stats_subtitle_format,
                    lastStats.first ?: 0,
                    variations.first
                ),
                icon = Icons.Default.History
            )

            InfoBlock(
                primaryLabel = stringResource(R.string.label_periods),
                secondaryLabel = stringResource(
                    R.string.stats_subtitle_format,
                    lastStats.second ?: 0,
                    variations.second
                ),
                icon = Icons.Default.Opacity
            )
        }
    }
}

@Composable
fun InfoBlock(
    primaryLabel: String,
    secondaryLabel: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}