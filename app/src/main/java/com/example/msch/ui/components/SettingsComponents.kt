package com.example.msch.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.msch.R
import com.example.msch.services.SettingsManager

@Composable
fun TimeDisplay(hour: Int, minute: Int, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = MaterialTheme.shapes.small,
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceVariant,
        enabled = enabled
    ) {
        Text(
            text = String.format("%02d:%02d", hour, minute),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}

@Composable
fun ThemeSelector(settingsManager: SettingsManager, onThemeChanged: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val themes = listOf(
        stringResource(R.string.theme_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark)
    )

    SettingsRow(label = stringResource(R.string.theme)) {
        TextButton(onClick = { showDialog = true }) {
            Text(themes[settingsManager.appTheme])
        }
    }

    if (showDialog) {
        ThemeSelectionDialog(
            currentSelection = settingsManager.appTheme,
            onDismiss = { showDialog = false },
            onSelect = {
                settingsManager.appTheme = it
                showDialog = false
                onThemeChanged()
            }
        )
    }
}

@Composable
fun LanguageSelector(settingsManager: SettingsManager) {
    var showDialog by remember { mutableStateOf(false) }
    val languages = mapOf(
        "system" to stringResource(R.string.lang_system),
        "ru" to "Русский",
        "en" to "English"
    )

    SettingsRow(label = stringResource(R.string.language)) {
        TextButton(onClick = { showDialog = true }) {
            Text(languages[settingsManager.appLanguage] ?: languages["system"]!!)
        }
    }

    if (showDialog) {
        LanguageSelectionDialog(
            currentSelection = settingsManager.appLanguage,
            onDismiss = { showDialog = false },
            onSelect = {
                settingsManager.appLanguage = it
                showDialog = false
                val appLocale = if (it == "system") LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(it)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        )
    }
}

@Composable
fun ThemeSelectionDialog(currentSelection: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val options = listOf(
        stringResource(R.string.theme_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column {
                options.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = (index == currentSelection), onClick = { onSelect(index) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (index == currentSelection), onClick = { onSelect(index) })
                        Text(text, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

@Composable
fun LanguageSelectionDialog(currentSelection: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val options = listOf(
        "system" to stringResource(R.string.lang_system),
        "ru" to "Русский",
        "en" to "English"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column {
                options.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = (code == currentSelection), onClick = { onSelect(code) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (code == currentSelection), onClick = { onSelect(code) })
                        Text(label, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}