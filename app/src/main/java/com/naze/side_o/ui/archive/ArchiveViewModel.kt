package com.naze.side_o.ui.archive

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.data.repository.TodoRepository
import com.naze.side_o.widget.TodoAppWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val RECENTLY_DAYS_MS = 7L * 24 * 60 * 60 * 1000
private const val LAST_WEEK_DAYS_MS = 14L * 24 * 60 * 60 * 1000
private const val PERMANENT_DELETE_DELAY_MS = 3000L

data class ArchiveSection(
    val title: String,
    val items: List<TodoEntity>
)

class ArchiveViewModel(
    private val repository: TodoRepository,
    private val application: Application
) : ViewModel() {

    val completedTodos: StateFlow<List<TodoEntity>> = repository.getCompletedTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val deletedTodos: StateFlow<List<TodoEntity>> =
        repository.getRecentlyDeletedTodos()
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

    private val pendingDeleteJobs = mutableMapOf<Long, Job>()
    private val pendingDeleteLock = Any()

    fun uncomplete(id: Long) {
        viewModelScope.launch {
            repository.setCompleted(id, false)
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            repository.restore(id)
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }

    /** Schedules permanent delete after delay. Call cancelPendingDelete to undo. */
    fun schedulePermanentDelete(id: Long) {
        synchronized(pendingDeleteLock) {
            pendingDeleteJobs[id]?.cancel()
            val job = viewModelScope.launch {
                delay(PERMANENT_DELETE_DELAY_MS)
                repository.deletePermanently(id)
                TodoAppWidgetProvider.updateAllWidgets(application)
            }
            job.invokeOnCompletion {
                synchronized(pendingDeleteLock) { pendingDeleteJobs.remove(id) }
            }
            pendingDeleteJobs[id] = job
        }
    }

    fun cancelPendingDelete(id: Long) {
        synchronized(pendingDeleteLock) {
            pendingDeleteJobs.remove(id)?.cancel()
        }
    }

    fun recomplete(id: Long) {
        viewModelScope.launch {
            repository.setCompleted(id, true)
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }

    fun markDeletedAgain(id: Long) {
        viewModelScope.launch {
            repository.markDeleted(id)
            TodoAppWidgetProvider.updateAllWidgets(application)
        }
    }
}

class ArchiveViewModelFactory(
    private val repository: TodoRepository,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchiveViewModel::class.java)) {
            return ArchiveViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
