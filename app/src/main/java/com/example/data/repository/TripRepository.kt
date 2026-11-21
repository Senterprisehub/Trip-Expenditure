package com.example.data.repository

import com.example.data.local.AppSettingsEntity
import com.example.data.local.CustomMapPinEntity
import com.example.data.local.DepositEntity
import com.example.data.local.DocumentType
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.data.local.ItineraryStopEntity
import com.example.data.local.SplitType
import com.example.data.local.StopCategory
import com.example.data.local.TravelDocumentEntity
import com.example.data.local.TravelerEntity
import com.example.data.local.TripEntity
import com.example.data.local.TripExpenditureDao
import com.example.domain.ExpenseCalculator
import kotlinx.coroutines.flow.Flow

class TripRepository(private val dao: TripExpenditureDao) {

    val appSettings: Flow<AppSettingsEntity?> = dao.observeAppSettings()
    val allTrips: Flow<List<TripEntity>> = dao.observeAllTrips()
    val allExpenses: Flow<List<ExpenseEntity>> = dao.observeAllExpenses()
    val allDeposits: Flow<List<DepositEntity>> = dao.observeAllDeposits()
    val allStops: Flow<List<ItineraryStopEntity>> = dao.observeAllStops()

    suspend fun getOrInitSettings(defaultCurrency: String, defaultTheme: String): AppSettingsEntity {
        val existing = dao.getAppSettingsOnce()
        if (existing != null) return existing
        val created = AppSettingsEntity(
            id = 1,
            appCurrency = defaultCurrency,
            themeId = defaultTheme,
            lastSelectedTripId = null
        )
        dao.upsertAppSettings(created)
        return created
    }

