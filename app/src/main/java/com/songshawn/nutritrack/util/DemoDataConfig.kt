package com.songshawn.nutritrack.util

import com.songshawn.nutritrack.BuildConfig

/**
 * Controls demo dataset and persona seeding.
 *
 * Automatically enabled in the `demo` product flavor (`BuildConfig.IS_DEMO_MODE == true`)
 * so interview and offline demo builds start with a signed-in demo user, 30 days of meal
 * logs and HEIFA score trends, and simulated LLM responses when no API key is present.
 */
object DemoDataConfig {
    const val DEMO_USER_ID = "demo_alex_rivera"
    const val DEMO_DISPLAY_NAME = "Alex Rivera"
    const val DEMO_EMAIL = "alex.rivera@demo.nutritrack.app"

    val isEnabled: Boolean get() = BuildConfig.IS_DEMO_MODE
}
