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
import com.example.msch.logic.AppConfig
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

    val baseTime = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(count = totalDays, key = { it }) { index ->
            val time = remember(index) {
                baseTime + (index - initialIndex) * AppConfig.MILLIS_IN_DAY
            }
            val date = remember(time) { Date(time) }
            val dayData = remember(time, sortedRecords, nextDateMillis) {
                calculateDayData(time, date, sortedRecords, nextDateMillis, settingsManager)
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
    val now = System.currentTimeMillis()
    val record = sortedRecords.lastOrNull { it.startDate <= time }

    val cleanNextDate = Calendar.getInstance().apply {
        timeInMillis = nextDateMillis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val isPeriod = record != null && run {
        val start = record.startDate
        val defaultEnd = start + (settingsManager.defaultPeriodLength * AppConfig.MILLIS_IN_DAY)
        val actualEnd = record.endDate ?: defaultEnd

        if (record.endDate == null) {
            time >= start && time <= now && time < actualEnd
        } else {
            time >= start && time <= actualEnd
        }
    }

    val isPrediction = !isPeriod && time >= cleanNextDate &&
            time < cleanNextDate + (settingsManager.defaultPeriodLength * AppConfig.MILLIS_IN_DAY)

    val status = when {
        isPeriod -> DayStatus.Period
        isPrediction -> DayStatus.Prediction
        else -> DayStatus.None
    }

    val referenceDate = if (status == DayStatus.Prediction) cleanNextDate else record?.startDate
    val dayOfCycle = referenceDate?.let {
        val diff = (time - it) / AppConfig.MILLIS_IN_DAY
        if (diff in 0..45) (diff + 1).toInt() else null
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
    val dayNameFormatter = remember { SimpleDateFormat("E", Locale.getDefault()) }
    val dayNumberFormatter = remember { SimpleDateFormat("d", Locale.getDefault()) }

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
            text = dayNameFormatter.format(day.date).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor.copy(alpha = 0.6f)
        )

        Text(
            text = dayNumberFormatter.format(day.date),
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