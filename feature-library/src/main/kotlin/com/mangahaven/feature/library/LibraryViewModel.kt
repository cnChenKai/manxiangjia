package com.mangahaven.feature.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangahaven.data.files.importer.SafImporter
import com.mangahaven.data.local.repository.LibraryRepository
import com.mangahaven.model.LibraryItem
import com.mangahaven.model.ReadingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val safImporter: SafImporter,
) : ViewModel() {

    // Filter states（私有可写 + 公开只读）
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null) // null = all, UNREAD, READING, COMPLETED
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    private val _isFavoriteFilter = MutableStateFlow<Boolean?>(null)
    val isFavoriteFilter: StateFlow<Boolean?> = _isFavoriteFilter.asStateFlow()

    private val _sortBy = MutableStateFlow("RECENT_READ")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    // 多选模式状态
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    // 观察过滤后的全部漫画
    val allItems: StateFlow<List<LibraryItem>> = combine(
        _searchQuery,
        _statusFilter,
        _isFavoriteFilter,
        _sortBy
    ) { query, status, isFavorite, sort ->
        FilterParams(
            query = query.takeIf { it.isNotBlank() },
            status = status,
            isFavorite = isFavorite,
            sortBy = sort
        )
    }.flatMapLatest { params ->
        libraryRepository.searchAndFilter(
            query = params.query,
            status = params.status,
            isFavorite = params.isFavorite,
            sortBy = params.sortBy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private data class FilterParams(
        val query: String?,
        val status: String?,
        val isFavorite: Boolean?,
        val sortBy: String
    )

    // 观察最近阅读
    val recentItems: StateFlow<List<LibraryItem>> = libraryRepository.observeRecentlyRead(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            safImporter.importFile(uri)
            _isImporting.value = false
        }
    }

    fun importDirectory(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            safImporter.importDirectory(uri)
            _isImporting.value = false
        }
    }

    // --- 筛选状态 setter（外部通过这些方法修改，防止直接写入 MutableStateFlow） ---

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateStatusFilter(status: String?) { _statusFilter.value = status }
    fun updateFavoriteFilter(isFavorite: Boolean?) { _isFavoriteFilter.value = isFavorite }
    fun updateSortBy(sort: String) { _sortBy.value = sort }

    fun toggleFavorite(item: LibraryItem) {
        viewModelScope.launch {
            libraryRepository.updateItem(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun updateReadingStatus(item: LibraryItem, status: ReadingStatus) {
        viewModelScope.launch {
            libraryRepository.updateItem(item.copy(readingStatus = status))
        }
    }

    // --- 多选模式操作 ---

    /** 进入多选模式并选中第一个条目 */
    fun enterMultiSelect(itemId: String) {
        _isMultiSelectMode.value = true
        _selectedIds.value = setOf(itemId)
    }

    /** 切换条目选中状态 */
    fun toggleSelection(itemId: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (itemId in current) {
            val updated = current - itemId
            if (updated.isEmpty()) {
                _isMultiSelectMode.value = false
            }
            updated
        } else {
            current + itemId
        }
    }

    /** 全选/取消全选 */
    fun toggleSelectAll() {
        val currentItems = allItems.value
        val currentSelected = _selectedIds.value
        if (currentSelected.size == currentItems.size) {
            _selectedIds.value = emptySet()
            _isMultiSelectMode.value = false
        } else {
            _selectedIds.value = currentItems.map { it.id }.toSet()
        }
    }

    /** 退出多选模式 */
    fun exitMultiSelect() {
        _isMultiSelectMode.value = false
        _selectedIds.value = emptySet()
    }

    /** 批量标记已读 */
    fun batchMarkAsRead() {
        viewModelScope.launch {
            val ids = _selectedIds.value
            for (id in ids) {
                libraryRepository.getItemById(id)?.let { item ->
                    libraryRepository.updateItem(item.copy(readingStatus = ReadingStatus.COMPLETED))
                }
            }
            exitMultiSelect()
        }
    }

    /** 批量标记未读 */
    fun batchMarkAsUnread() {
        viewModelScope.launch {
            val ids = _selectedIds.value
            for (id in ids) {
                libraryRepository.getItemById(id)?.let { item ->
                    libraryRepository.updateItem(item.copy(readingStatus = ReadingStatus.UNREAD))
                }
            }
            exitMultiSelect()
        }
    }

    /** 批量切换收藏 */
    fun batchToggleFavorite() {
        viewModelScope.launch {
            val ids = _selectedIds.value
            for (id in ids) {
                libraryRepository.getItemById(id)?.let { item ->
                    libraryRepository.updateItem(item.copy(isFavorite = !item.isFavorite))
                }
            }
            exitMultiSelect()
        }
    }

    /** 批量删除 */
    fun batchDelete() {
        viewModelScope.launch {
            val ids = _selectedIds.value
            for (id in ids) {
                libraryRepository.removeItem(id)
            }
            exitMultiSelect()
        }
    }
}
