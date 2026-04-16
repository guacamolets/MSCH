package com.example.msch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.msch.entities.CalendarDay
import com.example.msch.entities.DayStatus
import com.example.msch.entities.PeriodRecord
import com.example.msch.services.SettingsManager
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
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, index - initialIndex)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val time = calendar.timeInMillis

            val dayData = remember(time, sortedRecords, nextDateMillis) {
                calculateDayData(time, calendar.time, sortedRecords, nextDateMillis, settingsManager)
            }

            DayItem(
                day = dayData,
                isSelected = time == selectedDateMillis,
                onDateClick = { onDateSelected(time) }
            )
        }
    }
}

private fun calculateDayData(
    time: Long,
    date: Date,
    sortedRecords: List<PeriodRecord>,
    nextDateMillis: Long,
    settingsManager: SettingsManager
): CalendarDay {
    val isToday = android.text.format.DateUtils.isToday(time)

    val lastRealRecord = sortedRecords.lastOrNull { it.startDate <= time }

    val cleanNextDate = Calendar.getInstance().apply {
        timeInMillis = nextDateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val isPeriod = lastRealRecord != null &&
            time >= lastRealRecord.startDate &&
            time < lastRealRecord.startDate + (settingsManager.defaultPeriodLength * 86400000L)

    val isPrediction = time >= cleanNextDate &&
            time < cleanNextDate + (settingsManager.defaultPeriodLength * 86400000L)

    val status = when {
        isPeriod -> DayStatus.Period
        isPrediction -> DayStatus.Prediction
        else -> DayStatus.None
    }

    val referenceDate = if (time >= cleanNextDate && (lastRealRecord == null || cleanNextDate > lastRealRecord.startDate)) {
        cleanNextDate
    } else {
        lastRealRecord?.startDate
    }

    val dayOfCycle = referenceDate?.let {
        ((time - it) / 86400000L).toInt() + 1
    }

    return CalendarDay(
        date = date,
        timeInMillis = time,
        isToday = isToday,
        status = status,
        dayOfCycle = dayOfCycle
    )
}

@Composable
fun DayItem(day: CalendarDay, isSelected: Boolean, onDateClick: () -> Unit) {
    val backgroundColor = when (day.status) {
        DayStatus.Period -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        DayStatus.Prediction -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> if (day.isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else Color.Transparent
    }

    val contentColor = when (day.status) {
        DayStatus.Period -> MaterialTheme.colorScheme.onErrorContainer
        DayStatus.Prediction -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> if (day.isToday) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onDateClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else if (day.isToday && day.status == DayStatus.None)
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                else Modifier
            )
    ) {
        Text(
            text = SimpleDateFormat("E", Locale.getDefault()).format(day.date).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor.copy(alpha = 0.6f)
        )

        Text(
            text = SimpleDateFormat("d", Locale.getDefault()).format(day.date),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor
        )

        Text(
            text = day.dayOfCycle?.toString() ?: "—",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (day.status == DayStatus.Period) contentColor.copy(alpha = 0.8f) else contentColor.copy(alpha = 0.5f)
        )
    }
}