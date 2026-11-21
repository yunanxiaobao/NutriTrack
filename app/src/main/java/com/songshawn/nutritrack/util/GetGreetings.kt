package com.songshawn.nutritrack.util


fun getGreetings(hourOfDay: Int): String {
    return when {
        hourOfDay < 12 -> "Good morning"
        hourOfDay < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}
