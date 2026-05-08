package com.mangahaven.data.local.repository

import com.mangahaven.data.local.dao.SnapshotDao
import com.mangahaven.data.local.entity.SnapshotEntity
import com.mangahaven.model.Snapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 快照仓库。
 * 封装快照数据库操作。
 */
@Singleton
class LocalSnapshotRepository @Inject constructor(
    private val snapshotDao: SnapshotDao,
) {

    fun observeBySource(sourceId: String): Flow<List<Snapshot>> =
        snapshotDao.observeBySource(sourceId).map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun getBySource(sourceId: String): List<Snapshot> =
        snapshotDao.getBySource(sourceId).map { it.toModel() }

    suspend fun getById(id: String): Snapshot? =
        snapshotDao.getById(id)?.toModel()

    suspend fun save(snapshot: Snapshot) {
        snapshotDao.insert(snapshot.toEntity())
    }

    suspend fun saveAll(snapshots: List<Snapshot>) {
        snapshotDao.insertAll(snapshots.map { it.toEntity() })
    }

    suspend fun delete(id: String) {
        snapshotDao.deleteById(id)
    }

    suspend fun deleteBySource(sourceId: String) {
        snapshotDao.deleteBySource(sourceId)
    }

    suspend fun countBySource(sourceId: String): Int =
        snapshotDao.countBySource(sourceId)

    suspend fun searchByTitle(query: String): List<Snapshot> =
        snapshotDao.searchByTitle(query).map { it.toModel() }
}

private val json = Json { ignoreUnknownKeys = true }

private fun SnapshotEntity.toModel(): Snapshot = Snapshot(
    id = id,
    sourceId = sourceId,
    path = path,
    title = title,
    normalizedTitle = normalizedTitle,
    tags = try { json.decodeFromString<List<String>>(tagsJson) } catch (_: Exception) { emptyList() },
    pageCount = pageCount,
    updatedAt = updatedAt,
)

private fun Snapshot.toEntity(): SnapshotEntity = SnapshotEntity(
    id = id,
    sourceId = sourceId,
    path = path,
    title = title,
    normalizedTitle = normalizedTitle,
    tagsJson = json.encodeToString<List<String>>(tags),
    pageCount = pageCount,
    updatedAt = updatedAt,
)
