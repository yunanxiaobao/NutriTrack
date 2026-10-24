package com.haoshuang_34517812.nutritrack

import com.haoshuang_34517812.nutritrack.data.models.Gender
import com.haoshuang_34517812.nutritrack.data.models.ScoreType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the CSV score-extraction logic in [ScoreType].
 * These cover the pure parsing/mapping behaviour used when importing
 * patient HEIFA scores from the source CSV dataset.
 */
class ScoreTypeTest {

    @Test
    fun getColumnName_genderSpecific_returnsMaleColumnForMale() {
        assertEquals("HEIFAtotalscoreMale", ScoreType.HEIFA_TOTAL.getColumnName(Gender.MALE))
        assertEquals("FruitHEIFAscoreMale", ScoreType.FRUIT.getColumnName(Gender.MALE))
    }

    @Test
    fun getColumnName_genderSpecific_returnsFemaleColumnForFemale() {
        assertEquals("HEIFAtotalscoreFemale", ScoreType.HEIFA_TOTAL.getColumnName(Gender.FEMALE))
        assertEquals("FruitHEIFAscoreFemale", ScoreType.FRUIT.getColumnName(Gender.FEMALE))
    }

    @Test
    fun getColumnName_nonGenderSpecific_ignoresGender() {
        assertEquals(
            "Vegetablesvariationsscore",
            ScoreType.VEGETABLES_VARIATIONS.getColumnName(Gender.MALE)
        )
        assertEquals(
            "Vegetablesvariationsscore",
            ScoreType.VEGETABLES_VARIATIONS.getColumnName(Gender.FEMALE)
        )
    }

    @Test
    fun parseHeaderIndices_mapsTrimmedHeadersToPositions() {
        val indices = ScoreType.parseHeaderIndices("PhoneNumber, User_ID ,Sex,HEIFAtotalscoreMale")

        assertEquals(0, indices["PhoneNumber"])
        assertEquals(1, indices["User_ID"])
        assertEquals(2, indices["Sex"])
        assertEquals(3, indices["HEIFAtotalscoreMale"])
    }

    @Test
    fun validateRequiredColumns_trueWhenBaseColumnsPresent() {
        val indices = mapOf("PhoneNumber" to 0, "User_ID" to 1, "Sex" to 2)
        assertTrue(ScoreType.validateRequiredColumns(indices))
    }

    @Test
    fun validateRequiredColumns_falseWhenBaseColumnMissing() {
        val indices = mapOf("PhoneNumber" to 0, "User_ID" to 1)
        assertFalse(ScoreType.validateRequiredColumns(indices))
        assertFalse(ScoreType.validateRequiredColumns(emptyMap()))
    }

    @Test
    fun createPatientFromCsv_extractsScoresForGender() {
        val columnIndices = mapOf(
            "HEIFAtotalscoreMale" to 0,
            "HEIFAtotalscoreFemale" to 1,
            "FruitHEIFAscoreMale" to 2,
            "VegetablesHEIFAscoreMale" to 3
        )
        val values = listOf("88.5", "77.0", "9.5", "not-a-number")

        val male = ScoreType.createPatientFromCsv(values, columnIndices, "U1", "0400000000", Gender.MALE)
        assertEquals(88.5, male.heifaTotalScore, 0.001)
        assertEquals(9.5, male.fruitScore, 0.001)
        // Non-numeric cell falls back to 0.0 instead of throwing
        assertEquals(0.0, male.vegetableScore, 0.001)
        // Column absent from the CSV falls back to 0.0
        assertEquals(0.0, male.dairyScore, 0.001)

        val female = ScoreType.createPatientFromCsv(values, columnIndices, "U1", "0400000000", Gender.FEMALE)
        assertEquals(77.0, female.heifaTotalScore, 0.001)
    }
}

class GenderTest {

    @Test
    fun fromString_parsesKnownValuesCaseInsensitively() {
        assertEquals(Gender.MALE, Gender.fromString("male"))
        assertEquals(Gender.MALE, Gender.fromString(" Male "))
        assertEquals(Gender.FEMALE, Gender.fromString("FEMALE"))
        assertEquals(Gender.FEMALE, Gender.fromString("female"))
    }

    @Test
    fun fromString_unknownValueDefaultsToMale() {
        // Documents the current fallback behaviour for unrecognised input.
        assertEquals(Gender.MALE, Gender.fromString("unknown"))
        assertEquals(Gender.MALE, Gender.fromString(""))
    }
}