    suspend fun saveAppSettings(
        appCurrency: String? = null,
        themeId: String? = null,
        lastSelectedTripId: Long? = null
    ) {
        val current = dao.getAppSettingsOnce() ?: AppSettingsEntity(id = 1)
        val updated = current.copy(
            appCurrency = appCurrency ?: current.appCurrency,
            themeId = themeId ?: current.themeId,
            lastSelectedTripId = lastSelectedTripId ?: current.lastSelectedTripId,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertAppSettings(updated)
    }

    fun observeTravelersForTrip(tripId: Long): Flow<List<TravelerEntity>> =
        dao.observeTravelersForTrip(tripId)

    fun observeDepositsForTrip(tripId: Long): Flow<List<DepositEntity>> =
        dao.observeDepositsForTrip(tripId)

    fun observeStopsForTrip(tripId: Long): Flow<List<ItineraryStopEntity>> =
        dao.observeStopsForTrip(tripId)

    fun observeExpensesForTrip(tripId: Long): Flow<List<ExpenseEntity>> =
        dao.observeExpensesForTrip(tripId)

    fun observeDocumentsForTrip(tripId: Long): Flow<List<TravelDocumentEntity>> =
        dao.observeDocumentsForTrip(tripId)

    fun observeCustomPinsForTrip(tripId: Long): Flow<List<CustomMapPinEntity>> =
        dao.observeCustomPinsForTrip(tripId)

    suspend fun createTrip(
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        daysCount: Int,
        baseCurrency: String,
        totalBudget: Double,
        members: List<String>,
        offlineRegionName: String,
        centerLat: Double,
        centerLng: Double
    ): Long {
        val cleanTitle = title.trim()
        val cleanDest = destination.trim()
        val cleanMembers = members.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        val trip = TripEntity(
            title = cleanTitle,
            destination = cleanDest,
            startDate = startDate.trim(),
            endDate = endDate.trim(),
            daysCount = daysCount.coerceIn(1, 60),
            baseCurrency = baseCurrency.uppercase(),
            totalBudget = totalBudget.coerceAtLeast(0.0),
            membersCsv = cleanMembers.joinToString(","),
            tagline = if (cleanMembers.isEmpty()) {
                "Add travelers, deposits & expenses"
            } else {
                "${cleanMembers.size} Travelers • Group Trip"
            },
            offlineRegionName = offlineRegionName.trim().ifEmpty { "$cleanDest Offline Map" },
            offlineRegionDownloaded = true,
            offlineSizeMb = 36,
            centerLat = centerLat,
            centerLng = centerLng
        )
        val tripId = dao.insertTrip(trip)

        // Create corresponding TravelerEntity records for any initial travelers entered by user
        for (memberName in cleanMembers) {
            dao.insertTraveler(
                TravelerEntity(
                    tripId = tripId,
                    name = memberName,
                    phoneOrEmail = "",
                    roleOrNote = ""
                )
            )
        }

        saveAppSettings(lastSelectedTripId = tripId)
        return tripId
    }

    suspend fun deleteTrip(tripId: Long) {
        dao.deleteTripAndAllData(tripId)
    }

    // Traveler Details Add & Delete
    suspend fun addTravelerWithDetails(
        trip: TripEntity,
        name: String,
        phoneOrEmail: String,
        roleOrNote: String
    ): Long {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return 0L

        val id = dao.insertTraveler(
            TravelerEntity(
                tripId = trip.id,
                name = cleanName,
                phoneOrEmail = phoneOrEmail.trim(),
                roleOrNote = roleOrNote.trim()
            )
        )
        syncTripMembersFromTravelers(trip)
        return id
    }

    suspend fun deleteTraveler(trip: TripEntity, traveler: TravelerEntity) {
        dao.deleteTravelerById(traveler.id)
        syncTripMembersFromTravelers(trip, removedName = traveler.name)
    }

    suspend fun deleteTravelerByName(trip: TripEntity, memberName: String) {
        val currentTravelers = dao.getTravelersForTripOnce(trip.id)
        currentTravelers
            .filter { it.name.equals(memberName, ignoreCase = true) }
            .forEach { dao.deleteTravelerById(it.id) }
        val updatedNames = trip.memberList()
            .filterNot { it.equals(memberName, ignoreCase = true) }
        dao.updateTrip(
            trip.copy(
                membersCsv = updatedNames.joinToString(","),
                tagline = "${updatedNames.size} Travelers"
            )
        )
    }

    private suspend fun syncTripMembersFromTravelers(trip: TripEntity, removedName: String? = null) {
        val travelerRows = dao.getTravelersForTripOnce(trip.id)
        val namesFromTable = travelerRows.map { it.name.trim() }.filter { it.isNotEmpty() }
        val combined = (trip.memberList() + namesFromTable)
            .distinct()
            .filterNot { removedName != null && it.equals(removedName, ignoreCase = true) }
        dao.updateTrip(
            trip.copy(
                membersCsv = combined.joinToString(","),
                tagline = "${combined.size} Travelers"
            )
        )
    }

    // Deposit by Traveler Name Add & Delete
    suspend fun addDepositByName(
        trip: TripEntity,
        travelerName: String,
        amount: Double,
        currency: String,
        paymentMethod: String,
        note: String
    ): Long {
        val cleanName = travelerName.trim()
        val validAmount = amount.coerceAtLeast(0.01)
        val amountInBase = ExpenseCalculator.convertToBase(
            amount = validAmount,
            fromCurrency = currency,
            toBaseCurrency = trip.baseCurrency
        )

        // Ensure the depositing traveler is also in the trip's traveler roster
        val existingRows = dao.getTravelersForTripOnce(trip.id)
        if (cleanName.isNotEmpty() && existingRows.none { it.name.equals(cleanName, ignoreCase = true) }) {
            dao.insertTraveler(
                TravelerEntity(
                    tripId = trip.id,
                    name = cleanName,
                    phoneOrEmail = "",
                    roleOrNote = "Depositor"
                )
            )
            syncTripMembersFromTravelers(trip)
        }

        return dao.insertDeposit(
            DepositEntity(
                tripId = trip.id,
                travelerName = cleanName,
                amount = validAmount,
                currency = currency.uppercase(),
                amountInBase = amountInBase,
                paymentMethod = paymentMethod.trim(),
                note = note.trim()
            )
        )
    }

    suspend fun deleteDeposit(depositId: Long) {
        dao.deleteDepositById(depositId)
    }

    suspend fun toggleOfflineRegionStatus(trip: TripEntity) {
        val nextDownloaded = !trip.offlineRegionDownloaded
        val nextSize = if (nextDownloaded) (trip.offlineSizeMb.coerceAtLeast(42)) else 0
        dao.updateTrip(
            trip.copy(
                offlineRegionDownloaded = nextDownloaded,
                offlineSizeMb = nextSize
            )
        )
    }

    suspend fun addItineraryStop(
        tripId: Long,
        dayNumber: Int,
        title: String,
        category: StopCategory,
        startTime: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        estimatedCost: Double,
        currency: String,
        notes: String
    ): Long {
        val stop = ItineraryStopEntity(
            tripId = tripId,
            dayNumber = dayNumber.coerceAtLeast(1),
            title = title.trim(),
            category = category.name,
            startTime = startTime.trim(),
            locationName = locationName.trim(),
            latitude = latitude,
            longitude = longitude,
            estimatedCost = estimatedCost.coerceAtLeast(0.0),
            currency = currency.uppercase(),
            notes = notes.trim(),
            votersCsv = "",
            isVisited = false
        )
        return dao.insertStop(stop)
    }

    suspend fun toggleVoteOnStop(stop: ItineraryStopEntity, voterName: String) {
        val cleanVoter = voterName.trim().ifEmpty { "Traveler" }
        val currentVoters = stop.voterList().toMutableList()
        if (currentVoters.contains(cleanVoter)) {
            currentVoters.remove(cleanVoter)
        } else {
            currentVoters.add(cleanVoter)
        }
        dao.updateStop(stop.copy(votersCsv = currentVoters.joinToString(",")))
    }

    suspend fun toggleStopVisited(stop: ItineraryStopEntity) {
        dao.updateStop(stop.copy(isVisited = !stop.isVisited))
    }

    suspend fun deleteStop(stopId: Long) {
        dao.deleteStopById(stopId)
    }

    suspend fun addExpense(
        trip: TripEntity,
        title: String,
        category: ExpenseCategory,
        amount: Double,
        currency: String,
        paidByMember: String,
        splitMembers: List<String>,
        splitType: SplitType = SplitType.EQUAL,
        customSharesCsv: String = "",
        notes: String = "",
        isSettlement: Boolean = false
    ): Long {
        val validAmount = amount.coerceAtLeast(0.01)
        val amountInBase = ExpenseCalculator.convertToBase(
            amount = validAmount,
            fromCurrency = currency,
            toBaseCurrency = trip.baseCurrency
        )
        val participants = splitMembers.ifEmpty { trip.memberList() }
        val expense = ExpenseEntity(
            tripId = trip.id,
            title = title.trim(),
            category = category.name,
            amount = validAmount,
            currency = currency.uppercase(),
            amountInBase = amountInBase,
            paidByMember = paidByMember.trim(),
            splitAmongCsv = participants.joinToString(","),
            splitType = splitType.name,
            customSharesCsv = customSharesCsv,
            notes = notes.trim(),
            isSettlement = isSettlement
        )
        return dao.insertExpense(expense)
    }

    suspend fun recordSettlementPayment(
        trip: TripEntity,
        fromMember: String,
        toMember: String,
        amountInBase: Double,
        currency: String
    ): Long {
        return addExpense(
            trip = trip,
            title = "Settlement: $fromMember → $toMember",
            category = ExpenseCategory.SETTLEMENT,
            amount = amountInBase,
            currency = currency,
            paidByMember = fromMember,
            splitMembers = listOf(toMember),
            splitType = SplitType.EQUAL,
            notes = "Recorded debt settlement transfer",
            isSettlement = true
        )
    }

    suspend fun deleteExpense(expenseId: Long) {
        dao.deleteExpenseById(expenseId)
    }

    suspend fun addTravelDocument(
        tripId: Long,
        title: String,
        docType: DocumentType,
        referenceCode: String,
        provider: String,
        validDate: String,
        holderName: String,
        notes: String
    ): Long {
        val doc = TravelDocumentEntity(
            tripId = tripId,
            title = title.trim(),
            docType = docType.name,
            referenceCode = referenceCode.trim(),
            provider = provider.trim(),
            validDate = validDate.trim(),
            holderName = holderName.trim(),
            notes = notes.trim(),
            isOfflineCached = true
        )
        return dao.insertDocument(doc)
    }

    suspend fun deleteDocument(docId: Long) {
        dao.deleteDocumentById(docId)
    }

    suspend fun addCustomMapPin(
        tripId: Long,
        title: String,
        category: String,
        latitude: Double,
        longitude: Double,
        note: String,
        dayNumber: Int
    ): Long {
        val pin = CustomMapPinEntity(
            tripId = tripId,
            title = title.trim(),
            category = category.trim(),
            latitude = latitude,
            longitude = longitude,
            note = note.trim(),
            dayNumber = dayNumber
        )
        return dao.insertCustomPin(pin)
    }

    suspend fun deleteCustomMapPin(pinId: Long) {
        dao.deleteCustomPinById(pinId)
    }
}
