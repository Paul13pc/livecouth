package com.example.livecouth.ui.water.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import com.example.livecouth.R

@Composable
fun CustomAmountDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.custom_dialog_title)) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() }
                    isError = false
                },
                label = { Text(text = stringResource(id = R.string.custom_dialog_label)) },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toIntOrNull()
                    if (amount != null && amount > 0) {
                        onConfirm(amount)
                    } else {
                        isError = true
                    }
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
