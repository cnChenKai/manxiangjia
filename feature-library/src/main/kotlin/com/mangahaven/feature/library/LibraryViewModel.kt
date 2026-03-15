package com.mangahaven.feature.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangahaven.data.files.`import`.SafImporter
import com.mangahaven.data.local.repository.LibraryRepository
import com.mangahaven.model.LibraryItem
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

    // Filter states
    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow<String?>(null) // null = all, UNREAD, READING, COMPLETED
    val isFavoriteFilter = MutableStateFlow<Boolean?>(null)
    val sortBy = MutableStateFlow("RECENT_READ")

    // 观察过滤后的全部漫画
    val allItems: StateFlow<List<LibraryItem>> = combine(
        searchQuery,
        statusFilter,
        isFavoriteFilter,
        sortBy
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

    fun toggleFavorite(item: LibraryItem) {
        viewModelScope.launch {
            libraryRepository.updateItem(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun updateReadingStatus(item: LibraryItem, status: com.mangahaven.model.ReadingStatus) {
        viewModelScope.launch {
            libraryRepository.updateItem(item.copy(readingStatus = status))
        }
    }
}
