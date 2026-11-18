package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TodoRepository(private val todoDao: TodoDao) {
    val tasks: Flow<List<TodoTask>> = todoDao.observeAllTasks()

    suspend fun addTask(text: String): TodoTask? {
        val trimmed = text.trim().take(MAX_TASK_TEXT_LENGTH)
        if (trimmed.isEmpty()) return null
        val now = System.currentTimeMillis()
        val task = TodoTask(
            remoteId = UUID.randomUUID().toString(),
            text = trimmed,
            createdAt = now,
            updatedAt = now,
        )
        val insertedId = todoDao.insertTask(task)
        return task.copy(id = insertedId)
    }

    suspend fun toggleCompleted(task: TodoTask): TodoTask {
        val updated = task.copy(
            isCompleted = !task.isCompleted,
            updatedAt = System.currentTimeMillis(),
        )
        todoDao.updateTask(updated)
        return updated
    }

    suspend fun updateText(task: TodoTask, newText: String): TodoTask? {
        val trimmed = newText.trim().take(MAX_TASK_TEXT_LENGTH)
        if (trimmed.isEmpty()) return null
        val updated = task.copy(
            text = trimmed,
            updatedAt = System.currentTimeMillis(),
        )
        todoDao.updateTask(updated)
        return updated
    }

    suspend fun deleteTask(task: TodoTask) {
        todoDao.deleteTask(task)
    }

    suspend fun restoreTask(task: TodoTask): TodoTask {
        val insertedId = todoDao.insertTask(task)
        return task.copy(id = insertedId)
    }

    suspend fun getAllTasksOnce(): List<TodoTask> = todoDao.getAllTasksOnce()

    suspend fun upsertFromRemote(
        remoteId: String,
        text: String,
        completed: Boolean,
        clientCreatedAt: Long,
        remoteUpdatedAt: Long,
    ) {
        val existing = todoDao.getByRemoteId(remoteId)
        val safeText = text.trim().take(MAX_TASK_TEXT_LENGTH)
        if (safeText.isEmpty()) return

        if (existing == null) {
            todoDao.insertTask(
                TodoTask(
                    remoteId = remoteId,
                    text = safeText,
                    isCompleted = completed,
                    createdAt = if (clientCreatedAt > 0L) clientCreatedAt else remoteUpdatedAt,
                    updatedAt = remoteUpdatedAt,
                )
            )
        } else if (remoteUpdatedAt > existing.updatedAt + SYNC_DRIFT_GRACE_MS) {
            todoDao.updateTask(
                existing.copy(
                    text = safeText,
                    isCompleted = completed,
                    updatedAt = remoteUpdatedAt,
                )
            )
        }
    }

    companion object {
        const val MAX_TASK_TEXT_LENGTH = 2_000
        private const val SYNC_DRIFT_GRACE_MS = 1_500L
    }
}
