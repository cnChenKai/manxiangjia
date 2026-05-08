package com.mangahaven.data.local.repository

import com.mangahaven.data.local.dao.TagDao
import com.mangahaven.data.local.entity.LibraryItemTag
import com.mangahaven.data.local.entity.TagEntity
import com.mangahaven.model.Tag
import com.mangahaven.model.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 标签仓库实现。
 * 封装数据库操作，提供干净的领域模型接口。
 * 注意：不由 @Inject 构造，由 DatabaseModule.provideTagRepository() 提供。
 */
class LocalTagRepository(
    private val tagDao: TagDao,
) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> =
        tagDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun getById(id: String): Tag? =
        tagDao.getById(id)?.toModel()

    override suspend fun save(tag: Tag) {
        tagDao.insert(tag.toEntity())
    }

    override suspend fun delete(id: String) {
        tagDao.deleteById(id)
    }

    override suspend fun getTagsForItem(itemId: String): List<Tag> =
        tagDao.getTagsForItem(itemId).map { it.toModel() }

    override fun observeTagsForItem(itemId: String): Flow<List<Tag>> =
        tagDao.observeTagsForItem(itemId).map { entities -> entities.map { it.toModel() } }

    override suspend fun addTagToItem(itemId: String, tagId: String) {
        tagDao.insertItemTag(LibraryItemTag(itemId = itemId, tagId = tagId))
    }

    override suspend fun removeTagFromItem(itemId: String, tagId: String) {
        tagDao.deleteItemTagByIds(itemId, tagId)
    }

    override suspend fun getItemIdsByTag(tagId: String): List<String> =
        tagDao.getItemIdsByTag(tagId)
}

/** TagEntity -> Tag */
private fun TagEntity.toModel(): Tag = Tag(
    id = id,
    name = name,
    color = color,
)

/** Tag -> TagEntity */
private fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    color = color,
)
