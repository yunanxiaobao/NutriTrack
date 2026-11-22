package com.songshawn.nutritrack.data.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.songshawn.nutritrack.data.room.database.NutriTrackDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration6To7Test {

    private val testDbName = "migration_test_nutritrack.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NutriTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate6To7_preservesExistingPatientsAndCreatesFoodLoggingTables() {
        // 1. Create database at version 6 and seed a patient row
        helper.createDatabase(testDbName, 6).apply {
            execSQL(
                """
                INSERT INTO patient (
                    userId, phoneNumber, name, passwordHash, sex,
                    heifaTotalScore, vegetableScore, fruitScore, grainsScore, wholegrainsScore,
                    meatAndAlternativeScore, dairyScore, waterScore, saturatedFatScore,
                    unsaturatedFatScore, sodiumScore, sugarScore, alcoholScore,
                    discretionaryScore, vegetablesVariantionsScore, fruitVariantionsScore, fruitServeSize
                ) VALUES (
                    'u_legacy', '0400111222', 'Alice', '', 'FEMALE',
                    78.5, 8.0, 9.0, 4.0, 4.0,
                    8.5, 8.0, 4.5, 4.0,
                    4.0, 8.0, 7.5, 5.0,
                    4.0, 4.0, 4.5, 2.0
                )
                """.trimIndent(),
            )
            close()
        }

        // 2. Run MIGRATION_6_7 and validate against exported 7.json schema
        val migratedDb = helper.runMigrationsAndValidate(
            testDbName,
            7,
            true,
            NutriTrackDatabase.MIGRATION_6_7,
        )

        // 3. Verify legacy patient data is intact
        migratedDb.query("SELECT name, heifaTotalScore FROM patient WHERE userId = 'u_legacy'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Alice", cursor.getString(0))
            assertEquals(78.5, cursor.getDouble(1), 0.01)
        }

        // 4. Verify new tables and FTS4 trigger work
        migratedDb.execSQL(
            """
            INSERT INTO food_items (
                id, name, category, subCategory, servingSizeDesc, servingGrams,
                caloriesKcal, proteinG, fatG, saturatedFatG, unsaturatedFatG,
                carbsG, sugarG, addedSugarG, fiberG, sodiumMg, waterMl, alcoholStandardDrinks
            ) VALUES (
                'veg_spinach', 'Baby Spinach', 'VEGETABLES', 'dark_green', '1 cup (60g)', 60.0,
                14.0, 1.7, 0.2, 0.03, 0.1,
                1.0, 0.3, 0.0, 1.3, 45.0, 54.0, 0.0
            )
            """.trimIndent(),
        )
        migratedDb.query("SELECT name FROM food_items_fts WHERE food_items_fts MATCH 'spin*'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Baby Spinach", cursor.getString(0))
        }
        migratedDb.close()
    }

    @Test
    fun migrate7To8_createsAgentMemoryAndSummaryTables() {
        helper.createDatabase(testDbName, 7).close()
        val migratedDb = helper.runMigrationsAndValidate(
            testDbName,
            8,
            true,
            NutriTrackDatabase.MIGRATION_7_8,
        )
        migratedDb.execSQL(
            """
            INSERT INTO agent_messages (userId, role, content, traceJson, createdAt)
            VALUES ('u_legacy', 'USER', 'How is my score?', '[]', 1700000000)
            """.trimIndent(),
        )
        migratedDb.query("SELECT content FROM agent_messages WHERE userId = 'u_legacy'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("How is my score?", cursor.getString(0))
        }
        migratedDb.close()
    }

    @Test
    fun migrate8To9_createsSyncOutboxTable() {
        helper.createDatabase(testDbName, 8).close()
        val migratedDb = helper.runMigrationsAndValidate(
            testDbName,
            9,
            true,
            NutriTrackDatabase.MIGRATION_8_9,
        )
        migratedDb.execSQL(
            """
            INSERT INTO sync_outbox (
                documentUuid, userId, entityType, firestoreDocumentPath,
                payloadJson, attemptCount, lastError, createdAt
            ) VALUES (
                '550e8400-e29b-41d4-a716-446655440000', 'u_legacy', 'TIP',
                'users/u_legacy/tips/550e8400-e29b-41d4-a716-446655440000',
                '{"tipContent":"Eat more greens"}', 0, NULL, 1700000000
            )
            """.trimIndent(),
        )
        migratedDb.query("SELECT entityType FROM sync_outbox WHERE userId = 'u_legacy'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("TIP", cursor.getString(0))
        }
        migratedDb.close()
    }
}
