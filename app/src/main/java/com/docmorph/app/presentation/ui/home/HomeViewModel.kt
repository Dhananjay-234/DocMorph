package com.docmorph.app.presentation.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docmorph.app.data.model.RecentFileEntity
import com.docmorph.app.domain.usecase.GetRecentFilesUseCase
import com.docmorph.app.domain.usecase.OpenPdfUseCase
import com.docmorph.app.domain.usecase.RemoveRecentFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentFiles: List<RecentFileEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface HomeEvent {
    /** Emitted when a PDF is ready to be opened in the viewer. */
    data class NavigateToViewer(val uri: Uri) : HomeEvent
    data class ShowError(val message: String) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecentFiles: GetRecentFilesUseCase,
    private val openPdf: OpenPdfUseCase,
    private val removeRecentFile: RemoveRecentFileUseCase
) : ViewModel() {

    private val _uiState  = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        // Observe recent files from Room in real time
        getRecentFiles()
            .onEach { files -> _uiState.update { it.copy(recentFiles = files) } }
            .launchIn(viewModelScope)
    }

    // ─── Intent handlers ─────────────────────────────────────────────────────

    /**
     * Called once the user has selected a PDF via the system picker.
     * Reads metadata and records the file in the recent-files list.
     */
    fun onPdfSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { openPdf(uri) }
                .onSuccess { _events.emit(HomeEvent.NavigateToViewer(uri)) }
                .onFailure { e ->
                    _events.emit(HomeEvent.ShowError("Failed to open file: ${e.message}"))
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** Reopens a previously opened PDF from the recent list. */
    fun onRecentFileClicked(entity: RecentFileEntity) {
        val uri = Uri.parse(entity.uriString)
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToViewer(uri))
        }
    }

    /** Removes an entry from the recent files list. */
    fun onRecentFileRemoved(entity: RecentFileEntity) {
        viewModelScope.launch { removeRecentFile(entity) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
