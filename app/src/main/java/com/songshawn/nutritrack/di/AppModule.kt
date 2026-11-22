package com.songshawn.nutritrack.di

import android.content.Context
import com.songshawn.nutritrack.data.room.dao.DailyScoreDao
import com.songshawn.nutritrack.data.room.dao.FoodCatalogDao
import com.songshawn.nutritrack.data.room.dao.FruitCacheDao
import com.songshawn.nutritrack.data.room.dao.MealLogDao
import com.songshawn.nutritrack.data.room.dao.NutriCoachTipDao
import com.songshawn.nutritrack.data.room.dao.PatientDao
import com.songshawn.nutritrack.data.room.dao.QuestionnaireInfoDao
import com.songshawn.nutritrack.data.room.database.NutriTrackDatabase
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
        @ApplicationContext context: Context,
    ): NutriTrackDatabase = NutriTrackDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun providePatientDao(database: NutriTrackDatabase): PatientDao = database.patientDao()

    @Provides
    @Singleton
    fun provideQuestionnaireInfoDao(database: NutriTrackDatabase): QuestionnaireInfoDao =
        database.questionnaireInfoDao()

    @Provides
    @Singleton
    fun provideNutriCoachTipDao(database: NutriTrackDatabase): NutriCoachTipDao =
        database.nutriCoachTipDao()

    @Provides
    @Singleton
    fun provideFruitCacheDao(database: NutriTrackDatabase): FruitCacheDao =
        database.fruitCacheDao()

    @Provides
    @Singleton
    fun provideFoodCatalogDao(database: NutriTrackDatabase): FoodCatalogDao =
        database.foodCatalogDao()

    @Provides
    @Singleton
    fun provideMealLogDao(database: NutriTrackDatabase): MealLogDao =
        database.mealLogDao()

    @Provides
    @Singleton
    fun provideDailyScoreDao(database: NutriTrackDatabase): DailyScoreDao =
        database.dailyScoreDao()
}
