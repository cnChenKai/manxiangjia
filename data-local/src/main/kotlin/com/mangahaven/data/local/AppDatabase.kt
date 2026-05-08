package com.mangahaven.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mangahaven.data.local.dao.ItemSettingsDao
import com.mangahaven.data.local.dao.LibraryItemDao
import com.mangahaven.data.local.dao.ReadingProgressDao
import com.mangahaven.data.local.dao.SourceDao
import com.mangahaven.data.local.dao.TagDao
import com.mangahaven.data.local.dao.SnapshotDao
import com.mangahaven.data.local.entity.ItemSettingsEntity
import com.mangahaven.data.local.entity.LibraryItemEntity
import com.mangahaven.data.local.entity.LibraryItemTag
import com.mangahaven.data.local.entity.ReadingProgressEntity
import com.mangahaven.data.local.entity.SnapshotEntity
import com.mangahaven.data.local.entity.SourceEntity
import com.mangahaven.data.local.entity.TagEntity

/**
 * MangaHaven Room 数据库。
 */
@Database(
    entities = [
        SourceEntity::class,
        LibraryItemEntity::class,
        ReadingProgressEntity::class,
        ItemSettingsEntity::class,
        TagEntity::class,
        LibraryItemTag::class,
        SnapshotEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun libraryItemDao(): LibraryItemDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun itemSettingsDao(): ItemSettingsDao
    abstract fun tagDao(): TagDao
    abstract fun snapshotDao(): SnapshotDao

    companion object {
        /** 从版本 4 升级到 5：新增标签表、关联表和快照表 */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 给 sources 表添加 isVirtual 列
                db.execSQL("ALTER TABLE `sources` ADD COLUMN `isVirtual` INTEGER NOT NULL DEFAULT 0")
                // 创建标签表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT NOT NULL DEFAULT '#607D8B',
                        PRIMARY KEY(`id`)
                    )
                """)
                // 创建条目-标签关联表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `library_item_tags` (
                        `itemId` TEXT NOT NULL,
                        `tagId` TEXT NOT NULL,
                        PRIMARY KEY(`itemId`, `tagId`),
                        FOREIGN KEY(`itemId`) REFERENCES `library_items`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_item_tags_itemId` ON `library_item_tags` (`itemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_item_tags_tagId` ON `library_item_tags` (`tagId`)")
                // 创建快照表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `snapshots` (
                        `id` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `path` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `normalizedTitle` TEXT NOT NULL,
                        `tagsJson` TEXT NOT NULL DEFAULT '[]',
                        `pageCount` INTEGER,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`sourceId`) REFERENCES `sources`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_snapshots_sourceId` ON `snapshots` (`sourceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_snapshots_normalizedTitle` ON `snapshots` (`normalizedTitle`)")
            }
        }
    }
}
