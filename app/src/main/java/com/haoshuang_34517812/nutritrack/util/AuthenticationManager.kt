package com.haoshuang_34517812.nutritrack.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Session facade backed by Firebase Authentication.
 *
 * The rest of the app historically identified the signed-in user by a plain
 * String id; that contract is kept, but the id is now the Firebase UID and
 * the session itself (persistence, token refresh, sign-out) is owned by the
 * Firebase SDK. Per-user questionnaire completion flags remain in shared
 * preferences, keyed by UID.
 */
object AuthenticationManager {
    private const val PREF_NAME = Constants.SHARED_PREFS_NAME
    private const val KEY_QUESTIONNAIRE_PREFIX = Constants.KEY_QUESTIONNAIRE_PREFIX

    @Volatile
    private var currentUserId: String? = null
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        val auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid
        auth.addAuthStateListener { firebaseAuth ->
            currentUserId = firebaseAuth.currentUser?.uid
        }
    }

    fun getCurrentUserId(): String? =
        currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid

    fun logout(context: Context) {
        FirebaseAuth.getInstance().signOut()
        currentUserId = null
    }

    fun hasCompletedQuestionnaire(userId: String): Boolean {
        val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "$KEY_QUESTIONNAIRE_PREFIX$userId"
        return prefs.getBoolean(key, false)
    }

    fun setQuestionnaireCompleted(userId: String) {
        val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "$KEY_QUESTIONNAIRE_PREFIX$userId"
        prefs.edit().putBoolean(key, true).apply()
    }
}
