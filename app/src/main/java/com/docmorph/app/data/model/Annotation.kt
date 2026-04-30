package com.docmorph.app.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Sealed hierarchy for all overlay annotations placed on a PDF page.
 *
 * DocMorph uses a non-destructive overlay approach — the original PDF bytes are
 * never modified in memory; annotations are stored separately and flattened only
 * on save/export.
 */
sealed class Annotation {

    abstract val id: String
    abstract val pageIndex: Int

    // ─── Text box ───────────────────────────────────────────────────────────

    data class TextBox(
        override val id: String,
        override val pageIndex: Int,
        val text: String,
        val x: Float,
        val y: Float,
        val fontSize: Float = 14f,
        val color: Color = Color.Black,
        val isBold: Boolean = false,
        val isItalic: Boolean = false
    ) : Annotation()

    // ─── Highlight ───────────────────────────────────────────────────────────

    data class Highlight(
        override val id: String,
        override val pageIndex: Int,
        val startOffset: Offset,
        val endOffset: Offset,
        val color: Color = Color(0xFFFFEB3B)   // yellow
    ) : Annotation()

    // ─── Freehand drawing path ───────────────────────────────────────────────

    data class Drawing(
        override val id: String,
        override val pageIndex: Int,
        val path: List<Offset>,       // list of points; reconstructed into Path on render
        val strokeColor: Color = Color.Black,
        val strokeWidth: Float = 4f
    ) : Annotation()

    // ─── Shape ──────────────────────────────────────────────────────────────

    data class Shape(
        override val id: String,
        override val pageIndex: Int,
        val type: ShapeType,
        val startOffset: Offset,
        val endOffset: Offset,
        val color: Color = Color.Red,
        val strokeWidth: Float = 3f,
        val filled: Boolean = false
    ) : Annotation()

    // ─── Strikethrough ──────────────────────────────────────────────────────

    data class Strikethrough(
        override val id: String,
        override val pageIndex: Int,
        val startOffset: Offset,
        val endOffset: Offset,
        val color: Color = Color.Red
    ) : Annotation()

    // ─── Comment / sticky note ──────────────────────────────────────────────

    data class Comment(
        override val id: String,
        override val pageIndex: Int,
        val text: String,
        val anchorOffset: Offset
    ) : Annotation()
}

enum class ShapeType { RECTANGLE, CIRCLE, LINE, ARROW }

/** The current tool selected in Edit Mode. */
enum class EditTool {
    NONE,
    TEXT,
    DRAW,
    HIGHLIGHT,
    SHAPE_RECTANGLE,
    SHAPE_CIRCLE,
    SHAPE_LINE,
    STRIKETHROUGH,
    COMMENT,
    ERASER
}
