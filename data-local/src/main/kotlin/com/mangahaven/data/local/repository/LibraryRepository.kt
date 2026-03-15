package com.mangahaven.data.local.repository

import com.mangahaven.data.local.dao.LibraryItemDao
import com.mangahaven.data.local.dao.SourceDao
import com.mangahaven.data.local.mapper.toEntity
import com.mangahaven.data.local.mapper.toModel
import com.mangahaven.model.LibraryItem
import com.mangahaven.model.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书架仓库。
 * 封装数据库操作，提供干净的领域模型接口。
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val libraryItemDao: LibraryItemDao,
    private val sourceDao: SourceDao,
) {

    /**
     * 观察全部书架条目，按更新时间降序。
     */
    fun observeAllItems(): Flow<List<LibraryItem>> =
        libraryItemDao.observeAll().map { entities ->
            entities.map { it.toModel() }
        }

    /**
     * 观察最近阅读的条目。
     */
    fun observeRecentlyRead(limit: Int = 10): Flow<List<LibraryItem>> =
        libraryItemDao.observeRecentlyRead(limit).map { entities ->
            entities.map { it.toModel() }
        }

    /**
     * 按标题搜索条目。
     */
    fun searchByTitle(query: String): Flow<List<LibraryItem>> =
        libraryItemDao.searchByTitle(query).map { entities ->
            entities.map { it.toModel() }
        }

    /**
     * 根据 ID 获取单个条目。
     */
    suspend fun getItemById(id: String): LibraryItem? =
        libraryItemDao.getById(id)?.toModel()

    /**
     * 添加或更新一个书架条目。
     */
    suspend fun addItem(item: LibraryItem) {
        libraryItemDao.insert(item.toEntity())
    }

    /**
     * 批量添加书架条目。
     */
    suspend fun addItems(items: List<LibraryItem>) {
        libraryItemDao.insertAll(items.map { it.toEntity() })
    }

    /**
     * 更新书架条目。
     */
    suspend fun updateItem(item: LibraryItem) {
        libraryItemDao.update(item.toEntity())
    }

    /**
     * 删除书架条目。
     */
    suspend fun removeItem(id: String) {
        libraryItemDao.deleteById(id)
    }

    /**
     * 获取条目总数。
     */
    suspend fun count(): Int = libraryItemDao.count()

    // --- Source 操作 ---

    /**
     * 添加或更新一个内容源。
     */
    suspend fun addSource(source: Source) {
        sourceDao.insert(source.toEntity())
    }

    /**
     * 根据 ID 获取内容源。
     */
    suspend fun getSource(id: String): Source? =
        sourceDao.getById(id)?.toModel()

    /**
     * 观察全部内容源
     */
    fun observeAllSources(): Flow<List<Source>> =
        sourceDao.observeAll().map { entities -> 
            entities.map { it.toModel() }
        }

    /**
     * 删除内容源
     */
    suspend fun removeSource(id: String) {
        sourceDao.deleteById(id)
    }
}
