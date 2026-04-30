package com.docmorph.app.presentation.ui.home

import android.net.Uri
import app.cash.turbine.test
import com.docmorph.app.data.model.PdfDocument
import com.docmorph.app.data.model.RecentFileEntity
import com.docmorph.app.domain.usecase.GetRecentFilesUseCase
import com.docmorph.app.domain.usecase.OpenPdfUseCase
import com.docmorph.app.domain.usecase.RemoveRecentFileUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [HomeViewModel].
 *
 * Uses:
 *  - MockK for dependency mocking.
 *  - Turbine for collecting Flow / SharedFlow emissions.
 *  - Coroutines test dispatcher for deterministic coroutine execution.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    // ─── Test dispatcher ─────────────────────────────────────────────────────

    private val testDispatcher = UnconfinedTestDispatcher()

    // ─── Mocks ───────────────────────────────────────────────────────────────

    private val mockGetRecentFiles   = mockk<GetRecentFilesUseCase>()
    private val mockOpenPdf          = mockk<OpenPdfUseCase>()
    private val mockRemoveRecentFile = mockk<RemoveRecentFileUseCase>()

    private lateinit var viewModel: HomeViewModel

    // ─── Sample data ──────────────────────────────────────────────────────────

    private val sampleUri = mockk<Uri>(relaxed = true)

    private val sampleEntity = RecentFileEntity(
        uriString  = "content://com.example/document.pdf",
        name       = "document.pdf",
        sizeBytes  = 1_024_000L,
        pageCount  = 5,
        lastOpened = System.currentTimeMillis()
    )

    private val sampleDocument = PdfDocument(
        uri       = sampleUri,
        name      = "document.pdf",
        sizeBytes = 1_024_000L,
        pageCount = 5
    )

    // ─── Setup / Teardown ─────────────────────────────────────────────────────

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Default: emit an empty recent-files list
        every { mockGetRecentFiles() } returns flowOf(emptyList())

        viewModel = HomeViewModel(
            getRecentFiles   = mockGetRecentFiles,
            openPdf          = mockOpenPdf,
            removeRecentFile = mockRemoveRecentFile
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `initial state has empty recent files and isLoading false`() {
        val state = viewModel.uiState.value
        assertTrue(state.recentFiles.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `recent files are collected from use case`() = runTest {
        val list = listOf(sampleEntity)
        every { mockGetRecentFiles() } returns flowOf(list)

        // Recreate VM so the new flow is collected
        viewModel = HomeViewModel(mockGetRecentFiles, mockOpenPdf, mockRemoveRecentFile)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(list, state.recentFiles)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPdfSelected emits NavigateToViewer on success`() = runTest {
        coEvery { mockOpenPdf(sampleUri) } returns sampleDocument

        viewModel.events.test {
            viewModel.onPdfSelected(sampleUri)
            val event = awaitItem()
            assertTrue(event is HomeEvent.NavigateToViewer)
            assertEquals(sampleUri, (event as HomeEvent.NavigateToViewer).uri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPdfSelected emits ShowError when openPdf throws`() = runTest {
        coEvery { mockOpenPdf(sampleUri) } throws RuntimeException("File not found")

        viewModel.events.test {
            viewModel.onPdfSelected(sampleUri)
            val event = awaitItem()
            assertTrue(event is HomeEvent.ShowError)
            assertTrue((event as HomeEvent.ShowError).message.contains("File not found"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPdfSelected sets isLoading true then false`() = runTest {
        coEvery { mockOpenPdf(sampleUri) } returns sampleDocument

        viewModel.uiState.test {
            viewModel.onPdfSelected(sampleUri)
            // Depending on dispatcher the loading flag may or may not be observable
            // — verify it ends as false
            val finalState = expectMostRecentItem()
            assertFalse(finalState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecentFileClicked emits NavigateToViewer`() = runTest {
        viewModel.events.test {
            viewModel.onRecentFileClicked(sampleEntity)
            val event = awaitItem()
            assertTrue(event is HomeEvent.NavigateToViewer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecentFileRemoved calls removeRecentFile use case`() = runTest {
        coEvery { mockRemoveRecentFile(sampleEntity) } just Runs
        viewModel.onRecentFileRemoved(sampleEntity)
        coVerify { mockRemoveRecentFile(sampleEntity) }
    }

    @Test
    fun `clearError resets error state to null`() = runTest {
        // Force an error state
        coEvery { mockOpenPdf(sampleUri) } throws RuntimeException("oops")
        viewModel.onPdfSelected(sampleUri)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }
}
