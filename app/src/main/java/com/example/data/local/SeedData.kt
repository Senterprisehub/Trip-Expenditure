package com.example.data.local

object SeedData {
    /**
     * Per user requirement: No pre-filled demo data is inserted on startup.
     * The database starts 100% clean so users create their own trips, travelers, deposits, and expenses.
     */
    suspend fun populateInitialDataIfNeeded(@Suppress("UNUSED_PARAMETER") dao: TripExpenditureDao) {
        // Intentionally empty: no pre-filled data.
    }
}
