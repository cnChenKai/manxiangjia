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

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val safImporter: SafImporter,
) : ViewModel() {

    // 观察全部漫画
    val allItems: StateFlow<List<LibraryItem>> = libraryRepository.observeAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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
}
