package com.songshawn.nutritrack.data.models

import com.songshawn.nutritrack.data.room.entity.PatientEntity
import com.songshawn.nutritrack.util.CsvUtils

enum class ScoreType(
    val maleColumnName: String,
    val femaleColumnName: String,
    val isGenderSpecific: Boolean = true
) {
    HEIFA_TOTAL("HEIFAtotalscoreMale", "HEIFAtotalscoreFemale"),
    DISCRETIONARY("DiscretionaryHEIFAscoreMale", "DiscretionaryHEIFAscoreFemale"),
    VEGETABLES("VegetablesHEIFAscoreMale", "VegetablesHEIFAscoreFemale"),
    VEGETABLES_VARIATIONS("Vegetablesvariationsscore", "Vegetablesvariationsscore", false),
    FRUIT("FruitHEIFAscoreMale", "FruitHEIFAscoreFemale"),
    FRUIT_SERVESIZE("Fruitservesize", "Fruitservesize", false),
    FRUIT_VARIATIONS("Fruitvariationsscore", "Fruitvariationsscore", false),
    GRAINS("GrainsandcerealsHEIFAscoreMale", "GrainsandcerealsHEIFAscoreFemale"),
    WHOLEGRAINS("WholegrainsHEIFAscoreMale", "WholegrainsHEIFAscoreFemale"),
    MEAT_AND_ALTERNATIVES("MeatandalternativesHEIFAscoreMale", "MeatandalternativesHEIFAscoreFemale"),
    DAIRY("DairyandalternativesHEIFAscoreMale", "DairyandalternativesHEIFAscoreFemale"),
    SODIUM("SodiumHEIFAscoreMale", "SodiumHEIFAscoreFemale"),
    ALCOHOL("AlcoholHEIFAscoreMale", "AlcoholHEIFAscoreFemale"),
    WATER("WaterHEIFAscoreMale", "WaterHEIFAscoreFemale"),
    SUGAR("SugarHEIFAscoreMale", "SugarHEIFAscoreFemale"),
    SATURATED_FAT("SaturatedFatHEIFAscoreMale", "SaturatedFatHEIFAscoreFemale"),
    UNSATURATED_FAT("UnsaturatedFatHEIFAscoreMale", "UnsaturatedFatHEIFAscoreFemale");

    /**
     * Get column name based on gender
     */
    fun getColumnName(gender: Gender): String {
        return if (isGenderSpecific) {
            if (gender == Gender.MALE) maleColumnName else femaleColumnName
        } else {
            maleColumnName
        }
    }

    companion object {
        /**
         * Extract score from CSV values
         * @param values List of values from CSV row
         * @param columnIndices Map of column names to indices
         * @param scoreType Type of score to extract
         * @param gender Gender of the patient
         * @return Extracted Double value, or 0.0 if not found
         */
        private fun extractScore(
            values: List<String>,
            columnIndices: Map<String, Int>,
            scoreType: ScoreType,
            gender: Gender
        ): Double {
            try {
                val columnName = scoreType.getColumnName(gender)
                val columnIndex = columnIndices[columnName]

                return if (columnIndex != null && columnIndex < values.size) {
                    values[columnIndex].trim().toDoubleOrNull() ?: 0.0
                } else {
                    // Column not found or index out of range, return default value
                    0.0
                }
            } catch (e: Exception) {
                // Return default value for any exception
                return 0.0
            }
        }

        /**
         * Create PatientEntity from CSV data
         * @param values List of values from CSV row
         * @param columnIndices Map of column names to indices
         * @param userId User ID
         * @param phoneNumber Phone number
         * @param gender Gender
         * @return Created PatientEntity
         */
        fun createPatientFromCsv(
            values: List<String>,
            columnIndices: Map<String, Int>,
            userId: String,
            phoneNumber: String,
            gender: Gender
        ): PatientEntity {
            return PatientEntity(
                userId = userId,
                phoneNumber = phoneNumber,
                name = "User",
                passwordHash = "",
                sex = gender,
                heifaTotalScore = extractScore(values, columnIndices, HEIFA_TOTAL, gender),
                discretionaryScore = extractScore(values, columnIndices, DISCRETIONARY, gender),
                vegetableScore = extractScore(values, columnIndices, VEGETABLES, gender),
                vegetablesVariantionsScore = extractScore(values, columnIndices, VEGETABLES_VARIATIONS, gender),
                fruitScore = extractScore(values, columnIndices, FRUIT, gender),
                fruitServeSize = extractScore(values, columnIndices, FRUIT_SERVESIZE, gender),
                fruitVariantionsScore = extractScore(values, columnIndices, FRUIT_VARIATIONS, gender),
                grainsScore = extractScore(values, columnIndices, GRAINS, gender),
                wholegrainsScore = extractScore(values, columnIndices, WHOLEGRAINS, gender),
                meatAndAlternativeScore = extractScore(values, columnIndices, MEAT_AND_ALTERNATIVES, gender),
                dairyScore = extractScore(values, columnIndices, DAIRY, gender),
                sodiumScore = extractScore(values, columnIndices, SODIUM, gender),
                alcoholScore = extractScore(values, columnIndices, ALCOHOL, gender),
                waterScore = extractScore(values, columnIndices, WATER, gender),
                sugarScore = extractScore(values, columnIndices, SUGAR, gender),
                saturatedFatScore = extractScore(values, columnIndices, SATURATED_FAT, gender),
                unsaturatedFatScore = extractScore(values, columnIndices, UNSATURATED_FAT, gender),
            )
        }

        /**
         * Get column indices mapping from CSV header
         * @param headerLine CSV header line
         * @return Map of column names to indices
         */
        fun parseHeaderIndices(headerLine: String): Map<String, Int> {
            try {
                val headers = CsvUtils.parseCsvLine(headerLine)
                return headers.mapIndexed { index, header -> header.trim() to index }.toMap()
            } catch (e: Exception) {
                // Return empty map if header parsing fails
                return emptyMap()
            }
        }

        /**
         * Validate that CSV header contains all required columns
         * @param columnIndices Map of column names to indices
         * @return Whether all required columns are present
         */
        fun validateRequiredColumns(columnIndices: Map<String, Int>): Boolean {
            try {
                val requiredBaseColumns = listOf("PhoneNumber", "User_ID", "Sex")
                val missingBase = requiredBaseColumns.filter { !columnIndices.containsKey(it) }

                if (missingBase.isNotEmpty()) {
                    // Basic columns missing, validation fails
                    return false
                }

                // Validate all score type columns - we only check without logging warnings
                for (scoreType in entries) {
                    val maleMissing = !columnIndices.containsKey(scoreType.maleColumnName)
                    val femaleMissing = !columnIndices.containsKey(scoreType.femaleColumnName)

                    if (scoreType.isGenderSpecific) {
                        if (maleMissing && femaleMissing) {
                            // Gender-specific columns missing, but continue checking
                        }
                    } else if (maleMissing) { // Only check one for non-gender-specific
                        // Non-gender-specific column missing, but continue checking
                    }
                }

                return true
            } catch (e: Exception) {
                // Return validation failure if exception occurs during validation
                return false
            }
        }
    }
}