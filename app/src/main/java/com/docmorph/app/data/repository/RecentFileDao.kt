package com.docmorph.app.data.repository

import androidx.room.*
import com.docmorph.app.data.model.RecentFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {

    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC LIMIT 20")
    fun getAllRecent(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: RecentFileEntity)

    @Delete
    suspend fun delete(file: RecentFileEntity)

    @Query("DELETE FROM recent_files")
    suspend fun clearAll()
}
