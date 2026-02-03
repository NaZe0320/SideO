package com.naze.side_o.data.repository

import com.naze.side_o.data.local.TodoDao
import com.naze.side_o.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val dao: TodoDao
) {

    fun getActiveTodos(): Flow<List<TodoEntity>> = dao.getActiveTodos()

    fun getCompletedTodos(): Flow<List<TodoEntity>> = dao.getCompletedTodos()

    suspend fun getById(id: Long): TodoEntity? = dao.getById(id)

    suspend fun addTodo(title: String, isImportant: Boolean = false) {
        val orderIndex = dao.getNextOrderIndex()
        val entity = TodoEntity(
            title = title,
            isImportant = isImportant,
            createdAt = System.currentTimeMillis(),
            orderIndex = orderIndex
        )
        dao.insert(entity)
    }

    suspend fun updateTodo(entity: TodoEntity) {
        dao.update(entity)
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(isCompleted = completed))
    }

    suspend fun setImportant(id: Long, important: Boolean) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(isImportant = important))
    }

    suspend fun markDeleted(id: Long) {
        dao.markDeleted(id, System.currentTimeMillis())
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
}
