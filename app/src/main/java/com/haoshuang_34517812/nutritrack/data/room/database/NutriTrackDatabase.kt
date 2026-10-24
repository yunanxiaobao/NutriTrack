package com.haoshuang_34517812.nutritrack.data.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 4,
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

        fun getDatabase(context: Context): NutriTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NutriTrackDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}