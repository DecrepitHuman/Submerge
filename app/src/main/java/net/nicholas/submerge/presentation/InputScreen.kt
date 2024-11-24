package net.nicholas.submerge.presentation

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material.*

@Composable
fun InputDialog(currentValue: String, onSubmit: (String) -> Unit) {
    var inputValue by remember { mutableStateOf(currentValue) }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = { onSubmit(inputValue) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Enter new value:")

            Spacer(modifier = Modifier.height(8.dp))

            // TextField for input
            TextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                label = { Text("Input Value") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Submit when "Done" is pressed on the keyboard
                        onSubmit(inputValue)
                        focusManager.clearFocus() // Clear focus after "Done" to dismiss the keyboard
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Submit button
            TextButton(onClick = { onSubmit(inputValue) }) {
                Text("Submit")
            }
        }
    }
}