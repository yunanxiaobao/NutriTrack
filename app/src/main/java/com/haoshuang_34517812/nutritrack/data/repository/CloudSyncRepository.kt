package com.haoshuang_34517812.nutritrack.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.haoshuang_34517812.nutritrack.data.models.FoodCategory
import com.haoshuang_34517812.nutritrack.data.models.Persona
import com.haoshuang_34517812.nutritrack.data.room.dao.NutriCoachTipDao
import com.haoshuang_34517812.nutritrack.data.room.dao.QuestionnaireInfoDao
import com.haoshuang_34517812.nutritrack.data.room.entity.NutriCoachTipEntity
import com.haoshuang_34517812.nutritrack.data.room.entity.QuestionnaireInfoEntity
import com.haoshuang_34517812.nutritrack.util.AuthenticationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore sync for per-user data (profile flags, questionnaire answers and
 * NutriCoach history), following the account design:
 *
 *   users/{uid}                            profile + onboardingDone
 *   users/{uid}/questionnaire/current      questionnaire answers
 *   users/{uid}/tips/{timestamp_roomId}    NutriCoach tips
 *
 * Room stays the on-device source the UI reads; this repository mirrors
 * writes to Firestore and pulls remote data down on sign-in / home load so
 * a fresh device restores the user's answers and history. All operations
 * are best-effort: failures are logged and never break local behaviour.
 * Firestore's own offline persistence queues writes while offline.
 */
@Singleton
class CloudSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val questionnaireDao: QuestionnaireInfoDao,
    private val tipDao: NutriCoachTipDao
) {
    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    /** Mirrors a locally saved questionnaire to Firestore. */
    suspend fun pushQuestionnaire(info: QuestionnaireInfoEntity) {
        runCatching {
            val uid = info.patientId
            userDoc(uid).collection("questionnaire").document("current")
                .set(
                    mapOf(
                        "biggestMealTime" to info.biggestMealTime,
                        "sleepTime" to info.sleepTime,
                        "wakeTime" to info.wakeTime,
                        "persona" to info.persona.name,
                        "selectedCategories" to info.selectedCategories.map { it.name },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            userDoc(uid).set(mapOf("onboardingDone" to true), SetOptions.merge()).await()
        }.onFailure { Log.w(TAG, "pushQuestionnaire failed", it) }
    }

    /** Mirrors a locally saved NutriCoach tip to Firestore. */
    suspend fun pushTip(tip: NutriCoachTipEntity) {
        runCatching {
            val docId = "${tip.timestamp.time}_${tip.id}"
            userDoc(tip.userId).collection("tips").document(docId)
                .set(
                    mapOf(
                        "tipContent" to tip.tipContent,
                        "prompt" to tip.prompt,
                        "timestampMillis" to tip.timestamp.time,
                        "roomId" to tip.id
                    )
                ).await()
        }.onFailure { Log.w(TAG, "pushTip failed", it) }
    }

    /** Pulls questionnaire + tips for [uid] down into Room. Safe to call repeatedly. */
    suspend fun syncUserData(uid: String) {
        pullQuestionnaire(uid)
        pullTips(uid)
    }

    private suspend fun pullQuestionnaire(uid: String) {
        runCatching {
            if (questionnaireDao.getInfoByUser(uid) != null) return
            val snapshot = userDoc(uid).collection("questionnaire")
                .document("current").get().await()
            if (!snapshot.exists()) return
            val persona = runCatching {
                Persona.valueOf(snapshot.getString("persona") ?: "")
            }.getOrNull() ?: return
            val categories = (snapshot.get("selectedCategories") as? List<*>)
                ?.mapNotNull { name -> runCatching { FoodCategory.valueOf(name as String) }.getOrNull() }
                ?: emptyList()
            questionnaireDao.insertInfo(
                QuestionnaireInfoEntity(
                    patientId = uid,
                    biggestMealTime = snapshot.getString("biggestMealTime") ?: "",
                    sleepTime = snapshot.getString("sleepTime") ?: "",
                    wakeTime = snapshot.getString("wakeTime") ?: "",
                    persona = persona,
                    selectedCategories = categories
                )
            )
            AuthenticationManager.setQuestionnaireCompleted(uid)
        }.onFailure { Log.w(TAG, "pullQuestionnaire failed", it) }
    }

    private suspend fun pullTips(uid: String) {
        runCatching {
            val snapshot = userDoc(uid).collection("tips").get().await()
            if (snapshot.isEmpty) return
            val localKeys = tipDao.getTipsByUserId(uid).first()
                .map { it.timestamp.time to it.tipContent }
                .toHashSet()
            for (doc in snapshot.documents) {
                val millis = doc.getLong("timestampMillis") ?: continue
                val content = doc.getString("tipContent") ?: continue
                if ((millis to content) in localKeys) continue
                tipDao.insertTip(
                    NutriCoachTipEntity(
                        userId = uid,
                        tipContent = content,
                        timestamp = Date(millis),
                        prompt = doc.getString("prompt") ?: ""
                    )
                )
            }
        }.onFailure { Log.w(TAG, "pullTips failed", it) }
    }

    companion object {
        private const val TAG = "CloudSync"
    }
}
