package com.haoshuang_34517812.nutritrack.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import com.haoshuang_34517812.nutritrack.data.models.FoodCategory
import com.haoshuang_34517812.nutritrack.data.models.Persona


// No foreign key to the legacy patient table: since the Firebase account
// migration, patientId holds a Firebase UID, and questionnaire answers must
// exist independently of any course-dataset row.
@Entity(
    tableName = "food_intake",
    primaryKeys = ["patientId"],
    indices = [ Index("patientId") ]
)

data class QuestionnaireInfoEntity(
    val patientId: String,
    val biggestMealTime: String,
    val sleepTime: String,
    val wakeTime: String,
    val persona: Persona,
    val selectedCategories: List<FoodCategory>
)