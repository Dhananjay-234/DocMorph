package com.docmorph.app.presentation.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docmorph.app.utils.formatFileSize
import com.docmorph.app.utils.formatTimestamp
import com.docmorph.app.utils.resolveDisplayName
import com.docmorph.app.utils.resolveSize

/**
 * Displays read-only file metadata in an [AlertDialog]:
 * name, URI, size, and page count.
 */
@Composable
fun PropertiesDialog(
    uri: Uri,
    pageCount: Int,
    lastOpened: Long,
    onDismiss: () -> Unit
) {
    val context  = LocalContext.current
    val name     = uri.resolveDisplayName(context)
    val size     = uri.resolveSize(context)

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("File Properties") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PropertyRow(label = "Name",        value = name)
                PropertyRow(label = "Size",        value = formatFileSize(size))
                PropertyRow(label = "Pages",       value = pageCount.toString())
                PropertyRow(label = "Last Opened", value = formatTimestamp(lastOpened))
                PropertyRow(label = "URI",         value = uri.toString(), monospace = true)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PropertyRow(label: String, value: String, monospace: Boolean = false) {
    Column {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text  = value,
            style = if (monospace) MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ) else MaterialTheme.typography.bodyMedium
        )
    }
}
