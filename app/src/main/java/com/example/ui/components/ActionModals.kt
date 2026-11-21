package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentType
import com.example.data.local.ExpenseCategory
import com.example.data.local.SplitType
import com.example.data.local.StopCategory
import com.example.data.local.TravelerEntity
import com.example.data.local.TripEntity
import com.example.domain.ExpenseCalculator
import com.example.domain.TripDateUtils
import com.example.ui.theme.LocalTripDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripModalSheet(
    appCurrency: String,
    onDismiss: () -> Unit,
    onCreateTrip: (
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        daysCount: Int,
        totalBudget: Double,
        membersCsv: String
    ) -> Unit
) {
    // Strictly zero pre-filled values
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf("") }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var daysText by remember { mutableStateOf("") }
    var daysAutoCalculated by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf("") }
    var membersCsv by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedDays = daysText.toIntOrNull()?.takeIf { it > 0 }
    val nightsText = parsedDays?.let { TripDateUtils.calculateNightsFromDays(it).toString() }.orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Create New Trip",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "All fields start blank. Currency is set to $appCurrency (${ExpenseCalculator.currencySymbol(appCurrency)}) from Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Trip Name *") },
                placeholder = { Text("Enter trip name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_trip_title"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Destination") },
                placeholder = { Text("Enter city or region") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_trip_destination"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatePickerField(
                    label = "Start Date",
                    value = startDate,
                    selectedDateMillis = startDateMillis,
                    onDateSelected = { millis, formatted ->
                        startDateMillis = millis
                        startDate = formatted
                        val currentEnd = endDateMillis
                        if (currentEnd != null) {
                            val effectiveEnd = if (currentEnd < millis) {
                                endDateMillis = millis
                                endDate = formatted
                                millis
                            } else {
                                currentEnd
                            }
                            daysText = TripDateUtils.calculateInclusiveDays(millis, effectiveEnd).toString()
                            daysAutoCalculated = true
                        }
                    },
                    onClearDate = {
                        startDateMillis = null
                        startDate = ""
                        daysAutoCalculated = false
                    },
                    fieldTestTag = "input_trip_start_date",
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "End Date",
                    value = endDate,
                    selectedDateMillis = endDateMillis,
                    minDateMillis = startDateMillis,
                    initialDisplayMillis = startDateMillis,
                    onDateSelected = { millis, formatted ->
                        endDateMillis = millis
                        endDate = formatted
                        val currentStart = startDateMillis
                        if (currentStart != null) {
                            daysText = TripDateUtils.calculateInclusiveDays(currentStart, millis).toString()
                            daysAutoCalculated = true
                        }
                    },
                    onClearDate = {
                        endDateMillis = null
                        endDate = ""
                        daysAutoCalculated = false
                    },
                    fieldTestTag = "input_trip_end_date",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { raw ->
                        val digitsOnly = raw.filter { it.isDigit() }
                        daysText = digitsOnly
                        daysAutoCalculated = false
                    },
                    label = { Text("Number of Days") },
                    placeholder = { Text("e.g. 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_trip_days"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = nightsText,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Number of Nights") },
                    placeholder = { Text("e.g. 4") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = "Calculated number of nights",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("output_trip_nights"),
                    singleLine = true
                )
            }
            if (parsedDays != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (daysAutoCalculated) {
                        "Auto-calculated from dates: ${TripDateUtils.formatDurationLabel(parsedDays)} (editable)"
                    } else {
                        "Trip Duration: ${TripDateUtils.formatDurationLabel(parsedDays)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("trip_duration_summary_hint")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it },
                label = { Text("Budget ($appCurrency)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_trip_budget"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = membersCsv,
                onValueChange = { membersCsv = it },
                label = { Text("Initial Travelers (Optional, comma-separated)") },
                placeholder = { Text("You can also add/delete travelers later") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_trip_members"),
                singleLine = true
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Please enter a Trip Name"
                            return@Button
                        }
                        onCreateTrip(
                            title.trim(),
                            destination.trim(),
                            startDate.trim(),
                            endDate.trim(),
                            (daysText.toIntOrNull() ?: 1).coerceAtLeast(1),
                            budgetText.toDoubleOrNull() ?: 0.0,
                            membersCsv.trim()
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_create_trip_button")
                ) {
                    Text("Save Trip")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDepositByNameModalSheet(
    appCurrency: String,
    existingTravelers: List<String>,
    onDismiss: () -> Unit,
    onAddDeposit: (
        travelerName: String,
        amount: Double,
        paymentMethod: String,
        note: String
    ) -> Unit
) {
    var travelerName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Add Deposit by Traveler Name",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Record money deposited by a traveler into the trip fund ($appCurrency).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (existingTravelers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap a Traveler Name or enter below:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    existingTravelers.forEach { name ->
                        FilterChip(
                            selected = travelerName.equals(name, ignoreCase = true),
                            onClick = {
                                travelerName = name
                                errorMessage = null
                            },
                            label = { Text(name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = travelerName,
                onValueChange = {
                    travelerName = it
                    errorMessage = null
                },
                label = { Text("Traveler Name *") },
                placeholder = { Text("Enter name of person depositing") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_deposit_traveler_name"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Deposit Amount ($appCurrency) *") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_deposit_amount"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("Payment Mode (Optional)") },
                placeholder = { Text("Cash / UPI / Bank Transfer / Card") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Deposit Note (Optional)") },
                placeholder = { Text("Advance contribution / Pool top-up") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (travelerName.isBlank()) {
                            errorMessage = "Please enter or select a Traveler Name"
                            return@Button
                        }
                        if (amt <= 0.0) {
                            errorMessage = "Please enter a valid deposit amount greater than 0"
                            return@Button
                        }
                        onAddDeposit(
                            travelerName.trim(),
                            amt,
                            paymentMethod.trim(),
                            note.trim()
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_add_deposit_button")
                ) {
                    Text("Save Deposit")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTravelersModalSheet(
    trip: TripEntity,
    travelerEntities: List<TravelerEntity>,
    onDismiss: () -> Unit,
    onAddTraveler: (name: String, phoneOrEmail: String, roleOrNote: String) -> Unit,
    onDeleteTravelerEntity: (TravelerEntity) -> Unit,
    onDeleteTravelerByName: (String) -> Unit
) {
    val tokens = LocalTripDesignTokens.current
    var name by remember { mutableStateOf("") }
    var phoneOrEmail by remember { mutableStateOf("") }
    var roleOrNote by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val allNames = remember(trip, travelerEntities) {
        (travelerEntities.map { it.name } + trip.memberList()).distinct()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Travelers Details (Add & Delete)",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage group members for ${trip.title}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Add New Traveler Form
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Traveler Name *") },
                placeholder = { Text("Enter traveler full name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_traveler_name"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = phoneOrEmail,
                    onValueChange = { phoneOrEmail = it },
                    label = { Text("Phone / Email") },
                    placeholder = { Text("Contact info") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_traveler_contact"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = roleOrNote,
                    onValueChange = { roleOrNote = it },
                    label = { Text("Role / Details") },
                    placeholder = { Text("Notes / ID") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_traveler_role"),
                    singleLine = true
                )
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter the traveler's name"
                        return@Button
                    }
                    onAddTraveler(name.trim(), phoneOrEmail.trim(), roleOrNote.trim())
                    name = ""
                    phoneOrEmail = ""
                    roleOrNote = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_add_traveler_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Traveler")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = tokens.cardBorderColor)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Current Travelers (${allNames.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (allNames.isEmpty()) {
                Text(
                    text = "No travelers added yet. Use the form above to add travelers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                allNames.forEach { memberName ->
                    val entity = travelerEntities.firstOrNull {
                        it.name.equals(memberName, ignoreCase = true)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, tokens.cardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                    val details = listOf(entity.phoneOrEmail, entity.roleOrNote)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" • ")
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .testTag("delete_traveler_button_$memberName")
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

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Done")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ConfirmDeleteTripDialog(
    tripTitle: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Confirm Delete Trip",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = "Delete Trip?", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = "Are you sure you want to permanently delete '$tripTitle' and all of its deposits, travelers, expenses, and itinerary stops?"
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_trip_button")
            ) {
                Text("Delete Trip")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseModalSheet(
    trip: TripEntity,
    appCurrency: String,
    travelers: List<String>,
    prefillTitle: String = "",
    prefillAmount: String = "",
    onDismiss: () -> Unit,
    onAddExpense: (
        title: String,
        category: ExpenseCategory,
        amount: Double,
        currency: String,
        paidByMember: String,
        splitMembers: List<String>,
        splitType: SplitType,
        customSharesCsv: String,
        notes: String
    ) -> Unit
) {
    val members = remember(trip, travelers) {
        (travelers + trip.memberList()).distinct()
    }
    var title by remember { mutableStateOf(prefillTitle) }
    var amountText by remember { mutableStateOf(prefillAmount) }
    var currency by remember { mutableStateOf(appCurrency) }
    var category by remember { mutableStateOf(ExpenseCategory.DINING) }
    var paidBy by remember {
        mutableStateOf(ExpenseCalculator.DEPOSIT_POOL_PAYER)
    }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    val selectedParticipants = remember(members) {
        mutableStateListOf<String>().apply { addAll(members) }
    }
    var customSharesCsv by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Log Trip Expense",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Expense Description *") },
                placeholder = { Text("Enter item or bill description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_expense_title"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Amount ($currency) *") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_expense_amount"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExpenseCategory.entries.filter { it != ExpenseCategory.SETTLEMENT }.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text("${cat.badgeEmoji} ${cat.label}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Paid From / Paid By", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = paidBy == ExpenseCalculator.DEPOSIT_POOL_PAYER,
                    onClick = { paidBy = ExpenseCalculator.DEPOSIT_POOL_PAYER },
                    label = { Text("🏦 ${ExpenseCalculator.DEPOSIT_POOL_PAYER}") }
                )
                members.forEach { person ->
                    FilterChip(
                        selected = paidBy == person,
                        onClick = { paidBy = person },
                        label = { Text(person) }
                    )
                }
            }

            if (members.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Split Mode", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SplitType.entries.forEach { mode ->
                        FilterChip(
                            selected = splitType == mode,
                            onClick = { splitType = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }

                if (splitType != SplitType.EQUAL) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customSharesCsv,
                        onValueChange = { customSharesCsv = it },
                        label = {
                            Text(
                                if (splitType == SplitType.PERCENTAGE) {
                                    "Custom % (Name:60,Name2:40)"
                                } else {
                                    "Exact Shares (Name:500,Name2:300)"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Split Among Travelers", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    members.forEach { person ->
                        val included = selectedParticipants.contains(person)
                        FilterChip(
                            selected = included,
                            onClick = {
                                if (included && selectedParticipants.size > 1) {
                                    selectedParticipants.remove(person)
                                } else if (!included) {
                                    selectedParticipants.add(person)
                                }
                            },
                            label = { Text(person) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Receipt / Bill Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val finalAmt = amountText.toDoubleOrNull() ?: 0.0
                        if (title.isBlank()) {
                            errorMessage = "Please enter an expense description"
                            return@Button
                        }
                        if (finalAmt <= 0.0) {
                            errorMessage = "Please enter a valid expense amount"
                            return@Button
                        }
                        onAddExpense(
                            title.trim(),
                            category,
                            finalAmt,
                            currency,
                            paidBy,
                            selectedParticipants.toList(),
                            splitType,
                            customSharesCsv,
                            notes.trim()
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_add_expense_button")
                ) {
                    Text("Save Expense")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItineraryStopModalSheet(
    trip: TripEntity,
    appCurrency: String,
    initialDay: Int,
    onDismiss: () -> Unit,
    onAddStop: (
        dayNumber: Int,
        title: String,
        category: StopCategory,
        startTime: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        estimatedCost: Double,
        notes: String
    ) -> Unit
) {
    var dayNumber by remember { mutableIntStateOf(initialDay.coerceIn(1, trip.daysCount.coerceAtLeast(1))) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(StopCategory.SIGHTSEEING) }
    var startTime by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Add Itinerary Stop",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text("Select Day", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (d in 1..trip.daysCount.coerceAtLeast(1)) {
                    FilterChip(
                        selected = dayNumber == d,
                        onClick = { dayNumber = d },
                        label = { Text("Day $d") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Activity / Stop Name *") },
                placeholder = { Text("Enter place or activity") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_stop_title"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StopCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text("${cat.badgeEmoji} ${cat.label}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Time") },
                    placeholder = { Text("e.g. 09:30 AM") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Est. Cost ($appCurrency)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Address / Location") },
                placeholder = { Text("Enter location") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_stop_location"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Please enter an Activity / Stop Name"
                            return@Button
                        }
                        val offsetLat = (dayNumber * 0.009) - 0.012
                        val offsetLng = ((title.length % 7) - 3) * 0.008
                        onAddStop(
                            dayNumber,
                            title.trim(),
                            category,
                            startTime.trim(),
                            locationName.trim(),
                            trip.centerLat + offsetLat,
                            trip.centerLng + offsetLng,
                            costText.toDoubleOrNull() ?: 0.0,
                            notes.trim()
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_add_stop_button")
                ) {
                    Text("Add to Itinerary")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDocumentModalSheet(
    onDismiss: () -> Unit,
    onAddDocument: (
        title: String,
        docType: DocumentType,
        referenceCode: String,
        provider: String,
        validDate: String,
        holderName: String,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf(DocumentType.BOARDING_PASS) }
    var referenceCode by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var validDate by remember { mutableStateOf("") }
    var validDateMillis by remember { mutableStateOf<Long?>(null) }
    var holderName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Add Travel Document",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DocumentType.entries.forEach { dt ->
                    FilterChip(
                        selected = docType == dt,
                        onClick = { docType = dt },
                        label = { Text("${dt.badgeEmoji} ${dt.label}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Document Title *") },
                placeholder = { Text("Enter document title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_doc_title"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = referenceCode,
                onValueChange = { referenceCode = it },
                label = { Text("Booking Reference / PNR / Confirmation #") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_doc_reference"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Provider / Airline") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                DatePickerField(
                    label = "Valid Date",
                    value = validDate,
                    selectedDateMillis = validDateMillis,
                    onDateSelected = { millis, formatted ->
                        validDateMillis = millis
                        validDate = formatted
                    },
                    onClearDate = {
                        validDateMillis = null
                        validDate = ""
                    },
                    fieldTestTag = "input_doc_valid_date",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = holderName,
                onValueChange = { holderName = it },
                label = { Text("Traveler / Holder Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Please enter a Document Title"
                            return@Button
                        }
                        onAddDocument(
                            title.trim(),
                            docType,
                            referenceCode.trim(),
                            provider.trim(),
                            validDate.trim(),
                            holderName.trim(),
                            notes.trim()
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_add_document_button")
                ) {
                    Text("Save to Vault")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

