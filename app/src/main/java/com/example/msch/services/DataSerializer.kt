package com.example.msch.services

import com.example.msch.entities.PeriodRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataSerializer {
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    )

    fun toJson(records: List<PeriodRecord>): String {
        return Json.Default.encodeToString(records)
    }

    fun toCsv(records: List<PeriodRecord>): String {
        return buildString {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            append("Start Date,End Date\n")

            records.forEach { record ->
                val startDate = sdf.format(Date(record.startDate))
                val endDate = record.endDate?.let { sdf.format(Date(it)) } ?: ""
                append("$startDate,$endDate\n")
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

    fun fromCsv(csvString: String): List<PeriodRecord> {
        val records = mutableListOf<PeriodRecord>()

        csvString.lines().forEach { line ->
            if (line.isBlank() || line.contains("Start Date")) return@forEach

            val parts = line.split(",", ";")
            if (parts.isNotEmpty()) {
                val start = parseDate(parts[0].trim())
                val end = if (parts.size > 1) parseDate(parts[1].trim()) else null

                if (start != null) {
                    records.add(PeriodRecord(startDate = start, endDate = end))
                }
            }
        }
        return records
    }

    private fun parseDate(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        for (format in dateFormats) {
            try {
                return format.parse(dateStr)?.time
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }
}