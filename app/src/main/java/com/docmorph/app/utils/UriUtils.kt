package com.docmorph.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─── File size formatting ─────────────────────────────────────────────────────

/**
 * Converts a raw byte count to a human-readable string, e.g. "2.4 MB".
 */
fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024L                -> "$bytes B"
    bytes < 1_048_576L            -> "%.1f KB".format(bytes / 1_024f)
    bytes < 1_073_741_824L        -> "%.1f MB".format(bytes / 1_048_576f)
    else                          -> "%.2f GB".format(bytes / 1_073_741_824f)
}

// ─── Timestamp formatting ─────────────────────────────────────────────────────

/**
 * Returns a short, friendly date string relative to now.
 * E.g. "Just now", "5 min ago", "Yesterday", "28 Apr 2026".
 */
fun formatTimestamp(epochMillis: Long): String {
    val now   = System.currentTimeMillis()
    val delta = now - epochMillis

    return when {
        delta < TimeUnit.MINUTES.toMillis(1)  -> "Just now"
        delta < TimeUnit.HOURS.toMillis(1)    -> "${TimeUnit.MILLISECONDS.toMinutes(delta)} min ago"
        delta < TimeUnit.HOURS.toMillis(24)   -> "${TimeUnit.MILLISECONDS.toHours(delta)} hr ago"
        delta < TimeUnit.DAYS.toMillis(2)     -> "Yesterday"
        delta < TimeUnit.DAYS.toMillis(7)     -> "${TimeUnit.MILLISECONDS.toDays(delta)} days ago"
        else                                  ->
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
    }
}

// ─── URI helpers ─────────────────────────────────────────────────────────────

/**
 * Resolves the display file name for any content URI.
 * Falls back to the URI's last path segment if the query fails.
 */
fun Uri.resolveDisplayName(context: Context): String {
    if (scheme == "content") {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
    }
    return lastPathSegment ?: toString()
}

/**
 * Resolves the file size in bytes for a content URI.
 * Returns 0 if the size cannot be determined.
 */
fun Uri.resolveSize(context: Context): Long {
    if (scheme == "content") {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0) return cursor.getLong(idx)
            }
        }
    }
    return 0L
}

// ─── Persistable URI permission ───────────────────────────────────────────────

/**
 * Requests persistable read permission for a SAF URI so it survives reboots.
 * Always call this when the user picks a file via [ActivityResultContracts.OpenDocument].
 */
fun Uri.takePersistableReadPermission(context: Context) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            this,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}
