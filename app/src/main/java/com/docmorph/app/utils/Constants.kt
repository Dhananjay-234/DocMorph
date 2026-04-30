package com.docmorph.app.utils

/**
 * App-wide constants — single source of truth for magic strings and limits.
 * No instances; access via the object directly.
 */
object Constants {

    // ─── MIME types ──────────────────────────────────────────────────────────
    const val MIME_PDF  = "application/pdf"
    const val MIME_PNG  = "image/png"
    const val MIME_JPEG = "image/jpeg"
    const val MIME_TEXT = "text/plain"

    // ─── Export format keys ──────────────────────────────────────────────────
    const val FORMAT_PDF  = "pdf"
    const val FORMAT_PNG  = "png"
    const val FORMAT_JPEG = "jpg"
    const val FORMAT_TXT  = "txt"

    // ─── File extensions ──────────────────────────────────────────────────────
    const val EXT_PDF  = ".pdf"
    const val EXT_PNG  = ".png"
    const val EXT_JPEG = ".jpg"
    const val EXT_TXT  = ".txt"

    // ─── Performance limits (PRD §7.4) ────────────────────────────────────────
    /** Files above this size get a "large file" warning in the UI. */
    const val LARGE_FILE_THRESHOLD_BYTES = 20 * 1024 * 1024L   // 20 MB

    /** Maximum number of recent files shown on the Home screen. */
    const val MAX_RECENT_FILES = 20

    // ─── Undo/Redo ────────────────────────────────────────────────────────────
    /** Maximum number of undo snapshots stored per editor session. */
    const val MAX_UNDO_STACK_SIZE = 50

    // ─── Navigation argument keys ─────────────────────────────────────────────
    const val NAV_ARG_ENCODED_URI = "encodedUri"
    const val NAV_ARG_START_PAGE  = "startPage"

    // ─── DataStore preferences ───────────────────────────────────────────────
    const val PREF_THEME_KEY    = "theme_mode"        // "system" | "light" | "dark"
    const val PREF_SWIPE_H_KEY  = "swipe_horizontal"  // Boolean

    // ─── Render DPI ──────────────────────────────────────────────────────────
    const val THUMBNAIL_DPI   = 72f
    const val EXPORT_IMAGE_DPI = 150f

    // ─── FileProvider authority ───────────────────────────────────────────────
    const val FILE_PROVIDER_AUTHORITY = "com.docmorph.app.fileprovider"
}
