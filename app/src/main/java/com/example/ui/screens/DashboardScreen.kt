package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.example.data.local.TripEntity
import com.example.domain.ExpenseCalculator
import com.example.domain.TripDateUtils
import com.example.domain.TripFinancialSummary
import com.example.ui.components.LiveThemeSwitcherBar
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.LocalTripDesignTokens
import com.example.ui.viewmodel.TripExpenditureUiState
import com.example.ui.viewmodel.TripOverviewTab

@Composable
fun DashboardScreen(
    uiState: TripExpenditureUiState,
    onSelectTheme: (AppThemeOption) -> Unit,
    onOpenThemeShowcase: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTripOverview: (Long, TripOverviewTab) -> Unit,
    onSwitchActiveTrip: (Long) -> Unit,
    onRequestCreateTrip: () -> Unit,
    onRequestDeleteTrip: (TripEntity) -> Unit,
    onRequestAddDeposit: () -> Unit,
    onRequestAddExpense: () -> Unit,
    onRequestManageTravelers: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTripDesignTokens.current
    val activeTrip = uiState.activeTrip
    val financials = uiState.financials
    val appCurrency = uiState.appCurrency

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Application Name & Description Console Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_identity_console_banner"),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FlightTakeoff,
                                        contentDescription = "Trip Expenditure Logo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = uiState.appName,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "${uiState.appCurrency} (${ExpenseCalculator.currencySymbol(uiState.appCurrency)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { onOpenSettings() }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.appDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row {
                            if (activeTrip != null) {
                                IconButton(
                                    onClick = onExportPdf,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("dashboard_export_pdf_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Export Detailed PDF Report",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Pinned Live 1-Tap Theme Switcher Bar
        item {
            LiveThemeSwitcherBar(
                activeTheme = uiState.activeTheme,
                onSelectTheme = onSelectTheme,
                onOpenShowcase = onOpenThemeShowcase
            )
        }

        // 3. Zero Pre-Filled Data Welcome State OR Active Trip Hero Card
        if (activeTrip == null || financials == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_trips_welcome_card"),
                    shape = RoundedCornerShape(tokens.cardCornerRadius + 2.dp),
                    border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tokens.heroGradientBrush)
                            .padding(22.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "NO PRE-FILLED DATA • READY FOR YOUR TRIP",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Start Your First Group Trip",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create a trip to add travelers, record deposits by name, log expenses in ${uiState.appCurrency}, and export detailed PDF & CSV reports.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onRequestCreateTrip,
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp)
                                        .testTag("empty_state_create_trip_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create New Trip")
                                }
                                OutlinedButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Currency: ${uiState.appCurrency}")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val travelerNames = (uiState.activeTripTravelers.map { it.name } + activeTrip.memberList()).distinct()

            item {
                ActiveTripHeroCard(
                    trip = activeTrip,
                    travelers = travelerNames,
                    financials = financials,
                    appCurrency = appCurrency,
                    onOpenTripOverview = { tab -> onOpenTripOverview(activeTrip.id, tab) },
                    onRequestAddDeposit = onRequestAddDeposit,
                    onRequestAddExpense = onRequestAddExpense,
                    onRequestManageTravelers = onRequestManageTravelers,
                    onRequestDeleteTrip = { onRequestDeleteTrip(activeTrip) }
                )
            }

            // 4. Bento Summary Grid: Deposits Pool + Detailed PDF & CSV Data Export
            item {
                DashboardBentoGrid(
                    financials = financials,
                    appCurrency = appCurrency,
                    depositsCount = uiState.activeTripDeposits.size,
                    expensesCount = uiState.activeTripExpenses.size,
                    travelersCount = travelerNames.size,
                    onOpenDeposits = { onOpenTripOverview(activeTrip.id, TripOverviewTab.DEPOSITS) },
                    onOpenExportTab = { onOpenTripOverview(activeTrip.id, TripOverviewTab.EXPORT_REPORT) },
                    onQuickExportPdf = onExportPdf,
                    onQuickExportCsv = onExportCsv
                )
            }
        }

        // 5. All Saved Trips Header + "+ New Trip"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Trips (${uiState.trips.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onRequestCreateTrip,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("create_new_trip_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create New Trip", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Trip")
                }
            }
        }

        items(uiState.trips, key = { it.id }) { trip ->
            val tripFin = uiState.allTripsFinancialMap[trip.id]
            val isSelected = trip.id == activeTrip?.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSwitchActiveTrip(trip.id)
                        onOpenTripOverview(trip.id, TripOverviewTab.DEPOSITS)
                    }
                    .testTag("trip_card_${trip.id}"),
                shape = RoundedCornerShape(tokens.cardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else tokens.cardBorderWidth,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else tokens.cardBorderColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = trip.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (trip.destination.isNotBlank() || trip.startDate.isNotBlank()) {
                            Text(
                                text = listOf(trip.destination, trip.startDate, trip.endDate)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (tripFin != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Deposited: ${
                                    ExpenseCalculator.formatCurrency(
                                        tripFin.totalDepositedInBase,
                                        appCurrency
                                    )
                                } • Spent: ${
                                    ExpenseCalculator.formatCurrency(
                                        tripFin.totalSpentInBase,
                                        appCurrency
                                    )
                                }",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { onRequestDeleteTrip(trip) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_trip_card_button_${trip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Trip ${trip.title}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveTripHeroCard(
    trip: TripEntity,
    travelers: List<String>,
    financials: TripFinancialSummary,
    appCurrency: String,
    onOpenTripOverview: (TripOverviewTab) -> Unit,
    onRequestAddDeposit: () -> Unit,
    onRequestAddExpense: () -> Unit,
    onRequestManageTravelers: () -> Unit,
    onRequestDeleteTrip: () -> Unit
) {
    val tokens = LocalTripDesignTokens.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_trip_hero_card"),
        shape = RoundedCornerShape(tokens.cardCornerRadius + 2.dp),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.heroGradientBrush)
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (trip.destination.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "🌍 ${trip.destination.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = trip.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        val dateSummary = listOf(trip.startDate, trip.endDate)
                            .filter { it.isNotBlank() }
                            .joinToString(" – ")
                        val durationLabel = TripDateUtils.formatDurationLabel(trip.daysCount)
                        Text(
                            text = if (dateSummary.isNotBlank()) {
                                "$dateSummary • $durationLabel"
                            } else {
                                "$durationLabel Trip"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onRequestDeleteTrip,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hero_delete_trip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Active Trip",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Group Traveler Avatar Pills + "Manage Travelers (Add/Delete)" button
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    travelers.forEach { member ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, tokens.cardBorderColor),
                            modifier = Modifier.clickable { onRequestManageTravelers() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = member,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clickable { onRequestManageTravelers() }
                            .testTag("hero_manage_travelers_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Add or Delete Travelers",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Travelers (+/-)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Deposits Pool vs Expenditure Summary Box
                Surface(
                    shape = RoundedCornerShape(tokens.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, tokens.cardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL DEPOSITED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = ExpenseCalculator.formatCurrency(
                                        financials.totalDepositedInBase,
                                        appCurrency
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.positiveBalanceColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "TOTAL SPENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = ExpenseCalculator.formatCurrency(
                                        financials.totalSpentInBase,
                                        appCurrency
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "POOL BALANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = ExpenseCalculator.formatCurrency(
                                        financials.remainingPoolBalance,
                                        appCurrency
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (financials.remainingPoolBalance >= 0.0) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        tokens.warningAccentColor
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (financials.totalBudget > 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { financials.budgetProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Action Row: + Deposit | + Expense | Overview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRequestAddDeposit,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_add_deposit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("+ Deposit", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = onRequestAddExpense,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_add_expense_button")
                    ) {
                        Text("+ Expense", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    FilledTonalButton(
                        onClick = { onOpenTripOverview(TripOverviewTab.DEPOSITS) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_open_workspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Open Trip Overview",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Details", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBentoGrid(
    financials: TripFinancialSummary,
    appCurrency: String,
    depositsCount: Int,
    expensesCount: Int,
    travelersCount: Int,
    onOpenDeposits: () -> Unit,
    onOpenExportTab: () -> Unit,
    onQuickExportPdf: () -> Unit,
    onQuickExportCsv: () -> Unit
) {
    val tokens = LocalTripDesignTokens.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Bento Card: Deposits by Name & Member Balances
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onOpenDeposits() }
                .testTag("bento_deposits_card"),
            shape = RoundedCornerShape(tokens.cardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Deposits by Name",
                        tint = tokens.positiveBalanceColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEPOSITS BY NAME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = ExpenseCalculator.formatCurrency(financials.totalDepositedInBase, appCurrency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.positiveBalanceColor
                )

                Text(
                    text = "$depositsCount deposit entries recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Deposits & Splits →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Right Bento Card: Detailed PDF & CSV Data Export
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onOpenExportTab() }
                .testTag("bento_export_report_card"),
            shape = RoundedCornerShape(tokens.cardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Detailed PDF & CSV Export",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PDF & CSV EXPORT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Detailed Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "$travelersCount travelers • $expensesCount expenses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuickExportPdf() }
                            .testTag("bento_quick_pdf_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PDF",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuickExportCsv() }
                            .testTag("bento_quick_csv_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CSV",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
