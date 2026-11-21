package com.songshawn.nutritrack.data.models

import androidx.annotation.DrawableRes
import com.songshawn.nutritrack.R

/**
 * Food categories used in the questionnaire and food intake tracking
 * Each category has a display name and an associated image resource
 */
enum class InsightsScreenScoreCategory(
    val displayName: String,
    @DrawableRes val imageRes: Int,
    val maxScore: Int = 10 // 默认最大分数为10
) {
    FRUITS("Fruits", R.drawable.watermelon_10751209),
    VEGETABLES("Vegetables", R.drawable.bok_choy_4464919),
    MEAT_AND_ALTERNATIVES("Meat & Alternatives", R.drawable.meatloaf_1702814),
    DAIRY_AND_ALTERNATIVES("Dairy & Alternatives", R.drawable.milk),
    SODIUM("Sodium", R.drawable.sodium),
    SUGAR("Sugar", R.drawable.sugar),
    ALCOHOL("Alcohol", R.drawable.alcohol, 5),
    WATER("Water", R.drawable.water, 5),
    GRAINS_AND_CEREALS("Grains & Cereals", R.drawable.wheat_flour_3731151, 5),
    WHOLE_GRAINS("Whole Grains", R.drawable.whole_grains, 5),
    SATURATED_FATS("Saturated Fats", R.drawable.saturated_fat, 5),
    UNSATURATED_FATS("Unsaturated Fats", R.drawable.unsaturated_fat, 5),
    DISCRETIONARY_FOODS("Discretionary Foods", R.drawable.fast_food_737967);

    companion object {
        /**
         * Find a food category by its display name
         * @param name The display name to look for
         * @return The corresponding FoodCategory or null if not found
         */
        fun findByName(name: String): InsightsScreenScoreCategory? {
            return entries.find { it.displayName.equals(name, ignoreCase = true) }
        }

        /**
         * Get the maximum score for a category name
         * @param categoryName The display name of the category
         * @return The maximum score (5 or 10)
         */
        fun getMaxScoreForName(categoryName: String): Int {
            return findByName(categoryName)?.maxScore ?: 10
        }

    }
}