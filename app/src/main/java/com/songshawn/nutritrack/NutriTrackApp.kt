package com.songshawn.nutritrack

import android.app.Application
import android.content.Context
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.di.ApplicationScope
import com.songshawn.nutritrack.util.AuthenticationManager
import com.songshawn.nutritrack.util.Constants
import com.songshawn.nutritrack.util.CsvInitializer
import com.songshawn.nutritrack.util.DemoDataConfig
import com.songshawn.nutritrack.util.DemoPersonaSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NutriTrackApp : Application() {

    @Inject
    lateinit var patientRepository: PatientRepository

    @Inject
    lateinit var demoPersonaSeeder: DemoPersonaSeeder

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    companion object {
        private const val PREFS_NAME = Constants.SHARED_PREFS_NAME

        private fun isDataInitialized(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(Constants.KEY_DATA_INITIALIZED, false)
        }

        private fun setDataInitialized(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(Constants.KEY_DATA_INITIALIZED, true).apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        AuthenticationManager.init(this)
        initializeDataIfNeeded()
    }

    private fun initializeDataIfNeeded() {
        applicationScope.launch {
            demoPersonaSeeder.seedIfNeeded()
        }

        if (!DemoDataConfig.isEnabled) return
        if (!isDataInitialized(applicationContext)) {
            CsvInitializer.init(
                application = this,
                repository = patientRepository,
                scope = applicationScope,
                csvFileName = Constants.DATASET_NAME,
                forceImport = false,
                onComplete = { success ->
                    if (success) {
                        setDataInitialized(applicationContext)
                    }
                },
            )
        }
    }
}
