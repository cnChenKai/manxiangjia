package com.mangahaven.data.local.di

import android.content.Context
import androidx.room.Room
import com.mangahaven.data.local.AppDatabase
import com.mangahaven.data.local.AppSettingsDataStore
import com.mangahaven.data.local.IAppSettingsDataStore
import com.mangahaven.data.local.ISettingsDataStore
import com.mangahaven.data.local.SettingsDataStore
import com.mangahaven.data.local.dao.ItemSettingsDao
import com.mangahaven.data.local.dao.LibraryItemDao
import com.mangahaven.data.local.dao.ReadingProgressDao
import com.mangahaven.data.local.dao.SnapshotDao
import com.mangahaven.data.local.dao.SourceDao
import com.mangahaven.data.local.dao.TagDao
import com.mangahaven.data.local.repository.LocalTagRepository
import com.mangahaven.model.TagRepository
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
            .addMigrations(AppDatabase.MIGRATION_4_5)
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
    fun provideItemSettingsDao(database: AppDatabase): ItemSettingsDao {
        return database.itemSettingsDao()
    }

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    fun provideSnapshotDao(database: AppDatabase): SnapshotDao {
        return database.snapshotDao()
    }

    @Provides
    @Singleton
    fun provideTagRepository(tagDao: TagDao): TagRepository {
        return LocalTagRepository(tagDao)
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideISettingsDataStore(impl: SettingsDataStore): ISettingsDataStore = impl

    @Provides
    @Singleton
    fun provideIAppSettingsDataStore(impl: AppSettingsDataStore): IAppSettingsDataStore = impl
}
