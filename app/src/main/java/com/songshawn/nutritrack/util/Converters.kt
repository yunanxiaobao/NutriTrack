package com.songshawn.nutritrack.util

import androidx.room.TypeConverter
import com.songshawn.nutritrack.data.models.FoodCategory
import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.models.Persona
import java.util.Date

/**
 * Type converters for Room database
 * Handles conversion between complex types and SQLite-compatible types
 */
class Converters {
    /**
     * Gender enum conversions
     */
    @TypeConverter
    fun fromGender(value: Gender?): String? = value?.name

    @TypeConverter
    fun toGender(value: String?): Gender? = value?.let { Gender.valueOf(it) }

    /**
     * Persona enum conversions
     */
    @TypeConverter
    fun fromPersona(value: Persona?): String? = value?.name

    @TypeConverter
    fun toPersona(value: String?): Persona? = value?.let { Persona.valueOf(it) }

    /**
     * Food category list conversions
     */
    @TypeConverter
    fun fromCategoryList(value: List<FoodCategory>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toCategoryList(value: String): List<FoodCategory> =
        if (value.isBlank()) emptyList() else value.split(",").map { FoodCategory.valueOf(it) }

    /**
     * Date conversions
     */
    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }
}

