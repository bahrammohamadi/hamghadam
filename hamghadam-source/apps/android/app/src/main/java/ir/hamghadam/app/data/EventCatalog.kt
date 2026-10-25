package ir.hamghadam.app.data

import android.content.Context
import ir.hamghadam.app.domain.CalendarEvent
import ir.hamghadam.app.domain.EventJsonParser
import ir.hamghadam.app.domain.EventYear
import ir.hamghadam.app.domain.JalaliDate

/**
 * Reads the bundled holiday/cultural feeds from `assets/events/<year>.json`.
 *
 * The files are versioned data, not code: updating 1406 only means adding a new
 * asset. When a year has no file yet, fixed (solar) occasions from the closest
 * available year are projected forward so the month view never looks empty;
 * lunar dates are intentionally *not* projected because they shift every year.
 */
class EventCatalog(context: Context) {

    private val assets = context.applicationContext.assets
    private val cache = HashMap<Int, List<CalendarEvent>>()

    val availableYears: List<Int> by lazy {
        runCatching {
            assets.list("events")
                ?.mapNotNull { it.removeSuffix(".json").toIntOrNull() }
                ?.sorted()
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun eventsForYear(year: Int): List<CalendarEvent> {
        cache[year]?.let { return it }
        val direct = read(year)
        val events = if (direct != null) {
            direct.events
        } else {
            val templateYear = availableYears.minByOrNull { kotlin.math.abs(it - year) } ?: return emptyList()
            val template = read(templateYear) ?: return emptyList()
            template.events.mapNotNull { event ->
                if (event.lunar) return@mapNotNull null
                val date = runCatching { JalaliDate(year, event.date.month, event.date.day) }
                    .getOrNull()
                    ?.takeIf { it.isValid }
                    ?: return@mapNotNull null
                event.copy(id = "${event.id}@$year", date = date)
            }
        }.sortedWith(compareBy({ it.date }, { it.title }))
        cache[year] = events
        return events
    }

    /** All bundled events overlapping the Jalali years touched by [from]..[to]. */
    fun eventsBetween(from: JalaliDate, to: JalaliDate): List<CalendarEvent> =
        (from.year..to.year).flatMap { eventsForYear(it) }
            .filter { it.date >= from && it.date <= to }

    fun describeFeeds(): String =
        if (availableYears.isEmpty()) {
            "فایل مناسبت‌ها در دسترس نیست"
        } else {
            "داده‌های محلی: سال " + availableYears.joinToString("، ") + " — رویدادهای شخصی جداگانه ذخیره می‌شوند"
        }

    private fun read(year: Int): EventYear? = runCatching {
        val text = assets.open("events/$year.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        EventJsonParser.parse(text)
    }.getOrNull()
}
