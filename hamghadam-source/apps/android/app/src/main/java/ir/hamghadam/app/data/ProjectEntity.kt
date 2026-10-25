package ir.hamghadam.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-defined list used to group tasks (personal, work, university...). */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Int = 0xFF237A69.toInt(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)
