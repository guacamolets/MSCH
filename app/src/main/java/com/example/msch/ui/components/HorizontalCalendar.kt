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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.msch.ui.models.CalendarDay
import com.example.msch.ui.models.DayStatus
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.AppConfig
import com.example.msch.logic.CyclePredictor
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
                val rawTime = baseTime + (index - initialIndex) * AppConfig.MILLIS_IN_DAY
                CyclePredictor.toStartOfDay(rawTime)
            }
            val date = remember(time) { Date(time) }
            val dayData = remember(time, records, nextDateMillis) {
                calculateDayData(time, date, records, nextDateMillis, settingsManager)
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
    records: List<PeriodRecord>,
    nextDateMillis: Long,
    settingsManager: SettingsManager
): CalendarDay {
    val isToday = android.text.format.DateUtils.isToday(time)
    val record = records.lastOrNull { it.startDate <= time }

    val isPeriod = record != null && run {
        val start = record.startDate
        val defaultEnd = start + (settingsManager.defaultPeriodLength * AppConfig.MILLIS_IN_DAY)
        val actualEnd = record.endDate ?: defaultEnd

        if (record.endDate == null) {
            time in start..<actualEnd
        } else {
            time in start..actualEnd
        }
    }

    val cleanNextDate = CyclePredictor.toStartOfDay(nextDateMillis)
    val isPrediction = !isPeriod && time >= cleanNextDate &&
            time < cleanNextDate + (settingsManager.defaultPeriodLength * AppConfig.MILLIS_IN_DAY)

    val isOvulation = CyclePredictor.isOvulationDay(time, records, cleanNextDate, settingsManager.defaultCycleLength)

    val status = when {
        isPeriod -> DayStatus.Period
        isOvulation -> DayStatus.Ovulation
        isPrediction -> DayStatus.Prediction
        else -> DayStatus.None
    }

    val dayOfCycle = CyclePredictor.getDayOfCycleForDate(records, time)

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
        DayStatus.Ovulation -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        else -> if (day.isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else Color.Transparent
    }

    val contentColor = when (day.status) {
        DayStatus.Period -> MaterialTheme.colorScheme.onErrorContainer
        DayStatus.Prediction -> MaterialTheme.colorScheme.onTertiaryContainer
        DayStatus.Ovulation -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> if (day.isToday) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .alpha(if (day.timeInMillis < System.currentTimeMillis() && !day.isToday) 0.5f else 1f)
            .background(backgroundColor)
            .clickable { onDateClick() }
            .then(
                if (isSelected)
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else
                    Modifier
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