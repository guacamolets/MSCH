package com.example.msch.services

import com.example.msch.entities.PeriodRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataSerializer {
    fun toJson(records: List<PeriodRecord>): String {
        return Json.Default.encodeToString(records)
    }

    fun toCsv(records: List<PeriodRecord>): String {
        return buildString {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            append("ID,Start Date (Millis),Readable Date\n")

            records.forEach { record ->
                val date = sdf.format(Date(record.startDate))
                append("${record.id},${record.startDate},$date\n")
            }
        }
    }

    fun fromJson(jsonString: String): List<PeriodRecord> {
        return try {
            Json.Default.decodeFromString<List<PeriodRecord>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}