package com.mangahaven.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mangahaven.data.local.dao.ItemSettingsDao
import com.mangahaven.data.local.dao.LibraryItemDao
import com.mangahaven.data.local.dao.ReadingProgressDao
import com.mangahaven.data.local.dao.SourceDao
import com.mangahaven.data.local.entity.ItemSettingsEntity
import com.mangahaven.data.local.entity.LibraryItemEntity
import com.mangahaven.data.local.entity.ReadingProgressEntity
import com.mangahaven.data.local.entity.SourceEntity

/**
 * MangaHaven Room 数据库。
 */
@Database(
    entities = [
        SourceEntity::class,
        LibraryItemEntity::class,
        ReadingProgressEntity::class,
        ItemSettingsEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun libraryItemDao(): LibraryItemDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun itemSettingsDao(): ItemSettingsDao
}
