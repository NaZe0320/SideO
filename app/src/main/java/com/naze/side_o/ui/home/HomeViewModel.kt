package com.naze.side_o.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.data.repository.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    val activeTodos: StateFlow<List<TodoEntity>> = repository.getActiveTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addTodo(title: String, isImportant: Boolean = false) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addTodo(title.trim(), isImportant)
        }
    }

    fun updateTodo(entity: TodoEntity) {
        viewModelScope.launch {
            repository.updateTodo(entity)
        }
    }

    fun setCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setCompleted(id, completed)
        }
    }

    fun setImportant(id: Long, important: Boolean) {
        viewModelScope.launch {
            repository.setImportant(id, important)
        }
    }

    fun markDeleted(id: Long) {
        viewModelScope.launch {
            repository.markDeleted(id)
        }
    }

    fun reorder(items: List<TodoEntity>, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in items.indices || toIndex !in items.indices) return
        viewModelScope.launch {
            val reordered = items.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            repository.reorderActiveTodos(reordered.map { it.id })
        }
    }
}

class HomeViewModelFactory(
    private val repository: TodoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
