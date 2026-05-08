package com.mangahaven.data.files.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mangahaven.data.files.importer.RemoteScanner
import com.mangahaven.data.files.remote.SourceClientFactory
import com.mangahaven.data.files.sync.SnapshotBuilder
import com.mangahaven.data.local.repository.LibraryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * 后台同步 Worker。
 * 定期遍历所有已配置的远端 Source 并在静默状态下执行深度扫描，以自动更新书架。
 * 同时构建快照用于增量同步和变更检测。
 */
@HiltWorker
class LibrarySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val libraryRepository: LibraryRepository,
    private val sourceClientFactory: SourceClientFactory,
    private val remoteScanner: RemoteScanner,
    private val snapshotBuilder: SnapshotBuilder,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.i("Starting background Library Sync Worker...")

        return try {
            val sources = libraryRepository.observeAllSources().first()
            var totalAdded = 0
            var totalSnapshots = 0

            for (source in sources) {
                if (source.type == com.mangahaven.model.SourceType.LOCAL) continue

                try {
                    val client = sourceClientFactory.create(source)
                    Timber.d("Syncing remote source: ${source.name}")

                    // 扫描并导入新条目
                    val added = remoteScanner.scanDirectory(source, client, "/") { progress ->
                        Timber.d("Worker Scan Progress: $progress")
                    }
                    totalAdded += added

                    // 构建快照用于增量同步
                    val snapshotCount = snapshotBuilder.buildSnapshot(source, client) { progress ->
                        Timber.d("Snapshot Progress: $progress")
                    }
                    totalSnapshots += snapshotCount
                } catch (e: Exception) {
                    Timber.w(e, "Skipped syncing source: ${source.name} due to network unreachable.")
                }
            }

            Timber.i("Background Library Sync completed. Added $totalAdded new comics, built $totalSnapshots snapshots.")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Background Library Sync failed.")
            Result.retry()
        }
    }
}
