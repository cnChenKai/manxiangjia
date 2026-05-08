package com.mangahaven.data.files.sync

import com.mangahaven.data.files.SourceClient
import com.mangahaven.data.files.container.ImageFileUtils
import com.mangahaven.data.local.repository.LocalSnapshotRepository
import com.mangahaven.model.Snapshot
import com.mangahaven.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 快照构建器。
 * 扫描远程源目录，构建条目快照用于增量同步和变更检测。
 */
@Singleton
class SnapshotBuilder @Inject constructor(
    private val snapshotRepository: LocalSnapshotRepository,
) {

    /**
     * 为指定源构建完整快照。
     * 扫描远程目录，生成快照并与已有快照进行比对。
     *
     * @return 新增的快照数量
     */
    suspend fun buildSnapshot(
        source: Source,
        sourceClient: SourceClient,
        basePath: String = "/",
        onProgress: (String) -> Unit = {},
    ): Int = withContext(Dispatchers.IO) {
        val snapshots = mutableListOf<Snapshot>()
        try {
            scanDirectory(sourceClient, source.id, basePath, snapshots, onProgress)
        } catch (e: Exception) {
            Timber.e(e, "构建快照失败: ${source.name}")
        }

        if (snapshots.isNotEmpty()) {
            // 删除旧快照并插入新快照
            snapshotRepository.deleteBySource(source.id)
            snapshotRepository.saveAll(snapshots)
            Timber.i("源 ${source.name} 快照更新：共 ${snapshots.size} 个条目")
        }

        snapshots.size
    }

    /**
     * 增量比对：返回新增的快照列表。
     */
    suspend fun findNewEntries(
        sourceId: String,
        newSnapshots: List<Snapshot>,
    ): List<Snapshot> {
        val existing = snapshotRepository.getBySource(sourceId)
        val existingPaths = existing.map { it.path }.toSet()
        return newSnapshots.filter { it.path !in existingPaths }
    }

    private suspend fun scanDirectory(
        client: SourceClient,
        sourceId: String,
        path: String,
        result: MutableList<Snapshot>,
        onProgress: (String) -> Unit,
    ) {
        val entries = client.list(path)
        for (entry in entries) {
            if (ImageFileUtils.shouldIgnore(entry.name)) continue

            if (entry.isDirectory) {
                onProgress("扫描: ${entry.path}")
                val subEntries = client.list(entry.path)
                val imgCount = subEntries.count { !it.isDirectory && ImageFileUtils.isImageFile(it.name) }
                val dirCount = subEntries.count { it.isDirectory && !ImageFileUtils.shouldIgnore(it.name) }

                if (imgCount > 0 && dirCount == 0) {
                    // 纯图片目录，作为漫画条目
                    result.add(createSnapshot(sourceId, entry.path, entry.name, imgCount))
                } else if (dirCount > 0) {
                    // 递归扫描子目录
                    scanDirectory(client, sourceId, entry.path, result, onProgress)
                }
            } else if (ImageFileUtils.isArchive(entry.name)) {
                // 压缩包文件
                result.add(createSnapshot(sourceId, entry.path, entry.name.substringBeforeLast('.')))
            }
        }
    }

    private fun createSnapshot(sourceId: String, path: String, title: String, pageCount: Int? = null): Snapshot {
        val snapshotId = UUID.nameUUIDFromBytes("$sourceId:$path".toByteArray()).toString()
        return Snapshot(
            id = snapshotId,
            sourceId = sourceId,
            path = path,
            title = title,
            normalizedTitle = normalizeTitle(title),
            pageCount = pageCount,
            updatedAt = System.currentTimeMillis(),
        )
    }

    /** 标准化标题用于比对 */
    private fun normalizeTitle(title: String): String {
        return title.trim()
            .lowercase()
            .replace(Regex("[\\[\\]【】（）()\\s]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
