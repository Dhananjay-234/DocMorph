package com.docmorph.app.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * A small dialog with a [Slider] that lets the user pick a stroke width
 * between 1 dp and 20 dp for the Draw / Shape tools.
 */
@Composable
fun StrokeWidthDialog(
    currentWidth: Float,
    onWidthSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var width by remember(currentWidth) { mutableFloatStateOf(currentWidth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stroke Width") },
        text  = {
            Column {
                Slider(
                    value         = width,
                    onValueChange = { width = it },
                    valueRange    = 1f..20f,
                    steps         = 18,
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("1 pt", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text  = "%.0f pt".format(width),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("20 pt", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onWidthSelected(width); onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Dialog that lets the user change the font size for [Annotation.TextBox] entries.
 */
@Composable
fun FontSizeDialog(
    currentSize: Float,
    onSizeSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var size by remember(currentSize) { mutableFloatStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Font Size") },
        text  = {
            Column {
                Slider(
                    value         = size,
                    onValueChange = { size = it },
                    valueRange    = 8f..72f,
                    steps         = 63,
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("8 sp", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text  = "%.0f sp".format(size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("72 sp", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSizeSelected(size); onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
