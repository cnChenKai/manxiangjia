package com.mangahaven.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangahaven.data.files.PageProvider
import com.mangahaven.data.files.ProgressRepository
import com.mangahaven.data.files.provider.PageProviderFactory
import com.mangahaven.data.local.repository.LibraryRepository
import com.mangahaven.model.LibraryItem
import com.mangahaven.model.ReadingMode
import com.mangahaven.model.ReadingProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val progressRepository: ProgressRepository,
    private val pageProviderFactory: PageProviderFactory,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

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
                path = item.path,
                itemType = item.itemType
            )
            
            val totalPages = provider.getPageCount()
            val savedProgress = progressRepository.getProgress(itemId)
            val startPage = savedProgress?.currentPage?.coerceIn(0, (totalPages - 1).coerceAtLeast(0)) ?: 0
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                book = item,
                pageProvider = provider,
                currentPage = startPage,
                totalPages = totalPages
            )
            
            // 更新最后阅读时间
            libraryRepository.updateItem(item.copy(
                lastReadAt = System.currentTimeMillis(),
                readingStatus = com.mangahaven.model.ReadingStatus.READING
            ))
        }
    }

    /**
     * 当 Pager 滚动翻页时由 UI 调用
     */
    fun onPageChanged(pageIndex: Int) {
        if (_uiState.value.currentPage == pageIndex) return
        
        _uiState.value = _uiState.value.copy(currentPage = pageIndex)
        
        // 防抖保存进度
        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            delay(500)
            progressRepository.saveProgress(
                ReadingProgress(
                    itemId = itemId,
                    currentPage = pageIndex,
                    totalPages = _uiState.value.totalPages,
                    readingMode = ReadingMode.LEFT_TO_RIGHT,
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
}

data class ReaderUiState(
    val isLoading: Boolean = true,
    val book: LibraryItem? = null,
    val pageProvider: PageProvider? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0
)
