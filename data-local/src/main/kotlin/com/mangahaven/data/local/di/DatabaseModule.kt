package com.mangahaven.data.local.di

import android.content.Context
import androidx.room.Room
import com.mangahaven.data.local.AppDatabase
import com.mangahaven.data.local.SettingsDataStore
import com.mangahaven.data.local.dao.LibraryItemDao
import com.mangahaven.data.local.dao.ReadingProgressDao
import com.mangahaven.data.local.dao.SourceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 模块：提供 Database、DAO、DataStore 实例。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mangahaven.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSourceDao(database: AppDatabase): SourceDao {
        return database.sourceDao()
    }

    @Provides
    fun provideLibraryItemDao(database: AppDatabase): LibraryItemDao {
        return database.libraryItemDao()
    }

    @Provides
    fun provideReadingProgressDao(database: AppDatabase): ReadingProgressDao {
        return database.readingProgressDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }
}
