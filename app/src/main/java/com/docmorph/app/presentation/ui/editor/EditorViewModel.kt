package com.docmorph.app.presentation.ui.editor

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docmorph.app.data.model.Annotation
import com.docmorph.app.data.model.EditTool
import com.docmorph.app.data.model.ShapeType
import com.docmorph.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class EditorUiState(
    val pdfUri: Uri? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val selectedTool: EditTool = EditTool.NONE,
    val strokeColor: Color = Color.Black,
    val fillColor: Color = Color.Transparent,
    val strokeWidth: Float = 3f,
    val fontSize: Float = 14f,
    val annotations: List<Annotation> = emptyList(),
    val activeDrawPath: List<Offset> = emptyList(),
    val isMenuExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val showTextDialog: Boolean = false,
    val textDialogPosition: Offset = Offset.Zero,
    val showColorPicker: Boolean = false,
    val error: String? = null
)

sealed interface EditorEvent {
    object NavigateUp : EditorEvent
    data class ShowSnackbar(val message: String) : EditorEvent
    data class ShowSaveAsDialog(val uri: Uri) : EditorEvent
    data class ShowExportDialog(val uri: Uri) : EditorEvent
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val savePdf: SavePdfUseCase,
    private val exportPdf: ExportPdfUseCase,
    private val deletePage: DeletePageUseCase,
    private val rotatePage: RotatePageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    // Undo stack — each entry is the full annotation list snapshot before a change
    private val undoStack = ArrayDeque<List<Annotation>>()
    private val redoStack = ArrayDeque<List<Annotation>>()

    fun init(uri: Uri, startPage: Int) {
        if (_uiState.value.pdfUri != null) return
        _uiState.update { it.copy(pdfUri = uri, currentPage = startPage) }
    }

    fun onTotalPagesKnown(count: Int) = _uiState.update { it.copy(totalPages = count) }

    // ─── Tool selection ───────────────────────────────────────────────────────

    fun onToolSelected(tool: EditTool) = _uiState.update {
        it.copy(selectedTool = if (it.selectedTool == tool) EditTool.NONE else tool)
    }

    fun onStrokeColorChanged(color: Color) = _uiState.update { it.copy(strokeColor = color) }
    fun onStrokeWidthChanged(w: Float)     = _uiState.update { it.copy(strokeWidth = w) }
    fun onFontSizeChanged(size: Float)     = _uiState.update { it.copy(fontSize = size) }
    fun onColorPickerToggled()             = _uiState.update { it.copy(showColorPicker = !it.showColorPicker) }

    // ─── Canvas interaction ───────────────────────────────────────────────────

    fun onCanvasTapped(offset: Offset) {
        when (_uiState.value.selectedTool) {
            EditTool.TEXT    -> _uiState.update { it.copy(showTextDialog = true, textDialogPosition = offset) }
            EditTool.COMMENT -> addAnnotation(
                Annotation.Comment(
                    id           = newId(),
                    pageIndex    = _uiState.value.currentPage,
                    text         = "",
                    anchorOffset = offset
                )
            )
            else -> { /* other tools handled by drag events */ }
        }
    }

    fun onDrawStart(offset: Offset) {
        if (_uiState.value.selectedTool !in drawingTools) return
        _uiState.update { it.copy(activeDrawPath = listOf(offset)) }
    }

    fun onDrawMove(offset: Offset) {
        _uiState.update { it.copy(activeDrawPath = it.activeDrawPath + offset) }
    }

    fun onDrawEnd() {
        val state = _uiState.value
        val path  = state.activeDrawPath
        if (path.size < 2) { _uiState.update { it.copy(activeDrawPath = emptyList()) }; return }

        val annotation: Annotation = when (state.selectedTool) {
            EditTool.DRAW -> Annotation.Drawing(
                id          = newId(),
                pageIndex   = state.currentPage,
                path        = path,
                strokeColor = state.strokeColor,
                strokeWidth = state.strokeWidth
            )
            EditTool.HIGHLIGHT -> Annotation.Highlight(
                id          = newId(),
                pageIndex   = state.currentPage,
                startOffset = path.first(),
                endOffset   = path.last()
            )
            EditTool.STRIKETHROUGH -> Annotation.Strikethrough(
                id          = newId(),
                pageIndex   = state.currentPage,
                startOffset = path.first(),
                endOffset   = path.last(),
                color       = state.strokeColor
            )
            EditTool.SHAPE_RECTANGLE -> Annotation.Shape(
                id          = newId(),
                pageIndex   = state.currentPage,
                type        = ShapeType.RECTANGLE,
                startOffset = path.first(),
                endOffset   = path.last(),
                color       = state.strokeColor,
                strokeWidth = state.strokeWidth
            )
            EditTool.SHAPE_CIRCLE -> Annotation.Shape(
                id          = newId(),
                pageIndex   = state.currentPage,
                type        = ShapeType.CIRCLE,
                startOffset = path.first(),
                endOffset   = path.last(),
                color       = state.strokeColor,
                strokeWidth = state.strokeWidth
            )
            EditTool.SHAPE_LINE -> Annotation.Shape(
                id          = newId(),
                pageIndex   = state.currentPage,
                type        = ShapeType.LINE,
                startOffset = path.first(),
                endOffset   = path.last(),
                color       = state.strokeColor,
                strokeWidth = state.strokeWidth
            )
            else -> null
        } ?: run { _uiState.update { it.copy(activeDrawPath = emptyList()) }; return }

        addAnnotation(annotation)
        _uiState.update { it.copy(activeDrawPath = emptyList()) }
    }

