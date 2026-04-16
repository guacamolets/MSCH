package com.example.msch.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.msch.entities.PeriodRecord
import com.example.msch.services.DataSerializer
import java.io.File

class DataManager(private val context: Context) {
    fun shareFile(content: String, fileName: String) {
        try {
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, fileName)
            file.writeText(content)

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = if (fileName.endsWith("csv")) "text/csv" else "application/json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(sendIntent, "Share File"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun readImportFile(uri: Uri, onRecordsLoaded: (List<PeriodRecord>) -> Unit) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                val records = DataSerializer.fromJson(content)
                if (records.isNotEmpty()) {
                    onRecordsLoaded(records)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}