package com.example.msch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.SettingsManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HorizontalCalendar(
    records: List<PeriodRecord>,
    nextDateMillis: Long,
    settingsManager: SettingsManager,
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDays = 10000
    val initialIndex = 5000
    val listState = rememberLazyListState(initialIndex - 3)
    val sortedRecords = remember(records) { records.sortedBy { it.startDate } }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(totalDays) { index ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, index - initialIndex)

            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val time = calendar.timeInMillis

            val isToday = android.text.format.DateUtils.isToday(time)

            val lastRealRecord = sortedRecords.lastOrNull { record ->
                val recordCal = Calendar.getInstance().apply {
                    timeInMillis = record.startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                recordCal.timeInMillis <= time
            }

            val cleanRecordStart = lastRealRecord?.let {
                Calendar.getInstance().apply {
                    timeInMillis = it.startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            val cleanNextDate = Calendar.getInstance().apply {
                timeInMillis = nextDateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val referenceDate = if (time >= cleanNextDate && (cleanRecordStart == null || cleanNextDate > cleanRecordStart)) {
                cleanNextDate
            } else {
                cleanRecordStart
            }

            val dayOfCycle = if (referenceDate != null) {
                val diffMillis = time - referenceDate
                val days = (diffMillis / (24 * 60 * 60 * 1000L)).toInt()
                days + 1
            } else null

            val isPeriod = cleanRecordStart != null &&
                    time >= cleanRecordStart &&
                    time < cleanRecordStart + (settingsManager.defaultPeriodLength * 24 * 60 * 60 * 1000L)

            val isPrediction = time >= cleanNextDate &&
                    time < cleanNextDate + (settingsManager.defaultPeriodLength * 24 * 60 * 60 * 1000L)

            DayItem(
                date = calendar.time,
                isSelected = time == selectedDateMillis,
                onDateClick = { onDateSelected(time) },
                dayOfCycle = dayOfCycle,
                isToday = isToday,
                isPeriod = isPeriod,
                isPrediction = isPrediction
            )
        }
    }
}

@Composable
fun DayItem(
    date: Date,
    isSelected: Boolean,
    onDateClick: () -> Unit,
    dayOfCycle: Int?,
    isToday: Boolean,
    isPeriod: Boolean,
    isPrediction: Boolean
) {
    val backgroundColor = when {
        isPeriod -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        isPrediction -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isPeriod -> MaterialTheme.colorScheme.onErrorContainer
        isPrediction -> MaterialTheme.colorScheme.onTertiaryContainer
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val itemSize = 60.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(itemSize)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onDateClick() }
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else if (isToday && !isPeriod) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                } else Modifier
            )
    ) {
        Text(
            text = SimpleDateFormat("E", Locale.getDefault()).format(date).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor.copy(alpha = 0.6f)
        )

        Text(
            text = SimpleDateFormat("d", Locale.getDefault()).format(date),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor
        )

        Text(
            text = dayOfCycle?.toString() ?: "—",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isPeriod) contentColor.copy(alpha = 0.8f) else contentColor.copy(alpha = 0.5f)
        )
    }
}