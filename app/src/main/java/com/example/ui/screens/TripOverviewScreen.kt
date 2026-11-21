package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DepositEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ItineraryStopEntity
import com.example.data.local.TravelDocumentEntity
import com.example.data.local.TravelerEntity
import com.example.data.local.TripEntity
import com.example.domain.ExpenseCalculator
import com.example.domain.SettlementTransfer
import com.example.domain.TripExportDataBundle
import com.example.domain.TripFinancialSummary
import com.example.ui.components.DetailedExportReportSection
import com.example.ui.components.LiveThemeSwitcherBar
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.LocalTripDesignTokens
import com.example.ui.viewmodel.TripExpenditureUiState
import com.example.ui.viewmodel.TripOverviewTab
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripOverviewScreen(
    uiState: TripExpenditureUiState,
    onBackToDashboard: () -> Unit,
    onSelectTheme: (AppThemeOption) -> Unit,
    onOpenThemeShowcase: () -> Unit,
    onSelectTab: (TripOverviewTab) -> Unit,
    onSelectDay: (Int) -> Unit,
    onSelectVoterIdentity: (String) -> Unit,
    onRequestDeleteTrip: (TripEntity) -> Unit,
    onRequestAddDeposit: () -> Unit,
    onRequestManageTravelers: () -> Unit,
    onRequestAddStop: () -> Unit,
    onRequestAddExpense: (prefillTitle: String, prefillAmount: String) -> Unit,
    onRequestAddDocument: () -> Unit,
    onDeleteDeposit: (Long) -> Unit,
    onDeleteTravelerEntity: (TravelerEntity) -> Unit,
    onDeleteTravelerByName: (String) -> Unit,
    onToggleVoteStop: (ItineraryStopEntity) -> Unit,
    onToggleVisitedStop: (ItineraryStopEntity) -> Unit,
    onDeleteStop: (Long) -> Unit,
    onSettleTransfer: (SettlementTransfer) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onDeleteDocument: (Long) -> Unit,
    onSavePdfToDevice: (TripExportDataBundle) -> Unit,
    onSharePdfFile: (TripExportDataBundle) -> Unit,
    onSaveCsvToDevice: (TripExportDataBundle) -> Unit,
    onShareCsvFile: (TripExportDataBundle) -> Unit,
    onNotifyCopied: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToDashboard)

    val tokens = LocalTripDesignTokens.current
    val trip = uiState.activeTrip ?: return
    val financials = uiState.financials ?: ExpenseCalculator.calculateTripFinancials(
        trip = trip,
        expenses = uiState.activeTripExpenses,
        deposits = uiState.activeTripDeposits,
        overrideCurrency = uiState.appCurrency
    )
    val appCurrency = uiState.appCurrency
    val allTravelerNames = (uiState.activeTripTravelers.map { it.name } + trip.memberList()).distinct()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trip_overview_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Navigation & Trip Header with Export PDF/CSV & Delete Trip Options
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBackToDashboard,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("back_to_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = trip.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${allTravelerNames.size} Travelers • Currency: $appCurrency",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onRequestManageTravelers,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("overview_manage_travelers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Manage Travelers",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = { onSelectTab(TripOverviewTab.EXPORT_REPORT) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("overview_export_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export Detailed PDF or CSV Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("overview_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Application Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { onRequestDeleteTrip(trip) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("overview_delete_trip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Trip",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 2. Live Theme Switcher Bar
        item {
            LiveThemeSwitcherBar(
                activeTheme = uiState.activeTheme,
                onSelectTheme = onSelectTheme,
                onOpenShowcase = onOpenThemeShowcase
            )
        }

        // 3. Scrollable Workspace Tabs (Deposits | Expenses | Travelers | Itinerary | Vault | Export PDF/CSV)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TripOverviewTab.entries.forEach { tab ->
                    val isSelected = uiState.selectedTripTab == tab
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .clickable { onSelectTab(tab) }
                            .testTag("overview_tab_${tab.name.lowercase()}"),
                        shape = RoundedCornerShape(tokens.cardCornerRadius / 1.5f),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else tokens.cardBorderColor
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Tab Content
        when (uiState.selectedTripTab) {
            TripOverviewTab.DEPOSITS -> {
                item {
                    DepositsOverviewHeaderCard(
                        appCurrency = appCurrency,
                        financials = financials,
                        onRequestAddDeposit = onRequestAddDeposit,
                        onOpenExportReport = { onSelectTab(TripOverviewTab.EXPORT_REPORT) }
                    )
                }

                if (uiState.activeTripDeposits.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Deposits Recorded Yet",
                            subtitle = "Add deposits by traveler name to track each person's contribution to the trip fund.",
                            actionLabel = "+ Add Deposit by Name",
                            onAction = onRequestAddDeposit
                        )
                    }
                } else {
                    items(uiState.activeTripDeposits, key = { it.id }) { deposit ->
                        DepositItemCard(
                            deposit = deposit,
                            appCurrency = appCurrency,
                            onDelete = { onDeleteDeposit(deposit.id) }
                        )
                    }
                }
            }

            TripOverviewTab.EXPENSES -> {
                item {
                    ExpensesAndSplitsOverviewSection(
                        tripBaseCurrency = appCurrency,
                        financials = financials,
                        onRequestAddExpense = { onRequestAddExpense("", "") },
                        onSettleTransfer = onSettleTransfer,
                        onOpenExportReport = { onSelectTab(TripOverviewTab.EXPORT_REPORT) }
                    )
                }

                if (uiState.activeTripExpenses.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Expenses Logged Yet",
                            subtitle = "Log expenses paid from the Trip Deposit Pool or directly by any traveler.",
                            actionLabel = "+ Log Expense",
                            onAction = { onRequestAddExpense("", "") }
                        )
                    }
                } else {
                    items(uiState.activeTripExpenses, key = { it.id }) { expense ->
                        ExpenseLedgerItemCard(
                            expense = expense,
                            tripBaseCurrency = appCurrency,
                            onDelete = { onDeleteExpense(expense.id) }
                        )
                    }
                }
            }

            TripOverviewTab.TRAVELERS -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(tokens.cardCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Travelers Roster (${allTravelerNames.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Add or delete travelers and view their deposit & expense standing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onRequestManageTravelers,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("travelers_tab_add_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add / Edit")
                            }
                        }
                    }
                }

                if (allTravelerNames.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Travelers Added Yet",
                            subtitle = "Add travelers with their contact details and notes to split expenses and record deposits.",
                            actionLabel = "+ Add Traveler Details",
                            onAction = onRequestManageTravelers
                        )
                    }
                } else {
                    items(allTravelerNames, key = { it }) { memberName ->
                        val entity = uiState.activeTripTravelers.firstOrNull {
                            it.name.equals(memberName, ignoreCase = true)
                        }
                        val balance = financials.memberBalances.firstOrNull {
                            it.memberName.equals(memberName, ignoreCase = true)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("traveler_card_$memberName"),
                            shape = RoundedCornerShape(tokens.cardCornerRadius),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = memberName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (entity != null && (entity.phoneOrEmail.isNotBlank() || entity.roleOrNote.isNotBlank())) {
                                        Text(
                                            text = listOf(entity.phoneOrEmail, entity.roleOrNote)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (balance != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Deposited: ${
                                                ExpenseCalculator.formatCurrency(
                                                    balance.totalDepositedInBase,
                                                    appCurrency
                                                )
                                            } • Paid: ${
                                                ExpenseCalculator.formatCurrency(
                                                    balance.totalPaidInBase,
                                                    appCurrency
                                                )
                                            } • Share: ${
                                                ExpenseCalculator.formatCurrency(
                                                    balance.fairShareInBase,
                                                    appCurrency
                                                )
                                            }",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (entity != null) {
                                            onDeleteTravelerEntity(entity)
                                        } else {
                                            onDeleteTravelerByName(memberName)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("overview_delete_traveler_$memberName")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Traveler $memberName",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            TripOverviewTab.ITINERARY -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(tokens.cardCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "COLLABORATIVE SCHEDULE & VOTING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Day-by-day stops & group votes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = onRequestAddStop,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("itinerary_add_stop_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Stop")
                                }
                            }

                            if (allTravelerNames.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    allTravelerNames.forEach { member ->
                                        FilterChip(
                                            selected = uiState.activeVoterIdentity == member,
                                            onClick = { onSelectVoterIdentity(member) },
                                            label = { Text("Voting as: $member") }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (day in 1..trip.daysCount.coerceAtLeast(1)) {
                                    val dayStopCount = uiState.activeTripStops.count { it.dayNumber == day }
                                    FilterChip(
                                        selected = uiState.selectedDayNumber == day,
                                        onClick = { onSelectDay(day) },
                                        label = { Text("Day $day ($dayStopCount)") },
                                        modifier = Modifier.testTag("day_filter_pill_$day")
                                    )
                                }
                            }
                        }
                    }
                }

                val dayStops = uiState.activeTripStops.filter { it.dayNumber == uiState.selectedDayNumber }
                if (dayStops.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No stops scheduled for Day ${uiState.selectedDayNumber}",
                            subtitle = "Add a sightseeing spot, restaurant, or hotel stop for Day ${uiState.selectedDayNumber}.",
                            actionLabel = "+ Add Stop for Day ${uiState.selectedDayNumber}",
                            onAction = onRequestAddStop
                        )
                    }
                } else {
                    items(dayStops, key = { it.id }) { stop ->
                        ItineraryStopTimelineCard(
                            stop = stop,
                            appCurrency = appCurrency,
                            activeVoter = uiState.activeVoterIdentity,
                            onVote = { onToggleVoteStop(stop) },
                            onToggleVisited = { onToggleVisitedStop(stop) },
                            onLogExpenseForStop = {
                                onRequestAddExpense(
                                    stop.title,
                                    if (stop.estimatedCost > 0.0) stop.estimatedCost.toString() else ""
                                )
                            },
                            onDelete = { onDeleteStop(stop.id) }
                        )
                    }
                }
            }

            TripOverviewTab.DOCUMENT_VAULT -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Travel Document Vault",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Store tickets, booking references, and vouchers locally.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onRequestAddDocument,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("vault_add_document_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Doc")
                        }
                    }
                }

                if (uiState.activeTripDocuments.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Vault is Empty",
                            subtitle = "Save flight PNR codes, hotel vouchers, and rail passes on-device.",
                            actionLabel = "+ Add Booking Document",
                            onAction = onRequestAddDocument
                        )
                    }
                } else {
                    items(uiState.activeTripDocuments, key = { it.id }) { doc ->
                        TravelDocumentCard(
                            doc = doc,
                            onDelete = { onDeleteDocument(doc.id) }
                        )
                    }
                }
            }

            TripOverviewTab.EXPORT_REPORT -> {
                item {
                    val bundle = TripExportDataBundle(
                        trip = trip,
                        appCurrency = appCurrency,
                        financials = financials,
                        travelers = uiState.activeTripTravelers,
                        deposits = uiState.activeTripDeposits,
                        expenses = uiState.activeTripExpenses,
                        stops = uiState.activeTripStops,
                        documents = uiState.activeTripDocuments
                    )
                    DetailedExportReportSection(
                        bundle = bundle,
                        lastExportStatus = uiState.lastExportConfirmation,
                        onSavePdfToDevice = onSavePdfToDevice,
                        onSharePdfFile = onSharePdfFile,
                        onSaveCsvToDevice = onSaveCsvToDevice,
                        onShareCsvFile = onShareCsvFile,
                        onNotifyCopied = onNotifyCopied
                    )
                }
            }
        }
    }
}

