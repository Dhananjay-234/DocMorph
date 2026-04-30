package com.docmorph.app.presentation.ui.viewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docmorph.app.domain.usecase.OpenPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val pdfUri: Uri? = null,
    val fileName: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val isLoading: Boolean = true,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val isMenuExpanded: Boolean = false,
    val error: String? = null
)

sealed interface ViewerEvent {
    data class NavigateToEditor(val uri: Uri, val currentPage: Int) : ViewerEvent
    object NavigateUp : ViewerEvent
    data class ShowExportDialog(val uri: Uri) : ViewerEvent
    data class ShowShareSheet(val uri: Uri) : ViewerEvent
    data class ShowSnackbar(val message: String) : ViewerEvent
}

@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val openPdf: OpenPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ViewerEvent>()
    val events: SharedFlow<ViewerEvent> = _events.asSharedFlow()

    fun loadDocument(uri: Uri) {
        if (_uiState.value.pdfUri == uri) return   // already loaded
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pdfUri = uri) }
            runCatching { openPdf(uri) }
                .onSuccess { doc ->
                    _uiState.update {
                        it.copy(
                            fileName   = doc.name,
                            totalPages = doc.pageCount,
                            isLoading  = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun onPageChanged(page: Int) = _uiState.update { it.copy(currentPage = page) }

    fun onMenuToggled()  = _uiState.update { it.copy(isMenuExpanded = !it.isMenuExpanded) }
    fun onMenuDismissed() = _uiState.update { it.copy(isMenuExpanded = false) }

    fun onSearchToggled() = _uiState.update {
        it.copy(isSearchVisible = !it.isSearchVisible, searchQuery = "")
    }
    fun onSearchQueryChanged(q: String) = _uiState.update { it.copy(searchQuery = q) }

    fun onEditClicked() {
        val state = _uiState.value
        viewModelScope.launch {
            state.pdfUri?.let { uri ->
                _events.emit(ViewerEvent.NavigateToEditor(uri, state.currentPage))
            }
        }
        onMenuDismissed()
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            _events.emit(ViewerEvent.ShowSnackbar("No unsaved changes"))
        }
        onMenuDismissed()
    }

    fun onExportClicked() {
        viewModelScope.launch {
            _uiState.value.pdfUri?.let { _events.emit(ViewerEvent.ShowExportDialog(it)) }
        }
        onMenuDismissed()
    }

    fun onShareClicked() {
        viewModelScope.launch {
            _uiState.value.pdfUri?.let { _events.emit(ViewerEvent.ShowShareSheet(it)) }
        }
        onMenuDismissed()
    }

    fun onNavigateUp() {
        viewModelScope.launch { _events.emit(ViewerEvent.NavigateUp) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
