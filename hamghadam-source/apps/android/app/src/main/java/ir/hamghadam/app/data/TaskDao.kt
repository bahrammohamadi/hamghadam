package ir.hamghadam.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAtEpochMillis IS NULL
        ORDER BY completedAtEpochMillis IS NOT NULL,
                 starred DESC,
                 dueEpochDay IS NULL, dueEpochDay,
                 dueMinuteOfDay IS NULL, dueMinuteOfDay,
                 priority DESC, createdAtEpochMillis DESC
        """,
    )
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAtEpochMillis IS NULL")
    suspend fun loadAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET deletedAtEpochMillis = :now, updatedAtEpochMillis = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE tasks SET deletedAtEpochMillis = NULL, updatedAtEpochMillis = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("UPDATE tasks SET projectId = NULL, updatedAtEpochMillis = :now WHERE projectId = :projectId")
    suspend fun clearProject(projectId: String, now: Long)

    @Query("SELECT * FROM tasks WHERE deletedAtEpochMillis IS NULL AND completedAtEpochMillis IS NULL AND dueEpochDay IS NOT NULL AND reminderMinutesBefore IS NOT NULL")
    suspend fun loadWithReminders(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("DELETE FROM tasks WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)
}
