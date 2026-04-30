package com.docmorph.app.presentation.ui.viewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.docmorph.app.data.model.PdfDocument
import com.docmorph.app.domain.usecase.OpenPdfUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockOpenPdf    = mockk<OpenPdfUseCase>()
    private val mockUri        = mockk<Uri>(relaxed = true)

    private lateinit var viewModel: ViewerViewModel

    private val fakeDocument = PdfDocument(
        uri       = mockUri,
        name      = "report.pdf",
        sizeBytes = 5_242_880L,
        pageCount = 12
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ViewerViewModel(SavedStateHandle(), mockOpenPdf)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ─── Document loading ─────────────────────────────────────────────────────

    @Test
    fun `loadDocument updates fileName and totalPages on success`() = runTest {
        coEvery { mockOpenPdf(mockUri) } returns fakeDocument

        viewModel.loadDocument(mockUri)

        val state = viewModel.uiState.value
        assertEquals("report.pdf", state.fileName)
        assertEquals(12, state.totalPages)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadDocument sets error on failure`() = runTest {
        coEvery { mockOpenPdf(mockUri) } throws RuntimeException("Cannot open")

        viewModel.loadDocument(mockUri)

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadDocument is idempotent - second call for same URI is ignored`() = runTest {
        coEvery { mockOpenPdf(mockUri) } returns fakeDocument

        viewModel.loadDocument(mockUri)
        viewModel.loadDocument(mockUri)

        coVerify(exactly = 1) { mockOpenPdf(mockUri) }
    }

    // ─── Page navigation ──────────────────────────────────────────────────────

    @Test
    fun `onPageChanged updates currentPage`() {
        viewModel.onPageChanged(7)
        assertEquals(7, viewModel.uiState.value.currentPage)
    }

    // ─── Search ──────────────────────────────────────────────────────────────

    @Test
    fun `onSearchToggled shows and hides search bar`() {
        assertFalse(viewModel.uiState.value.isSearchVisible)
        viewModel.onSearchToggled()
        assertTrue(viewModel.uiState.value.isSearchVisible)
        viewModel.onSearchToggled()
        assertFalse(viewModel.uiState.value.isSearchVisible)
    }

    @Test
    fun `onSearchToggled resets query when hiding`() {
        viewModel.onSearchToggled()
        viewModel.onSearchQueryChanged("hello")
        viewModel.onSearchToggled()
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `onSearchQueryChanged updates searchQuery`() {
        viewModel.onSearchToggled()
        viewModel.onSearchQueryChanged("invoice")
        assertEquals("invoice", viewModel.uiState.value.searchQuery)
    }

    // ─── Menu ────────────────────────────────────────────────────────────────

    @Test
    fun `onMenuToggled toggles isMenuExpanded`() {
        assertFalse(viewModel.uiState.value.isMenuExpanded)
        viewModel.onMenuToggled()
        assertTrue(viewModel.uiState.value.isMenuExpanded)
    }

    @Test
    fun `onMenuDismissed collapses menu`() {
        viewModel.onMenuToggled()
        viewModel.onMenuDismissed()
        assertFalse(viewModel.uiState.value.isMenuExpanded)
    }

    // ─── Edit navigation ─────────────────────────────────────────────────────

    @Test
    fun `onEditClicked emits NavigateToEditor`() = runTest {
        coEvery { mockOpenPdf(mockUri) } returns fakeDocument
        viewModel.loadDocument(mockUri)
        viewModel.onPageChanged(3)

        viewModel.events.test {
            viewModel.onEditClicked()
            val event = awaitItem()
            assertTrue(event is ViewerEvent.NavigateToEditor)
            assertEquals(3, (event as ViewerEvent.NavigateToEditor).currentPage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Error handling ───────────────────────────────────────────────────────

    @Test
    fun `clearError resets error to null`() = runTest {
        coEvery { mockOpenPdf(mockUri) } throws RuntimeException("fail")
        viewModel.loadDocument(mockUri)
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }
}
