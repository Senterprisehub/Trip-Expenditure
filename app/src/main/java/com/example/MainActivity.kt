package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.TripEntity
import com.example.data.local.TripExpenditureDatabase
import com.example.data.repository.TripRepository
import com.example.domain.TripExportDataBundle
import com.example.domain.TripReportExporter
import com.example.ui.components.AddDepositByNameModalSheet
import com.example.ui.components.AddDocumentModalSheet
import com.example.ui.components.AddExpenseModalSheet
import com.example.ui.components.AddItineraryStopModalSheet
import com.example.ui.components.ConfirmDeleteTripDialog
import com.example.ui.components.CreateTripModalSheet
import com.example.ui.components.ManageTravelersModalSheet
import com.example.ui.components.ThemeVisualShowcaseBottomSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripOverviewScreen
import com.example.ui.theme.LocalTripDesignTokens
import com.example.ui.theme.TripExpenditureTheme
import com.example.ui.viewmodel.AppScreenDestination
import com.example.ui.viewmodel.TripExpenditureViewModel
import com.example.ui.viewmodel.TripOverviewTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContext = LocalContext.current.applicationContext
            val viewModel: TripExpenditureViewModel = viewModel(
                factory = TripExpenditureViewModel.provideFactory(appContext)
            )
            TripExpenditureApp(viewModel = viewModel)
        }
    }
}

