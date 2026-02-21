package com.naze.side_o.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query(
        "SELECT * FROM todos WHERE isCompleted = 0 AND isDeleted = 0 ORDER BY orderIndex ASC, createdAt ASC"
    )
    fun getActiveTodos(): Flow<List<TodoEntity>>

    @Query(
        "SELECT * FROM todos WHERE isCompleted = 1 AND isDeleted = 0 ORDER BY COALESCE(completedAt, createdAt) DESC LIMIT 30"
    )
    fun getCompletedTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): TodoEntity?

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM todos WHERE isDeleted = 0")
    suspend fun getNextOrderIndex(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TodoEntity): Long

    @Update
    suspend fun update(entity: TodoEntity)

    @Query("UPDATE todos SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markDeleted(id: Long, deletedAt: Long)

    @Query(
        "SELECT * FROM todos WHERE isDeleted = 1 AND deletedAt >= :since ORDER BY deletedAt DESC"
    )
    fun getRecentlyDeletedTodos(since: Long): Flow<List<TodoEntity>>

    @Query("UPDATE todos SET isDeleted = 0, deletedAt = null WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deletePermanently(id: Long)
}
