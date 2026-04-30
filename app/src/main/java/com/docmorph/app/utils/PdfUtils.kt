package com.docmorph.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility functions for low-level PDF operations that do not belong
 * in a Repository or ViewModel.
 */
object PdfUtils {

    /**
     * Renders a single PDF page to a [Bitmap] using Android's built-in
     * [PdfRenderer]. Used for generating thumbnail previews on the Home screen.
     *
     * @param context     Application context — needed for ContentResolver.
     * @param uri         Content URI of the PDF.
     * @param pageIndex   Zero-based page index.
     * @param width       Desired bitmap width in pixels (height is auto-calculated).
     * @return            The rendered [Bitmap], or null if rendering fails.
     */
    suspend fun renderPageThumbnail(
        context: Context,
        uri: Uri,
        pageIndex: Int = 0,
        width: Int = 400
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex >= renderer.pageCount) return@runCatching null
                    renderer.openPage(pageIndex).use { page ->
                        val ratio  = width.toFloat() / page.width
                        val height = (page.height * ratio).toInt()
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // Fill white background (PDFs are transparent by default)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }.getOrNull()
    }

    /**
     * Returns the page count of a PDF identified by [uri].
     * Returns 0 if the file cannot be opened.
     */
    suspend fun getPageCount(context: Context, uri: Uri): Int =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { it.pageCount }
                } ?: 0
            }.getOrDefault(0)
        }

    /**
     * Checks whether [uri] points to a valid, openable PDF file.
     * A quick sanity-check before navigating to the viewer.
     */
    suspend fun isValidPdf(context: Context, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    // PDF magic bytes: %PDF
                    val header = ByteArray(4)
                    stream.read(header)
                    header.decodeToString() == "%PDF"
                } ?: false
            }.getOrDefault(false)
        }
}
