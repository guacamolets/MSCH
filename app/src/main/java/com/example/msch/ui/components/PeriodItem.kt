package com.example.msch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.entities.PeriodRecord
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun PeriodItem(
    record: PeriodRecord,
    cycleLength: Int?,
    periodDuration: Int?,
    ovulationDay: Int,
    onClick: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val sdf = remember(locale) { SimpleDateFormat("d MMMM yyyy", locale) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sdf.format(Date(record.startDate)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                cycleLength?.let { days ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = stringResource(R.string.days_count_format, days),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (periodDuration != null) {
                        stringResource(R.string.period_duration_format, periodDuration)
                    } else {
                        stringResource(R.string.period_active)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (cycleLength != null && periodDuration != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    SegmentedCycleBar(
                        periodDays = periodDuration,
                        totalDays = cycleLength,
                        ovulationDay = ovulationDay,
                        modifier = Modifier
                            .fillMaxWidth(fraction = (cycleLength.toFloat() / 40f).coerceAtMost(1f))
                            .height(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedCycleBar(
    periodDays: Int,
    totalDays: Int,
    ovulationDay: Int,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFFFF5252)
    val ovulationColor = Color(0xFF4DB6AC)
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val gap = 2.dp.toPx()
        val displayDays = totalDays.coerceIn(1, 45)
        val dayWidth = (size.width - (displayDays - 1) * gap) / displayDays

        for (i in 0 until displayDays) {
            val startX = i * (dayWidth + gap)
            val color = when {
                i < periodDays -> activeColor
                i == ovulationDay -> ovulationColor
                else -> inactiveColor
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(startX, 0f),
                size = Size(dayWidth, size.height),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}