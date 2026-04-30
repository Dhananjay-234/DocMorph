package com.docmorph.app.di

import android.content.Context
import androidx.room.Room
import com.docmorph.app.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ─── Database ────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DocMorphDatabase =
        Room.databaseBuilder(
            context,
            DocMorphDatabase::class.java,
            "docmorph.db"
        )
            .fallbackToDestructiveMigration()   // acceptable for MVP; add migrations in Phase 2
            .build()

    @Provides
    fun provideRecentFileDao(db: DocMorphDatabase): RecentFileDao = db.recentFileDao()
}

// ─── Repository binding ──────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPdfRepository(impl: PdfRepositoryImpl): PdfRepository
}
