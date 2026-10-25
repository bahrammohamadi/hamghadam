package ir.hamghadam.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.RepeatRule
import java.time.LocalTime

/**
 * A task as stored on the device.
 *
 * The due date is kept as an epoch day (`LocalDate.toEpochDay`) and the time of
 * day as minutes from midnight, so a task never moves when the phone time zone
 * changes mid-day. `deletedAtEpochMillis` is a tombstone used for undo and for
 * the future sync layer; queries always filter it out.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index("dueEpochDay"),
        Index("projectId"),
        Index("completedAtEpochMillis"),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String = "",
    val projectId: String? = null,
    val dueEpochDay: Long? = null,
    val dueMinuteOfDay: Int? = null,
    val priority: Int = PRIORITY_NORMAL,
    val repeatRule: String = RepeatRule.NONE.storageValue,
    val reminderMinutesBefore: Int? = null,
    /** Comma separated labels ("برچسب‌ها") such as `کار,خرید`. */
    val tags: String = "",
    val completedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val sortOrder: Long = 0,
    val deletedAtEpochMillis: Long? = null,
    /** Owning local account; "" is the implicit first account. */
    val profileId: String = "",
) {
    val isCompleted: Boolean get() = completedAtEpochMillis != null

    val repeat: RepeatRule get() = RepeatRule.fromStorage(repeatRule)

    // Stored values are validated on write, but backups from older builds and
    // hand-edited files can carry anything; a bad row must never crash the list.
    val dueDate: JalaliDate?
        get() = dueEpochDay?.let { runCatching { JalaliDate.fromEpochDay(it) }.getOrNull() }

    val dueTime: LocalTime?
        get() = dueMinuteOfDay?.takeIf { it in 0 until DAY_MINUTES }?.let { LocalTime.of(it / 60, it % 60) }

    val tagList: List<String> get() = ir.hamghadam.app.domain.Tags.parseCsv(tags)

    val completedDate: JalaliDate?
        get() = completedAtEpochMillis?.let { millis ->
            runCatching {
                JalaliDate.fromGregorian(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
            }.getOrNull()
        }

    companion object {
        private const val DAY_MINUTES = 24 * 60
        const val PRIORITY_LOW = 0
        const val PRIORITY_NORMAL = 1
        const val PRIORITY_HIGH = 2

        fun priorityLabel(priority: Int): String = when (priority) {
            PRIORITY_LOW -> "کم"
            PRIORITY_HIGH -> "مهم"
            else -> "معمولی"
        }
    }
}
