package com.docmorph.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.docmorph.app.data.model.Annotation
import com.docmorph.app.data.model.PdfDocument
import com.docmorph.app.data.model.RecentFileEntity
import com.docmorph.app.data.model.ShapeType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recentFileDao: RecentFileDao
) : PdfRepository {

    // ─── Recent Files ───────────────────────────────────────────────────────

    override fun getRecentFiles(): Flow<List<RecentFileEntity>> =
        recentFileDao.getAllRecent()

    override suspend fun addRecentFile(document: PdfDocument) {
        recentFileDao.upsert(
            RecentFileEntity(
                uriString  = document.uri.toString(),
                name       = document.name,
                sizeBytes  = document.sizeBytes,
                pageCount  = document.pageCount,
                lastOpened = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeRecentFile(entity: RecentFileEntity) {
        recentFileDao.delete(entity)
    }

    // ─── Metadata ───────────────────────────────────────────────────────────

    override suspend fun loadDocumentMetadata(uri: Uri): PdfDocument =
        withContext(Dispatchers.IO) {
            var name      = "document.pdf"
            var sizeBytes = 0L

            // Query ContentResolver for display name and size
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name      = cursor.getString(nameIdx)
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }

            // Use Android's built-in PdfRenderer just to get the page count
            val pageCount = runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { it.pageCount }
                } ?: 0
            }.getOrDefault(0)

            PdfDocument(uri = uri, name = name, sizeBytes = sizeBytes, pageCount = pageCount)
        }

    // ─── Save / Export ──────────────────────────────────────────────────────

    override suspend fun savePdf(
        sourceUri: Uri,
        annotations: List<Annotation>,
        destUri: Uri?
    ): Uri = withContext(Dispatchers.IO) {
        val targetUri = destUri ?: sourceUri

        context.contentResolver.openInputStream(sourceUri)!!.use { input ->
            val pdDocument = PDDocument.load(input)
            val renderer   = PDFRenderer(pdDocument)

            annotations.groupBy { it.pageIndex }.forEach { (pageIdx, pageAnnotations) ->
                if (pageIdx >= pdDocument.numberOfPages) return@forEach
                val page   = pdDocument.getPage(pageIdx)
                val height = page.mediaBox.height

                PDPageContentStream(
                    pdDocument, page,
                    PDPageContentStream.AppendMode.APPEND,
                    true
                ).use { stream ->
                    pageAnnotations.forEach { annotation ->
                        flattenAnnotation(annotation, stream, height, pdDocument)
                    }
                }
            }

            context.contentResolver.openOutputStream(targetUri)!!.use { out ->
                pdDocument.save(out)
            }
            pdDocument.close()
        }
        targetUri
    }

    private fun flattenAnnotation(
        annotation: Annotation,
        stream: PDPageContentStream,
        pageHeight: Float,
        doc: PDDocument
    ) {
        when (annotation) {
            is Annotation.TextBox -> {
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, annotation.fontSize)
                stream.newLineAtOffset(annotation.x, pageHeight - annotation.y)
                stream.showText(annotation.text)
                stream.endText()
            }
            is Annotation.Highlight -> {
                val c = annotation.color
                stream.setNonStrokingColor(c.red, c.green, c.blue)
                val x = annotation.startOffset.x
                val y = pageHeight - annotation.startOffset.y
                val w = annotation.endOffset.x - x
                val h = 14f
                stream.addRect(x, y, w, h)
                stream.fill()
            }
            is Annotation.Drawing -> {
                if (annotation.path.size < 2) return
                val c = annotation.strokeColor
                stream.setStrokingColor(c.red, c.green, c.blue)
                stream.setLineWidth(annotation.strokeWidth)
                val first = annotation.path.first()
                stream.moveTo(first.x, pageHeight - first.y)
                annotation.path.drop(1).forEach { pt ->
                    stream.lineTo(pt.x, pageHeight - pt.y)
                }
                stream.stroke()
            }
            is Annotation.Strikethrough -> {
                val c = annotation.color
                stream.setStrokingColor(c.red, c.green, c.blue)
                stream.setLineWidth(2f)
                val midY = pageHeight - (annotation.startOffset.y + annotation.endOffset.y) / 2
                stream.moveTo(annotation.startOffset.x, midY)
                stream.lineTo(annotation.endOffset.x, midY)
                stream.stroke()
            }
            is Annotation.Shape -> {
                val c = annotation.color
                stream.setStrokingColor(c.red, c.green, c.blue)
                stream.setLineWidth(annotation.strokeWidth)
                val x1 = annotation.startOffset.x
                val y1 = pageHeight - annotation.startOffset.y
                val x2 = annotation.endOffset.x
                val y2 = pageHeight - annotation.endOffset.y
                when (annotation.type) {
                    ShapeType.RECTANGLE -> {
                        stream.addRect(x1, minOf(y1, y2), Math.abs(x2 - x1), Math.abs(y1 - y2))
                        if (annotation.filled) stream.fill() else stream.stroke()
                    }
                    ShapeType.LINE -> {
                        stream.moveTo(x1, y1)
                        stream.lineTo(x2, y2)
                        stream.stroke()
                    }
                    else -> { /* Circle / Arrow — TODO in Phase 2 */ }
                }
            }
            is Annotation.Comment -> { /* Comments are metadata-only — not flattened to PDF */ }
        }
    }

    override suspend fun exportPageAsImage(
        sourceUri: Uri,
        pageIndex: Int,
        format: String,
        destUri: Uri
    ) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(sourceUri)!!.use { input ->
            val pdDocument = PDDocument.load(input)
            val renderer   = PDFRenderer(pdDocument)
            val bitmap     = renderer.renderImageWithDPI(pageIndex, 150f, android.graphics.Bitmap.Config.ARGB_8888)
            context.contentResolver.openOutputStream(destUri)!!.use { out ->
                val compressFormat = if (format == "png") Bitmap.CompressFormat.PNG
                                     else                Bitmap.CompressFormat.JPEG
                bitmap.compress(compressFormat, 95, out)
            }
            pdDocument.close()
        }
    }

    override suspend fun exportAsText(sourceUri: Uri, destUri: Uri) =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(sourceUri)!!.use { input ->
                val pdDocument = PDDocument.load(input)
                val stripper   = PDFTextStripper()
                val text       = stripper.getText(pdDocument)
                pdDocument.close()
                context.contentResolver.openOutputStream(destUri)!!.bufferedWriter().use {
                    it.write(text)
                }
            }
        }

    // ─── Page Operations ────────────────────────────────────────────────────

    override suspend fun deletePage(sourceUri: Uri, pageIndex: Int, destUri: Uri): Uri =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(sourceUri)!!.use { input ->
                val pdDocument = PDDocument.load(input)
                pdDocument.removePage(pageIndex)
                context.contentResolver.openOutputStream(destUri)!!.use { out ->
                    pdDocument.save(out)
                }
                pdDocument.close()
            }
            destUri
        }

    override suspend fun rotatePage(
        sourceUri: Uri,
        pageIndex: Int,
        degrees: Int,
        destUri: Uri
    ): Uri = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(sourceUri)!!.use { input ->
            val pdDocument = PDDocument.load(input)
            val page       = pdDocument.getPage(pageIndex)
            page.rotation  = (page.rotation + degrees) % 360
            context.contentResolver.openOutputStream(destUri)!!.use { out ->
                pdDocument.save(out)
            }
            pdDocument.close()
        }
        destUri
    }
}
