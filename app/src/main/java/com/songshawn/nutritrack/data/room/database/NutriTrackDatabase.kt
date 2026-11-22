package com.songshawn.nutritrack.data.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.songshawn.nutritrack.data.room.dao.AgentMemoryDao
import com.songshawn.nutritrack.data.room.dao.DailyScoreDao
import com.songshawn.nutritrack.data.room.dao.FoodCatalogDao
import com.songshawn.nutritrack.data.room.dao.FruitCacheDao
import com.songshawn.nutritrack.data.room.dao.MealLogDao
import com.songshawn.nutritrack.data.room.dao.NutriCoachTipDao
import com.songshawn.nutritrack.data.room.dao.PatientDao
import com.songshawn.nutritrack.data.room.dao.QuestionnaireInfoDao
import com.songshawn.nutritrack.data.room.dao.SyncOutboxDao
import com.songshawn.nutritrack.data.room.entity.AgentMessageEntity
import com.songshawn.nutritrack.data.room.entity.AgentSessionSummaryEntity
import com.songshawn.nutritrack.data.room.entity.DailyScoreEntity
import com.songshawn.nutritrack.data.room.entity.FoodItemEntity
import com.songshawn.nutritrack.data.room.entity.FoodItemFtsEntity
import com.songshawn.nutritrack.data.room.entity.FruitCacheEntity
import com.songshawn.nutritrack.data.room.entity.MealLogEntity
import com.songshawn.nutritrack.data.room.entity.MealLogItemEntity
import com.songshawn.nutritrack.data.room.entity.NutriCoachTipEntity
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import com.songshawn.nutritrack.data.room.entity.QuestionnaireInfoEntity
import com.songshawn.nutritrack.data.room.entity.SyncOutboxEntity
import com.songshawn.nutritrack.util.Constants
import com.songshawn.nutritrack.util.Converters

