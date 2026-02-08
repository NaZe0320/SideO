package com.naze.side_o.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.data.repository.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    val deletedTodos: StateFlow<List<TodoEntity>> =
        repository.getRecentlyDeletedTodos()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun restore(id: Long) {
        viewModelScope.launch {
            repository.restore(id)
        }
    }
}

class TrashViewModelFactory(
    private val repository: TodoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrashViewModel::class.java)) {
            return TrashViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
