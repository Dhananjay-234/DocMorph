package com.docmorph.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted record of recently opened PDFs shown on the Home screen.
 * The URI string is used as the unique identifier.
 */
@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey
    val uriString: String,
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val lastOpened: Long = System.currentTimeMillis()
)
