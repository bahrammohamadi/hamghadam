package ir.hamghadam.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A daily habit the user checks off on a week strip. */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val sortOrder: Long = 0,
)

/** One check-in. The composite key makes double taps idempotent. */
@Entity(tableName = "habit_logs", primaryKeys = ["habitId", "epochDay"])
data class HabitLogEntity(
    val habitId: String,
    val epochDay: Long,
    val doneAtEpochMillis: Long = System.currentTimeMillis(),
)
