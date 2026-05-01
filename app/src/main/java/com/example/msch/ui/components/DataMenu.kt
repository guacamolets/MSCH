package com.example.msch.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.msch.R
import com.example.msch.entities.PeriodRecord
import com.example.msch.services.DataSerializer

@Composable
fun DataMenu(
    records: List<PeriodRecord>,
    onShare: (String, String) -> Unit,
    onImportFile: () -> Unit,
    onImportText: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_title))
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.json_export_title)) },
                leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                onClick = {
                    onShare(DataSerializer.toJson(records), "backup.json")
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.csv_export_title)) },
                leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                onClick = {
                    onShare(DataSerializer.toCsv(records), "history.csv")
                    menuExpanded = false
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            DropdownMenuItem(
                text = { Text(stringResource(R.string.import_from_file)) },
                leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                onClick = {
                    onImportFile()
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.import_from_text)) },
                leadingIcon = { Icon(Icons.Default.ContentPaste, null) },
                onClick = {
                    showTextDialog = true
                    menuExpanded = false
                }
            )
        }
    }

    if (showTextDialog) {
        ImportTextDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = {
                onImportText(it)
                showTextDialog = false
            }
        )
    }
}

@Composable
fun ImportTextDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.import_dialog_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text) }
            ) { Text(stringResource(R.string.button_import)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
        }
    )
}