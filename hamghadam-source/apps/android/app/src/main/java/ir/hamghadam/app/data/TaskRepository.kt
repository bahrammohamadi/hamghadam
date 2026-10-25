package ir.hamghadam.app.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TaskRepository(private val dao: TaskDao) {
    fun observeTasks(): Flow<List<TaskEntity>> = dao.observeTasks()

    suspend fun addTask(title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val now = System.currentTimeMillis()
        dao.upsert(TaskEntity(id = UUID.randomUUID().toString(), title = cleanTitle, createdAtEpochMillis = now, updatedAtEpochMillis = now))
    }

    suspend fun setCompleted(task: TaskEntity, completed: Boolean) {
        dao.setCompleted(task.id, completed, System.currentTimeMillis())
    }

    suspend fun delete(task: TaskEntity) = dao.deleteById(task.id)
}
