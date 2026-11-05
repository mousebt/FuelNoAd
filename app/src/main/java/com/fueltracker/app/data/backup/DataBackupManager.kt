package com.fueltracker.app.data.backup

import android.content.Context
import android.net.Uri
import com.fueltracker.app.model.FuelRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataBackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * Exports records to JSON string
     */
    fun exportToJson(records: List<FuelRecord>): String {
        return json.encodeToString(records)
    }

    /**
     * Parses records from JSON string
     */
    fun importFromJson(jsonString: String): List<FuelRecord> {
        return json.decodeFromString(jsonString)
    }

    /**
     * Parses records from standard CSV string
     */
    fun importFromCsv(csvText: String, vehicleId: Long = 1L): List<FuelRecord> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val records = mutableListOf<FuelRecord>()
        val dataLines = if (lines.first().contains("时间") || lines.first().contains("里程")) {
            lines.drop(1)
        } else {
            lines
        }

        val fallbackFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (line in dataLines) {
            val parts = line.split(",").map { it.trim() }
            if (parts.size < 5) continue

            try {
                val dateStr = parts[0]
                val timestamp = try {
                    dateFormat.parse(dateStr)?.time
                        ?: fallbackFormat.parse(dateStr)?.time
                        ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                val odometer = parts[1].toDoubleOrNull() ?: continue
                val fuelAmount = parts[2].toDoubleOrNull() ?: continue
                val unitPrice = parts[3].toDoubleOrNull() ?: continue
                val totalPrice = parts[4].toDoubleOrNull() ?: (fuelAmount * unitPrice)

                val fuelType = if (parts.size > 5 && parts[5].isNotBlank()) parts[5] else "92#"
                val isFull = if (parts.size > 6) parts[6] == "是" || parts[6].contains("加满") else true
                val isMissed = if (parts.size > 7) parts[7] == "是" else false

                val station = if (parts.size > 10 && parts[10].isNotBlank()) parts[10] else null
                val note = if (parts.size > 11 && parts[11].isNotBlank()) parts[11] else null

                records.add(
                    FuelRecord(
                        vehicleId = vehicleId,
                        timestamp = timestamp,
                        odometer = odometer,
                        fuelAmount = fuelAmount,
                        unitPrice = unitPrice,
                        totalPrice = totalPrice,
                        fuelType = fuelType,
                        isFull = isFull,
                        isMissedPrevious = isMissed,
                        stationName = station,
                        note = note
                    )
                )
            } catch (_: Exception) {
                // skip malformed line
            }
        }

        return records
    }

    /**
     * Exports records to standard CSV format
     */
    fun exportToCsv(records: List<FuelRecord>): String {
        val sb = StringBuilder()
        sb.append("时间,总里程(km),加油量(L),单价(元/L),总金额(元),燃油号,是否加满,是否漏记,百公里油耗(L/100km),增量里程(km),加油站,备注\n")

        records.forEach { r ->
            val dateStr = dateFormat.format(Date(r.timestamp))
            val isFullStr = if (r.isFull) "是" else "否"
            val isMissedStr = if (r.isMissedPrevious) "是" else "否"
            val consumptionStr = r.calculatedConsumption?.toString() ?: ""
            val deltaStr = r.distanceTraveled?.toString() ?: ""
            val station = r.stationName?.replace(",", " ") ?: ""
            val note = r.note?.replace(",", " ") ?: ""

            sb.append("$dateStr,${r.odometer},${r.fuelAmount},${r.unitPrice},${r.totalPrice},${r.fuelType},$isFullStr,$isMissedStr,$consumptionStr,$deltaStr,$station,$note\n")
        }

        return sb.toString()
    }

    /**
     * Writes content to an external URI granted by Android Storage Access Framework
     */
    suspend fun writeToUri(context: Context, uri: Uri, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("无法打开文件写入流"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads text from an external URI granted by Android Storage Access Framework
     */
    suspend fun readFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    Result.success(reader.readText())
                }
            } ?: Result.failure(Exception("无法打开文件读取流"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
