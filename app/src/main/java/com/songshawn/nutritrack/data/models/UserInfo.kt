package com.songshawn.nutritrack.data.models
/**
 * UserInfo merges CSV-based ClinicianData with the user's Questionnaire selections.
 */
data class UserInfo(
    val name: String,
    val phoneNumber: String,
    val userId: String,
    val sex: String
)