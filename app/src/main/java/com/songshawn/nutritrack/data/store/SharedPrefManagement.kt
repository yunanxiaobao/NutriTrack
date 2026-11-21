package com.songshawn.nutritrack.data.store

import android.content.Context
import android.content.SharedPreferences
import com.songshawn.nutritrack.util.Constants
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Management of application preferences.
 */
@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = Constants.SHARED_PREFS_NAME
        private const val KEY_ADMIN_PASSWORD = Constants.KEY_ADMIN_PASSWORD

        private const val DEFAULT_ADMIN_PASSWORD = Constants.DEFAULT_ADMIN_PASSWORD
    }

    /**
     * Get the current admin password.
     * If not set, set the default password and return it.
     */
    private fun getAdminPassword(): String {
        val storedPassword = sharedPreferences.getString(KEY_ADMIN_PASSWORD, null)

        if (storedPassword == null) {
            setAdminPassword(DEFAULT_ADMIN_PASSWORD)
            return DEFAULT_ADMIN_PASSWORD
        }

        return storedPassword
    }

    /**
     * Set a new admin password.
     */
    private fun setAdminPassword(newPassword: String) {
        sharedPreferences.edit().putString(KEY_ADMIN_PASSWORD, newPassword).apply()
    }

    /**
     * Check if the provided password matches the stored admin password.
     */
    fun verifyAdminPassword(inputPassword: String): Boolean {
        return inputPassword == getAdminPassword()
    }
}