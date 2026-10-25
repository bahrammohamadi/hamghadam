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
    version = 5,
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

        /**
         * v3 -> v4: multi-account support. Every user-owned table gets a
         * `profileId`; existing rows keep the empty id, which is the implicit
         * first account, so nobody loses data when updating.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("tasks", "projects", "habits", "personal_events", "focus_sessions", "task_templates")) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `profileId` TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        /**
         * v4 -> v5: task checklists («زیروظیفه‌ها») and the star flag.
         * Both are additive with a default, so existing rows stay valid and no
         * data is rewritten (audit C-11 rule: additive-only migrations).
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN subtasks TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN starred INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Opens the database with SQLCipher when the native library is present.
         * If anything about encryption fails we still return a working (plain)
         * database instead of crashing the app on launch — but we never silently
         * downgrade: the failure is logged and surfaced through [cryptoStatus].
         */
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }

        /** Human-readable state for the profile screen; never throws. */
        @Volatile var cryptoStatus: String = "unknown"
            private set

        private fun build(context: Context): AppDatabase {
            val app = context.applicationContext
            val encrypted = runCatching {
                DatabaseCrypto.loadLibrary()
                DatabaseCrypto.migrateIfNeeded(app)
            }.getOrElse {
                android.util.Log.e("AppDatabase", "SQLCipher unavailable; using plain database", it)
                false
            }
            val builder = Room.databaseBuilder(app, AppDatabase::class.java, DatabaseCrypto.DB_NAME)
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                // Older pre-release schemas (v1) can still be thrown away safely.
                .fallbackToDestructiveMigration()

            if (encrypted) {
                val key = DatabaseCrypto.passphrase(app)
                val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(key.toByteArray(Charsets.UTF_8))
                cryptoStatus = if (DatabaseCrypto.isKeyKeystoreProtected(app)) "encrypted+keystore" else "encrypted+prefs"
                return builder.openHelperFactory(factory).build()
            }
            cryptoStatus = "plaintext"
            return builder.build()
        }
    }
}
