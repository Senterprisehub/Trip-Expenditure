package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class StopCategory(val label: String, val badgeEmoji: String) {
    SIGHTSEEING("Sightseeing", "⛩️"),
    DINING("Food & Dining", "🍜"),
    LODGING("Hotel & Stay", "🏨"),
    TRANSIT("Transit & Flight", "🚄"),
    ACTIVITY("Adventure", "🎿")
}

enum class ExpenseCategory(val label: String, val badgeEmoji: String) {
    DINING("Food & Drinks", "🍽️"),
    LODGING("Accommodation", "🛏️"),
    TRANSIT("Flights & Transit", "🚆"),
    ACTIVITIES("Tickets & Tours", "🎟️"),
    SHOPPING("Supplies & Gear", "🛍️"),
    SETTLEMENT("Debt Settlement", "🤝")
}

enum class SplitType(val label: String) {
    EQUAL("Equal Split"),
    EXACT("Exact Amounts"),
    PERCENTAGE("Percentage %")
}

enum class DocumentType(val label: String, val badgeEmoji: String) {
    BOARDING_PASS("Flight / Boarding Pass", "✈️"),
    HOTEL_VOUCHER("Hotel Booking", "🏨"),
    RAIL_PASS("Rail / Transit Pass", "🚅"),
    INSURANCE("Travel Insurance", "🛡️"),
    TICKET("Event / Attraction Ticket", "🎫")
}

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val appCurrency: String = "INR",
    val themeId: String = "MODERN_HORIZON",
    val lastSelectedTripId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val daysCount: Int,
    val baseCurrency: String,
    val totalBudget: Double,
    val membersCsv: String,
    val tagline: String,
    val offlineRegionName: String,
    val offlineRegionDownloaded: Boolean,
    val offlineSizeMb: Int,
    val centerLat: Double,
    val centerLng: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun memberList(): List<String> =
        membersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@Entity(
    tableName = "travelers",
    indices = [Index(value = ["tripId"])]
)
data class TravelerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tripId: Long,
    val name: String,
    val phoneOrEmail: String = "",
    val roleOrNote: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "deposits",
    indices = [Index(value = ["tripId"])]
)
data class DepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tripId: Long,
    val travelerName: String,
    val amount: Double,
    val currency: String,
    val amountInBase: Double,
    val paymentMethod: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "itinerary_stops",
    indices = [Index(value = ["tripId"]), Index(value = ["tripId", "dayNumber"])]
)
data class ItineraryStopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tripId: Long,
    val dayNumber: Int,
    val title: String,
    val category: String,
    val startTime: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val estimatedCost: Double,
    val currency: String,
    val notes: String,
    val votersCsv: String,
    val isVisited: Boolean = false
) {
    fun voterList(): List<String> =
        votersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun stopCategory(): StopCategory =
        StopCategory.entries.firstOrNull { it.name == category } ?: StopCategory.SIGHTSEEING
}

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["tripId"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tripId: Long,
    val title: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val amountInBase: Double,
    val paidByMember: String,
    val splitAmongCsv: String,
    val splitType: String = SplitType.EQUAL.name,
    val customSharesCsv: String = "",
    val notes: String = "",
    val isSettlement: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun splitParticipants(): List<String> =
        splitAmongCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun expenseCategory(): ExpenseCategory =
        ExpenseCategory.entries.firstOrNull { it.name == category } ?: ExpenseCategory.DINING

    fun resolvedSplitType(): SplitType =
        SplitType.entries.firstOrNull { it.name == splitType } ?: SplitType.EQUAL

    fun customShareMap(): Map<String, Double> {
        if (customSharesCsv.isBlank()) return emptyMap()
        return customSharesCsv.split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val name = parts[0].trim()
                    val value = parts[1].trim().toDoubleOrNull()
                    if (name.isNotEmpty() && value != null) name to value else null
                } else null
            }
            .toMap()
    }
}

@Entity(
    tableName = "travel_documents",
    indices = [Index(value = ["tripId"])]
)
data class TravelDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tripId: Long,
    val title: String,
    val docType: String,
    val referenceCode: String,
    val provider: String,
    val validDate: String,
    val holderName: String,
    val notes: String,
    val isOfflineCached: Boolean = true
) {
    fun documentType(): DocumentType =
        DocumentType.entries.firstOrNull { it.name == docType } ?: DocumentType.TICKET
}

@Entity(
    tableName = "custom_map_pins",
    indices = [Index(value = ["tripId"])]
)
data class CustomMapPinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tripId: Long,
    val title: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val note: String,
    val dayNumber: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
