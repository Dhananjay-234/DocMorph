package com.docmorph.app.data.repository

import android.net.Uri
import com.docmorph.app.data.model.Annotation
import com.docmorph.app.data.model.PdfDocument
import com.docmorph.app.data.model.RecentFileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all PDF-related data operations.
 * Implemented by [PdfRepositoryImpl].
 */
interface PdfRepository {

    // ─── Recent Files ───────────────────────────────────────────────────────

    /** Emits the 20 most recently opened PDFs. */
    fun getRecentFiles(): Flow<List<RecentFileEntity>>

    /** Persists or updates a recently opened file entry. */
    suspend fun addRecentFile(document: PdfDocument)

    /** Removes a single recent file record. */
    suspend fun removeRecentFile(entity: RecentFileEntity)

    // ─── Document Metadata ──────────────────────────────────────────────────

    /**
     * Reads basic metadata from [uri] (name, size, page-count) without fully
     * rendering the PDF — keeps open time under 2 s for ≤ 10 MB files (PRD §7.4).
     */
    suspend fun loadDocumentMetadata(uri: Uri): PdfDocument

    // ─── Save / Export ──────────────────────────────────────────────────────

    /**
     * Flattens [annotations] onto the source PDF and writes the result.
     *
     * @param sourceUri   Original PDF content URI.
     * @param annotations All overlay annotations to embed.
     * @param destUri     Destination URI (null = overwrite source).
     * @return The URI of the saved file.
     */
    suspend fun savePdf(
        sourceUri: Uri,
        annotations: List<Annotation>,
        destUri: Uri? = null
    ): Uri

    /**
     * Exports a single page as a bitmap (PNG or JPEG).
     *
     * @param sourceUri  Original PDF content URI.
     * @param pageIndex  Zero-based page index.
     * @param format     "png" or "jpg".
     * @param destUri    Where to write the image.
     */
    suspend fun exportPageAsImage(
        sourceUri: Uri,
        pageIndex: Int,
        format: String,
        destUri: Uri
    )

    /**
     * Extracts all visible text from the PDF and writes it to [destUri].
     */
    suspend fun exportAsText(sourceUri: Uri, destUri: Uri)

    // ─── Page Operations ────────────────────────────────────────────────────

    /**
     * Deletes the page at [pageIndex] and returns the URI of the new PDF.
     * Always writes to a new file so the original is preserved.
     */
    suspend fun deletePage(sourceUri: Uri, pageIndex: Int, destUri: Uri): Uri

    /**
     * Rotates the page at [pageIndex] by [degrees] (90, 180, or 270) and
     * returns the URI of the new PDF.
     */
    suspend fun rotatePage(sourceUri: Uri, pageIndex: Int, degrees: Int, destUri: Uri): Uri
}
