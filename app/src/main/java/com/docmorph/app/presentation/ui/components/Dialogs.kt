package com.docmorph.app.presentation.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// ─── Text Input Dialog ────────────────────────────────────────────────────────

/**
 * Small dialog that captures text for a [Annotation.TextBox].
 * Appears after the user taps the canvas with the Text tool selected.
 */
@Composable
fun TextInputDialog(
    position: Offset,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Add Text") },
        text    = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                label         = { Text("Enter text") },
                modifier      = Modifier.fillMaxWidth(),
                maxLines      = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm(text) })
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─── Export Dialog ────────────────────────────────────────────────────────────

/**
 * Lets the user choose an export format: PDF, PNG, JPEG, or TXT.
 * The actual file-creation picker is launched per selection.
 */
@Composable
fun ExportDialog(
    uri: Uri,
    pageIndex: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Export As", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                ExportOption(
                    icon  = Icons.Default.PictureAsPdf,
                    label = "PDF Document",
                    desc  = "Save as PDF file",
                    tint  = Color(0xFFE53935)
                ) { onDismiss() /* TODO: wire SAF create + save */ }

                ExportOption(
                    icon  = Icons.Default.Image,
                    label = "PNG Image",
                    desc  = "Current page as PNG",
                    tint  = Color(0xFF43A047)
                ) { onDismiss() }

                ExportOption(
                    icon  = Icons.Default.Photo,
                    label = "JPEG Image",
                    desc  = "Current page as JPEG",
                    tint  = Color(0xFF1E88E5)
                ) { onDismiss() }

                ExportOption(
                    icon  = Icons.Default.TextSnippet,
                    label = "Plain Text",
                    desc  = "Extract all text as TXT",
                    tint  = Color(0xFF8E24AA)
                ) { onDismiss() }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun ExportOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    desc: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(tint.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(desc,  style = MaterialTheme.typography.labelSmall,
                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
    }
}

// ─── Page Jump Dialog ─────────────────────────────────────────────────────────

@Composable
fun PageJumpDialog(
    totalPages: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val pageNum = input.toIntOrNull()
    val valid   = pageNum != null && pageNum in 1..totalPages

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Go to Page") },
        text    = {
            OutlinedTextField(
                value         = input,
                onValueChange = { input = it.filter { c -> c.isDigit() } },
                label         = { Text("Page (1 – $totalPages)") },
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = {
                    if (valid) onJump(pageNum!! - 1)
                }),
                isError       = input.isNotEmpty() && !valid,
                singleLine    = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onJump(pageNum!! - 1) }, enabled = valid) {
                Text("Go")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─── Color Picker Dialog ──────────────────────────────────────────────────────

private val presetColors = listOf(
    Color.Black,
    Color.White,
    Color(0xFFE53935), // Red
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF3F51B5), // Indigo
    Color(0xFF2196F3), // Blue
    Color(0xFF00BCD4), // Cyan
    Color(0xFF4CAF50), // Green
    Color(0xFF8BC34A), // Light green
    Color(0xFFFFEB3B), // Yellow
    Color(0xFFFF9800), // Orange
    Color(0xFF795548), // Brown
    Color(0xFF607D8B), // Blue grey
    Color(0xFF9E9E9E), // Grey
    Color(0xFF212121)  // Dark
)

@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Pick a Colour", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    items(presetColors) { color ->
                        val isSelected = color == currentColor
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else            Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(color) }
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint     = if (color == Color.White) Color.Black else Color.White,
                                    modifier = Modifier.align(Alignment.Center).size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}
