package com.songshawn.nutritrack.util

import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.models.ScoreType
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * CSV utility class, providing general CSV parsing functionality
 */
object CsvUtils {
    /**
     * Parse CSV line into a list of column values
     */
    fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        var inQuotes = false
        var currentValue = StringBuilder()

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    values.add(currentValue.toString())
                    currentValue = StringBuilder()
                }
                else -> currentValue.append(char)
            }
        }

        // Add the last value
        values.add(currentValue.toString())
        return values
    }
}

/**
 * CSV parser using enhanced enums
 */
suspend fun parseCsvWithEnhancedEnums(
    repository: PatientRepository,
    inputStream: InputStream
): Int {
    return withContext(Dispatchers.IO) {
        try {
            var insertedCount = 0
            val patients = mutableListOf<PatientEntity>()

            // Read all lines from the CSV file
            val allLines = inputStream.bufferedReader().readLines()

            if (allLines.isEmpty()) {
                return@withContext 0
            }

            // Parse header to get column indices
            val headerLine = allLines[0]

            // Use enum's companion object method to parse header
            val columnIndices = ScoreType.parseHeaderIndices(headerLine)

            // Validate that necessary columns exist
            if (!ScoreType.validateRequiredColumns(columnIndices)) {
                return@withContext 0
            }

            // Process data rows
            for (i in 1 until allLines.size) {
                val line = allLines[i]
                try {
                    val values = CsvUtils.parseCsvLine(line)

                    // Extract basic data
                    val phoneNumberIdx = columnIndices["PhoneNumber"] ?: 0
                    val userIdIdx = columnIndices["User_ID"] ?: 1
                    val sexIdx = columnIndices["Sex"] ?: 2

                    if (values.size <= maxOf(phoneNumberIdx, userIdIdx, sexIdx)) {
                        continue
                    }

                    val phoneNumber = values[phoneNumberIdx].trim()
                    val userId = values[userIdIdx].trim()
                    val genderStr = values[sexIdx].trim()

                    if (userId.isBlank() || phoneNumber.isBlank()) {
                        continue
                    }

                    try {
                        val gender = Gender.fromString(genderStr)

                        // Use enum's companion object method to create patient entity
                        val patient = ScoreType.createPatientFromCsv(
                            values, columnIndices, userId, phoneNumber, gender
                        )

                        patients.add(patient)

                        // Insert every 25 records
                        if (patients.size >= 25) {
                            repository.insertPatients(patients)
                            insertedCount += patients.size
                            patients.clear()
                        }

                    } catch (e: Exception) {
                        // Skip this record on error
                    }
                } catch (e: Exception) {
                    // Skip this line on error
                }
            }

            // Insert remaining patient records
            if (patients.isNotEmpty()) {
                repository.insertPatients(patients)
                insertedCount += patients.size
            }

            insertedCount
        } catch (e: Exception) {
            0
        }
    }
}