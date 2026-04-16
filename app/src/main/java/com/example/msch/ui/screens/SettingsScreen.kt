package com.example.msch.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.msch.R
import com.example.msch.logic.SettingsManager
import android.Manifest
import android.content.pm.PackageManager
import com.example.msch.logic.NotificationScheduler
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsScreen(settingsManager: SettingsManager, nextDateMillis: Long) {
    var cycleLength by remember { mutableIntStateOf(settingsManager.defaultCycleLength) }
    var periodLength by remember { mutableIntStateOf(settingsManager.defaultPeriodLength) }
    var reminderDays by remember { mutableIntStateOf(settingsManager.reminderDaysBefore) }
    var reminderHour by remember { mutableIntStateOf(settingsManager.reminderHour) }
    var reminderMinute by remember { mutableIntStateOf(settingsManager.reminderMinute) }
    var remindToday by remember { mutableStateOf(settingsManager.remindToday) }
    var remindBeforeEnabled by remember { mutableStateOf(settingsManager.remindBeforeEnabled) }

    val context = LocalContext.current
    val scheduler = remember { NotificationScheduler(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scheduler.scheduleNotification(nextDateMillis, reminderDays)
        }
    }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            reminderHour = hour
            reminderMinute = minute
            settingsManager.reminderHour = hour
            settingsManager.reminderMinute = minute
            NotificationScheduler(context).scheduleNotification(nextDateMillis, settingsManager.reminderDaysBefore)
        },
        reminderHour,
        reminderMinute,
        true
    )

    val checkAndRequestPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        SettingsSection(title = stringResource(R.string.cycle_settings)) {
            SettingsRow(label = stringResource(R.string.average_cycle_length)) {
                NumberPicker(value = cycleLength, onValueChange = {
                    cycleLength = it
                    settingsManager.defaultCycleLength = it
                })
            }
            SettingsRow(label = stringResource(R.string.period_duration)) {
                NumberPicker(value = periodLength, onValueChange = {
                    periodLength = it
                    settingsManager.defaultPeriodLength = it
                })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource(R.string.notifications)) {
            SettingsRow(label = stringResource(R.string.remind_before_start)) {
                Switch(
                    checked = remindBeforeEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            checkAndRequestPermission()
                            scheduler.scheduleNotification(nextDateMillis, reminderDays)
                        } else {
                            scheduler.cancelNotification("before_work")
                        }
                        remindBeforeEnabled = isChecked
                        settingsManager.remindBeforeEnabled = isChecked
                    }
                )
            }

            AnimatedVisibility(
                visible = remindBeforeEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsRow(
                    label = stringResource(R.string.remind_me_before),
                    labelModifier = Modifier.padding(start = 16.dp)
                ) {
                    NumberPicker(
                        value = reminderDays,
                        onValueChange = {
                            reminderDays = it
                            settingsManager.reminderDaysBefore = it
                            scheduler.scheduleNotification(nextDateMillis, it)
                        }
                    )
                }
            }

            SettingsRow(label = stringResource(R.string.remind_on_start_day)) {
                Switch(
                    checked = remindToday,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            checkAndRequestPermission()
                            scheduler.scheduleNotification(nextDateMillis, 0)
                        } else {
                            scheduler.cancelNotification("today_work")
                        }
                        remindToday = isChecked
                        settingsManager.remindToday = isChecked
                    }
                )
            }

            val isAnyNotificationEnabled = remindBeforeEnabled || remindToday

            SettingsRow(label = stringResource(R.string.notification_time)) {
                Surface(
                    onClick = { if (isAnyNotificationEnabled) timePickerDialog.show() },
                    shape = MaterialTheme.shapes.small,
                    color = if (isAnyNotificationEnabled)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    enabled = isAnyNotificationEnabled
                ) {
                    Text(
                        text = String.format("%02d:%02d", reminderHour, reminderMinute),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isAnyNotificationEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(8.dp), content = content)
        }
    }
}

@Composable
fun SettingsRow(label: String, labelModifier: Modifier = Modifier, control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).then(labelModifier)
        )
        control()
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