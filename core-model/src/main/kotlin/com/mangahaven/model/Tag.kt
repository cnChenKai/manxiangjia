package com.mangahaven.model

import kotlinx.serialization.Serializable

/**
 * 标签模型。
 * 用于对书架条目进行分类管理。
 */
@Serializable
data class Tag(
    val id: String,
    val name: String,
    /** 标签颜色，十六进制格式，例如 #FF5722 */
    val color: String = "#607D8B",
)

/**
 * 标签仓库接口。
 * 定义标签相关的数据操作。
 */
interface TagRepository {
    /** 观察所有标签 */
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<Tag>>

    /** 根据 ID 获取标签 */
    suspend fun getById(id: String): Tag?

    /** 添加或更新标签 */
    suspend fun save(tag: Tag)

    /** 删除标签 */
    suspend fun delete(id: String)

    /** 获取指定条目的所有标签 */
    suspend fun getTagsForItem(itemId: String): List<Tag>

    /** 观察指定条目的所有标签 */
    fun observeTagsForItem(itemId: String): kotlinx.coroutines.flow.Flow<List<Tag>>

    /** 为条目添加标签 */
    suspend fun addTagToItem(itemId: String, tagId: String)

    /** 移除条目的标签 */
    suspend fun removeTagFromItem(itemId: String, tagId: String)

    /** 获取带有指定标签的所有条目 ID */
    suspend fun getItemIdsByTag(tagId: String): List<String>
}
