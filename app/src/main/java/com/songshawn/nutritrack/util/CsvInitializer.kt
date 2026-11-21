package com.songshawn.nutritrack.util

import android.app.Application
import android.content.Context
import com.songshawn.nutritrack.data.repository.PatientRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles one-time CSV import on app launch.
 */
object CsvInitializer {
    /**
     * Initialize CSV data
     *
     * @param application Application context
     * @param repository PatientRepository instance
     * @param csvFileName Name of the CSV file in assets
     * @param forceImport Whether to force import even if already imported
     * @param onComplete Callback to be invoked when import completes
     */
    fun init(
        application: Application,
        repository: PatientRepository,
        csvFileName: String = Constants.DATASET_NAME,
        forceImport: Boolean = false,
        onComplete: ((success: Boolean) -> Unit)? = null
    ) {
        val prefs = application
            .applicationContext
            .getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        val alreadyImported = prefs.getBoolean(Constants.KEY_CSV_IMPORTED, false)

        if (!alreadyImported || forceImport) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Check if data already exists in database
                    val count = repository.getCount()

                    if (count <= 1 || forceImport) {
                        val inputStream = application.assets.open(csvFileName)
                        // Use enhanced enum parser
                        val importedCount = parseCsvWithEnhancedEnums(repository, inputStream)

                        if (importedCount > 0) {
                            prefs.edit().putBoolean(Constants.KEY_CSV_IMPORTED, true).apply()
                            withContext(Dispatchers.Main) {
                                onComplete?.invoke(true)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onComplete?.invoke(false)
                            }
                        }
                    } else {
                        // Skip import if data already exists
                        withContext(Dispatchers.Main) {
                            onComplete?.invoke(true)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false)
                    }
                }
            }
        } else {
            // Already imported, skip
            onComplete?.invoke(true)
        }
    }
}