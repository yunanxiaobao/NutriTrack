package com.haoshuang_34517812.nutritrack

import com.haoshuang_34517812.nutritrack.util.CsvUtils
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [CsvUtils.parseCsvLine], the single-line CSV parser used
 * by the patient-data import path.
 */
class CsvUtilsTest {

    @Test
    fun parseCsvLine_simpleRow() {
        assertEquals(listOf("a", "b", "c"), CsvUtils.parseCsvLine("a,b,c"))
    }

    @Test
    fun parseCsvLine_quotedValueMayContainComma() {
        assertEquals(
            listOf("Smith, John", "42", "Male"),
            CsvUtils.parseCsvLine("\"Smith, John\",42,Male")
        )
    }

    @Test
    fun parseCsvLine_trailingCommaYieldsEmptyLastValue() {
        assertEquals(listOf("a", "b", ""), CsvUtils.parseCsvLine("a,b,"))
    }

    @Test
    fun parseCsvLine_emptyLineYieldsSingleEmptyValue() {
        assertEquals(listOf(""), CsvUtils.parseCsvLine(""))
    }

    @Test
    fun parseCsvLine_keepsWhitespaceInsideValues() {
        assertEquals(listOf(" 1 ", "2"), CsvUtils.parseCsvLine(" 1 ,2"))
    }
}
