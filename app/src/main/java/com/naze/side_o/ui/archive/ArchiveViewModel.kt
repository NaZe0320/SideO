package com.naze.side_o.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val RECENTLY_DAYS_MS = 7L * 24 * 60 * 60 * 1000
private const val LAST_WEEK_DAYS_MS = 14L * 24 * 60 * 60 * 1000

data class ArchiveSection(
    val title: String,
    val items: List<TodoEntity>
)

class ArchiveViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    val completedTodos: StateFlow<List<TodoEntity>> = repository.getCompletedTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val sections: StateFlow<List<ArchiveSection>> = completedTodos
        .map { list ->
            val now = System.currentTimeMillis()
            val recently = mutableListOf<TodoEntity>()
            val lastWeek = mutableListOf<TodoEntity>()
            for (todo in list) {
                val completedAt = todo.completedAt ?: todo.createdAt
                when {
                    completedAt >= now - RECENTLY_DAYS_MS -> recently.add(todo)
                    completedAt >= now - LAST_WEEK_DAYS_MS -> lastWeek.add(todo)
                    else -> lastWeek.add(todo)
                }
            }
            buildList {
                if (recently.isNotEmpty()) {
                    add(ArchiveSection("RECENTLY COMPLETED", recently))
                }
                if (lastWeek.isNotEmpty()) {
                    add(ArchiveSection("LAST WEEK", lastWeek))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun uncomplete(id: Long) {
        viewModelScope.launch {
            repository.setCompleted(id, false)
        }
    }

    fun markDeleted(id: Long) {
        viewModelScope.launch {
            repository.markDeleted(id)
        }
    }

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun toggleSelection(id: Long) {
        _selectedIds.value = if (id in _selectedIds.value) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                repository.markDeleted(id)
            }
            _selectedIds.value = emptySet()
        }
    }
}

class ArchiveViewModelFactory(
    private val repository: TodoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchiveViewModel::class.java)) {
            return ArchiveViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
