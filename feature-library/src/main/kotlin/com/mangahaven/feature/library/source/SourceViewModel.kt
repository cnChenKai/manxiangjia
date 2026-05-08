package com.mangahaven.feature.library.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangahaven.data.files.remote.SourceClientFactory
import com.mangahaven.data.local.repository.LibraryRepository
import com.mangahaven.model.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.mangahaven.model.SourceType

@HiltViewModel
class SourceViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val sourceClientFactory: SourceClientFactory
) : ViewModel() {

    val sources: StateFlow<List<Source>> = libraryRepository.observeAllSources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addSource(source: Source, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // 预先测试连接性（所有远程源类型）
                if (source.type != com.mangahaven.model.SourceType.LOCAL) {
                    val client = sourceClientFactory.create(source)
                    client.list("/") // 探测根目录能否通行
                }
                libraryRepository.addSource(source)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message ?: "连接失败")
            }
        }
    }

    fun removeSource(id: String) {
        viewModelScope.launch {
            libraryRepository.removeSource(id)
        }
    }
}
