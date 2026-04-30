package com.docmorph.app.presentation.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docmorph.app.data.model.Annotation
import com.docmorph.app.domain.usecase.ExportPdfUseCase
import com.docmorph.app.domain.usecase.SavePdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SaveAsEvent {
    data class Success(val destUri: Uri) : SaveAsEvent
    data class Error(val message: String) : SaveAsEvent
}

/**
 * Thin ViewModel that drives the "Save As" and "Export" flows that are
 * initiated from both [ViewerScreen] and [EditorScreen].
 *
 * The SAF create-document launcher lives in the Composable layer;
 * this VM handles the I/O once a destination URI is chosen.
 */
@HiltViewModel
class SaveExportViewModel @Inject constructor(
    private val savePdf: SavePdfUseCase,
    private val exportPdf: ExportPdfUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<SaveAsEvent>()
    val events: SharedFlow<SaveAsEvent> = _events

    // ─── Save As ─────────────────────────────────────────────────────────────

    fun saveAs(
        sourceUri: Uri,
        annotations: List<Annotation>,
        destUri: Uri
    ) {
        viewModelScope.launch {
            runCatching { savePdf(sourceUri, annotations, destUri) }
                .onSuccess { _events.emit(SaveAsEvent.Success(it)) }
                .onFailure { _events.emit(SaveAsEvent.Error(it.message ?: "Save failed")) }
        }
    }

    // ─── Export as image ─────────────────────────────────────────────────────

    fun exportAsImage(
        sourceUri: Uri,
        pageIndex: Int,
        format: String,        // "png" or "jpg"
        destUri: Uri
    ) {
        viewModelScope.launch {
            runCatching { exportPdf.asImage(sourceUri, pageIndex, format, destUri) }
                .onSuccess { _events.emit(SaveAsEvent.Success(destUri)) }
                .onFailure { _events.emit(SaveAsEvent.Error(it.message ?: "Export failed")) }
        }
    }

    // ─── Export as plain text ─────────────────────────────────────────────────

    fun exportAsText(sourceUri: Uri, destUri: Uri) {
        viewModelScope.launch {
            runCatching { exportPdf.asText(sourceUri, destUri) }
                .onSuccess { _events.emit(SaveAsEvent.Success(destUri)) }
                .onFailure { _events.emit(SaveAsEvent.Error(it.message ?: "Export failed")) }
        }
    }
}
