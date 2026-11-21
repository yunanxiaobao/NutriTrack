package com.songshawn.nutritrack.data.models

import androidx.annotation.DrawableRes
import com.songshawn.nutritrack.R

/**
 * User personas representing different health attitudes and behaviors
 * Used to personalize recommendations and coach interactions
 */
enum class Persona(
    val displayName: String,
    val description: String,
    @DrawableRes val imageRes: Int
) {
    HEALTH_DEVOTEE(
        "Health Devotee",
        "I’m passionate about healthy eating & health plays a big part in my life. I use social media to follow active lifestyle personalities or get new recipes/exercise ideas. I may even buy superfoods or follow a particular type of diet. I like to think I am super healthy.",
        R.drawable.persona_1
    ),
    MINDFUL_EATER(
        "Mindful Eater",
        "I’m health-conscious and being healthy and eating healthy is important to me. Although health means different things to different people, I make conscious lifestyle decisions about eating based on what I believe healthy means. I look for new recipes and healthy eating information on social media.",
        R.drawable.persona_2
    ),
    WELLNESS_STRIVER(
        "Wellness Striver",
        "I aspire to be healthy (but struggle sometimes). Healthy eating is hard work! I’ve tried to improve my diet, but always find things that make it difficult to stick with the changes. Sometimes I notice recipe ideas or healthy eating hacks, and if it seems easy enough, I’ll give it a go.",
        R.drawable.persona_3
    ),
    BALANCE_SEEKER(
        "Balance Seeker",
        "I try and live a balanced lifestyle, and I think that all foods are okay in moderation. I shouldn’t have to feel guilty about eating a piece of cake now and again. I get all sorts of inspiration from social media like finding out about new restaurants, fun recipes and sometimes healthy eating tips.",
        R.drawable.persona_4
    ),
    HEALTH_PROCRASTINATOR(
        "Health Procrastinator",
        "I’m contemplating healthy eating but it’s not a priority for me right now. I know the basics about what it means to be healthy, but it doesn’t seem relevant to me right now. I have taken a few steps to be healthier but I am not motivated to make it a high priority because I have too many other things going on in my life.",
        R.drawable.persona_5
    ),
    FOOD_CAREFREE(
        "Food Carefree",
        "I’m not bothered about healthy eating. I don’t really see the point and I don’t think about it. I don’t really notice healthy eating tips or recipes and I don’t care what I eat.",
        R.drawable.persona_6
    )
}