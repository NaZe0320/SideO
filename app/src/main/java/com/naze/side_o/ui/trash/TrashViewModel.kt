package com.naze.side_o.ui.trash

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

class TrashViewModel(
    private val repository: TodoRepository,
    private val application: Application
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
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }
}

class TrashViewModelFactory(
    private val repository: TodoRepository,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrashViewModel::class.java)) {
            return TrashViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
