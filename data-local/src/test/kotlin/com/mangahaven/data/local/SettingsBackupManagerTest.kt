package com.mangahaven.data.local

import com.mangahaven.data.local.dao.SourceDao
import com.mangahaven.data.local.entity.SourceEntity
import com.mangahaven.model.AppSettings
import com.mangahaven.model.ReaderSettings
import com.mangahaven.model.ReadingMode
import com.mangahaven.model.SettingsExportData
import com.mangahaven.model.Source
import com.mangahaven.model.SourceType
import com.mangahaven.model.TapZoneProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsBackupManagerTest {

    private lateinit var fakeSourceDao: FakeSourceDao
    private lateinit var fakeSettingsDataStore: FakeSettingsDataStore
    private lateinit var fakeAppSettingsDataStore: FakeAppSettingsDataStore
    private lateinit var manager: SettingsBackupManager
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Before
    fun setup() {
        fakeSourceDao = FakeSourceDao()
        fakeSettingsDataStore = FakeSettingsDataStore()
        fakeAppSettingsDataStore = FakeAppSettingsDataStore()
        manager = SettingsBackupManager(
            settingsDataStore = fakeSettingsDataStore,
            appSettingsDataStore = fakeAppSettingsDataStore,
            sourceDao = fakeSourceDao,
        )
    }

    // --- isMigratable ---

    @Test
    fun `isMigratable returns true for WEBDAV`() {
        assertTrue(SettingsBackupManager.isMigratable(SourceType.WEBDAV))
    }

    @Test
    fun `isMigratable returns true for SMB`() {
        assertTrue(SettingsBackupManager.isMigratable(SourceType.SMB))
    }

    @Test
    fun `isMigratable returns true for OPDS`() {
        assertTrue(SettingsBackupManager.isMigratable(SourceType.OPDS))
    }

    @Test
    fun `isMigratable returns false for LOCAL`() {
        assertFalse(SettingsBackupManager.isMigratable(SourceType.LOCAL))
    }

    @Test
    fun `isMigratable returns false for SAF_TREE`() {
        assertFalse(SettingsBackupManager.isMigratable(SourceType.SAF_TREE))
    }

    // --- Export: 只包含远程源 ---

    @Test
    fun `export only includes migratable sources`() = runTest {
        fakeSourceDao.seed(
            SourceEntity("1", "WEBDAV", "WebDAV Server", "http://example.com", "user:pass"),
            SourceEntity("2", "SMB", "SMB Share", "192.168.1.5", "admin:pw"),
            SourceEntity("3", "LOCAL", "Local Folder", "/sdcard/comics", null),
            SourceEntity("4", "SAF_TREE", "SAF Tree", "content://tree/1", null),
            SourceEntity("5", "OPDS", "OPDS Catalog", "http://opds.example.com", null),
        )

        val resultJson = manager.exportToJson()
        val data = json.decodeFromString<SettingsExportData>(resultJson)

        assertEquals(3, data.sources.size)
        val types = data.sources.map { it.type }.toSet()
        assertTrue(SourceType.WEBDAV in types)
        assertTrue(SourceType.SMB in types)
        assertTrue(SourceType.OPDS in types)
        assertFalse(SourceType.LOCAL in types)
        assertFalse(SourceType.SAF_TREE in types)
    }

    @Test
    fun `export includes reader settings and app settings`() = runTest {
        fakeSettingsDataStore.updateSettings(
            ReaderSettings(
                readingMode = ReadingMode.RIGHT_TO_LEFT,
                enableCrop = true,
                enablePreload = false,
                doublePageMode = true,
                keepScreenOn = false,
                volumeKeysPaging = true,
                tapZoneProfile = TapZoneProfile.L_SHAPE,
            )
        )
        fakeAppSettingsDataStore.setPrivacyLockEnabled(true)

        val resultJson = manager.exportToJson()
        val data = json.decodeFromString<SettingsExportData>(resultJson)

        assertEquals(ReadingMode.RIGHT_TO_LEFT, data.readerSettings.readingMode)
        assertTrue(data.readerSettings.enableCrop)
        assertFalse(data.readerSettings.enablePreload)
        assertTrue(data.readerSettings.doublePageMode)
        assertFalse(data.readerSettings.keepScreenOn)
        assertTrue(data.appSettings.privacyLockEnabled)
    }

    @Test
    fun `export produces valid JSON`() = runTest {
        fakeSourceDao.seed(
            SourceEntity("1", "WEBDAV", "Server", "http://example.com", "user:pass"),
        )

        val resultJson = manager.exportToJson()

        // 不应抛异常
        json.decodeFromString<SettingsExportData>(resultJson)
    }

    // --- Import: 去重逻辑 ---

    @Test
    fun `import inserts new sources`() = runTest {
        val data = SettingsExportData(
            sources = listOf(
                Source("new-1", SourceType.WEBDAV, "Server A", "http://a.com", "u:p"),
                Source("new-2", SourceType.SMB, "Server B", "192.168.1.1", null),
            )
        )

        val result = manager.importFromJson(json.encodeToString(data))

        assertEquals(2, result.insertedSources)
        assertEquals(0, result.updatedSources)
    }

    @Test
    fun `import updates existing sources with same type and configJson`() = runTest {
        fakeSourceDao.seed(
            SourceEntity("existing-id", "WEBDAV", "Old Name", "http://same.com", "old:pass"),
        )

        val data = SettingsExportData(
            sources = listOf(
                Source("new-id", SourceType.WEBDAV, "New Name", "http://same.com", "new:pass"),
            )
        )

        val result = manager.importFromJson(json.encodeToString(data))

        assertEquals(0, result.insertedSources)
        assertEquals(1, result.updatedSources)
        // 验证保留了原 id
        val inserted = fakeSourceDao.lastInserted!!
        assertEquals("existing-id", inserted.id)
        assertEquals("New Name", inserted.name)
        assertEquals("new:pass", inserted.authRef)
    }

    @Test
    fun `import does not duplicate when same type but different configJson`() = runTest {
        fakeSourceDao.seed(
            SourceEntity("id-1", "WEBDAV", "Server A", "http://a.com", "u:p"),
        )

        val data = SettingsExportData(
            sources = listOf(
                Source("id-2", SourceType.WEBDAV, "Server B", "http://b.com", "u:p"),
            )
        )

        val result = manager.importFromJson(json.encodeToString(data))

        assertEquals(1, result.insertedSources)
        assertEquals(0, result.updatedSources)
    }

    @Test
    fun `import restores reader and app settings`() = runTest {
        val data = SettingsExportData(
            readerSettings = ReaderSettings(
                readingMode = ReadingMode.RIGHT_TO_LEFT,
                enableCrop = true,
                doublePageMode = true,
            ),
            appSettings = AppSettings(privacyLockEnabled = true),
        )

        manager.importFromJson(json.encodeToString(data))

        val settings = fakeSettingsDataStore.settingsFlow.first()
        assertEquals(ReadingMode.RIGHT_TO_LEFT, settings.readingMode)
        assertTrue(settings.enableCrop)
        assertTrue(settings.doublePageMode)
        assertTrue(fakeAppSettingsDataStore.privacyLockEnabledFlow.first())
    }

    // --- JSON 前向兼容 ---

    @Test
    fun `import ignores unknown fields in JSON`() = runTest {
        val jsonStr = """
        {
            "version": 99,
            "futureField": "should be ignored",
            "readerSettings": {
                "readingMode": "LEFT_TO_RIGHT",
                "unknownReaderProp": 42
            },
            "appSettings": {
                "privacyLockEnabled": false
            },
            "sources": []
        }
        """.trimIndent()

        val result = manager.importFromJson(jsonStr)

        assertEquals(0, result.insertedSources)
        assertEquals(0, result.updatedSources)
    }

    // --- 往返测试 ---

    @Test
    fun `export then import roundtrip preserves settings`() = runTest {
        // 设置初始状态
        fakeSettingsDataStore.updateSettings(
            ReaderSettings(
                readingMode = ReadingMode.RIGHT_TO_LEFT,
                enableCrop = true,
                enablePreload = false,
                doublePageMode = true,
                keepScreenOn = false,
                volumeKeysPaging = true,
                tapZoneProfile = TapZoneProfile.KINDLE_STYLE,
            )
        )
        fakeAppSettingsDataStore.setPrivacyLockEnabled(true)
        fakeSourceDao.seed(
            SourceEntity("1", "WEBDAV", "My Server", "http://my.server.com", "admin:secret"),
            SourceEntity("2", "SMB", "NAS", "10.0.0.1", "user:pw"),
        )

        // 导出
        val exportedJson = manager.exportToJson()

        // 重置状态
        fakeSettingsDataStore.updateSettings(ReaderSettings())
        fakeAppSettingsDataStore.setPrivacyLockEnabled(false)
        fakeSourceDao.clear()

        // 导入
        val result = manager.importFromJson(exportedJson)

        // 验证设置恢复
        val settings = fakeSettingsDataStore.settingsFlow.first()
        assertEquals(ReadingMode.RIGHT_TO_LEFT, settings.readingMode)
        assertTrue(settings.enableCrop)
        assertFalse(settings.enablePreload)
        assertTrue(settings.doublePageMode)
        assertFalse(settings.keepScreenOn)
        assertTrue(settings.volumeKeysPaging)
        assertEquals(TapZoneProfile.KINDLE_STYLE, settings.tapZoneProfile)
        assertTrue(fakeAppSettingsDataStore.privacyLockEnabledFlow.first())

        // 验证源恢复
        assertEquals(2, result.insertedSources)
        assertEquals(0, result.updatedSources)
        assertEquals(2, fakeSourceDao.insertedCount)
    }

    // --- Fakes ---

    /**
     * Fake SourceDao，支持种子数据和按 type+configJson 查找。
     */
    private class FakeSourceDao : SourceDao {
        private val sources = mutableListOf<SourceEntity>()
        var insertedCount = 0
            private set
        var lastInserted: SourceEntity? = null
            private set

        fun seed(vararg entities: SourceEntity) {
            sources.clear()
            sources.addAll(entities)
        }

        fun clear() {
            sources.clear()
            insertedCount = 0
            lastInserted = null
        }

        override suspend fun getAll(): List<SourceEntity> = sources.toList()

        override suspend fun findByTypeAndConfig(type: String, configJson: String): SourceEntity? =
            sources.find { it.type == type && it.configJson == configJson }

        override suspend fun insert(source: SourceEntity) {
            val index = sources.indexOfFirst { it.id == source.id }
            if (index >= 0) {
                sources[index] = source
            } else {
                sources.add(source)
            }
            insertedCount++
            lastInserted = source
        }

        override fun observeAll(): Flow<List<SourceEntity>> = MutableStateFlow(sources.toList())
        override suspend fun getById(id: String): SourceEntity? = sources.find { it.id == id }
        override suspend fun update(source: SourceEntity) {
            val index = sources.indexOfFirst { it.id == source.id }
            if (index >= 0) sources[index] = source
        }
        override suspend fun delete(source: SourceEntity) { sources.remove(source) }
        override suspend fun deleteById(id: String) { sources.removeAll { it.id == id } }
    }

    /**
     * Fake SettingsDataStore，内存存储。
     */
    private class FakeSettingsDataStore : ISettingsDataStore {
        private val _settings = MutableStateFlow(ReaderSettings())

        override val settingsFlow: Flow<ReaderSettings> = _settings

        override suspend fun updateSettings(settings: ReaderSettings) {
            _settings.value = settings
        }
    }

    /**
     * Fake AppSettingsDataStore，内存存储。
     */
    private class FakeAppSettingsDataStore : IAppSettingsDataStore {
        private val _privacyLock = MutableStateFlow(false)

        override val privacyLockEnabledFlow: Flow<Boolean> = _privacyLock

        override suspend fun setPrivacyLockEnabled(enabled: Boolean) {
            _privacyLock.value = enabled
        }
    }
}
