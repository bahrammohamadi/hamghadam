package ir.hamghadam.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        ProjectEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        PersonalEventEntity::class,
        ExternalEventEntity::class,
        FocusSessionEntity::class,
        TemplateEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun habitDao(): HabitDao
    abstract fun personalEventDao(): PersonalEventDao
    abstract fun externalEventDao(): ExternalEventDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v2 -> v3: task tags plus the four new tables (events, focus, templates,
         * Google cache). Written by hand so updated installs keep their data.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `personal_events` (" +
                        "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                        "`colorArgb` INTEGER NOT NULL, `yearlyRepeat` INTEGER NOT NULL, " +
                        "`notes` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, " +
                        "`updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `external_events` (" +
                        "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                        "`endEpochDay` INTEGER NOT NULL, `allDay` INTEGER NOT NULL, " +
                        "`location` TEXT NOT NULL, `htmlLink` TEXT NOT NULL, " +
                        "`colorArgb` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `focus_sessions` (" +
                        "`id` TEXT NOT NULL, `taskId` TEXT, `epochDay` INTEGER NOT NULL, " +
                        "`seconds` INTEGER NOT NULL, `completed` INTEGER NOT NULL, " +
                        "`startedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_templates` (" +
                        "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`notes` TEXT NOT NULL, `priority` INTEGER NOT NULL, `minuteOfDay` INTEGER, " +
                        "`tagCsv` TEXT NOT NULL, `projectId` TEXT, `builtin` INTEGER NOT NULL, " +
                        "`createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hamghadam.db",
            )
                .addMigrations(MIGRATION_2_3)
                // Older pre-release schemas (v1) can still be thrown away safely.
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
