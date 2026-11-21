package com.songshawn.nutritrack.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "nutricoach_tips")
data class NutriCoachTipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val tipContent: String,
    val timestamp: Date = Date(),
    val prompt: String
)