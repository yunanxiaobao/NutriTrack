package com.songshawn.nutritrack.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Session facade backed by Firebase Authentication, with an automatic fallback
 * to the built-in demo user ([DemoDataConfig.DEMO_USER_ID]) in the `demo` product flavor.
 */
object AuthenticationManager {
    private const val PREF_NAME = Constants.SHARED_PREFS_NAME
    private const val KEY_QUESTIONNAIRE_PREFIX = Constants.KEY_QUESTIONNAIRE_PREFIX

    @Volatile
    private var currentUserId: String? = null

    @Volatile
    private var demoLoggedOutExplicitly: Boolean = false
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        val auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid
        auth.addAuthStateListener { firebaseAuth ->
            currentUserId = firebaseAuth.currentUser?.uid
        }
        if (DemoDataConfig.isEnabled) {
            setQuestionnaireCompleted(DemoDataConfig.DEMO_USER_ID)
        }
    }

    fun getCurrentUserId(): String? {
        val firebaseUid = currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid
        if (firebaseUid != null) return firebaseUid
        return if (DemoDataConfig.isEnabled && !demoLoggedOutExplicitly) {
            DemoDataConfig.DEMO_USER_ID
        } else {
            null
        }
    }

    /** Display name of the signed-in Firebase user (or the demo persona in demo builds). */
    fun getCurrentDisplayName(): String? {
        val firebaseName = FirebaseAuth.getInstance().currentUser?.displayName
        if (!firebaseName.isNullOrBlank()) return firebaseName
        return if (DemoDataConfig.isEnabled && !demoLoggedOutExplicitly) {
            DemoDataConfig.DEMO_DISPLAY_NAME
        } else {
            null
        }
    }

    fun logout(context: Context) {
        FirebaseAuth.getInstance().signOut()
        currentUserId = null
        demoLoggedOutExplicitly = true
    }

    fun hasCompletedQuestionnaire(userId: String): Boolean {
        if (DemoDataConfig.isEnabled && userId == DemoDataConfig.DEMO_USER_ID) return true
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
