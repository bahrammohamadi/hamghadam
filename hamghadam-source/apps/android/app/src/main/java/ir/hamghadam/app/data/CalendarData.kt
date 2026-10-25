package ir.hamghadam.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import ir.hamghadam.app.domain.CalendarEvent
import ir.hamghadam.app.domain.CalendarSource
import ir.hamghadam.app.domain.FocusRecord
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersonalEvent
import kotlinx.coroutines.flow.Flow

/** A user-created all-day event, optionally repeating every year. */
@Entity(tableName = "personal_events")
data class PersonalEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val epochDay: Long,
    val colorArgb: Int,
    val yearlyRepeat: Boolean = false,
    val notes: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val profileId: String = "",
) {
    /** Null when the stored epoch day is outside the supported Jalali range. */
    val dateOrNull: JalaliDate?
        get() = runCatching { JalaliDate.fromEpochDay(epochDay) }.getOrNull()

    fun toModel(): PersonalEvent? {
        val date = dateOrNull ?: return null
        return PersonalEvent(
            id = id,
            title = title,
            date = date,
            colorArgb = colorArgb,
            yearlyRepeat = yearlyRepeat,
            notes = notes,
        )
    }
}

/** Cached copy of Google Calendar events; never a source of truth. */
@Entity(tableName = "external_events")
data class ExternalEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val epochDay: Long,
    val endEpochDay: Long,
    val allDay: Boolean = true,
    val location: String = "",
    val htmlLink: String = "",
    val colorArgb: Int = CalendarSource.GOOGLE.colorArgb,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
) {
    fun toEvent(): CalendarEvent? = runCatching {
        CalendarEvent(
            id = "google-$id",
            title = title,
            date = JalaliDate.fromEpochDay(epochDay),
            source = CalendarSource.GOOGLE,
            colorArgb = colorArgb,
            description = location,
            notes = location.ifBlank { "از تقویم گوگل" },
        )
    }.getOrNull()
}

/** One focus/pomodoro block. */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val taskId: String? = null,
    val epochDay: Long,
    val seconds: Int,
    val completed: Boolean = true,
    val startedAtEpochMillis: Long = System.currentTimeMillis(),
    val profileId: String = "",
) {
    fun toRecord(): FocusRecord = FocusRecord(
        id = id,
        taskId = taskId,
        epochDay = epochDay,
        seconds = seconds,
        completed = completed,
        startedAtEpochMillis = startedAtEpochMillis,
    )
}

/** A reusable task recipe ("تمپلیت") shown on the templates screen. */
@Entity(tableName = "task_templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val title: String,
    val notes: String = "",
    val priority: Int = TaskEntity.PRIORITY_NORMAL,
    val minuteOfDay: Int? = null,
    val tagCsv: String = "",
    val projectId: String? = null,
    val builtin: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val profileId: String = "",
)

@Dao
interface PersonalEventDao {
        @Query("SELECT * FROM personal_events ORDER BY epochDay ASC, createdAtEpochMillis ASC")
        fun observeAll(): Flow<List<PersonalEventEntity>>

        @Query("SELECT * FROM personal_events ORDER BY epochDay ASC")
        suspend fun loadAll(): List<PersonalEventEntity>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun upsert(event: PersonalEventEntity)

        @Query("DELETE FROM personal_events WHERE id = :id")
        suspend fun deleteById(id: String)

    @Query("DELETE FROM personal_events")
    suspend fun deleteAll()

    @Query("DELETE FROM personal_events WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)
}

@Dao
interface ExternalEventDao {
        @Query("SELECT * FROM external_events ORDER BY epochDay ASC")
        fun observeAll(): Flow<List<ExternalEventEntity>>

        @Query("SELECT * FROM external_events ORDER BY epochDay ASC")
        suspend fun loadAll(): List<ExternalEventEntity>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun upsertAll(events: List<ExternalEventEntity>)

        @Query("DELETE FROM external_events")
        suspend fun deleteAll()

    @Query("DELETE FROM external_events WHERE updatedAtEpochMillis < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface FocusSessionDao {
        @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMillis DESC")
        fun observeAll(): Flow<List<FocusSessionEntity>>

        @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMillis DESC")
        suspend fun loadAll(): List<FocusSessionEntity>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun upsert(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()

    @Query("DELETE FROM focus_sessions WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)
}

@Dao
interface TemplateDao {
        @Query("SELECT * FROM task_templates ORDER BY builtin DESC, createdAtEpochMillis ASC")
        fun observeAll(): Flow<List<TemplateEntity>>

        @Query("SELECT * FROM task_templates")
        suspend fun loadAll(): List<TemplateEntity>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun upsertAll(templates: List<TemplateEntity>)

        @Query("DELETE FROM task_templates WHERE id = :id")
        suspend fun deleteById(id: String)

        @Query("DELETE FROM task_templates WHERE profileId = :profileId")
        suspend fun deleteByProfile(profileId: String)

        @Query("DELETE FROM task_templates")
        suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM task_templates")
    suspend fun count(): Int
}
