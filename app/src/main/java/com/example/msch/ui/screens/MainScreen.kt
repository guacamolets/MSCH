package com.example.msch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.AppConfig
import com.example.msch.logic.CyclePredictor
import com.example.msch.services.SettingsManager
import com.example.msch.ui.components.HorizontalCalendar
import kotlinx.coroutines.launch
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
    var selectedDate by remember {
        mutableLongStateOf(CyclePredictor.toStartOfDay())
    }

    val now = System.currentTimeMillis()

    val dateFormatter = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    val nextDateMillis = remember(records, settingsManager.defaultCycleLength) {
        CyclePredictor.predictNextCycle(records, settingsManager.defaultCycleLength)
    }
    val nextDateStr = remember(nextDateMillis) {
        dateFormatter.format(Date(nextDateMillis))
    }

    val currentDay = remember(records) { CyclePredictor.getCurrentDay(records) }

    val daysUntil = remember(nextDateMillis, selectedDate) {
        CyclePredictor.getDaysUntilTarget(nextDateMillis, selectedDate)
    }

    val lastStats = remember(records) { CyclePredictor.getLastStats(records) }

    val sortedRecords = remember(records) { records.sortedByDescending { it.startDate } }

    val isOvulationSelected = remember(selectedDate, sortedRecords, nextDateMillis) {
        CyclePredictor.isOvulationDay(
            selectedDate,
            sortedRecords,
            nextDateMillis,
            settingsManager.defaultCycleLength
        )
    }

    val activeRecord = remember(records) {
        records.maxByOrNull { it.startDate }?.takeIf {
            it.endDate == null && (now - it.startDate) < AppConfig.AUTO_CLOSE_DAYS * AppConfig.MILLIS_IN_DAY
        }
    }

    val initialIndex = 5000
    val listState = rememberLazyListState(initialIndex - 3)
    val coroutineScope = rememberCoroutineScope()
    val today = remember { CyclePredictor.toStartOfDay() }

    val showReturnButton by remember {
        derivedStateOf {
            selectedDate != today || listState.firstVisibleItemIndex != (initialIndex - 3)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            HorizontalCalendar(
                records = records,
                listState = listState,
                nextDateMillis = nextDateMillis,
                settingsManager = settingsManager,
                selectedDateMillis = selectedDate,
                onDateSelected = { selectedDate = it }
            )

            Spacer(Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            ) {
                if (activeRecord != null) {
                    Text(
                        text = stringResource(R.string.period_active),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = when {
                            isOvulationSelected -> stringResource(R.string.ovulation_day)
                            daysUntil > 0 -> stringResource(R.string.period_header_expected)
                            daysUntil == 0 -> stringResource(R.string.period_soon)
                            else -> stringResource(R.string.period_overdue)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isOvulationSelected)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier.heightIn(min = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            daysUntil > 0 && !isOvulationSelected -> {
                                Text(
                                    text = stringResource(R.string.days_count_format, daysUntil),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
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
                        MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (activeRecord == null)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onTertiaryContainer
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
                        else -> stringResource(R.string.log_period_button)
                    }
                )
            }

            Spacer(Modifier.height(48.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoBlock(
                    primaryLabel = stringResource(R.string.label_сurrent_cycle),
                    secondaryLabel = currentDay?.let {
                        stringResource(R.string.status_subtitle_format, it, nextDateStr)
                    } ?: stringResource(R.string.no_data),
                    icon = Icons.Default.Event
                )

                InfoBlock(
                    primaryLabel = stringResource(R.string.label_cycle),
                    secondaryLabel = lastStats.first?.let {
                        val isNormal = CyclePredictor.isLengthNormal(it, isCycle = true)
                        stringResource(
                            if (isNormal) R.string.stats_status_normal else R.string.stats_status_abnormal,
                            it
                        )
                    } ?: stringResource(R.string.no_data),
                    icon = Icons.Default.History
                )

                InfoBlock(
                    primaryLabel = stringResource(R.string.label_periods),
                    secondaryLabel = lastStats.second?.let {
                        val isNormal = CyclePredictor.isLengthNormal(it, isCycle = false)
                        stringResource(
                            if (isNormal) R.string.stats_status_normal else R.string.stats_status_abnormal,
                            it
                        )
                    } ?: stringResource(R.string.no_data),
                    icon = Icons.Default.Opacity
                )
            }
        }

        if (showReturnButton) {
            SmallFloatingActionButton(
                onClick = {
                    selectedDate = today
                    coroutineScope.launch {
                        listState.animateScrollToItem(initialIndex - 3)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Today, contentDescription = null)
            }
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