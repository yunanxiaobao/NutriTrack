package com.haoshuang_34517812.nutritrack.util

/**
 * The course HEIFA dataset (assets/data.csv) is demo/seed data only.
 *
 * Since the Firebase account migration, consumer builds never seed it:
 * accounts are real user accounts, not rows of a course dataset. Seeding
 * stays available for debug builds (e.g. clinician demos, coursework
 * screenshots) by flipping this flag in a local debug change - it must
 * never ship enabled.
 */
object DemoDataConfig {
    val isEnabled: Boolean = false
}
