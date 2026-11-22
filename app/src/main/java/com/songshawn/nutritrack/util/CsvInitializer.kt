package com.songshawn.nutritrack.util

import android.app.Application
import android.content.Context
import com.songshawn.nutritrack.data.repository.PatientRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Handles one-time CSV import bound to an explicit application [CoroutineScope]
 * and serialized by a [Mutex] to prevent duplicate concurrent imports.
 */
object CsvInitializer {
    private val importMutex = Mutex()

    fun init(
        application: Application,
        repository: PatientRepository,
        scope: CoroutineScope,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
        csvFileName: String = Constants.DATASET_NAME,
        forceImport: Boolean = false,
        onComplete: ((success: Boolean) -> Unit)? = null,
    ): Job = scope.launch(ioDispatcher) {
        importMutex.withLock {
            val prefs = application
                .applicationContext
                .getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val alreadyImported = prefs.getBoolean(Constants.KEY_CSV_IMPORTED, false)

            if (alreadyImported && !forceImport) {
                withContext(mainDispatcher) { onComplete?.invoke(true) }
                return@withLock
            }

            try {
                val count = repository.getCount()
                if (count <= 1 || forceImport) {
                    val importedCount = application.assets.open(csvFileName).use { inputStream ->
                        parseCsvWithEnhancedEnums(repository, inputStream)
                    }
                    val ok = importedCount > 0
                    if (ok) {
                        prefs.edit().putBoolean(Constants.KEY_CSV_IMPORTED, true).apply()
                    }
                    withContext(mainDispatcher) { onComplete?.invoke(ok) }
                } else {
                    withContext(mainDispatcher) { onComplete?.invoke(true) }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                withContext(mainDispatcher) { onComplete?.invoke(false) }
            }
        }
    }
}
