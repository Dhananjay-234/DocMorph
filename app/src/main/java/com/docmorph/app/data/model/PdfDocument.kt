package com.docmorph.app.data.model

import android.net.Uri

/**
 * Core domain model representing an opened PDF document.
 *
 * @param uri         Content URI pointing to the PDF on the device.
 * @param name        Display file name (e.g. "report.pdf").
 * @param sizeBytes   File size in bytes; used for "< 2 s open" perf check (PRD §7.4).
 * @param pageCount   Total page count, populated after the PDF is loaded.
 * @param lastOpened  Unix epoch millis of the last open time (recent files list).
 */
data class PdfDocument(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int = 0,
    val lastOpened: Long = System.currentTimeMillis()
)
