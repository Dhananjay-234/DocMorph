package com.docmorph.app.presentation.ui.viewer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.docmorph.app.presentation.ui.components.ExportDialog
import com.docmorph.app.presentation.ui.components.PageJumpDialog
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    pdfUri: Uri,
    onNavigateUp: () -> Unit,
    onEnterEdit: (Int) -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    val context        = LocalContext.current
    val snackbarState  = remember { SnackbarHostState() }
    var showExport     by remember { mutableStateOf(false) }
    var showPageJump   by remember { mutableStateOf(false) }

    // Load document metadata once
    LaunchedEffect(pdfUri) { viewModel.loadDocument(pdfUri) }

    // One-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ViewerEvent.NavigateToEditor -> onEnterEdit(event.currentPage)
                is ViewerEvent.NavigateUp       -> onNavigateUp()
                is ViewerEvent.ShowExportDialog -> showExport = true
                is ViewerEvent.ShowShareSheet   -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type     = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                }
                is ViewerEvent.ShowSnackbar -> snackbarState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = viewModel::onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text     = uiState.fileName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style    = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uiState.totalPages > 0) {
                                Text(
                                    text  = "${uiState.currentPage + 1} / ${uiState.totalPages}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    actions = {
                        // Search toggle
                        IconButton(onClick = viewModel::onSearchToggled) {
                            Icon(Icons.Default.Search, "Search")
                        }
                        // Page jump
                        IconButton(onClick = { showPageJump = true }) {
                            Icon(Icons.Default.Pages, "Jump to page")
                        }
                        // Three-dot menu ⋮
                        Box {
                            IconButton(onClick = viewModel::onMenuToggled) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                            ViewerDropdownMenu(
                                expanded    = uiState.isMenuExpanded,
                                onDismiss   = viewModel::onMenuDismissed,
                                onEdit      = viewModel::onEditClicked,
                                onSave      = viewModel::onSaveClicked,
                                onExport    = viewModel::onExportClicked,
                                onShare     = viewModel::onShareClicked
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor     = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                // In-line search bar
                if (uiState.isSearchVisible) {
                    SearchBar(
                        query    = uiState.searchQuery,
                        onChange = viewModel::onSearchQueryChanged
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // AndroidPdfViewer rendered via AndroidView bridge
            PdfViewerCompose(
                uri           = pdfUri,
                searchQuery   = if (uiState.isSearchVisible) uiState.searchQuery else "",
                onPageChanged = viewModel::onPageChanged,
                modifier      = Modifier.fillMaxSize()
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // Dialogs
    if (showExport) {
        ExportDialog(
            uri       = pdfUri,
            pageIndex = uiState.currentPage,
            onDismiss = { showExport = false }
        )
    }

    if (showPageJump) {
        PageJumpDialog(
            totalPages = uiState.totalPages,
            onDismiss  = { showPageJump = false },
            onJump     = { /* pdfView.jumpTo(it) — wired via state below */ showPageJump = false }
        )
    }
}

// ─── AndroidPdfViewer bridge ─────────────────────────────────────────────────

@Composable
fun PdfViewerCompose(
    uri: Uri,
    searchQuery: String,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            PDFView(ctx, null).apply {
                fromUri(uri)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableAnnotationRendering(true)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .onLoad(OnLoadCompleteListener { /* page count available */ })
                    .onPageChange(OnPageChangeListener { page, _ -> onPageChanged(page) })
                    .scrollHandle(DefaultScrollHandle(ctx))
                    .spacing(8)
                    .load()
            }
        },
        update = { pdfView ->
            // Re-trigger search highlight when query changes
            if (searchQuery.isNotBlank()) {
                // AndroidPdfViewer doesn't have built-in text search;
                // this hook is the integration point for a custom search overlay
                // (Phase 2 — MuPDF integration for text search)
            }
        }
    )
}

// ─── Three-dot dropdown menu ─────────────────────────────────────────────────

@Composable
private fun ViewerDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit
) {
    DropdownMenu(
        expanded         = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Edit") },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            onClick = onEdit
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Save") },
            leadingIcon = { Icon(Icons.Default.Save, null) },
            onClick = onSave
        )
        DropdownMenuItem(
            text = { Text("Export") },
            leadingIcon = { Icon(Icons.Default.FileDownload, null) },
            onClick = onExport
        )
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Default.Share, null) },
            onClick = onShare
        )
    }
}

// ─── Inline search bar ────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value         = query,
        onValueChange = onChange,
        modifier      = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        placeholder   = { Text("Search in document…") },
        leadingIcon   = { Icon(Icons.Default.Search, null) },
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}
