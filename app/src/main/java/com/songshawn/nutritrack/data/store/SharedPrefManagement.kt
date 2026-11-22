package com.songshawn.nutritrack.data.store

import com.songshawn.nutritrack.util.DemoDataConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages application-level access checks.
 *
 * The legacy hardcoded plaintext clinician password has been removed: the local
 * Clinician cohort dashboard is now gated exclusively to the `demo` product flavor
 * ([DemoDataConfig.isEnabled]).
 */
@Singleton
class PreferencesManager @Inject constructor() {

    fun isClinicianDemoAccessAllowed(): Boolean = DemoDataConfig.isEnabled

    fun verifyAdminPassword(inputPassword: String): Boolean =
        DemoDataConfig.isEnabled && inputPassword.isNotBlank()
}
