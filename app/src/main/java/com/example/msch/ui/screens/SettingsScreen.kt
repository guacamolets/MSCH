package com.example.msch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.logic.SettingsManager

@Composable
fun SettingsScreen(settingsManager: SettingsManager) {
    var cycleLength by remember { mutableIntStateOf(settingsManager.defaultCycleLength) }
    var periodLength by remember { mutableIntStateOf(settingsManager.defaultPeriodLength) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.average_cycle_length),
            style = MaterialTheme.typography.labelMedium
        )
        NumberPicker(value = cycleLength, onValueChange = {
            cycleLength = it
            settingsManager.defaultCycleLength = it
        })

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.period_duration),
            style = MaterialTheme.typography.labelMedium
        )
        NumberPicker(value = periodLength, onValueChange = {
            periodLength = it
            settingsManager.defaultPeriodLength = it
        })

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.notifications),
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun NumberPicker(value: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (value > 1) onValueChange(value - 1) }) {
            Icon(Icons.Default.Remove, contentDescription = null)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = { if (value < 100) onValueChange(value + 1) }) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}