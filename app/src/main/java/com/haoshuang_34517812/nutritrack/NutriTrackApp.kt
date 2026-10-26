package com.haoshuang_34517812.nutritrack

import android.app.Application
import android.content.Context
import com.haoshuang_34517812.nutritrack.data.repository.PatientRepository
import com.haoshuang_34517812.nutritrack.util.AuthenticationManager
import com.haoshuang_34517812.nutritrack.util.Constants
import com.haoshuang_34517812.nutritrack.util.DemoDataConfig
import com.haoshuang_34517812.nutritrack.util.CsvInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NutriTrackApp : Application() {

    @Inject
    lateinit var patientRepository: PatientRepository

    companion object {
        private const val PREFS_NAME = Constants.SHARED_PREFS_NAME

        // Check if data is already initialized
        private fun isDataInitialized(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val initialized = prefs.getBoolean(Constants.KEY_DATA_INITIALIZED, false)
            return initialized
        }

        // Mark data as initialized
        private fun setDataInitialized(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(Constants.KEY_DATA_INITIALIZED, true).apply()
        }
    }

    override fun onCreate() {
        super.onCreate()

        AuthenticationManager.init(this)

        // Initialize CSV data only if not already done
        initializeDataIfNeeded()
    }

    private fun initializeDataIfNeeded() {
        // Consumer builds never seed the course dataset: accounts are real
        // Firebase accounts now. Demo seeding is debug-only, see DemoDataConfig.
        if (!BuildConfig.DEBUG || !DemoDataConfig.isEnabled) return
        if (!isDataInitialized(applicationContext)) {

            CsvInitializer.init(
                application = this,
                repository = patientRepository,
                csvFileName = Constants.DATASET_NAME,
                forceImport = false,
                onComplete = { success ->
                    if (success) {
                        setDataInitialized(applicationContext)
                    }
                }
            )
        }
    }
}