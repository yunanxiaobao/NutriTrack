package com.songshawn.nutritrack.data.models

import androidx.annotation.DrawableRes
import com.songshawn.nutritrack.R

/**
 * Food categories used in the questionnaire and food intake tracking
 * Each category has a display name and an associated image resource
 */
enum class FoodCategory(
    val displayName: String,
    @DrawableRes val imageRes: Int
) {
    FRUITS("Fruits", R.drawable.watermelon_10751209),
    VEGETABLES("Vegetables", R.drawable.bok_choy_4464919),
    GRAINS("Grains", R.drawable.wheat_flour_3731151),
    RED_MEAT("Red Meat", R.drawable.meatloaf_1702814),
    SEAFOOD("Seafood", R.drawable.seafood_5473993),
    POULTRY("Poultry", R.drawable.chicken_5609171),
    FISH("Fish", R.drawable.fish_9757121),
    EGGS("Eggs", R.drawable.eggs_2503808),
    NUTS_SEEDS("Nuts/Seeds", R.drawable.peanuts_18385009)
}

