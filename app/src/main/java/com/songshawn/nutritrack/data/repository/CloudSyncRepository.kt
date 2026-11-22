package com.songshawn.nutritrack.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.songshawn.nutritrack.data.models.FoodCategory
import com.songshawn.nutritrack.data.models.Persona
import com.songshawn.nutritrack.data.room.dao.NutriCoachTipDao
import com.songshawn.nutritrack.data.room.dao.QuestionnaireInfoDao
import com.songshawn.nutritrack.data.room.dao.SyncOutboxDao
import com.songshawn.nutritrack.data.room.entity.NutriCoachTipEntity
import com.songshawn.nutritrack.data.room.entity.QuestionnaireInfoEntity
import com.songshawn.nutritrack.data.room.entity.SyncOutboxEntity
import com.songshawn.nutritrack.util.AuthenticationManager
import com.songshawn.nutritrack.util.DemoDataConfig
import com.songshawn.nutritrack.worker.OutboxSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reliable Firestore synchronization using the Transactional Outbox pattern (`sync_outbox` Room table)
 * paired with [OutboxSyncWorker] (WorkManager exponential backoff) and deterministic UUID document IDs.
 *
 * Local writes are persisted into `sync_outbox` first so offline or transient network failures never
 * silently drop user data, and deterministic UUIDs guarantee idempotent retries without duplicate docs.
 */
@Singleton
class CloudSyncRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val firestore: FirebaseFirestore,
    private val questionnaireDao: QuestionnaireInfoDao,
    private val tipDao: NutriCoachTipDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val gson: Gson,
) {
    private val drainMutex = Mutex()

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    /**
     * Stages a questionnaire update in the durable outbox and schedules [OutboxSyncWorker].
     */
    suspend fun pushQuestionnaire(info: QuestionnaireInfoEntity) {
        val uid = info.patientId
        if (DemoDataConfig.isEnabled && uid == DemoDataConfig.DEMO_USER_ID) return

        val docUuid = SyncOutboxEntity.deterministicUuid("questionnaire:$uid")
        val payload = mapOf(
            "biggestMealTime" to info.biggestMealTime,
            "sleepTime" to info.sleepTime,
            "wakeTime" to info.wakeTime,
            "persona" to info.persona.name,
            "selectedCategories" to info.selectedCategories.map { it.name },
        )
        syncOutboxDao.upsertOutboxItem(
            SyncOutboxEntity(
                documentUuid = docUuid,
                userId = uid,
                entityType = ENTITY_QUESTIONNAIRE,
                firestoreDocumentPath = "users/$uid/questionnaire/current",
                payloadJson = gson.toJson(payload),
            ),
        )
        enqueueAndAttemptImmediateDrain()
    }

    /**
     * Stages a saved NutriCoach tip in the durable outbox using a deterministic UUID document ID
     * (`users/{uid}/tips/{uuid}`) so retries are 100% idempotent.
     */
    suspend fun pushTip(tip: NutriCoachTipEntity) {
        val uid = tip.userId
        if (DemoDataConfig.isEnabled && uid == DemoDataConfig.DEMO_USER_ID) return

        val docUuid = SyncOutboxEntity.deterministicUuid(
            "tip:$uid:${tip.timestamp.time}:${tip.tipContent.hashCode()}",
        )
        val payload = mapOf(
            "documentUuid" to docUuid,
            "tipContent" to tip.tipContent,
            "prompt" to tip.prompt,
            "timestampMillis" to tip.timestamp.time,
        )
        syncOutboxDao.upsertOutboxItem(
            SyncOutboxEntity(
                documentUuid = docUuid,
                userId = uid,
                entityType = ENTITY_TIP,
                firestoreDocumentPath = "users/$uid/tips/$docUuid",
                payloadJson = gson.toJson(payload),
            ),
        )
        enqueueAndAttemptImmediateDrain()
    }

    /**
     * Drains pending entries from `sync_outbox` to Firestore.
     *
     * @return `true` if all pending items in the batch succeeded, `false` if any failed and should
     *         be retried by WorkManager with exponential backoff.
     */
    suspend fun drainOutboxBatch(): Boolean = drainMutex.withLock {
        val batch = syncOutboxDao.getPendingBatch()
        if (batch.isEmpty()) return@withLock true

        var allSucceeded = true
        val mapType = object : TypeToken<Map<String, Any>>() {}.type

        for (entry in batch) {
            try {
                val dataMap: MutableMap<String, Any> = gson.fromJson<Map<String, Any>>(
                    entry.payloadJson,
                    mapType,
                ).toMutableMap()
                dataMap["updatedAt"] = FieldValue.serverTimestamp()

                firestore.document(entry.firestoreDocumentPath).set(dataMap).await()
                if (entry.entityType == ENTITY_QUESTIONNAIRE) {
                    userDoc(entry.userId).set(mapOf("onboardingDone" to true), SetOptions.merge()).await()
                }
                syncOutboxDao.deleteByUuid(entry.documentUuid)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                allSucceeded = false
                syncOutboxDao.recordFailedAttempt(entry.documentUuid, e.message)
                Log.w(TAG, "Outbox sync failed for ${entry.documentUuid} (attempt ${entry.attemptCount + 1})", e)
            }
        }
        allSucceeded
    }

    /** Pulls questionnaire + tips for [uid] down into Room and flushes any pending outbox items. */
    suspend fun syncUserData(uid: String) {
        if (DemoDataConfig.isEnabled && uid == DemoDataConfig.DEMO_USER_ID) return
        drainOutboxBatch()
        pullQuestionnaire(uid)
        pullTips(uid)
    }

    private suspend fun enqueueAndAttemptImmediateDrain() {
        runCatching { OutboxSyncWorker.enqueue(appContext) }
        drainOutboxBatch()
    }

    private suspend fun pullQuestionnaire(uid: String) {
        try {
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
                    selectedCategories = categories,
                ),
            )
            AuthenticationManager.setQuestionnaireCompleted(uid)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "pullQuestionnaire failed", e)
        }
    }

    private suspend fun pullTips(uid: String) {
        try {
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
                        prompt = doc.getString("prompt") ?: "",
                    ),
                )
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "pullTips failed", e)
        }
    }

    companion object {
        private const val TAG = "CloudSync"
        const val ENTITY_QUESTIONNAIRE = "QUESTIONNAIRE"
        const val ENTITY_TIP = "TIP"
    }
}
