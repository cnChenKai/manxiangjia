package com.mangahaven.feature.library.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangahaven.data.files.`import`.RemoteScanner
import com.mangahaven.data.files.remote.SourceClientFactory
import com.mangahaven.data.local.repository.LibraryRepository
import com.mangahaven.model.SourceEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RemoteBrowserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val sourceClientFactory: SourceClientFactory,
    private val remoteScanner: RemoteScanner,
) : ViewModel() {

    private val sourceId: String = checkNotNull(savedStateHandle["sourceId"])
    private var currentPath: String = "/"

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState

    init {
        loadCurrentPath()
    }

    private fun loadCurrentPath() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val source = libraryRepository.getSource(sourceId)
                if (source == null) {
                    _uiState.update { it.copy(isLoading = false, error = "源不存在") }
                    return@launch
                }
                
                _uiState.update { it.copy(sourceName = source.name) }
                
                val client = sourceClientFactory.create(source)
                // 获取上级目录 (如果非根)
                val isRoot = currentPath == "/" || currentPath.isEmpty()
                val entries = client.list(currentPath)
                
                // 仅过滤出目录和压缩包等可能包含漫画的内容
                val filtered = entries.sortedBy { !it.isDirectory }

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        currentPath = currentPath,
                        isRoot = isRoot,
                        entries = filtered
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading path: $currentPath")
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "网络错误") }
            }
        }
    }

    fun navigateInto(path: String) {
        currentPath = path
        loadCurrentPath()
    }

    fun navigateUp() {
        if (currentPath == "/" || currentPath.isEmpty()) return
        
        // 算出父目录
        var parent = currentPath.trimEnd('/')
        val lastSlash = parent.lastIndexOf('/')
        parent = if (lastSlash > 0) {
            parent.substring(0, lastSlash)
        } else {
            "/"
        }
        currentPath = parent
        loadCurrentPath()
    }

    fun addToLibrary(entry: SourceEntry, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val source = libraryRepository.getSource(sourceId) ?: return@launch
                val client = sourceClientFactory.create(source)
                
                onResult("正在深度扫描以导入: ${entry.name}...")
                
                val count = remoteScanner.scanDirectory(source, client, entry.path) { progress ->
                    Timber.d("Scan: $progress")
                }
                
                onResult("导入完成，新增了 $count 本漫画")
            } catch (e: Exception) {
                Timber.e(e, "Import error")
                onResult("导入过程中断: ${e.message}")
            }
        }
    }
}

data class BrowserUiState(
    val sourceName: String = "",
    val isLoading: Boolean = false,
    val currentPath: String = "/",
    val isRoot: Boolean = true,
    val entries: List<SourceEntry> = emptyList(),
    val error: String? = null
)
