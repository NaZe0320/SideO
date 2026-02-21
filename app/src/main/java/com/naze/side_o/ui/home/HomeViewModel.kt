package com.naze.side_o.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.data.repository.TodoRepository
import com.naze.side_o.widget.TodoAppWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TodoRepository,
    private val application: Application
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
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }

    fun updateTodo(entity: TodoEntity) {
        viewModelScope.launch {
            repository.updateTodo(entity)
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }

    fun setCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setCompleted(id, completed)
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }

    fun setImportant(id: Long, important: Boolean) {
        viewModelScope.launch {
            repository.setImportant(id, important)
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }

    fun markDeleted(id: Long) {
        viewModelScope.launch {
            repository.markDeleted(id)
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            repository.restore(id)
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }

    fun reorder(items: List<TodoEntity>, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in items.indices || toIndex !in items.indices) return
        viewModelScope.launch {
            val reordered = items.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            repository.reorderActiveTodos(reordered.map { it.id })
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }
}

class HomeViewModelFactory(
    private val repository: TodoRepository,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
