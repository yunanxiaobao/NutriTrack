package com.songshawn.nutritrack.util

import androidx.room.TypeConverter
import com.songshawn.nutritrack.data.models.FoodCategory
import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.models.HeifaFoodGroup
import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.models.Persona
import java.util.Date

/**
 * Type converters for Room database.
 */
class Converters {
    @TypeConverter
    fun fromGender(value: Gender?): String? = value?.name

    @TypeConverter
    fun toGender(value: String?): Gender? = value?.let { Gender.valueOf(it) }

    @TypeConverter
    fun fromPersona(value: Persona?): String? = value?.name

    @TypeConverter
    fun toPersona(value: String?): Persona? = value?.let { Persona.valueOf(it) }

    @TypeConverter
    fun fromCategoryList(value: List<FoodCategory>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toCategoryList(value: String): List<FoodCategory> =
        if (value.isBlank()) emptyList() else value.split(",").map { FoodCategory.valueOf(it) }

    @TypeConverter
    fun fromHeifaFoodGroup(value: HeifaFoodGroup?): String? = value?.name

    @TypeConverter
    fun toHeifaFoodGroup(value: String?): HeifaFoodGroup? = value?.let { HeifaFoodGroup.valueOf(it) }

    @TypeConverter
    fun fromMealType(value: MealType?): String? = value?.name

    @TypeConverter
    fun toMealType(value: String?): MealType? = value?.let { MealType.valueOf(it) }

    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }
}
