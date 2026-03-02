package com.naze.do_swipe.data.repository

import com.naze.do_swipe.data.local.TodoDao
import com.naze.do_swipe.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow

private const val MAX_TITLE_LENGTH = 60

class TodoRepository(
    private val dao: TodoDao
) {

    fun getActiveTodos(): Flow<List<TodoEntity>> = dao.getActiveTodos()

    suspend fun getActiveTodosOnce(): List<TodoEntity> = dao.getActiveTodosOnce()

    fun getCompletedTodos(): Flow<List<TodoEntity>> = dao.getCompletedTodos()

    suspend fun getById(id: Long): TodoEntity? = dao.getById(id)

    suspend fun addTodo(title: String, isImportant: Boolean = false) {
        val normalized = title.trim().take(MAX_TITLE_LENGTH)
        if (normalized.isBlank()) return
        val orderIndex = dao.getNextOrderIndex()
        val entity = TodoEntity(
            title = normalized,
            isImportant = isImportant,
            createdAt = System.currentTimeMillis(),
            orderIndex = orderIndex
        )
        dao.insert(entity)
    }

    suspend fun updateTodo(entity: TodoEntity) {
        val normalized = entity.title.trim().take(MAX_TITLE_LENGTH)
        if (normalized.isBlank()) return
        dao.update(entity.copy(title = normalized))
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        val entity = dao.getById(id) ?: return
        dao.update(
            entity.copy(
                isCompleted = completed,
                completedAt = if (completed) System.currentTimeMillis() else null
            )
        )
    }

    suspend fun setImportant(id: Long, important: Boolean) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(isImportant = important))
    }

    suspend fun markDeleted(id: Long) {
        dao.markDeleted(id, System.currentTimeMillis())
    }

    private val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

    fun getRecentlyDeletedTodos(): Flow<List<TodoEntity>> {
        val since = System.currentTimeMillis() - sevenDaysMs
        return dao.getRecentlyDeletedTodos(since)
    }

    suspend fun restore(id: Long) {
        dao.restore(id)
    }

    suspend fun deletePermanently(id: Long) {
        dao.deletePermanently(id)
    }

    /**
     * 순서 변경: 주어진 id 목록 순서대로 orderIndex를 0, 1, 2, ... 로 갱신한다.
     * (꾹 눌러 드래그 후 놓은 최종 순서 리스트를 넘기면 된다.)
     */
    suspend fun reorderActiveTodos(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            val entity = dao.getById(id) ?: return@forEachIndexed
            if (entity.orderIndex != index) {
                dao.update(entity.copy(orderIndex = index))
            }
        }
    }

    suspend fun clearAllTodos() {
        dao.deleteAll()
    }
}
