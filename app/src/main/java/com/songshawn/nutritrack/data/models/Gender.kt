package com.songshawn.nutritrack.data.models

enum class Gender(val code: Int) {
    MALE(0),
    FEMALE(1);

    companion object {
        /**
         * Converts a string (ignoring case and extra spaces) into a Gender enum.
         * Defaults to MALE if the input is unrecognized.
         */
        fun fromString(value: String): Gender {
            return when (value.trim().lowercase()) {
                "male" -> MALE
                "female" -> FEMALE
                else -> MALE
            }
        }
    }
}