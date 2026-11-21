package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.domain.TripDateUtils

/**
 * Reusable Material 3 read-only date input field backed by [DatePickerDialog].
 *
 * Displays dates formatted as `DD MMM YYYY` (e.g., `20 Nov 2026`) and opens the calendar picker
 * when tapped anywhere on the field. Supports an optional [minDateMillis] lower bound to prevent
 * selecting end dates earlier than a start date, plus a one-tap Clear action inside the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,
    selectedDateMillis: Long?,
    onDateSelected: (utcMillis: Long, formattedDate: String) -> Unit,
    onClearDate: (() -> Unit)? = null,
    minDateMillis: Long? = null,
    initialDisplayMillis: Long? = null,
    placeholder: String = "DD MMM YYYY",
    fieldTestTag: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Select $label"
            ) {
                showDatePicker = true
            }
            .testTag(fieldTestTag)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, maxLines = 1) },
            placeholder = { Text(placeholder, maxLines = 1) },
            textStyle = MaterialTheme.typography.bodyMedium,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Open calendar for $label",
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                disabledBorderColor = if (value.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                disabledLabelColor = if (value.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDatePicker) {
        val minDayStart = minDateMillis?.let { TripDateUtils.normalizeToUtcDayStart(it) }
        val selectableDates = remember(minDayStart) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return minDayStart == null || utcTimeMillis >= minDayStart
                }
            }
        }
        val initialSelected = selectedDateMillis?.takeIf { minDayStart == null || it >= minDayStart }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelected,
            initialDisplayedMonthMillis = initialSelected ?: initialDisplayMillis ?: minDayStart,
            selectableDates = selectableDates
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis
                        if (picked != null) {
                            val normalized = TripDateUtils.normalizeToUtcDayStart(picked)
                            onDateSelected(normalized, TripDateUtils.formatDisplayDate(normalized))
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                    modifier = Modifier.testTag("${fieldTestTag}_confirm")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                Row {
                    if (selectedDateMillis != null && onClearDate != null) {
                        TextButton(
                            onClick = {
                                onClearDate()
                                showDatePicker = false
                            },
                            modifier = Modifier.testTag("${fieldTestTag}_clear")
                        ) {
                            Text("Clear")
                        }
                    }
                    TextButton(
                        onClick = { showDatePicker = false },
                        modifier = Modifier.testTag("${fieldTestTag}_cancel")
                    ) {
                        Text("Cancel")
                    }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
