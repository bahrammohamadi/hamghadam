package ir.hamghadam.app.notify

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Single place that owns every notification channel.
 *
 * Channels follow the Android guidance of one channel per user-facing kind of
 * alert, with the *lowest* importance that still does the job:
 *  - REMINDERS  (high)   timestamps the user asked for, sound + heads-up
 *  - FOCUS      (low)    ongoing timer, no sound, only one alert
 *  - DIGEST     (default)morning summary, sound but no heads-up
 *  - HABITS     (default)habit nudges
 *
 * Importance is immutable after creation, so ids are versioned: changing the
 * behaviour later means creating `*_v2` rather than silently editing an
 * existing channel (which Android would ignore).
 */
object Notifications {

    const val CHANNEL_REMINDERS = "reminders_v1"
    const val CHANNEL_FOCUS = "focus_v1"
    const val CHANNEL_DIGEST = "digest_v1"
    const val CHANNEL_HABITS = "habits_v1"

    /** Group used to bundle several task reminders into one stack. */
    const val GROUP_TASKS = "ir.hamghadam.app.TASKS"

    const val ID_DIGEST = 10_000
    const val ID_FOCUS = 10_001
    const val ID_GROUP_SUMMARY = 10_002
    const val ID_FOCUS_DONE = 10_003
    const val ID_SNOOZE_CONFIRM = 10_004

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = NotificationManagerCompat.from(context)
        val channels = listOf(
            NotificationChannelCompat.Builder(CHANNEL_REMINDERS, NotificationManager.IMPORTANCE_HIGH)
                .setName("یادآور کارها")
                .setDescription("یادآور کارهای دارای تاریخ و ساعت؛ با دکمهٔ انجام شد و ۱۰ دقیقه بعد")
                .setShowBadge(true)
                .build(),
            NotificationChannelCompat.Builder(CHANNEL_FOCUS, NotificationManager.IMPORTANCE_LOW)
                .setName("تایمر تمرکز")
                .setDescription("نشانی بی‌صدا و همیشگی جلسهٔ تمرکز در حال اجرا")
                .setShowBadge(false)
                .setSound(null, null)
                .setVibrationEnabled(false)
                .build(),
            NotificationChannelCompat.Builder(CHANNEL_DIGEST, NotificationManager.IMPORTANCE_DEFAULT)
                .setName("خلاصهٔ روز")
                .setDescription("خلاصهٔ صبحگاهی کارها و رویدادهای روز")
                .setShowBadge(true)
                .build(),
            NotificationChannelCompat.Builder(CHANNEL_HABITS, NotificationManager.IMPORTANCE_DEFAULT)
                .setName("عادت‌ها")
                .setDescription("یادآور ثبت عادت‌های روزانه")
                .setShowBadge(true)
                .build(),
        )
        // Called from Application.onCreate; a broken channel (OEM policy, user
        // restriction) must never prevent the app from opening.
        runCatching { manager.createNotificationChannelsCompat(channels) }
            .onFailure { Log.w("Notifications", "creating channels failed", it) }
    }

    /** Old single channel from 0.3.0; removed so users do not see duplicates. */
    fun removeLegacyChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf("task_reminders").forEach { id ->
            runCatching { manager.deleteNotificationChannel(id) }
        }
    }

}
