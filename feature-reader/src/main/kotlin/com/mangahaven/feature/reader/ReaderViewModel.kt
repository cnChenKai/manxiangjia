package com.mangahaven.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangahaven.data.files.PageProvider
import com.mangahaven.data.files.ProgressRepository
import com.mangahaven.data.files.provider.PageProviderFactory
import com.mangahaven.data.local.SettingsDataStore
import com.mangahaven.data.local.repository.ItemSettingsRepository
import com.mangahaven.data.local.repository.LibraryRepository
import com.mangahaven.data.local.repository.ResolvedReaderSettings
import com.mangahaven.data.files.thumbnail.ThumbnailGenerator
import com.mangahaven.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val progressRepository: ProgressRepository,
    private val pageProviderFactory: PageProviderFactory,
    private val itemSettingsRepository: ItemSettingsRepository,
    private val settingsDataStore: SettingsDataStore,
    val thumbnailGenerator: ThumbnailGenerator,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    private val _uiState = MutableStateFlow(ReaderUiState())
    
    // 监听独立设置/全局设置合并结果
    val settings = itemSettingsRepository.resolveSettings(itemId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ResolvedReaderSettings(
                readingMode = ReadingMode.LEFT_TO_RIGHT,
                enableCrop = false,
                doublePageMode = false,
                pageOffset = 0,
                keepScreenOn = true,
                enablePreload = true,
                isOverridden = false
            )
        )

    val uiState = combine(_uiState, settings) { state, resolvedSettings ->
        state.copy(settings = resolvedSettings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderUiState()
    )

    private var saveProgressJob: Job? = null

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val item = libraryRepository.getItemById(itemId) ?: return@launch
            
            // 建立文件读取 Provider
            val provider = pageProviderFactory.create(
                itemId = item.id,
                sourceId = item.sourceId,
                path = item.path,
                itemType = item.itemType
            )
            
            val totalPages = provider.getPageCount()
            val savedProgress = progressRepository.getProgress(itemId)
            val startPage = savedProgress?.currentPage?.coerceIn(0, (totalPages - 1).coerceAtLeast(0)) ?: 0
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    book = item,
                    pageProvider = provider,
                    currentPage = startPage,
                    totalPages = totalPages
                )
            }
            
            // 更新最后阅读时间
            libraryRepository.updateItem(item.copy(
                lastReadAt = System.currentTimeMillis(),
                readingStatus = com.mangahaven.model.ReadingStatus.READING
            ))
        }
    }

    /**
     * 当翻页滚动时由 UI 调用
     */
    fun onPageChanged(pageIndex: Int) {
        if (_uiState.value.currentPage == pageIndex) return
        
        _uiState.update { it.copy(currentPage = pageIndex) }
        
        // 防抖保存进度
        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            delay(500)
            progressRepository.saveProgress(
                ReadingProgress(
                    itemId = itemId,
                    currentPage = pageIndex,
                    totalPages = _uiState.value.totalPages,
                    readingMode = settings.value.readingMode,
                    updatedAt = System.currentTimeMillis()
                )
            )
            // 更新数据库状态（如果是最后一页则标记已读）
            if (pageIndex == _uiState.value.totalPages - 1) {
                _uiState.value.book?.let {
                    libraryRepository.updateItem(it.copy(
                        readingStatus = com.mangahaven.model.ReadingStatus.COMPLETED
                    ))
                }
            }
        }
    }
    
    // --- 独立设置与全局设置管理 ---
    
    fun updateReadingMode(mode: ReadingMode, saveAsGlobal: Boolean) {
        viewModelScope.launch {
            if (saveAsGlobal) {
                val current = settingsDataStore.settingsFlow.first()
                settingsDataStore.updateSettings(current.copy(readingMode = mode))
                // 如果是存为全局，则移除当前覆盖以跟随全局
                removeOverrideIfAny()
            } else {
                saveOrUpdateOverride { it.copy(readingMode = mode) }
            }
        }
    }

    fun toggleCrop(enabled: Boolean, saveAsGlobal: Boolean) {
        viewModelScope.launch {
            if (saveAsGlobal) {
                val current = settingsDataStore.settingsFlow.first()
                settingsDataStore.updateSettings(current.copy(enableCrop = enabled))
                removeOverrideIfAny()
            } else {
                saveOrUpdateOverride { it.copy(cropEnabled = enabled) }
            }
        }
    }

    fun toggleDoublePage(enabled: Boolean, saveAsGlobal: Boolean) {
        viewModelScope.launch {
            if (saveAsGlobal) {
                val current = settingsDataStore.settingsFlow.first()
                settingsDataStore.updateSettings(current.copy(doublePageMode = enabled))
                removeOverrideIfAny()
            } else {
                saveOrUpdateOverride { it.copy(doublePageMode = enabled) }
            }
        }
    }

    fun toggleVolumeKeysPaging(enabled: Boolean, saveAsGlobal: Boolean) {
        viewModelScope.launch {
            if (saveAsGlobal) {
                val current = settingsDataStore.settingsFlow.first()
                settingsDataStore.updateSettings(current.copy(volumeKeysPaging = enabled))
                removeOverrideIfAny()
            } else {
                saveOrUpdateOverride { it.copy(volumeKeysPaging = enabled) }
            }
        }
    }

    fun updatePageOffset(offset: Int) {
        // 页码偏移只能是局部设置
        viewModelScope.launch { saveOrUpdateOverride { it.copy(pageOffset = offset) } }
    }

    fun resetToGlobalSettings() {
        viewModelScope.launch {
            itemSettingsRepository.resetToGlobal(itemId)
        }
    }

    private suspend fun saveOrUpdateOverride(updater: (ItemReaderSettingsOverride) -> ItemReaderSettingsOverride) {
        val current = itemSettingsRepository.getOverride(itemId) ?: ItemReaderSettingsOverride(itemId)
        itemSettingsRepository.saveOverride(updater(current))
    }

    private suspend fun removeOverrideIfAny() {
        val current = itemSettingsRepository.getOverride(itemId)
        if (current != null) {
            itemSettingsRepository.resetToGlobal(itemId)
        }
    }
}

data class ReaderUiState(
    val isLoading: Boolean = true,
    val book: LibraryItem? = null,
    val pageProvider: PageProvider? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val settings: ResolvedReaderSettings? = null
)