private data class BottomNavTabItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun TripExpenditureApp(viewModel: TripExpenditureViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Modal state holders
    var showCreateTripModal by remember { mutableStateOf(false) }
    var showAddDepositModal by remember { mutableStateOf(false) }
    var showManageTravelersModal by remember { mutableStateOf(false) }
    var showAddExpenseModal by remember { mutableStateOf(false) }
    var expensePrefillTitle by remember { mutableStateOf("") }
    var expensePrefillAmount by remember { mutableStateOf("") }
    var showAddStopModal by remember { mutableStateOf(false) }
    var showAddDocumentModal by remember { mutableStateOf(false) }
    var tripPendingDelete by remember { mutableStateOf<TripEntity?>(null) }

    // System Document Savers for PDF & CSV
    val createPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val bundle = viewModel.buildActiveTripExportBundle()
            if (bundle != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        TripReportExporter.writeDetailedPdfToStream(bundle, out)
                    }
                    viewModel.recordExportConfirmation(
                        "Saved detailed PDF report (${TripReportExporter.defaultPdfFileName(bundle.trip.title)})"
                    )
                } catch (e: Exception) {
                    viewModel.showStatusMessage("Failed to write PDF: ${e.localizedMessage}")
                }
            }
        }
    }

    val createCsvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val bundle = viewModel.buildActiveTripExportBundle()
            if (bundle != null) {
                try {
                    val csv = TripReportExporter.generateDetailedCsv(bundle)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.recordExportConfirmation(
                        "Saved detailed CSV report (${TripReportExporter.defaultCsvFileName(bundle.trip.title)})"
                    )
                } catch (e: Exception) {
                    viewModel.showStatusMessage("Failed to write CSV: ${e.localizedMessage}")
                }
            }
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    kotlinx.coroutines.runBlocking {
                        com.example.domain.TripBackupManager.exportCompressedBackup(
                            context,
                            TripRepository(TripExpenditureDatabase.getInstance(context).dao()),
                            out as java.io.FileOutputStream
                        )
                    }
                }
                viewModel.recordExportConfirmation("Successfully exported compressed backup archive")
            } catch (e: Exception) {
                viewModel.showStatusMessage("Failed to export backup: ${e.localizedMessage}")
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    kotlinx.coroutines.runBlocking {
                        com.example.domain.TripBackupManager.importCompressedBackup(
                            context,
                            TripRepository(TripExpenditureDatabase.getInstance(context).dao()),
                            input
                        )
                    }
                }
                viewModel.showStatusMessage("Successfully imported backup archive")
            } catch (e: Exception) {
                viewModel.showStatusMessage("Failed to import backup: ${e.localizedMessage}")
            }
        }
    }

    val handleSavePdf: (TripExportDataBundle?) -> Unit = { exportBundle ->
        val bundle = exportBundle ?: viewModel.buildActiveTripExportBundle()
        if (bundle == null) {
            viewModel.showStatusMessage("Create or select a trip first to export PDF")
        } else {
            try {
                val cachedFile = TripReportExporter.exportPdfToCacheFile(context, bundle)
                viewModel.recordExportConfirmation(
                    "Generated PDF (${cachedFile.name}) — ready to save or share"
                )
                try {
                    createPdfDocumentLauncher.launch(cachedFile.name)
                } catch (_: Exception) {
                    // Fallback if system document picker is unavailable
                }
            } catch (e: Exception) {
                viewModel.showStatusMessage("Error generating PDF: ${e.localizedMessage}")
            }
        }
    }

    val handleSharePdf: (TripExportDataBundle?) -> Unit = { exportBundle ->
        val bundle = exportBundle ?: viewModel.buildActiveTripExportBundle()
        if (bundle == null) {
            viewModel.showStatusMessage("Create or select a trip first to share PDF")
        } else {
            try {
                val pdfFile = TripReportExporter.exportPdfToCacheFile(context, bundle)
                viewModel.recordExportConfirmation("Exported PDF report: ${pdfFile.name}")
                TripReportExporter.shareExportedFile(
                    context = context,
                    file = pdfFile,
                    mimeType = "application/pdf",
                    chooserTitle = "Share Detailed Trip PDF Report"
                )
            } catch (e: Exception) {
                viewModel.showStatusMessage("Unable to share PDF: ${e.localizedMessage}")
            }
        }
    }

    val handleSaveCsv: (TripExportDataBundle?) -> Unit = { exportBundle ->
        val bundle = exportBundle ?: viewModel.buildActiveTripExportBundle()
        if (bundle == null) {
            viewModel.showStatusMessage("Create or select a trip first to export CSV")
        } else {
            try {
                val cachedFile = TripReportExporter.exportCsvToCacheFile(context, bundle)
                viewModel.recordExportConfirmation(
                    "Generated CSV (${cachedFile.name}) — ready to save or share"
                )
                try {
                    createCsvDocumentLauncher.launch(cachedFile.name)
                } catch (_: Exception) {
                    // Fallback if system document picker is unavailable
                }
            } catch (e: Exception) {
                viewModel.showStatusMessage("Error generating CSV: ${e.localizedMessage}")
            }
        }
    }

    val handleShareCsv: (TripExportDataBundle?) -> Unit = { exportBundle ->
        val bundle = exportBundle ?: viewModel.buildActiveTripExportBundle()
        if (bundle == null) {
            viewModel.showStatusMessage("Create or select a trip first to share CSV")
        } else {
            try {
                val csvFile = TripReportExporter.exportCsvToCacheFile(context, bundle)
                viewModel.recordExportConfirmation("Exported CSV report: ${csvFile.name}")
                TripReportExporter.shareExportedFile(
                    context = context,
                    file = csvFile,
                    mimeType = "text/csv",
                    chooserTitle = "Share Detailed Trip CSV Report"
                )
            } catch (e: Exception) {
                viewModel.showStatusMessage("Unable to share CSV: ${e.localizedMessage}")
            }
        }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearStatusBanner()
        }
    }

    TripExpenditureTheme(themeOption = uiState.activeTheme) {
        val tokens = LocalTripDesignTokens.current
        val navItems = listOf(
            BottomNavTabItem(
                label = "Home",
                icon = Icons.Default.Home,
                testTag = "nav_dashboard",
                selected = uiState.currentScreen == AppScreenDestination.DASHBOARD,
                onClick = { viewModel.navigateBackToDashboard() }
            ),
            BottomNavTabItem(
                label = "Deposits",
                icon = Icons.Default.Savings,
                testTag = "nav_deposits",
                selected = uiState.currentScreen == AppScreenDestination.TRIP_OVERVIEW &&
                    uiState.selectedTripTab == TripOverviewTab.DEPOSITS,
                onClick = { viewModel.navigateToTripOverview(TripOverviewTab.DEPOSITS) }
            ),
            BottomNavTabItem(
                label = "Expenses",
                icon = Icons.Default.ReceiptLong,
                testTag = "nav_splits",
                selected = uiState.currentScreen == AppScreenDestination.TRIP_OVERVIEW &&
                    uiState.selectedTripTab == TripOverviewTab.EXPENSES,
                onClick = { viewModel.navigateToTripOverview(TripOverviewTab.EXPENSES) }
            ),
            BottomNavTabItem(
                label = "Travelers",
                icon = Icons.Default.Groups,
                testTag = "nav_travelers",
                selected = uiState.currentScreen == AppScreenDestination.TRIP_OVERVIEW &&
                    uiState.selectedTripTab == TripOverviewTab.TRAVELERS,
                onClick = { viewModel.navigateToTripOverview(TripOverviewTab.TRAVELERS) }
            ),
            BottomNavTabItem(
                label = "Export",
                icon = Icons.Default.PictureAsPdf,
                testTag = "nav_export",
                selected = uiState.currentScreen == AppScreenDestination.TRIP_OVERVIEW &&
                    uiState.selectedTripTab == TripOverviewTab.EXPORT_REPORT,
                onClick = { viewModel.navigateToTripOverview(TripOverviewTab.EXPORT_REPORT) }
            ),
            BottomNavTabItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                testTag = "nav_settings",
                selected = uiState.currentScreen == AppScreenDestination.SETTINGS,
                onClick = { viewModel.navigateToSettings() }
            )
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    border = BorderStroke(1.dp, tokens.cardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_bottom_navigation")
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        HorizontalDivider(color = tokens.cardBorderColor.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { item.onClick() }
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                        .testTag(item.testTag),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 42.dp, height = 26.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (item.selected) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    Color.Transparent
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = if (item.selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            lineHeight = 12.sp
                                        ),
                                        fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (item.selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    when (uiState.currentScreen) {
                        AppScreenDestination.DASHBOARD -> {
                            DashboardScreen(
                                uiState = uiState,
                                onSelectTheme = viewModel::selectTheme,
                                onOpenThemeShowcase = { viewModel.setThemeShowcaseModalVisible(true) },
                                onOpenSettings = viewModel::navigateToSettings,
                                onOpenTripOverview = viewModel::selectTripAndOpenOverview,
                                onSwitchActiveTrip = viewModel::setActiveTrip,
                                onRequestCreateTrip = { showCreateTripModal = true },
                                onRequestDeleteTrip = { trip -> tripPendingDelete = trip },
                                onRequestAddDeposit = { showAddDepositModal = true },
                                onRequestAddExpense = {
                                    expensePrefillTitle = ""
                                    expensePrefillAmount = ""
                                    showAddExpenseModal = true
                                },
                                onRequestManageTravelers = { showManageTravelersModal = true },
                                onExportPdf = {
                                    viewModel.navigateToTripOverview(TripOverviewTab.EXPORT_REPORT)
                                    handleSavePdf(null)
                                },
                                onExportCsv = {
                                    viewModel.navigateToTripOverview(TripOverviewTab.EXPORT_REPORT)
                                    handleSaveCsv(null)
                                }
                            )
                        }

                        AppScreenDestination.TRIP_OVERVIEW -> {
                            TripOverviewScreen(
                                uiState = uiState,
                                onBackToDashboard = viewModel::navigateBackToDashboard,
                                onSelectTheme = viewModel::selectTheme,
                                onOpenThemeShowcase = { viewModel.setThemeShowcaseModalVisible(true) },
                                onSelectTab = viewModel::selectOverviewTab,
                                onSelectDay = viewModel::selectDayNumber,
                                onSelectVoterIdentity = viewModel::selectVoterIdentity,
                                onRequestDeleteTrip = { trip -> tripPendingDelete = trip },
                                onRequestAddDeposit = { showAddDepositModal = true },
                                onRequestManageTravelers = { showManageTravelersModal = true },
                                onRequestAddStop = { showAddStopModal = true },
                                onRequestAddExpense = { title, amt ->
                                    expensePrefillTitle = title
                                    expensePrefillAmount = amt
                                    showAddExpenseModal = true
                                },
                                onRequestAddDocument = { showAddDocumentModal = true },
                                onDeleteDeposit = viewModel::deleteDeposit,
                                onDeleteTravelerEntity = viewModel::deleteTraveler,
                                onDeleteTravelerByName = viewModel::deleteTravelerByName,
                                onToggleVoteStop = viewModel::toggleVoteOnStop,
                                onToggleVisitedStop = viewModel::toggleStopVisited,
                                onDeleteStop = viewModel::deleteStop,
                                onSettleTransfer = viewModel::settleTransfer,
                                onDeleteExpense = viewModel::deleteExpense,
                                onDeleteDocument = viewModel::deleteDocument,
                                onSavePdfToDevice = handleSavePdf,
                                onSharePdfFile = handleSharePdf,
                                onSaveCsvToDevice = handleSaveCsv,
                                onShareCsvFile = handleShareCsv,
                                onNotifyCopied = viewModel::recordExportConfirmation,
                                onOpenSettings = viewModel::navigateToSettings
                            )
                        }

                        AppScreenDestination.SETTINGS -> {
                            SettingsScreen(
                                uiState = uiState,
                                onBackToDashboard = viewModel::navigateBackToDashboard,
                                onSelectAppCurrency = viewModel::selectApplicationCurrency,
                                onSelectTheme = viewModel::selectTheme,
                                onOpenThemeShowcase = { viewModel.setThemeShowcaseModalVisible(true) },
                                onExportPdf = {
                                    viewModel.navigateToTripOverview(TripOverviewTab.EXPORT_REPORT)
                                    handleSavePdf(null)
                                },
                                onExportCsv = {
                                    viewModel.navigateToTripOverview(TripOverviewTab.EXPORT_REPORT)
                                    handleSaveCsv(null)
                                },
                                onExportBackup = {
                                    try {
                                        createBackupLauncher.launch(com.example.domain.TripBackupManager.defaultBackupFileName())
                                    } catch (_: Exception) {}
                                },
                                onImportBackup = {
                                    try {
                                        openBackupLauncher.launch(arrayOf("*/*"))
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                    }
                }
            }
        }

        // Visual Theme Showcase Bottom Sheet
        if (uiState.showThemeShowcaseModal) {
            ThemeVisualShowcaseBottomSheet(
                activeTheme = uiState.activeTheme,
                onSelectTheme = viewModel::selectTheme,
                onDismiss = { viewModel.setThemeShowcaseModalVisible(false) }
            )
        }

        if (showCreateTripModal) {
            CreateTripModalSheet(
                appCurrency = uiState.appCurrency,
                onDismiss = { showCreateTripModal = false },
                onCreateTrip = viewModel::createNewTrip
            )
        }

        val activeTrip = uiState.activeTrip
        val allTravelerNames = remember(activeTrip, uiState.activeTripTravelers) {
            (uiState.activeTripTravelers.map { it.name } + activeTrip?.memberList().orEmpty()).distinct()
        }

        if (showAddDepositModal && activeTrip != null) {
            AddDepositByNameModalSheet(
                appCurrency = uiState.appCurrency,
                existingTravelers = allTravelerNames,
                onDismiss = { showAddDepositModal = false },
                onAddDeposit = viewModel::addDepositByName
            )
        }

        if (showManageTravelersModal && activeTrip != null) {
            ManageTravelersModalSheet(
                trip = activeTrip,
                travelerEntities = uiState.activeTripTravelers,
                onDismiss = { showManageTravelersModal = false },
                onAddTraveler = viewModel::addTravelerWithDetails,
                onDeleteTravelerEntity = viewModel::deleteTraveler,
                onDeleteTravelerByName = viewModel::deleteTravelerByName
            )
        }

        if (showAddExpenseModal && activeTrip != null) {
            AddExpenseModalSheet(
                trip = activeTrip,
                appCurrency = uiState.appCurrency,
                travelers = allTravelerNames,
                prefillTitle = expensePrefillTitle,
                prefillAmount = expensePrefillAmount,
                onDismiss = { showAddExpenseModal = false },
                onAddExpense = viewModel::addExpense
            )
        }

        if (showAddStopModal && activeTrip != null) {
            AddItineraryStopModalSheet(
                trip = activeTrip,
                appCurrency = uiState.appCurrency,
                initialDay = uiState.selectedDayNumber,
                onDismiss = { showAddStopModal = false },
                onAddStop = viewModel::addItineraryStop
            )
        }

        if (showAddDocumentModal && activeTrip != null) {
            AddDocumentModalSheet(
                onDismiss = { showAddDocumentModal = false },
                onAddDocument = viewModel::addTravelDocument
            )
        }

        tripPendingDelete?.let { tripToDelete ->
            ConfirmDeleteTripDialog(
                tripTitle = tripToDelete.title,
                onConfirmDelete = { viewModel.deleteTrip(tripToDelete.id) },
                onDismiss = { tripPendingDelete = null }
            )
        }
    }
}
