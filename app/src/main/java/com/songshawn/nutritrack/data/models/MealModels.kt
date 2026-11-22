package com.songshawn.nutritrack.data.models

/**
 * Primary Australian Guide to Healthy Eating (AGTHE) food group for a catalog item,
 * used by [com.songshawn.nutritrack.domain.scoring.HeifaScoreEngine] to tally daily serves.
 */
enum class HeifaFoodGroup(val displayName: String) {
    VEGETABLES("Vegetables"),
    FRUITS("Fruits"),
    GRAINS("Grains & Cereals"),
    WHOLE_GRAINS("Whole Grains"),
    MEAT_AND_ALTERNATIVES("Meat & Alternatives"),
    DAIRY_AND_ALTERNATIVES("Dairy & Alternatives"),
    WATER("Water & Hydration"),
    UNSATURATED_FATS("Healthy Fats & Oils"),
    DISCRETIONARY("Discretionary & Treats"),
    ALCOHOL("Alcohol"),
}

/**
 * Meal slot within a day.
 */
enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
}
