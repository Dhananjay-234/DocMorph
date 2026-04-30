package com.docmorph.app.presentation.ui.editor

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import com.docmorph.app.data.model.Annotation
import com.docmorph.app.data.model.EditTool
import com.docmorph.app.domain.usecase.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockSavePdf    = mockk<SavePdfUseCase>()
    private val mockExportPdf  = mockk<ExportPdfUseCase>()
    private val mockDeletePage = mockk<DeletePageUseCase>()
    private val mockRotatePage = mockk<RotatePageUseCase>()

    private lateinit var viewModel: EditorViewModel

    private val fakeUri = mockk<Uri>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = EditorViewModel(mockSavePdf, mockExportPdf, mockDeletePage, mockRotatePage)
        viewModel.init(fakeUri, startPage = 0)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Test
    fun `init sets pdfUri and currentPage`() {
        val state = viewModel.uiState.value
        assertEquals(fakeUri, state.pdfUri)
        assertEquals(0, state.currentPage)
    }

    @Test
    fun `init is idempotent - second call is ignored`() {
        val anotherUri = mockk<Uri>(relaxed = true)
        viewModel.init(anotherUri, startPage = 3)
        // URI should still be the first one
        assertEquals(fakeUri, viewModel.uiState.value.pdfUri)
    }

    // ─── Tool selection ───────────────────────────────────────────────────────

    @Test
    fun `onToolSelected selects the tool`() {
        viewModel.onToolSelected(EditTool.DRAW)
        assertEquals(EditTool.DRAW, viewModel.uiState.value.selectedTool)
    }

    @Test
    fun `onToolSelected toggles tool off when same tool tapped twice`() {
        viewModel.onToolSelected(EditTool.DRAW)
        viewModel.onToolSelected(EditTool.DRAW)
        assertEquals(EditTool.NONE, viewModel.uiState.value.selectedTool)
    }

    @Test
    fun `onStrokeColorChanged updates strokeColor`() {
        viewModel.onStrokeColorChanged(Color.Red)
        assertEquals(Color.Red, viewModel.uiState.value.strokeColor)
    }

    // ─── Canvas tap ───────────────────────────────────────────────────────────

    @Test
    fun `onCanvasTapped with TEXT tool opens text dialog`() {
        viewModel.onToolSelected(EditTool.TEXT)
        viewModel.onCanvasTapped(Offset(100f, 200f))
        assertTrue(viewModel.uiState.value.showTextDialog)
        assertEquals(Offset(100f, 200f), viewModel.uiState.value.textDialogPosition)
    }

    @Test
    fun `onTextConfirmed adds TextBox annotation and closes dialog`() {
        viewModel.onToolSelected(EditTool.TEXT)
        viewModel.onCanvasTapped(Offset(50f, 50f))
        viewModel.onTextConfirmed("Hello DocMorph")

        val state = viewModel.uiState.value
        assertFalse(state.showTextDialog)
        assertEquals(1, state.annotations.size)
        val annotation = state.annotations.first()
        assertTrue(annotation is Annotation.TextBox)
        assertEquals("Hello DocMorph", (annotation as Annotation.TextBox).text)
    }

    @Test
    fun `onTextConfirmed with blank text does NOT add annotation`() {
        viewModel.onToolSelected(EditTool.TEXT)
        viewModel.onCanvasTapped(Offset(50f, 50f))
        viewModel.onTextConfirmed("   ")  // blank / whitespace

        assertTrue(viewModel.uiState.value.annotations.isEmpty())
    }

    @Test
    fun `onTextDismissed closes dialog without adding annotation`() {
        viewModel.onToolSelected(EditTool.TEXT)
        viewModel.onCanvasTapped(Offset(50f, 50f))
        viewModel.onTextDismissed()

        assertFalse(viewModel.uiState.value.showTextDialog)
        assertTrue(viewModel.uiState.value.annotations.isEmpty())
    }

    // ─── Freehand drawing ─────────────────────────────────────────────────────

    @Test
    fun `draw gesture adds Drawing annotation`() {
        viewModel.onToolSelected(EditTool.DRAW)
        viewModel.onDrawStart(Offset(0f, 0f))
        viewModel.onDrawMove(Offset(10f, 10f))
        viewModel.onDrawMove(Offset(20f, 20f))
        viewModel.onDrawEnd()

        assertEquals(1, viewModel.uiState.value.annotations.size)
        assertTrue(viewModel.uiState.value.annotations.first() is Annotation.Drawing)
    }

    @Test
    fun `draw gesture clears activeDrawPath after end`() {
        viewModel.onToolSelected(EditTool.DRAW)
        viewModel.onDrawStart(Offset(0f, 0f))
        viewModel.onDrawMove(Offset(5f, 5f))
        viewModel.onDrawEnd()

        assertTrue(viewModel.uiState.value.activeDrawPath.isEmpty())
    }

    @Test
    fun `single-point draw gesture does NOT add annotation`() {
        viewModel.onToolSelected(EditTool.DRAW)
        viewModel.onDrawStart(Offset(0f, 0f))
        viewModel.onDrawEnd()   // only 1 point — below minimum

        assertTrue(viewModel.uiState.value.annotations.isEmpty())
    }

    // ─── Undo / Redo ──────────────────────────────────────────────────────────

    @Test
    fun `undo removes last added annotation`() {
        viewModel.onToolSelected(EditTool.TEXT)
        viewModel.onCanvasTapped(Offset(0f, 0f))
        viewModel.onTextConfirmed("A")

        viewModel.undo()

        assertTrue(viewModel.uiState.value.annotations.isEmpty())
        assertFalse(viewModel.canUndo)
    }

    @Test
    fun `redo restores annotation after undo`() {
        viewModel.onToolSelected(EditTool.TEXT)
        viewModel.onCanvasTapped(Offset(0f, 0f))
        viewModel.onTextConfirmed("B")

        viewModel.undo()
        viewModel.redo()

        assertEquals(1, viewModel.uiState.value.annotations.size)
        assertFalse(viewModel.canRedo)
    }

    @Test
    fun `adding a new annotation clears the redo stack`() {
        viewModel.onToolSelected(EditTool.TEXT)
        viewModel.onCanvasTapped(Offset(0f, 0f))
        viewModel.onTextConfirmed("C")
        viewModel.undo()
        // Now there is something to redo
        assertTrue(viewModel.canRedo)

        // Adding new annotation should clear redo
        viewModel.onToolSelected(EditTool.TEXT)
        viewModel.onCanvasTapped(Offset(0f, 0f))
        viewModel.onTextConfirmed("D")

        assertFalse(viewModel.canRedo)
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    @Test
    fun `onSave calls savePdf use case and emits ShowSnackbar on success`() = runTest {
        coEvery { mockSavePdf(fakeUri, emptyList(), null) } returns fakeUri

        viewModel.events.test {
            viewModel.onSave()
            val event = awaitItem()
            assertTrue(event is EditorEvent.ShowSnackbar)
            assertTrue((event as EditorEvent.ShowSnackbar).message.contains("Saved"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSave emits ShowSnackbar with error message on failure`() = runTest {
        coEvery { mockSavePdf(fakeUri, any(), null) } throws RuntimeException("Disk full")

        viewModel.events.test {
            viewModel.onSave()
            val event = awaitItem()
            assertTrue(event is EditorEvent.ShowSnackbar)
            assertTrue((event as EditorEvent.ShowSnackbar).message.contains("Disk full"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Page management ─────────────────────────────────────────────────────

    @Test
    fun `onDeletePage emits ShowSnackbar on success`() = runTest {
        coEvery { mockDeletePage(fakeUri, 0, fakeUri) } returns fakeUri

        viewModel.events.test {
            viewModel.onDeletePage()
            val event = awaitItem()
            assertTrue(event is EditorEvent.ShowSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRotatePage emits ShowSnackbar on success`() = runTest {
        coEvery { mockRotatePage(fakeUri, 0, 90, fakeUri) } returns fakeUri

        viewModel.events.test {
            viewModel.onRotatePage(90)
            val event = awaitItem()
            assertTrue(event is EditorEvent.ShowSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Menu ────────────────────────────────────────────────────────────────

    @Test
    fun `onMenuToggled opens and closes menu`() {
        assertFalse(viewModel.uiState.value.isMenuExpanded)
        viewModel.onMenuToggled()
        assertTrue(viewModel.uiState.value.isMenuExpanded)
        viewModel.onMenuToggled()
        assertFalse(viewModel.uiState.value.isMenuExpanded)
    }
}