@Database(
    entities = [
        PatientEntity::class,
        QuestionnaireInfoEntity::class,
        NutriCoachTipEntity::class,
        FruitCacheEntity::class,
        FoodItemEntity::class,
        FoodItemFtsEntity::class,
        MealLogEntity::class,
        MealLogItemEntity::class,
        DailyScoreEntity::class,
        AgentMessageEntity::class,
        AgentSessionSummaryEntity::class,
        SyncOutboxEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NutriTrackDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun questionnaireInfoDao(): QuestionnaireInfoDao
    abstract fun nutriCoachTipDao(): NutriCoachTipDao
    abstract fun fruitCacheDao(): FruitCacheDao
    abstract fun foodCatalogDao(): FoodCatalogDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun dailyScoreDao(): DailyScoreDao
    abstract fun agentMemoryDao(): AgentMemoryDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        @Volatile
        private var INSTANCE: NutriTrackDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
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
                    """.trimIndent(),
                )
            }
        }

        /**
         * Migration 6 -> 7: Adds on-device food catalog (`food_items` + `food_items_fts`),
         * relational meal logging (`meal_logs` + `meal_log_items`), and computed daily
         * HEIFA scores (`daily_scores`).
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createFoodCatalogTables(db)
                createMealLogTables(db)
                createDailyScoresTable(db)
            }
        }

        private fun createFoodCatalogTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `food_items` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `subCategory` TEXT NOT NULL,
                    `servingSizeDesc` TEXT NOT NULL,
                    `servingGrams` REAL NOT NULL,
                    `caloriesKcal` REAL NOT NULL,
                    `proteinG` REAL NOT NULL,
                    `fatG` REAL NOT NULL,
                    `saturatedFatG` REAL NOT NULL,
                    `unsaturatedFatG` REAL NOT NULL,
                    `carbsG` REAL NOT NULL,
                    `sugarG` REAL NOT NULL,
                    `addedSugarG` REAL NOT NULL,
                    `fiberG` REAL NOT NULL,
                    `sodiumMg` REAL NOT NULL,
                    `waterMl` REAL NOT NULL,
                    `alcoholStandardDrinks` REAL NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_category` ON `food_items` (`category`)")
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `food_items_fts` USING FTS4(" +
                    "`name` TEXT NOT NULL, `subCategory` TEXT NOT NULL, `servingSizeDesc` TEXT NOT NULL, " +
                    "content=`food_items`)",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_food_items_fts_BEFORE_UPDATE " +
                    "BEFORE UPDATE ON `food_items` BEGIN " +
                    "DELETE FROM `food_items_fts` WHERE `docid`=OLD.`rowid`; END",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_food_items_fts_BEFORE_DELETE " +
                    "BEFORE DELETE ON `food_items` BEGIN " +
                    "DELETE FROM `food_items_fts` WHERE `docid`=OLD.`rowid`; END",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_food_items_fts_AFTER_UPDATE " +
                    "AFTER UPDATE ON `food_items` BEGIN " +
                    "INSERT INTO `food_items_fts`(`docid`, `name`, `subCategory`, `servingSizeDesc`) " +
                    "VALUES (NEW.`rowid`, NEW.`name`, NEW.`subCategory`, NEW.`servingSizeDesc`); END",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_food_items_fts_AFTER_INSERT " +
                    "AFTER INSERT ON `food_items` BEGIN " +
                    "INSERT INTO `food_items_fts`(`docid`, `name`, `subCategory`, `servingSizeDesc`) " +
                    "VALUES (NEW.`rowid`, NEW.`name`, NEW.`subCategory`, NEW.`servingSizeDesc`); END",
            )
        }

        private fun createMealLogTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `meal_logs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `userId` TEXT NOT NULL,
                    `date` TEXT NOT NULL,
                    `mealType` TEXT NOT NULL,
                    `loggedAt` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_logs_userId_date` ON `meal_logs` (`userId`, `date`)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_meal_logs_userId_date_mealType` " +
                    "ON `meal_logs` (`userId`, `date`, `mealType`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `meal_log_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `mealLogId` INTEGER NOT NULL,
                    `foodId` TEXT NOT NULL,
                    `servings` REAL NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`mealLogId`) REFERENCES `meal_logs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_log_items_mealLogId` ON `meal_log_items` (`mealLogId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_log_items_foodId` ON `meal_log_items` (`foodId`)")
        }

        private fun createDailyScoresTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `daily_scores` (
                    `userId` TEXT NOT NULL,
                    `date` TEXT NOT NULL,
                    `heifaTotalScore` REAL NOT NULL,
                    `vegetableScore` REAL NOT NULL,
                    `fruitScore` REAL NOT NULL,
                    `grainsScore` REAL NOT NULL,
                    `wholegrainsScore` REAL NOT NULL,
                    `meatAndAlternativeScore` REAL NOT NULL,
                    `dairyScore` REAL NOT NULL,
                    `waterScore` REAL NOT NULL,
                    `saturatedFatScore` REAL NOT NULL,
                    `unsaturatedFatScore` REAL NOT NULL,
                    `sodiumScore` REAL NOT NULL,
                    `sugarScore` REAL NOT NULL,
                    `alcoholScore` REAL NOT NULL,
                    `discretionaryScore` REAL NOT NULL,
                    `totalCaloriesKcal` REAL NOT NULL,
                    `totalProteinG` REAL NOT NULL,
                    `totalCarbsG` REAL NOT NULL,
                    `totalFatG` REAL NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`userId`, `date`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_scores_date` ON `daily_scores` (`date`)")
        }

        /**
         * Migration 7 -> 8: Adds persistent NutriCoach agent conversation history (`agent_messages`)
         * and rolling session summaries (`agent_session_summaries`).
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agent_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `traceJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_agent_messages_userId_createdAt` " +
                        "ON `agent_messages` (`userId`, `createdAt`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agent_session_summaries` (
                        `userId` TEXT NOT NULL,
                        `summaryText` TEXT NOT NULL,
                        `summarizedMessageCount` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Migration 8 -> 9: Adds durable transactional outbox (`sync_outbox`) for reliable
         * WorkManager background Firestore synchronization with UUID idempotency.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_outbox` (
                        `documentUuid` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `firestoreDocumentPath` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `attemptCount` INTEGER NOT NULL,
                        `lastError` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`documentUuid`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sync_outbox_userId_createdAt` " +
                        "ON `sync_outbox` (`userId`, `createdAt`)",
                )
            }
        }

        fun getDatabase(context: Context): NutriTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NutriTrackDatabase::class.java,
                    Constants.DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