    // ─── Text annotation ──────────────────────────────────────────────────────

    fun onTextConfirmed(text: String) {
        val state = _uiState.value
        if (text.isNotBlank()) {
            addAnnotation(
                Annotation.TextBox(
                    id        = newId(),
                    pageIndex = state.currentPage,
                    text      = text,
                    x         = state.textDialogPosition.x,
                    y         = state.textDialogPosition.y,
                    fontSize  = state.fontSize,
                    color     = state.strokeColor
                )
            )
        }
        _uiState.update { it.copy(showTextDialog = false) }
    }

    fun onTextDismissed() = _uiState.update { it.copy(showTextDialog = false) }

    // ─── Eraser ───────────────────────────────────────────────────────────────

    fun onEraserTapped(offset: Offset) {
        val state = _uiState.value
        if (state.selectedTool != EditTool.ERASER) return
        val target = state.annotations.firstOrNull { annotation ->
            annotation.pageIndex == state.currentPage && annotation.hitTest(offset)
        } ?: return
        pushUndo(state.annotations)
        _uiState.update { it.copy(annotations = it.annotations - target) }
    }

    // ─── Undo / Redo ──────────────────────────────────────────────────────────

    fun undo() {
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeLast()
        redoStack.addLast(_uiState.value.annotations)
        _uiState.update { it.copy(annotations = prev) }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(_uiState.value.annotations)
        _uiState.update { it.copy(annotations = next) }
    }

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    // ─── Page management ──────────────────────────────────────────────────────

    fun onDeletePage() {
        viewModelScope.launch {
            val state = _uiState.value
            val uri   = state.pdfUri ?: return@launch
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                deletePage(uri, state.currentPage, uri)
            }.onSuccess {
                _events.emit(EditorEvent.ShowSnackbar("Page ${state.currentPage + 1} deleted"))
            }.onFailure { e ->
                _events.emit(EditorEvent.ShowSnackbar("Delete failed: ${e.message}"))
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun onRotatePage(degrees: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val uri   = state.pdfUri ?: return@launch
            _uiState.update { it.copy(isSaving = true) }
            runCatching { rotatePage(uri, state.currentPage, degrees, uri) }
                .onSuccess { _events.emit(EditorEvent.ShowSnackbar("Page rotated $degrees°")) }
                .onFailure { e -> _events.emit(EditorEvent.ShowSnackbar("Rotate failed: ${e.message}")) }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    // ─── Save / Menu ──────────────────────────────────────────────────────────

    fun onMenuToggled()  = _uiState.update { it.copy(isMenuExpanded = !it.isMenuExpanded) }
    fun onMenuDismissed() = _uiState.update { it.copy(isMenuExpanded = false) }

    fun onSave() {
        viewModelScope.launch {
            val state = _uiState.value
            val uri   = state.pdfUri ?: return@launch
            _uiState.update { it.copy(isSaving = true) }
            runCatching { savePdf(uri, state.annotations) }
                .onSuccess { _events.emit(EditorEvent.ShowSnackbar("Saved successfully")) }
                .onFailure { e -> _events.emit(EditorEvent.ShowSnackbar("Save failed: ${e.message}")) }
            _uiState.update { it.copy(isSaving = false) }
        }
        onMenuDismissed()
    }

    fun onSaveAs() {
        viewModelScope.launch {
            _uiState.value.pdfUri?.let { _events.emit(EditorEvent.ShowSaveAsDialog(it)) }
        }
        onMenuDismissed()
    }

    fun onExport() {
        viewModelScope.launch {
            _uiState.value.pdfUri?.let { _events.emit(EditorEvent.ShowExportDialog(it)) }
        }
        onMenuDismissed()
    }

    fun onNavigateUp() = viewModelScope.launch { _events.emit(EditorEvent.NavigateUp) }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun addAnnotation(annotation: Annotation) {
        pushUndo(_uiState.value.annotations)
        redoStack.clear()
        _uiState.update { it.copy(annotations = it.annotations + annotation) }
    }

    private fun pushUndo(snapshot: List<Annotation>) {
        undoStack.addLast(snapshot)
        if (undoStack.size > 50) undoStack.removeFirst()
    }

    private fun newId() = UUID.randomUUID().toString()

    private val drawingTools = setOf(
        EditTool.DRAW,
        EditTool.HIGHLIGHT,
        EditTool.STRIKETHROUGH,
        EditTool.SHAPE_RECTANGLE,
        EditTool.SHAPE_CIRCLE,
        EditTool.SHAPE_LINE
    )

    fun clearError() = _uiState.update { it.copy(error = null) }
}

// ─── Hit-test extension ──────────────────────────────────────────────────────

private fun Annotation.hitTest(tap: Offset, slop: Float = 24f): Boolean = when (this) {
    is Annotation.TextBox     -> tap.x in (x - slop)..(x + 100 + slop) && tap.y in (y - slop)..(y + 20 + slop)
    is Annotation.Highlight   -> tap.x in (startOffset.x - slop)..(endOffset.x + slop) && tap.y in (startOffset.y - 20f)..(startOffset.y + 20f)
    is Annotation.Drawing     -> path.any { (it - tap).getDistance() < slop }
    is Annotation.Shape       -> tap.x in (minOf(startOffset.x, endOffset.x) - slop)..(maxOf(startOffset.x, endOffset.x) + slop)
    is Annotation.Strikethrough -> tap.x in (startOffset.x - slop)..(endOffset.x + slop)
    is Annotation.Comment     -> (anchorOffset - tap).getDistance() < slop
}
