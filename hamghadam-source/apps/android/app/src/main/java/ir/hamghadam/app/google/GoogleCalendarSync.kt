package ir.hamghadam.app.google

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.hamghadam.app.data.AppDatabase
import ir.hamghadam.app.data.ExternalEventEntity
import ir.hamghadam.app.data.SettingsStore
import ir.hamghadam.app.domain.SimpleJson
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Thin, dependency-free client for the read-only Calendar v3 events list. */
object GoogleCalendarApi {

    private const val BASE = "https://www.googleapis.com/calendar/v3/calendars/primary/events"

    fun eventsUrl(timeMinIso: String, timeMaxIso: String, maxResults: Int = 250): String {
        fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
        return buildString {
            append(BASE)
            append("?timeMin=").append(enc(timeMinIso))
            append("&timeMax=").append(enc(timeMaxIso))
            append("&singleEvents=true&orderBy=startTime")
            append("&maxResults=").append(maxResults)
        }
    }

    fun fetchEvents(accessToken: String, daysBack: Long = 30, daysForward: Long = 180): List<ExternalEventEntity> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val timeMin = today.minusDays(daysBack).atStartOfDay(zone).toOffsetDateTime().toString()
        val timeMax = today.plusDays(daysForward).atStartOfDay(zone).toOffsetDateTime().toString()
        return fetchUrl(eventsUrl(timeMin, timeMax), accessToken)
    }

    fun fetchUrl(url: String, accessToken: String): List<ExternalEventEntity> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Calendar API ${connection.responseCode}: ${error.take(200)}")
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            return parseEvents(text)
        } finally {
            connection.disconnect()
        }
    }

    /** Parses an `events.list` payload; unknown fields are ignored. */
    fun parseEvents(json: String): List<ExternalEventEntity> {
        val root = SimpleJson.parse(json) as? Map<*, *> ?: return emptyList()
        val items = root["items"] as? List<*> ?: return emptyList()
        val now = System.currentTimeMillis()
        return items.mapNotNull { raw ->
            val entry = raw as? Map<*, *> ?: return@mapNotNull null
            val id = (entry["id"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (entry["status"] == "cancelled") return@mapNotNull null
            val start = entry["start"] as? Map<*, *> ?: return@mapNotNull null
            val end = entry["end"] as? Map<*, *>
            val allDay = (start["date"] as? String) != null
            val startDay = parseDay(start["date"] as? String, start["dateTime"] as? String) ?: return@mapNotNull null
            val endDay = parseDay(end?.get("date") as? String, end?.get("dateTime") as? String) ?: startDay
            val title = (entry["summary"] as? String)?.takeIf { it.isNotBlank() } ?: "رویداد بدون عنوان"
            ExternalEventEntity(
                id = id,
                title = title,
                epochDay = startDay.toEpochDay(),
                endEpochDay = endDay.toEpochDay(),
                allDay = allDay,
                location = (entry["location"] as? String).orEmpty(),
                htmlLink = (entry["htmlLink"] as? String).orEmpty(),
                updatedAtEpochMillis = now,
            )
        }
    }

    /** Accepts both `2026-10-25` and full RFC3339 date-times. */
    fun parseDay(date: String?, dateTime: String?): LocalDate? {
        val raw = date?.take(10) ?: dateTime
        if (raw.isNullOrBlank()) return null
        return runCatching {
            if (raw.length >= 10 && raw[4] == '-' && raw[7] == '-' && raw.length == 10) {
                LocalDate.parse(raw)
            } else {
                OffsetDateTime.parse(dateTime).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
            }
        }.getOrNull()
    }
}

/**
 * Background refresh of the Google Calendar cache. Runs only when the user has
 * connected an account; every failure is swallowed so offline usage is intact.
 */
class GoogleSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settings = SettingsStore(context)
        val store = GoogleTokenStore(context)
        val clientId = settings.settings.value.googleClientId
        if (clientId.isBlank() || !store.isConnected()) return Result.success()

        val outcome = runCatching {
            val token = store.validAccessToken(clientId) ?: throw IOException("نیاز به ورود دوباره")
            val events = GoogleCalendarApi.fetchEvents(token)
            AppDatabase.get(context).externalEventDao().let { dao ->
                dao.deleteAll()
                if (events.isNotEmpty()) dao.upsertAll(events)
            }
            store.setLastSync(System.currentTimeMillis())
            settings.setGoogleConnected(true)
            events.size
        }
        return outcome.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            },
        )
    }
}

object GoogleSyncScheduler {

    private const val PERIODIC_WORK = "hamghadam-google-sync"
    private const val ONCE_WORK = "hamghadam-google-sync-once"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<GoogleSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<GoogleSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
    }
}
