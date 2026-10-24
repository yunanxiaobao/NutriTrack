package com.haoshuang_34517812.nutritrack.data.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.haoshuang_34517812.nutritrack.data.room.dao.FruitCacheDao
import com.haoshuang_34517812.nutritrack.data.room.dao.NutriCoachTipDao
import com.haoshuang_34517812.nutritrack.data.room.dao.PatientDao
import com.haoshuang_34517812.nutritrack.data.room.dao.QuestionnaireInfoDao
import com.haoshuang_34517812.nutritrack.data.room.entity.FruitCacheEntity
import com.haoshuang_34517812.nutritrack.data.room.entity.NutriCoachTipEntity
import com.haoshuang_34517812.nutritrack.data.room.entity.PatientEntity
import com.haoshuang_34517812.nutritrack.data.room.entity.QuestionnaireInfoEntity
import com.haoshuang_34517812.nutritrack.util.Constants
import com.haoshuang_34517812.nutritrack.util.Converters

@Database(
    entities = [
        PatientEntity::class,
        QuestionnaireInfoEntity::class,
        NutriCoachTipEntity::class,
        FruitCacheEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NutriTrackDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun questionnaireInfoDao(): QuestionnaireInfoDao
    abstract fun nutriCoachTipDao(): NutriCoachTipDao
    abstract fun fruitCacheDao(): FruitCacheDao

    companion object {
        @Volatile
        private var INSTANCE: NutriTrackDatabase? = null

        // Migration from version 3 to 4: Add fruit_cache table
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS fruit_cache (
                        name TEXT NOT NULL PRIMARY KEY,
                        family TEXT NOT NULL,
                        `order` TEXT NOT NULL,
                        genus TEXT NOT NULL,
                        carbohydrates REAL NOT NULL,
                        protein REAL NOT NULL,
                        fat REAL NOT NULL,
                        calories REAL NOT NULL,
                        sugar REAL NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): NutriTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NutriTrackDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}