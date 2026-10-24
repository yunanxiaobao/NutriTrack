package com.haoshuang_34517812.nutritrack.di

import android.content.Context
import com.haoshuang_34517812.nutritrack.data.room.dao.FruitCacheDao
import com.haoshuang_34517812.nutritrack.data.room.dao.NutriCoachTipDao
import com.haoshuang_34517812.nutritrack.data.room.dao.PatientDao
import com.haoshuang_34517812.nutritrack.data.room.dao.QuestionnaireInfoDao
import com.haoshuang_34517812.nutritrack.data.room.database.NutriTrackDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNutriTrackDatabase(
        @ApplicationContext context: Context
    ): NutriTrackDatabase {
        return NutriTrackDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun providePatientDao(database: NutriTrackDatabase): PatientDao {
        return database.patientDao()
    }

    @Provides
    @Singleton
    fun provideQuestionnaireInfoDao(database: NutriTrackDatabase): QuestionnaireInfoDao {
        return database.questionnaireInfoDao()
    }

    @Provides
    @Singleton
    fun provideNutriCoachTipDao(database: NutriTrackDatabase): NutriCoachTipDao {
        return database.nutriCoachTipDao()
    }

    @Provides
    @Singleton
    fun provideFruitCacheDao(database: NutriTrackDatabase): FruitCacheDao {
        return database.fruitCacheDao()
    }
}