@Composable
private fun DepositsOverviewHeaderCard(
    appCurrency: String,
    financials: TripFinancialSummary,
    onRequestAddDeposit: () -> Unit,
    onOpenExportReport: () -> Unit
) {
    val tokens = LocalTripDesignTokens.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DEPOSITS BY TRAVELER NAME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Total Pool: ${
                            ExpenseCalculator.formatCurrency(
                                financials.totalDepositedInBase,
                                appCurrency
                            )
                        }",
                        style = MaterialTheme.typography.headlineMedium,
                        color = tokens.positiveBalanceColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Spent: ${
                            ExpenseCalculator.formatCurrency(
                                financials.totalSpentInBase,
                                appCurrency
                            )
                        } • Remaining Pool: ${
                            ExpenseCalculator.formatCurrency(
                                financials.remainingPoolBalance,
                                appCurrency
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onRequestAddDeposit,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("deposits_tab_add_button")
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Deposit")
                }
            }

            if (financials.memberBalances.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DEPOSIT & STANDING BY TRAVELER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                financials.memberBalances.forEach { mb ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = mb.memberName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Deposited: ${
                                    ExpenseCalculator.formatCurrency(
                                        mb.totalDepositedInBase,
                                        appCurrency
                                    )
                                } • Share of Spend: ${
                                    ExpenseCalculator.formatCurrency(
                                        mb.fairShareInBase,
                                        appCurrency
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (mb.netBalanceInBase >= 0.0) {
                                "+${ExpenseCalculator.formatCurrency(mb.netBalanceInBase, appCurrency)}"
                            } else {
                                ExpenseCalculator.formatCurrency(mb.netBalanceInBase, appCurrency)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (mb.netBalanceInBase >= 0.0) {
                                tokens.positiveBalanceColor
                            } else {
                                tokens.warningAccentColor
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenExportReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("deposits_open_export_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Detailed PDF & CSV Report")
            }
        }
    }
}

@Composable
private fun DepositItemCard(
    deposit: DepositEntity,
    appCurrency: String,
    onDelete: () -> Unit
) {
    val tokens = LocalTripDesignTokens.current
    val converted = ExpenseCalculator.convertToBase(deposit.amount, deposit.currency, appCurrency)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deposit_item_card_${deposit.id}"),
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💰 ${deposit.travelerName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val details = listOf(deposit.paymentMethod, deposit.note)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                if (details.isNotBlank()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${ExpenseCalculator.formatCurrency(converted, appCurrency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.positiveBalanceColor
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_deposit_button_${deposit.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Deposit",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ItineraryStopTimelineCard(
    stop: ItineraryStopEntity,
    appCurrency: String,
    activeVoter: String,
    onVote: () -> Unit,
    onToggleVisited: () -> Unit,
    onLogExpenseForStop: () -> Unit,
    onDelete: () -> Unit
) {
    val tokens = LocalTripDesignTokens.current
    val voters = stop.voterList()
    val hasVoted = activeVoter.isNotBlank() && voters.contains(activeVoter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("itinerary_stop_card_${stop.id}"),
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${stop.stopCategory().badgeEmoji} ${stop.startTime.ifBlank { "Scheduled" }} • ${stop.stopCategory().label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (stop.estimatedCost > 0.0) {
                    val estConv = ExpenseCalculator.convertToBase(stop.estimatedCost, stop.currency, appCurrency)
                    Text(
                        text = "Est. ${ExpenseCalculator.formatCurrency(estConv, appCurrency)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stop.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            if (stop.locationName.isNotBlank()) {
                Text(
                    text = "📍 ${stop.locationName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (stop.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stop.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (voters.isEmpty()) {
                    "No votes yet — tap below to upvote"
                } else {
                    "👍 Upvoted by: ${voters.joinToString(", ")}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = tokens.positiveBalanceColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onVote,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("stop_vote_button_${stop.id}")
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = "Upvote Stop", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasVoted) "Voted (${voters.size})" else "Vote (${voters.size})")
                }

                OutlinedButton(
                    onClick = onToggleVisited,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Mark Visited",
                        tint = if (stop.isVisited) tokens.positiveBalanceColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (stop.isVisited) "Done" else "Visit")
                }

                IconButton(
                    onClick = onLogExpenseForStop,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Log Expense for Stop",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpensesAndSplitsOverviewSection(
    tripBaseCurrency: String,
    financials: TripFinancialSummary,
    onRequestAddExpense: () -> Unit,
    onSettleTransfer: (SettlementTransfer) -> Unit,
    onOpenExportReport: () -> Unit
) {
    val tokens = LocalTripDesignTokens.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(tokens.cardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EXPENSES & GROUP SPLITS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Total Spent: ${
                                ExpenseCalculator.formatCurrency(
                                    financials.totalSpentInBase,
                                    tripBaseCurrency
                                )
                            }",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onRequestAddExpense,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("expenses_tab_add_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Expense")
                    }
                }

                if (financials.memberBalances.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    financials.memberBalances.forEach { mb ->
                        val isPositive = mb.netBalanceInBase > 0.01
                        val isNegative = mb.netBalanceInBase < -0.01
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = mb.memberName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Deposited ${
                                        ExpenseCalculator.formatCurrency(
                                            mb.totalDepositedInBase,
                                            tripBaseCurrency
                                        )
                                    } • Paid ${
                                        ExpenseCalculator.formatCurrency(
                                            mb.totalPaidInBase,
                                            tripBaseCurrency
                                        )
                                    } • Share ${
                                        ExpenseCalculator.formatCurrency(
                                            mb.fairShareInBase,
                                            tripBaseCurrency
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = when {
                                    isPositive -> "+${
                                        ExpenseCalculator.formatCurrency(
                                            mb.netBalanceInBase,
                                            tripBaseCurrency
                                        )
                                    }"
                                    isNegative -> "-${
                                        ExpenseCalculator.formatCurrency(
                                            abs(mb.netBalanceInBase),
                                            tripBaseCurrency
                                        )
                                    }"
                                    else -> "Settled ✓"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = when {
                                    isPositive -> tokens.positiveBalanceColor
                                    isNegative -> tokens.warningAccentColor
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (financials.settlementTransfers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "RECOMMENDED SETTLEMENTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    financials.settlementTransfers.forEachIndexed { idx, transfer ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, tokens.cardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${transfer.fromMember} pays ${transfer.toMember}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = ExpenseCalculator.formatCurrency(
                                            transfer.amountInBase,
                                            transfer.currency
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onSettleTransfer(transfer) },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("settle_transfer_button_$idx")
                                ) {
                                    Text("Settle Up")
                                }
                            }
                        }
                    }
                }

                if (financials.categoryBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "EXPENDITURE BY CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    financials.categoryBreakdown.forEach { catSum ->
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${catSum.category.badgeEmoji} ${catSum.category.label}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${
                                        ExpenseCalculator.formatCurrency(
                                            catSum.amountInBase,
                                            tripBaseCurrency
                                        )
                                    } (${(catSum.fractionOfTotal * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { catSum.fractionOfTotal },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenExportReport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("expenses_open_export_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Detailed PDF & CSV Report")
                }
            }
        }
    }
}

@Composable
private fun ExpenseLedgerItemCard(
    expense: ExpenseEntity,
    tripBaseCurrency: String,
    onDelete: () -> Unit
) {
    val tokens = LocalTripDesignTokens.current
    val cat = expense.expenseCategory()
    val converted = ExpenseCalculator.convertToBase(expense.amount, expense.currency, tripBaseCurrency)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_item_card_${expense.id}"),
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${cat.badgeEmoji} ${expense.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paid by ${expense.paidByMember} • ${expense.resolvedSplitType().label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.notes.isNotBlank()) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ExpenseCalculator.formatCurrency(converted, tripBaseCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (expense.isSettlement) {
                            tokens.positiveBalanceColor
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Expense",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TravelDocumentCard(
    doc: TravelDocumentEntity,
    onDelete: () -> Unit
) {
    val tokens = LocalTripDesignTokens.current
    val dt = doc.documentType()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("travel_doc_card_${doc.id}"),
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${dt.badgeEmoji} ${dt.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Document",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (doc.referenceCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = doc.referenceCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            val meta = listOf(doc.provider, doc.validDate, doc.holderName)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            if (meta.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (doc.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = doc.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val tokens = LocalTripDesignTokens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.height(48.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}
