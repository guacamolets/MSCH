package com.example.msch.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.msch.R
import com.example.msch.data.PeriodRecord
import com.example.msch.logic.DataSerializer

@Composable
fun ExportMenu(
    records: List<PeriodRecord>,
    onShare: (String, String) -> Unit,
    onImport: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.menu_title))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.json_export_title)) },
                onClick = {
                    val json = DataSerializer.toJson(records)
                    onShare(json, "backup.json")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.csv_export_title)) },
                onClick = {
                    val csv = DataSerializer.toCsv(records)
                    onShare(csv, "history.csv")
                    expanded = false
                }
            )
            Divider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.json_import_title)) },
                onClick = {
                    onImport()
                    expanded = false
                }
            )
            Divider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings)) },
                leadingIcon = {
                    Icon(Icons.Default.Settings, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onNavigateToSettings()
                }
            )
        }
    }
}