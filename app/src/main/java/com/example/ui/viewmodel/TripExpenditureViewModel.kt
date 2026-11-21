package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.example.data.local.TripExpenditureDatabase
import com.example.data.repository.TripRepository
import com.example.domain.ExpenseCalculator
import com.example.domain.SettlementTransfer
import com.example.domain.TripExportDataBundle
import com.example.domain.TripFinancialSummary
import com.example.domain.TripReportExporter
import com.example.ui.theme.AppThemeOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreenDestination {
    DASHBOARD,
    TRIP_OVERVIEW,
    SETTINGS
}

enum class TripOverviewTab(val title: String) {
    DEPOSITS("Deposits"),
    EXPENSES("Expenses"),
    TRAVELERS("Travelers"),
    ITINERARY("Itinerary"),
    DOCUMENT_VAULT("Vault"),
    EXPORT_REPORT("Export PDF/CSV")
}

data class TripExpenditureUiState(
    val appName: String = "Trip Expenditure",
    val appDescription: String = "Social travel planning, traveler deposits by name, group expense splitting, and detailed PDF & CSV data export.",
    val appCurrency: String = "INR",
    val activeTheme: AppThemeOption = AppThemeOption.MODERN_HORIZON,
    val showThemeShowcaseModal: Boolean = false,
    val currentScreen: AppScreenDestination = AppScreenDestination.DASHBOARD,
    val selectedTripTab: TripOverviewTab = TripOverviewTab.DEPOSITS,
    val selectedDayNumber: Int = 1,
    val activeVoterIdentity: String = "",
    val trips: List<TripEntity> = emptyList(),
    val activeTrip: TripEntity? = null,
    val activeTripTravelers: List<TravelerEntity> = emptyList(),
    val activeTripDeposits: List<DepositEntity> = emptyList(),
    val activeTripStops: List<ItineraryStopEntity> = emptyList(),
    val activeTripExpenses: List<ExpenseEntity> = emptyList(),
    val activeTripDocuments: List<TravelDocumentEntity> = emptyList(),
    val financials: TripFinancialSummary? = null,
    val allTripsFinancialMap: Map<Long, TripFinancialSummary> = emptyMap(),
    val lastExportConfirmation: String? = null,
    val statusBannerMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TripExpenditureViewModel(
    private val repository: TripRepository,
    private val prefsContext: Context? = null
) : ViewModel() {

    private val prefs by lazy {
        prefsContext?.getSharedPreferences("trip_expenditure_permanent_prefs", Context.MODE_PRIVATE)
    }

    private val _appCurrency = MutableStateFlow(
        prefs?.getString("app_currency", null) ?: "INR"
    )
    private val _activeTheme = MutableStateFlow(
        AppThemeOption.fromId(prefs?.getString("selected_theme_id", AppThemeOption.MODERN_HORIZON.id))
    )
    private val _showThemeShowcaseModal = MutableStateFlow(false)
    private val _currentScreen = MutableStateFlow(AppScreenDestination.DASHBOARD)
    private val _selectedTripId = MutableStateFlow<Long?>(
        prefs?.getLong("last_trip_id", -1L)?.takeIf { it > 0L }
    )
    private val _selectedTripTab = MutableStateFlow(TripOverviewTab.DEPOSITS)
    private val _selectedDayNumber = MutableStateFlow(1)
    private val _activeVoterIdentity = MutableStateFlow("")
    private val _lastExportConfirmation = MutableStateFlow<String?>(null)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)

    val activeTheme: StateFlow<AppThemeOption> = _activeTheme.asStateFlow()

    init {
        // Synchronize settings from Room SQLite on startup so settings are unchangeable across reboots
        viewModelScope.launch {
            val saved = repository.getOrInitSettings(
                defaultCurrency = _appCurrency.value,
                defaultTheme = _activeTheme.value.id
            )
            _appCurrency.value = saved.appCurrency
            _activeTheme.value = AppThemeOption.fromId(saved.themeId)
            if (_selectedTripId.value == null && saved.lastSelectedTripId != null) {
                _selectedTripId.value = saved.lastSelectedTripId
            }
            persistToPrefsSync(
                currency = saved.appCurrency,
                themeId = saved.themeId,
                tripId = saved.lastSelectedTripId
            )
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun persistToPrefsSync(currency: String, themeId: String, tripId: Long?) {
        prefs?.edit()?.apply {
            putString("app_currency", currency)
            putString("selected_theme_id", themeId)
            if (tripId != null) putLong("last_trip_id", tripId)
            commit() // Synchronous commit guarantees durability across immediate reboot
        }
    }

    private val resolvedTripFlow = combine(
        repository.allTrips,
        _selectedTripId
    ) { trips, selectedId ->
        trips.firstOrNull { it.id == selectedId } ?: trips.firstOrNull()
    }

    private val activeDetailsFlow = resolvedTripFlow.flatMapLatest { trip ->
        if (trip == null) {
            flowOf(ActiveTripDetailsBundle())
        } else {
            val groupA = combine(
                repository.observeTravelersForTrip(trip.id),
                repository.observeDepositsForTrip(trip.id),
                repository.observeStopsForTrip(trip.id)
            ) { travelers, deposits, stops ->
                Triple(travelers, deposits, stops)
            }
            val groupB = combine(
                repository.observeExpensesForTrip(trip.id),
                repository.observeDocumentsForTrip(trip.id)
            ) { expenses, docs ->
                expenses to docs
            }
            combine(groupA, groupB) { a, b ->
                ActiveTripDetailsBundle(
                    travelers = a.first,
                    deposits = a.second,
                    stops = a.third,
                    expenses = b.first,
                    documents = b.second
                )
            }
        }
    }

    private val baseDataFlow = combine(
        repository.allTrips,
        repository.allExpenses,
        repository.allDeposits,
        resolvedTripFlow,
        activeDetailsFlow
    ) { trips, allExpenses, allDeposits, activeTrip, details ->
        RawDataSnapshot(
            trips = trips,
            allExpenses = allExpenses,
            allDeposits = allDeposits,
            activeTrip = activeTrip,
            details = details
        )
    }

    private val uiControlsFlow = combine(
        _appCurrency,
        _activeTheme,
        _showThemeShowcaseModal,
        _currentScreen,
        _selectedTripTab
    ) { currency, theme, showModal, screen, tab ->
        UiControlsBundle(currency, theme, showModal, screen, tab)
    }

    private val auxStateFlow = combine(
        _selectedDayNumber,
        _activeVoterIdentity,
        _lastExportConfirmation,
        _statusBannerMessage
    ) { day, voter, exportConfirm, banner ->
        AuxBundle(day, voter, exportConfirm, banner)
    }

    val uiState: StateFlow<TripExpenditureUiState> = combine(
        baseDataFlow,
        uiControlsFlow,
        auxStateFlow
    ) { raw, controls, aux ->
        val globalCurrency = controls.currency
        val expensesByTrip = raw.allExpenses.groupBy { it.tripId }
        val depositsByTrip = raw.allDeposits.groupBy { it.tripId }

        val allFinancials = raw.trips.associate { t ->
            t.id to ExpenseCalculator.calculateTripFinancials(
                trip = t,
                expenses = expensesByTrip[t.id].orEmpty(),
                deposits = depositsByTrip[t.id].orEmpty(),
                overrideCurrency = globalCurrency
            )
        }
        val activeFinancials = raw.activeTrip?.let { t ->
            ExpenseCalculator.calculateTripFinancials(
                trip = t,
                expenses = raw.details.expenses,
                deposits = raw.details.deposits,
                overrideCurrency = globalCurrency
            )
        }

        val memberNames = raw.details.travelers.map { it.name }.ifEmpty {
            raw.activeTrip?.memberList().orEmpty()
        }
        val validVoter = if (memberNames.contains(aux.voter)) {
            aux.voter
        } else {
            memberNames.firstOrNull().orEmpty()
        }

        TripExpenditureUiState(
            appCurrency = globalCurrency,
            activeTheme = controls.theme,
            showThemeShowcaseModal = controls.showModal,
            currentScreen = controls.screen,
            selectedTripTab = controls.tab,
            selectedDayNumber = aux.day.coerceIn(1, (raw.activeTrip?.daysCount ?: 1).coerceAtLeast(1)),
            activeVoterIdentity = validVoter,
            trips = raw.trips,
            activeTrip = raw.activeTrip,
            activeTripTravelers = raw.details.travelers,
            activeTripDeposits = raw.details.deposits,
            activeTripStops = raw.details.stops,
            activeTripExpenses = raw.details.expenses,
            activeTripDocuments = raw.details.documents,
            financials = activeFinancials,
            allTripsFinancialMap = allFinancials,
            lastExportConfirmation = aux.exportConfirmation,
            statusBannerMessage = aux.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TripExpenditureUiState(
            appCurrency = _appCurrency.value,
            activeTheme = _activeTheme.value
        )
    )

    fun selectApplicationCurrency(currencyCode: String) {
        val clean = currencyCode.uppercase()
        _appCurrency.value = clean
        persistToPrefsSync(
            currency = clean,
            themeId = _activeTheme.value.id,
            tripId = _selectedTripId.value
        )
        viewModelScope.launch {
            repository.saveAppSettings(appCurrency = clean)
            showBanner("Application currency permanently locked to $clean (${ExpenseCalculator.currencySymbol(clean)})")
        }
    }

    fun selectTheme(themeOption: AppThemeOption) {
        _activeTheme.value = themeOption
        persistToPrefsSync(
            currency = _appCurrency.value,
            themeId = themeOption.id,
            tripId = _selectedTripId.value
        )
        viewModelScope.launch {
            repository.saveAppSettings(themeId = themeOption.id)
            showBanner("Theme permanently locked to ${themeOption.displayName}")
        }
    }

    fun setThemeShowcaseModalVisible(visible: Boolean) {
        _showThemeShowcaseModal.value = visible
    }

    fun selectTripAndOpenOverview(
        tripId: Long,
        initialTab: TripOverviewTab = TripOverviewTab.DEPOSITS
    ) {
        _selectedTripId.value = tripId
        _selectedTripTab.value = initialTab
        _selectedDayNumber.value = 1
        _currentScreen.value = AppScreenDestination.TRIP_OVERVIEW
        persistToPrefsSync(_appCurrency.value, _activeTheme.value.id, tripId)
        viewModelScope.launch {
            repository.saveAppSettings(lastSelectedTripId = tripId)
        }
    }

    fun setActiveTrip(tripId: Long) {
        _selectedTripId.value = tripId
        _selectedDayNumber.value = 1
        persistToPrefsSync(_appCurrency.value, _activeTheme.value.id, tripId)
        viewModelScope.launch {
            repository.saveAppSettings(lastSelectedTripId = tripId)
        }
    }

    fun navigateToTripOverview(tab: TripOverviewTab = TripOverviewTab.DEPOSITS) {
        if (uiState.value.activeTrip == null) {
            showBanner("Create a trip first to open Trip Overview")
            _currentScreen.value = AppScreenDestination.DASHBOARD
            return
        }
        _selectedTripTab.value = tab
        _currentScreen.value = AppScreenDestination.TRIP_OVERVIEW
    }

    fun navigateBackToDashboard() {
        _currentScreen.value = AppScreenDestination.DASHBOARD
    }

    fun navigateToSettings() {
        _currentScreen.value = AppScreenDestination.SETTINGS
    }

    fun selectOverviewTab(tab: TripOverviewTab) {
        _selectedTripTab.value = tab
    }

    fun selectDayNumber(day: Int) {
        _selectedDayNumber.value = day
    }

    fun selectVoterIdentity(memberName: String) {
        _activeVoterIdentity.value = memberName
    }

    fun recordExportConfirmation(message: String) {
        _lastExportConfirmation.value = message
        showBanner(message)
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun showStatusMessage(message: String) {
        showBanner(message)
    }

    private fun showBanner(message: String) {
        _statusBannerMessage.update { message }
    }

    fun createNewTrip(
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        daysCount: Int,
        totalBudget: Double,
        membersCsv: String
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            showBanner("Please enter a Trip Title")
            return
        }
        viewModelScope.launch {
            val members = membersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val newId = repository.createTrip(
                title = cleanTitle,
                destination = destination.trim(),
                startDate = startDate.trim(),
                endDate = endDate.trim(),
                daysCount = daysCount,
                baseCurrency = _appCurrency.value,
                totalBudget = totalBudget,
                members = members,
                offlineRegionName = "",
                centerLat = 0.0,
                centerLng = 0.0
            )
            _selectedTripId.value = newId
            persistToPrefsSync(_appCurrency.value, _activeTheme.value.id, newId)
            showBanner("Created trip '$cleanTitle'")
        }
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            val deletingTitle = uiState.value.trips.firstOrNull { it.id == tripId }?.title ?: "Trip"
            repository.deleteTrip(tripId)
            if (_selectedTripId.value == tripId) {
                _selectedTripId.value = null
            }
            if (_currentScreen.value == AppScreenDestination.TRIP_OVERVIEW) {
                _currentScreen.value = AppScreenDestination.DASHBOARD
            }
            showBanner("Deleted '$deletingTitle'")
        }
    }

    // Traveler Details Add & Delete
    fun addTravelerWithDetails(
        name: String,
        phoneOrEmail: String,
        roleOrNote: String
    ) {
        val trip = uiState.value.activeTrip ?: return
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            showBanner("Please enter traveler's name")
            return
        }
        viewModelScope.launch {
            repository.addTravelerWithDetails(
                trip = trip,
                name = cleanName,
                phoneOrEmail = phoneOrEmail,
                roleOrNote = roleOrNote
            )
            showBanner("Added traveler '$cleanName'")
        }
    }

    fun deleteTraveler(traveler: TravelerEntity) {
        val trip = uiState.value.activeTrip ?: return
        viewModelScope.launch {
            repository.deleteTraveler(trip, traveler)
            showBanner("Removed traveler '${traveler.name}'")
        }
    }

    fun deleteTravelerByName(name: String) {
        val trip = uiState.value.activeTrip ?: return
        viewModelScope.launch {
            repository.deleteTravelerByName(trip, name)
            showBanner("Removed traveler '$name'")
        }
    }

    // Deposit Option by Name
    fun addDepositByName(
        travelerName: String,
        amount: Double,
        paymentMethod: String,
        note: String
    ) {
        val trip = uiState.value.activeTrip ?: return
        val cleanName = travelerName.trim()
        if (cleanName.isEmpty()) {
            showBanner("Please select or enter a traveler name for the deposit")
            return
        }
        if (amount <= 0.0) {
            showBanner("Please enter a valid deposit amount")
            return
        }
        val cur = _appCurrency.value
        viewModelScope.launch {
            repository.addDepositByName(
                trip = trip,
                travelerName = cleanName,
                amount = amount,
                currency = cur,
                paymentMethod = paymentMethod,
                note = note
            )
            showBanner("Added deposit of ${ExpenseCalculator.formatCurrency(amount, cur)} by $cleanName")
        }
    }

    fun deleteDeposit(depositId: Long) {
        viewModelScope.launch {
            repository.deleteDeposit(depositId)
            showBanner("Deleted deposit entry")
        }
    }

    fun addItineraryStop(
        dayNumber: Int,
        title: String,
        category: StopCategory,
        startTime: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        estimatedCost: Double,
        notes: String
    ) {
        val trip = uiState.value.activeTrip ?: return
        if (title.isBlank()) {
            showBanner("Please enter a stop title")
            return
        }
        viewModelScope.launch {
            repository.addItineraryStop(
                tripId = trip.id,
                dayNumber = dayNumber,
                title = title,
                category = category,
                startTime = startTime,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                estimatedCost = estimatedCost,
                currency = _appCurrency.value,
                notes = notes
            )
            _selectedDayNumber.value = dayNumber
            showBanner("Added '${title.trim()}' to Day $dayNumber")
        }
    }

    fun toggleVoteOnStop(stop: ItineraryStopEntity) {
        val voter = uiState.value.activeVoterIdentity.ifEmpty { "Traveler" }
        viewModelScope.launch {
            repository.toggleVoteOnStop(stop, voter)
        }
    }

    fun toggleStopVisited(stop: ItineraryStopEntity) {
        viewModelScope.launch {
            repository.toggleStopVisited(stop)
        }
    }

    fun deleteStop(stopId: Long) {
        viewModelScope.launch {
            repository.deleteStop(stopId)
            showBanner("Removed stop from itinerary")
        }
    }

    fun addExpense(
        title: String,
        category: ExpenseCategory,
        amount: Double,
        currency: String,
        paidByMember: String,
        splitMembers: List<String>,
        splitType: SplitType,
        customSharesCsv: String,
        notes: String
    ) {
        val trip = uiState.value.activeTrip ?: return
        if (title.isBlank() || amount <= 0.0) {
            showBanner("Please enter expense description and valid amount")
            return
        }
        viewModelScope.launch {
            repository.addExpense(
                trip = trip,
                title = title,
                category = category,
                amount = amount,
                currency = currency,
                paidByMember = paidByMember,
                splitMembers = splitMembers,
                splitType = splitType,
                customSharesCsv = customSharesCsv,
                notes = notes
            )
            showBanner("Logged expense '${title.trim()}' (${ExpenseCalculator.formatCurrency(amount, currency)})")
        }
    }

    fun settleTransfer(transfer: SettlementTransfer) {
        val trip = uiState.value.activeTrip ?: return
        viewModelScope.launch {
            repository.recordSettlementPayment(
                trip = trip,
                fromMember = transfer.fromMember,
                toMember = transfer.toMember,
                amountInBase = transfer.amountInBase,
                currency = _appCurrency.value
            )
            showBanner("Settled: ${transfer.fromMember} paid ${transfer.toMember}")
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            showBanner("Expense deleted")
        }
    }

    fun addTravelDocument(
        title: String,
        docType: DocumentType,
        referenceCode: String,
        provider: String,
        validDate: String,
        holderName: String,
        notes: String
    ) {
        val trip = uiState.value.activeTrip ?: return
        if (title.isBlank()) {
            showBanner("Please enter a document title")
            return
        }
        viewModelScope.launch {
            repository.addTravelDocument(
                tripId = trip.id,
                title = title,
                docType = docType,
                referenceCode = referenceCode,
                provider = provider,
                validDate = validDate,
                holderName = holderName,
                notes = notes
            )
            showBanner("Saved '${title.trim()}' to local Document Vault")
        }
    }

    fun deleteDocument(docId: Long) {
        viewModelScope.launch {
            repository.deleteDocument(docId)
            showBanner("Removed document from vault")
        }
    }

    fun buildActiveTripExportBundle(): TripExportDataBundle? {
        val state = uiState.value
        val trip = state.activeTrip ?: return null
        val fin = state.financials ?: ExpenseCalculator.calculateTripFinancials(
            trip = trip,
            expenses = state.activeTripExpenses,
            deposits = state.activeTripDeposits,
            overrideCurrency = state.appCurrency
        )
        return TripExportDataBundle(
            trip = trip,
            appCurrency = state.appCurrency,
            financials = fin,
            travelers = state.activeTripTravelers,
            deposits = state.activeTripDeposits,
            expenses = state.activeTripExpenses,
            stops = state.activeTripStops,
            documents = state.activeTripDocuments
        )
    }

    fun buildShareableReport(): String {
        val bundle = buildActiveTripExportBundle() ?: return "No active trip selected."
        return TripReportExporter.generateDetailedCsv(bundle)
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val db = TripExpenditureDatabase.getInstance(appContext)
                    val repo = TripRepository(db.dao())
                    return TripExpenditureViewModel(repo, appContext) as T
                }
            }
    }
}

private data class ActiveTripDetailsBundle(
    val travelers: List<TravelerEntity> = emptyList(),
    val deposits: List<DepositEntity> = emptyList(),
    val stops: List<ItineraryStopEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val documents: List<TravelDocumentEntity> = emptyList()
)

private data class RawDataSnapshot(
    val trips: List<TripEntity>,
    val allExpenses: List<ExpenseEntity>,
    val allDeposits: List<DepositEntity>,
    val activeTrip: TripEntity?,
    val details: ActiveTripDetailsBundle
)

private data class UiControlsBundle(
    val currency: String,
    val theme: AppThemeOption,
    val showModal: Boolean,
    val screen: AppScreenDestination,
    val tab: TripOverviewTab
)

private data class AuxBundle(
    val day: Int,
    val voter: String,
    val exportConfirmation: String?,
    val banner: String?
)
