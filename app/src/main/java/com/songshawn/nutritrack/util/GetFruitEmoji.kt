package com.songshawn.nutritrack.util

import androidx.compose.runtime.Composable

/**
 * Singleton object holding fruit emoji mappings
 */
object FruitEmojiMapper {
    private val fruitEmojiMap = mapOf(
        "apple" to "🍎",
        "banana" to "🍌",
        "orange" to "🍊",
        "lemon" to "🍋",
        "strawberry" to "🍓",
        "pear" to "🍐",
        "peach" to "🍑",
        "cherry" to "🍒",
        "grapes" to "🍇",
        "grape" to "🍇",
        "watermelon" to "🍉",
        "pineapple" to "🍍",
        "mango" to "🥭",
        "kiwi" to "🥝",
        "coconut" to "🥥",
        "avocado" to "🥑",
        "blueberry" to "🫐",
        "blackberry" to "🫐",
        "tomato" to "🍅",
        "gooseberry" to "🫐",
    )

    fun getEmoji(fruitName: String): String {
        return fruitEmojiMap.getOrDefault(fruitName.lowercase(), "🍏")
    }
}

/**
 * Maps fruit names to emoji representations
 */
@Composable
fun getFruitEmoji(fruitName: String): String {
    return FruitEmojiMapper.getEmoji(fruitName)
}