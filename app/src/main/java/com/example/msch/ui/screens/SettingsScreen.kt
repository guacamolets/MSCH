package com.example.msch.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.room3.vo.Warning
import com.example.msch.R
import com.example.msch.notifications.NotificationScheduler
import com.example.msch.services.SettingsManager
import com.example.msch.ui.components.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    nextDateMillis: Long,
    onThemeChanged: () -> Unit,
    onDeleteAll: () -> Unit
) {
    val context = LocalContext.current
    val scheduler = remember { NotificationScheduler(context) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var cycleLength by remember { mutableIntStateOf(settingsManager.defaultCycleLength) }
    var periodLength by remember { mutableIntStateOf(settingsManager.defaultPeriodLength) }
    var reminderDays by remember { mutableIntStateOf(settingsManager.reminderDaysBefore) }
    var reminderHour by remember { mutableIntStateOf(settingsManager.reminderHour) }
    var reminderMinute by remember { mutableIntStateOf(settingsManager.reminderMinute) }
    var remindToday by remember { mutableStateOf(settingsManager.remindToday) }
    var remindBeforeEnabled by remember { mutableStateOf(settingsManager.remindBeforeEnabled) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) scheduler.scheduleNotification(nextDateMillis, reminderDays)
    }

    val checkPermissions = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val timePickerDialog = TimePickerDialog(context, { _, h, m ->
        reminderHour = h
        reminderMinute = m
        settingsManager.reminderHour = h
        settingsManager.reminderMinute = m
        scheduler.scheduleNotification(nextDateMillis, reminderDays)
    }, reminderHour, reminderMinute, true)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        SettingsSection(title = stringResource(R.string.cycle_settings)) {
            SettingsRow(label = stringResource(R.string.average_cycle_length)) {
                NumberPicker(cycleLength) {
                    cycleLength = it
                    settingsManager.defaultCycleLength = it
                }
            }
            SettingsRow(label = stringResource(R.string.period_duration)) {
                NumberPicker(periodLength) {
                    periodLength = it
                    settingsManager.defaultPeriodLength = it
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(R.string.notifications)) {
            SettingsRow(label = stringResource(R.string.remind_before_start)) {
                Switch(remindBeforeEnabled, {
                    remindBeforeEnabled = it
                    settingsManager.remindBeforeEnabled = it
                    if (it) { checkPermissions(); scheduler.scheduleNotification(nextDateMillis, reminderDays) }
                    else scheduler.cancelNotification("before_work")
                })
            }

            AnimatedVisibility(remindBeforeEnabled, enter = expandVertically(), exit = shrinkVertically()) {
                SettingsRow(stringResource(R.string.remind_me_before), Modifier.padding(start = 16.dp)) {
                    NumberPicker(reminderDays) {
                        reminderDays = it
                        settingsManager.reminderDaysBefore = it
                        scheduler.scheduleNotification(nextDateMillis, it)
                    }
                }
            }

            SettingsRow(label = stringResource(R.string.remind_on_start_day)) {
                Switch(remindToday, {
                    remindToday = it
                    settingsManager.remindToday = it
                    if (it) { checkPermissions(); scheduler.scheduleNotification(nextDateMillis, 0) }
                    else scheduler.cancelNotification("today_work")
                })
            }

            SettingsRow(label = stringResource(R.string.notification_time)) {
                val enabled = remindBeforeEnabled || remindToday
                TimeDisplay(reminderHour, reminderMinute, enabled) { timePickerDialog.show() }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(R.string.appearance_and_language)) {
            ThemeSelector(settingsManager, onThemeChanged)
            HorizontalDivider(Modifier.padding(vertical = 4.dp), 0.5.dp)
            LanguageSelector(settingsManager)
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(R.string.data_management)) {
            TextButton(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.clear_history))
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.delete_all_title)) },
            text = { Text(stringResource(R.string.delete_all_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAll()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.button_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }
}