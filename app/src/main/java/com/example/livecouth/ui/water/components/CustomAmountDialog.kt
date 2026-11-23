package com.example.livecouth.ui.water.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.livecouth.R
import com.example.livecouth.domain.model.DrinkType
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.model.ValidationError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAmountDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, DrinkType, Float) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var factorText by remember { mutableStateOf(DrinkType.WATER.defaultHydrationFactor.toString()) }
    var drinkType by remember { mutableStateOf(DrinkType.WATER) }
    var isDrinkMenuExpanded by remember { mutableStateOf(false) }
    var isFactorError by remember { mutableStateOf(false) }
    val amountError = InputValidation.validateWaterAmount(amountText)
    val factor = factorText.toFloatOrNull()
    val isValidFactor = factor != null && factor.isFinite() && factor in 0f..2f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.custom_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = InputValidation.filterIntegerInput(input)
                    },
                    label = { Text(text = stringResource(id = R.string.custom_dialog_label)) },
                    singleLine = true,
                    isError = amountError != null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = isDrinkMenuExpanded,
                    onExpandedChange = { isDrinkMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = stringResource(id = drinkType.labelResId),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = stringResource(id = R.string.drink_type_label)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDrinkMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = isDrinkMenuExpanded,
                        onDismissRequest = { isDrinkMenuExpanded = false },
                    ) {
                        DrinkType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = option.labelResId)) },
                                onClick = {
                                    drinkType = option
                                    factorText = option.defaultHydrationFactor.toString()
                                    isDrinkMenuExpanded = false
                                    isFactorError = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = factorText,
                    onValueChange = { input ->
                        val filtered = input
                            .filter { it.isDigit() || it == '.' || it == ',' }
                            .replace(',', '.')
                        factorText = filtered
                        val parsedFactor = filtered.toFloatOrNull()
                        isFactorError = parsedFactor == null ||
                            !parsedFactor.isFinite() ||
                            parsedFactor !in 0f..2f
                    },
                    label = { Text(text = stringResource(id = R.string.hydration_factor)) },
                    supportingText = { Text(text = stringResource(id = R.string.hydration_factor_hint)) },
                    singleLine = true,
                    isError = isFactorError,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (isFactorError) {
                    Text(
                        text = stringResource(id = R.string.custom_dialog_invalid),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amountError == null && isValidFactor,
                onClick = {
                    val amount = amountText.toIntOrNull() ?: return@TextButton
                    val validFactor = factor ?: return@TextButton
                    onConfirm(amount, drinkType, validFactor)
                },
            ) {
                Text(text = stringResource(id = R.string.custom_dialog_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.custom_dialog_cancel))
            }
        },
    )
}
