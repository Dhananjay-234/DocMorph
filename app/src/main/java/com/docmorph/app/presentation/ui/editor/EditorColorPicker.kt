package com.docmorph.app.presentation.ui.editor

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.docmorph.app.presentation.ui.components.ColorPickerDialog

/**
 * Stateful wrapper that connects the [ColorPickerDialog] to the editor's ViewModel.
 * Rendered as an overlay on top of [EditorScreen] when the colour swatch is tapped.
 *
 * Usage: call this composable inside the same scope as [EditorScreen].
 */
@Composable
fun EditorColorPickerOverlay(
    isVisible: Boolean,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        ColorPickerDialog(
            currentColor    = currentColor,
            onColorSelected = { color ->
                onColorSelected(color)
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}
