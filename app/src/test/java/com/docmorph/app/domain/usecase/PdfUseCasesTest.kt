package com.docmorph.app.domain.usecase

import android.net.Uri
import com.docmorph.app.data.model.Annotation
import com.docmorph.app.data.model.PdfDocument
import com.docmorph.app.data.model.RecentFileEntity
import com.docmorph.app.data.repository.PdfRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PdfUseCasesTest {

    private val mockRepository = mockk<PdfRepository>()
    private val fakeUri        = mockk<Uri>(relaxed = true)
    private val fakeDestUri    = mockk<Uri>(relaxed = true)

    private val fakeDocument = PdfDocument(
        uri       = fakeUri,
        name      = "test.pdf",
        sizeBytes = 2_048L,
        pageCount = 3
    )

    @Before
    fun setUp() {
        // Default mocks
        coEvery { mockRepository.loadDocumentMetadata(any()) } returns fakeDocument
        coEvery { mockRepository.addRecentFile(any()) }        just Runs
    }

    // ─── OpenPdfUseCase ───────────────────────────────────────────────────────

    @Test
    fun `OpenPdfUseCase loads metadata and records recent file`() = runTest {
        val useCase  = OpenPdfUseCase(mockRepository)
        val result   = useCase(fakeUri)

        assertEquals(fakeDocument, result)
        coVerify { mockRepository.loadDocumentMetadata(fakeUri) }
        coVerify { mockRepository.addRecentFile(fakeDocument) }
    }

    // ─── GetRecentFilesUseCase ────────────────────────────────────────────────

    @Test
    fun `GetRecentFilesUseCase delegates to repository`() {
        val entities = listOf<RecentFileEntity>()
        every { mockRepository.getRecentFiles() } returns flowOf(entities)

        val useCase = GetRecentFilesUseCase(mockRepository)
        val flow    = useCase()

        verify { mockRepository.getRecentFiles() }
        assertNotNull(flow)
    }

    // ─── RemoveRecentFileUseCase ──────────────────────────────────────────────

    @Test
    fun `RemoveRecentFileUseCase delegates to repository`() = runTest {
        val entity = mockk<RecentFileEntity>(relaxed = true)
        coEvery { mockRepository.removeRecentFile(entity) } just Runs

        val useCase = RemoveRecentFileUseCase(mockRepository)
        useCase(entity)

        coVerify { mockRepository.removeRecentFile(entity) }
    }

    // ─── SavePdfUseCase ───────────────────────────────────────────────────────

    @Test
    fun `SavePdfUseCase delegates to repository and returns dest URI`() = runTest {
        val annotations = listOf<Annotation>()
        coEvery { mockRepository.savePdf(fakeUri, annotations, fakeDestUri) } returns fakeDestUri

        val useCase = SavePdfUseCase(mockRepository)
        val result  = useCase(fakeUri, annotations, fakeDestUri)

        assertEquals(fakeDestUri, result)
        coVerify { mockRepository.savePdf(fakeUri, annotations, fakeDestUri) }
    }

    @Test
    fun `SavePdfUseCase passes null destUri for overwrite`() = runTest {
        val annotations = listOf<Annotation>()
        coEvery { mockRepository.savePdf(fakeUri, annotations, null) } returns fakeUri

        val useCase = SavePdfUseCase(mockRepository)
        val result  = useCase(fakeUri, annotations)   // default destUri = null

        assertEquals(fakeUri, result)
        coVerify { mockRepository.savePdf(fakeUri, annotations, null) }
    }

    // ─── DeletePageUseCase ────────────────────────────────────────────────────

    @Test
    fun `DeletePageUseCase delegates to repository`() = runTest {
        coEvery { mockRepository.deletePage(fakeUri, 1, fakeDestUri) } returns fakeDestUri

        val useCase = DeletePageUseCase(mockRepository)
        val result  = useCase(fakeUri, 1, fakeDestUri)

        assertEquals(fakeDestUri, result)
    }

    // ─── RotatePageUseCase ────────────────────────────────────────────────────

    @Test
    fun `RotatePageUseCase delegates to repository with correct degrees`() = runTest {
        coEvery { mockRepository.rotatePage(fakeUri, 0, 90, fakeDestUri) } returns fakeDestUri

        val useCase = RotatePageUseCase(mockRepository)
        val result  = useCase(fakeUri, 0, 90, fakeDestUri)

        assertEquals(fakeDestUri, result)
        coVerify { mockRepository.rotatePage(fakeUri, 0, 90, fakeDestUri) }
    }
}
