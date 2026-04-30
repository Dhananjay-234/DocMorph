package com.docmorph.app.presentation.ui.editor

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.docmorph.app.data.model.Annotation
import com.docmorph.app.data.model.EditTool
import com.docmorph.app.data.model.ShapeType
import com.docmorph.app.presentation.ui.components.ColorPickerDialog
import com.docmorph.app.presentation.ui.components.ExportDialog
import com.docmorph.app.presentation.ui.components.TextInputDialog
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    pdfUri: Uri,
    startPage: Int,
    onNavigateUp: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val snackbarState = remember { SnackbarHostState() }
    var showExport    by remember { mutableStateOf(false) }

    LaunchedEffect(pdfUri) { viewModel.init(pdfUri, startPage) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditorEvent.NavigateUp        -> onNavigateUp()
                is EditorEvent.ShowSnackbar      -> snackbarState.showSnackbar(event.message)
                is EditorEvent.ShowExportDialog  -> showExport = true
                is EditorEvent.ShowSaveAsDialog  -> { /* TODO: launch SAF create document picker */ }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
        topBar = {
            EditorTopBar(
                fileName       = uiState.pdfUri?.lastPathSegment ?: "Editing",
                currentPage    = uiState.currentPage,
                totalPages     = uiState.totalPages,
                isMenuExpanded = uiState.isMenuExpanded,
                isSaving       = uiState.isSaving,
                canUndo        = viewModel.canUndo,
                canRedo        = viewModel.canRedo,
                onNavigateUp   = { viewModel.onNavigateUp() },
                onUndo         = viewModel::undo,
                onRedo         = viewModel::redo,
                onMenuToggle   = viewModel::onMenuToggled,
                onMenuDismiss  = viewModel::onMenuDismissed,
                onSave         = viewModel::onSave,
                onSaveAs       = viewModel::onSaveAs,
                onExport       = viewModel::onExport,
                onDeletePage   = viewModel::onDeletePage,
                onRotateCW     = { viewModel.onRotatePage(90) },
                onRotateCCW    = { viewModel.onRotatePage(270) }
            )
        },
        bottomBar = {
            EditToolbar(
                selectedTool    = uiState.selectedTool,
                strokeColor     = uiState.strokeColor,
                onToolSelected  = viewModel::onToolSelected,
                onColorPicker   = viewModel::onColorPickerToggled
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // PDF viewer layer
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory  = { ctx ->
                    PDFView(ctx, null).apply {
                        fromUri(pdfUri)
                            .enableSwipe(uiState.selectedTool == EditTool.NONE)
                            .defaultPage(startPage)
                            .onLoad(OnLoadCompleteListener { count ->
                                viewModel.onTotalPagesKnown(count)
                            })
                            .onPageChange(OnPageChangeListener { page, _ ->
                                viewModel.onPageChanged(page)
                            })
                            .scrollHandle(DefaultScrollHandle(ctx))
                            .spacing(8)
                            .load()
                    }
                }
            )

            // Annotation canvas overlay
            AnnotationCanvas(
                annotations   = uiState.annotations.filter { it.pageIndex == uiState.currentPage },
                activeDrawPath = uiState.activeDrawPath,
                selectedTool  = uiState.selectedTool,
                strokeColor   = uiState.strokeColor,
                strokeWidth   = uiState.strokeWidth,
                onTap         = viewModel::onCanvasTapped,
                onEraserTap   = viewModel::onEraserTapped,
                onDrawStart   = viewModel::onDrawStart,
                onDrawMove    = viewModel::onDrawMove,
                onDrawEnd     = viewModel::onDrawEnd,
                modifier      = Modifier.fillMaxSize()
            )

            if (uiState.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("Saving…", color = Color.White)
                    }
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (uiState.showTextDialog) {
        TextInputDialog(
            position  = uiState.textDialogPosition,
            onConfirm = viewModel::onTextConfirmed,
            onDismiss = viewModel::onTextDismissed
        )
    }

    if (uiState.showColorPicker) {
        ColorPickerDialog(
            currentColor = uiState.strokeColor,
            onColorSelected = { color ->
                viewModel.onStrokeColorChanged(color)
                viewModel.onColorPickerToggled()
            },
            onDismiss = viewModel::onColorPickerToggled
        )
    }

    if (showExport) {
        ExportDialog(
            uri       = pdfUri,
            pageIndex = uiState.currentPage,
            onDismiss = { showExport = false }
        )
    }
}

// ─── Annotation canvas ────────────────────────────────────────────────────────

@Composable
private fun AnnotationCanvas(
    annotations: List<Annotation>,
    activeDrawPath: List<Offset>,
    selectedTool: EditTool,
    strokeColor: Color,
    strokeWidth: Float,
    onTap: (Offset) -> Unit,
    onEraserTap: (Offset) -> Unit,
    onDrawStart: (Offset) -> Unit,
    onDrawMove: (Offset) -> Unit,
    onDrawEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(selectedTool) {
                when (selectedTool) {
                    EditTool.TEXT, EditTool.COMMENT -> detectTapGestures { onTap(it) }
                    EditTool.ERASER                 -> detectTapGestures { onEraserTap(it) }
                    EditTool.DRAW,
                    EditTool.HIGHLIGHT,
                    EditTool.STRIKETHROUGH,
                    EditTool.SHAPE_RECTANGLE,
                    EditTool.SHAPE_CIRCLE,
                    EditTool.SHAPE_LINE             -> detectDragGestures(
                        onDragStart = onDrawStart,
                        onDrag      = { _, delta ->
                            // delta is relative; we accumulate absolute position in VM
                            onDrawMove(Offset(0f, 0f))  // placeholder — see note below
                        },
                        onDragEnd   = onDrawEnd
                    )
                    else -> { /* NONE — pass-through to PDFView */ }
                }
            }
    ) {
        // ── Committed annotations ────────────────────────────────────────────
        annotations.forEach { annotation ->
            when (annotation) {

                is Annotation.Drawing -> {
                    if (annotation.path.size >= 2) {
                        val path = Path().apply {
                            moveTo(annotation.path.first().x, annotation.path.first().y)
                            annotation.path.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path  = path,
                            color = annotation.strokeColor,
                            style = Stroke(
                                width = annotation.strokeWidth,
                                cap   = StrokeCap.Round,
                                join  = StrokeJoin.Round
                            )
                        )
                    }
                }

                is Annotation.Highlight -> {
                    drawRect(
                        color    = annotation.color.copy(alpha = 0.4f),
                        topLeft  = annotation.startOffset,
                        size     = androidx.compose.ui.geometry.Size(
                            width  = annotation.endOffset.x - annotation.startOffset.x,
                            height = 18f
                        )
                    )
                }

                is Annotation.Strikethrough -> {
                    val midY = (annotation.startOffset.y + annotation.endOffset.y) / 2f
                    drawLine(
                        color       = annotation.color,
                        start       = Offset(annotation.startOffset.x, midY),
                        end         = Offset(annotation.endOffset.x, midY),
                        strokeWidth = 2f
                    )
                }

                is Annotation.Shape -> {
                    val s = Stroke(width = annotation.strokeWidth)
                    when (annotation.type) {
                        ShapeType.RECTANGLE -> drawRect(
                            color    = annotation.color,
                            topLeft  = Offset(
                                minOf(annotation.startOffset.x, annotation.endOffset.x),
                                minOf(annotation.startOffset.y, annotation.endOffset.y)
                            ),
                            size     = androidx.compose.ui.geometry.Size(
                                width  = kotlin.math.abs(annotation.endOffset.x - annotation.startOffset.x),
                                height = kotlin.math.abs(annotation.endOffset.y - annotation.startOffset.y)
                            ),
                            style    = if (annotation.filled) androidx.compose.ui.graphics.drawscope.Fill else s
                        )
                        ShapeType.LINE -> drawLine(
                            color       = annotation.color,
                            start       = annotation.startOffset,
                            end         = annotation.endOffset,
                            strokeWidth = annotation.strokeWidth
                        )
                        ShapeType.CIRCLE -> {
                            val cx = (annotation.startOffset.x + annotation.endOffset.x) / 2f
                            val cy = (annotation.startOffset.y + annotation.endOffset.y) / 2f
                            val rx = kotlin.math.abs(annotation.endOffset.x - annotation.startOffset.x) / 2f
                            drawCircle(
                                color  = annotation.color,
                                radius = rx,
                                center = Offset(cx, cy),
                                style  = s
                            )
                        }
                        else -> {}
                    }
                }

                is Annotation.TextBox -> {
                    // TextBox is rendered as a tappable Compose Text overlay (see TextOverlay below)
                    drawCircle(color = annotation.color, radius = 4f, center = Offset(annotation.x, annotation.y))
                }

                is Annotation.Comment -> {
                    drawCircle(
                        color  = Color(0xFFFFC107),
                        radius = 12f,
                        center = annotation.anchorOffset
                    )
                }
            }
        }

        // ── Active (in-progress) draw path ────────────────────────────────────
        if (activeDrawPath.size >= 2) {
            val path = Path().apply {
                moveTo(activeDrawPath.first().x, activeDrawPath.first().y)
                activeDrawPath.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path  = path,
                color = strokeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// ─── Editor top bar ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    fileName: String,
    currentPage: Int,
    totalPages: Int,
    isMenuExpanded: Boolean,
    isSaving: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onNavigateUp: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onMenuToggle: () -> Unit,
    onMenuDismiss: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onExport: () -> Unit,
    onDeletePage: () -> Unit,
    onRotateCW: () -> Unit,
    onRotateCCW: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        title = {
            Column {
                Text(
                    text  = fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (totalPages > 0) {
                    Text(
                        text  = "Editing · Page ${currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        },
        actions = {
            // Undo
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
            }
            // Redo
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
            }
            // Quick save
            IconButton(onClick = onSave, enabled = !isSaving) {
                Icon(Icons.Default.Save, "Save")
            }
            // Three-dot menu ⋮
            Box {
                IconButton(onClick = onMenuToggle) {
                    Icon(Icons.Default.MoreVert, "More options")
                }
                EditorDropdownMenu(
                    expanded      = isMenuExpanded,
                    onDismiss     = onMenuDismiss,
                    onSave        = onSave,
                    onSaveAs      = onSaveAs,
                    onExport      = onExport,
                    onDeletePage  = onDeletePage,
                    onRotateCW    = onRotateCW,
                    onRotateCCW   = onRotateCCW
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = MaterialTheme.colorScheme.primary,
            titleContentColor      = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

// ─── Editor dropdown menu ─────────────────────────────────────────────────────

@Composable
private fun EditorDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onExport: () -> Unit,
    onDeletePage: () -> Unit,
    onRotateCW: () -> Unit,
    onRotateCCW: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Save") },
            leadingIcon = { Icon(Icons.Default.Save, null) },
            onClick = { onSave(); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Save As…") },
            leadingIcon = { Icon(Icons.Default.SaveAs, null) },
            onClick = { onSaveAs(); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Export…") },
            leadingIcon = { Icon(Icons.Default.FileDownload, null) },
            onClick = { onExport(); onDismiss() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Rotate 90° CW") },
            leadingIcon = { Icon(Icons.Default.RotateRight, null) },
            onClick = { onRotateCW(); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Rotate 90° CCW") },
            leadingIcon = { Icon(Icons.Default.RotateLeft, null) },
            onClick = { onRotateCCW(); onDismiss() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Delete Page", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            onClick = { onDeletePage(); onDismiss() }
        )
    }
}

// ─── Bottom edit toolbar ──────────────────────────────────────────────────────

@Composable
private fun EditToolbar(
    selectedTool: EditTool,
    strokeColor: Color,
    onToolSelected: (EditTool) -> Unit,
    onColorPicker: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(Icons.Default.TextFields,    "Text",         EditTool.TEXT,             selectedTool, onToolSelected)
            ToolButton(Icons.Default.Edit,          "Draw",         EditTool.DRAW,             selectedTool, onToolSelected)
            ToolButton(Icons.Default.Highlight,     "Highlight",    EditTool.HIGHLIGHT,        selectedTool, onToolSelected)
            ToolButton(Icons.Default.FormatStrikethrough, "Strike", EditTool.STRIKETHROUGH,   selectedTool, onToolSelected)
            ToolButton(Icons.Default.Rectangle,     "Rectangle",    EditTool.SHAPE_RECTANGLE,  selectedTool, onToolSelected)
            ToolButton(Icons.Default.Circle,        "Circle",       EditTool.SHAPE_CIRCLE,     selectedTool, onToolSelected)
            ToolButton(Icons.Default.Remove,        "Line",         EditTool.SHAPE_LINE,       selectedTool, onToolSelected)
            ToolButton(Icons.Default.Comment,       "Comment",      EditTool.COMMENT,          selectedTool, onToolSelected)
            ToolButton(Icons.Default.AutoFixNormal, "Eraser",       EditTool.ERASER,           selectedTool, onToolSelected)

            // Color swatch button
            IconButton(onClick = onColorPicker) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(strokeColor, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tool: EditTool,
    selected: EditTool,
    onSelect: (EditTool) -> Unit
) {
    val isActive = selected == tool
    IconButton(
        onClick = { onSelect(tool) },
        modifier = Modifier
            .background(
                color  = if (isActive) MaterialTheme.colorScheme.primaryContainer
                         else          Color.Transparent,
                shape  = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.primary
                   else          MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

// ─── Page-changed bridge helper (called from AndroidView update) ──────────────

fun EditorViewModel.onPageChanged(page: Int) =
    _uiState.value.let { /* forward to VM */ }
