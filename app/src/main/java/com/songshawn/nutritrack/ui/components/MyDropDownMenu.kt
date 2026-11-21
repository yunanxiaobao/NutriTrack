package com.songshawn.nutritrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.songshawn.nutritrack.theme.LightGrey

/**
 * Composable function for a dropdown menu that displays clinician IDs.
 *
 * @param ids List of clinician IDs to be displayed in the dropdown
 * @param selectedId The currently selected clinician ID
 * @param expanded Whether the dropdown menu is expanded or collapsed
 * @param onExpandedChange Callback when the dropdown's expanded state changes
 * @param onSelected Callback when a clinician ID is selected
 * @param modifier Modifier to be applied to the dropdown
 * @param menuBackgroundColor Background color of the dropdown menu
 * @param menuElevation Elevation (shadow) of the dropdown menu in dp
 * @param disabledItems Set of IDs that should be displayed as disabled (non-selectable)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicianIdDropdown(
    ids: List<String>,
    selectedId: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    menuBackgroundColor: Color = LightGrey,
    menuElevation: Int = 2,
    disabledItems: Set<String> = emptySet()
) {
    // Create a TextFieldState to store the selected ID
    val textFieldState = remember {
        TextFieldState().apply {
            if (selectedId.isNotEmpty()) {
                edit { replace(0, length, selectedId) }
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        // Custom TextField as the dropdown anchor
        MyTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = MenuAnchorType.SecondaryEditable,
                    enabled = true
                ),
            textFieldState = textFieldState,
            hint = "Clinician ID",
            leadingIcon = Icons.Filled.Person,
            trailingIcon = if (expanded)
                Icons.Filled.KeyboardArrowUp
            else
                Icons.Filled.KeyboardArrowDown,
            onTrailingClick = { onExpandedChange(!expanded) },
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
        )

        // Custom styled dropdown menu
        if (expanded) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .exposedDropdownSize()
                    .background(menuBackgroundColor)
                    .heightIn(max = 250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(menuElevation.dp)
            ) {
                // Create menu items for each ID
                ids.forEachIndexed { index, id ->
                    val isDisabled = id in disabledItems

                    // Menu item
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = id,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDisabled)
                                    Color.Gray
                                else
                                    LocalContentColor.current
                            )
                        },
                        onClick = {
                            if (!isDisabled) {
                                textFieldState.edit { replace(0, length, id) }
                                onSelected(id)
                                onExpandedChange(false)
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .then(
                                if (isDisabled)
                                    Modifier
                                else
                                    Modifier.clickable { }
                            )
                    )

                    // Add divider between items, except after the last item
                    if (index < ids.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}