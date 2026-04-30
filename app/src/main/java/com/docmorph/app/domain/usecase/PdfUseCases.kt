package com.docmorph.app.domain.usecase

import android.net.Uri
import com.docmorph.app.data.model.Annotation
import com.docmorph.app.data.model.PdfDocument
import com.docmorph.app.data.model.RecentFileEntity
import com.docmorph.app.data.repository.PdfRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ─── Open / Metadata ─────────────────────────────────────────────────────────

/**
 * Loads document metadata and records the file in the recent-files list.
 */
class OpenPdfUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(uri: Uri): PdfDocument {
        val document = repository.loadDocumentMetadata(uri)
        repository.addRecentFile(document)
        return document
    }
}

// ─── Recent Files ─────────────────────────────────────────────────────────────

class GetRecentFilesUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    operator fun invoke(): Flow<List<RecentFileEntity>> = repository.getRecentFiles()
}

class RemoveRecentFileUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(entity: RecentFileEntity) = repository.removeRecentFile(entity)
}

// ─── Save / Export ────────────────────────────────────────────────────────────

class SavePdfUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(
        sourceUri: Uri,
        annotations: List<Annotation>,
        destUri: Uri? = null
    ): Uri = repository.savePdf(sourceUri, annotations, destUri)
}

class ExportPdfUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    /** Export as PNG or JPEG image. */
    suspend fun asImage(
        sourceUri: Uri,
        pageIndex: Int,
        format: String,
        destUri: Uri
    ) = repository.exportPageAsImage(sourceUri, pageIndex, format, destUri)

    /** Export extracted plain text. */
    suspend fun asText(sourceUri: Uri, destUri: Uri) =
        repository.exportAsText(sourceUri, destUri)
}

// ─── Page Management ──────────────────────────────────────────────────────────

class DeletePageUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(sourceUri: Uri, pageIndex: Int, destUri: Uri): Uri =
        repository.deletePage(sourceUri, pageIndex, destUri)
}

class RotatePageUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(
        sourceUri: Uri,
        pageIndex: Int,
        degrees: Int,
        destUri: Uri
    ): Uri = repository.rotatePage(sourceUri, pageIndex, degrees, destUri)
}
