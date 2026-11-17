package com.example.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    val tasks: Flow<List<TodoTask>> = todoDao.observeAllTasks()

    suspend fun addTask(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            todoDao.insertTask(TodoTask(text = trimmed))
        }
    }

    suspend fun toggleCompleted(task: TodoTask) {
        todoDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun updateText(task: TodoTask, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isNotEmpty()) {
            todoDao.updateTask(task.copy(text = trimmed))
        }
    }

    suspend fun deleteTask(task: TodoTask) {
        todoDao.deleteTask(task)
    }

    suspend fun restoreTask(task: TodoTask) {
        todoDao.insertTask(task)
    }
}
