package com.example.livecouth.ui.water.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.livecouth.R
import com.example.livecouth.data.local.entity.QuickAddButton
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.model.ValidationError

@Composable
fun QuickAddButtonsDialog(
    buttons: List<QuickAddButton>,
    onDismiss: () -> Unit,
    onSave: (id: Long?, name: String, amountMl: Int) -> Unit,
    onDelete: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var nameText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val nameError = InputValidation.validateName(nameText)
    val amountError = InputValidation.validateWaterAmount(amountText)

    fun beginEditing(button: QuickAddButton?) {
        editingId = button?.id
        nameText = button?.name.orEmpty()
        amountText = button?.amountMl?.toString().orEmpty()
        isEditing = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    id = when {
                        !isEditing -> R.string.quick_buttons_title
                        editingId == null -> R.string.quick_button_add
                        else -> R.string.quick_button_edit
                    },
                ),
            )
        },
        text = {
            if (isEditing) {
                Column {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = {
                            nameText = it
                        },
                        label = { Text(text = stringResource(id = R.string.quick_button_name)) },
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = nameError?.let { error ->
                            {
                                Text(
                                    text = stringResource(
                                        id = when (error) {
                                            ValidationError.REQUIRED -> R.string.validation_name_required
                                            ValidationError.TOO_LONG -> R.string.validation_name_too_long
                                            else -> R.string.validation_name_required
                                        },
                                    ),
                                )
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = InputValidation.filterIntegerInput(it)
                        },
                        label = { Text(text = stringResource(id = R.string.quick_button_amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = amountError != null,
                        supportingText = amountError?.let { error ->
                            {
                                Text(
                                    text = stringResource(
                                        id = when (error) {
                                            ValidationError.REQUIRED -> R.string.validation_water_required
                                            ValidationError.INVALID_FORMAT -> R.string.validation_water_integer
                                            else -> R.string.validation_water_range
                                        },
                                    ),
                                )
                            }
                        },
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (buttons.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.quick_button_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    buttons.forEachIndexed { index, button ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = button.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = stringResource(
                                        id = R.string.quick_button_display_amount,
                                        button.amountMl,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = { onMove(button.id, -1) },
                                enabled = index > 0,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = stringResource(id = R.string.quick_button_move_up),
                                )
                            }
                            IconButton(
                                onClick = { onMove(button.id, 1) },
                                enabled = index < buttons.lastIndex,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(id = R.string.quick_button_move_down),
                                )
                            }
                            IconButton(onClick = { beginEditing(button) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(id = R.string.quick_button_edit),
                                )
                            }
                            IconButton(onClick = { onDelete(button.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(id = R.string.quick_button_delete),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { beginEditing(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.quick_button_add))
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                TextButton(
                    enabled = nameError == null && amountError == null,
                    onClick = {
                        val amount = amountText.toIntOrNull()
                        if (nameError == null && amountError == null && amount != null) {
                            onSave(editingId, InputValidation.normalizeName(nameText), amount)
                            isEditing = false
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.quick_button_save))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.custom_dialog_cancel))
                }
            }
        },
        dismissButton = {
            if (isEditing) {
                TextButton(onClick = { isEditing = false }) {
                    Text(text = stringResource(id = R.string.custom_dialog_cancel))
                }
            }
        },
    )
}
