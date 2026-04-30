package com.docmorph.app.data.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.docmorph.app.data.model.RecentFileEntity

@Database(
    entities  = [RecentFileEntity::class],
    version   = 1,
    exportSchema = true
)
abstract class DocMorphDatabase : RoomDatabase() {
    abstract fun recentFileDao(): RecentFileDao
}
