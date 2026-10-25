package ir.hamghadam.app.domain

/**
 * A calendar source shown in the month view and in the calendar settings.
 *
 * The official/cultural feeds ship as versioned JSON assets; personal events are
 * stored in Room; Google events are only a cache of an opt-in online sync.
 */
enum class CalendarSource(val id: String, val label: String, val colorArgb: Int, val shortLabel: String) {
    OFFICIAL("official", "تعطیلات رسمی", 0xFFC8453C.toInt(), "رسمی"),
    CULTURAL("cultural", "مناسبت‌های فرهنگی", 0xFFC9872B.toInt(), "فرهنگی"),
    PERSONAL("personal", "رویدادهای من", 0xFF6C5CE7.toInt(), "شخصی"),
    GOOGLE("google", "تقویم گوگل", 0xFF3B78D8.toInt(), "گوگل");

    companion object {
        val defaultEnabled: Set<String> = setOf(OFFICIAL.id, CULTURAL.id, PERSONAL.id)

        val allIds: Set<String> get() = entries.map { it.id }.toSet()

        fun fromId(id: String?): CalendarSource? = entries.firstOrNull { it.id == id }

        fun sanitizeSources(raw: Set<String>): Set<String> {
            val known = raw.filter { fromId(it) != null }.toSet()
            return known.ifEmpty { defaultEnabled }
        }
    }
}

/** A user-owned all-day event. Yearly repetition re-uses the Jalali day/month. */
data class PersonalEvent(
    val id: String,
    val title: String,
    val date: JalaliDate,
    val colorArgb: Int = EventPalette.personalDefault,
    val yearlyRepeat: Boolean = false,
    val notes: String = "",
) {
    /** Returns the occurrence of this event inside [year], honouring yearly repeat. */
    fun occurrenceIn(year: Int): JalaliDate? {
        if (!yearlyRepeat) return date.takeIf { it.year == year }
        return runCatching { JalaliDate(year, date.month, date.day) }
            .getOrNull()
            ?.takeIf { it.isValid }
    }
}

/** One all-day item on the calendar, already projected to a concrete date. */
data class CalendarEvent(
    val id: String,
    val title: String,
    val date: JalaliDate,
    val source: CalendarSource,
    val holiday: Boolean = false,
    val colorArgb: Int = source.colorArgb,
    val description: String = "",
    /** Lunar-based dates can move by a day depending on the official announcement. */
    val approximate: Boolean = false,
    val lunar: Boolean = false,
    val notes: String = "",
) {
    val subtitle: String
        get() = when {
            source == CalendarSource.GOOGLE -> notes.ifBlank { "از تقویم گوگل" }
            source == CalendarSource.PERSONAL -> if (notes.isBlank()) "رویداد شخصی" else notes
            description.isNotBlank() -> description
            else -> source.label
        }
}

data class DayEvents(val date: JalaliDate, val events: List<CalendarEvent>) {
    val hasHoliday: Boolean get() = events.any { it.holiday }
    val hasApproximate: Boolean get() = events.any { it.approximate }
    val dots: List<Int> get() = events.map { it.colorArgb }.distinct().take(3)
}

object EventPalette {
    val personalDefault: Int = 0xFF6C5CE7.toInt()

    /** Warm multi-colour palette offered for personal events. */
    val picks: List<Int> = listOf(
        0xFFD9614C, // terracotta
        0xFFE39A3B, // amber
        0xFF4E8C6A, // olive
        0xFF3E7CB1, // sky
        0xFF6C5CE7, // violet
        0xFFC4598B, // rose
    ).map { it.toInt() }
}

object EventProjection {

    /** Keeps only events whose source is enabled in settings. */
    fun enabled(events: List<CalendarEvent>, activeSources: Set<String>): List<CalendarEvent> =
        events.filter { it.source.id in activeSources }

    fun forDay(events: List<CalendarEvent>, date: JalaliDate): List<CalendarEvent> =
        events.filter { it.date == date }
            .sortedWith(compareBy({ if (it.holiday) 0 else 1 }, { it.source.ordinal }, { it.title }))

    fun byDay(events: List<CalendarEvent>, year: Int, month: Int): Map<Long, List<CalendarEvent>> =
        events.filter { it.date.year == year && it.date.month == month }
            .groupBy { it.date.toEpochDay() }
            .mapValues { (_, list) -> list.sortedWith(compareBy({ if (it.holiday) 0 else 1 }, { it.title })) }

    /** Expands personal events (with yearly repeat) into the requested Jalali year. */
    fun expandPersonal(events: List<PersonalEvent>, year: Int): List<CalendarEvent> =
        events.mapNotNull { personal ->
            personal.occurrenceIn(year)?.let { date ->
                CalendarEvent(
                    id = "${personal.id}@$year",
                    title = personal.title,
                    date = date,
                    source = CalendarSource.PERSONAL,
                    colorArgb = personal.colorArgb,
                    description = personal.notes,
                    notes = personal.notes,
                )
            }
        }

    /** Events worth a coloured dot per day, capped for a calm month grid. */
    fun dotColors(dayEvents: List<CalendarEvent>, limit: Int = 3): List<Int> {
        val ordered = dayEvents.sortedWith(
            compareBy({ if (it.holiday) 0 else 1 }, { it.source.ordinal }),
        )
        return ordered.map { it.colorArgb }.distinct().take(limit)
    }
}

/** Result of reading one `assets/events/<year>.json` file. */
data class EventYear(val year: Int, val events: List<CalendarEvent>)

/**
 * Parses the versioned holiday/cultural feed. Pure Kotlin (no Android types) so
 * it is covered by JVM tests; the asset reader lives in `data/EventCatalog`.
 */
object EventJsonParser {

    fun parse(text: String): EventYear {
        val root = SimpleJson.parse(text) as? Map<*, *>
            ?: throw IllegalArgumentException("فایل مناسبت‌ها معتبر نیست")
        val year = (root["year"] as? Number)?.toInt()
            ?: throw IllegalArgumentException("سال فایل مناسبت‌ها مشخص نیست")
        val raw = root["events"] as? List<*> ?: emptyList<Any?>()
        val events = raw.mapNotNull { parseEvent(it, year) }
        return EventYear(year = year, events = events)
    }

    private fun parseEvent(raw: Any?, year: Int): CalendarEvent? {
        val entry = raw as? Map<*, *> ?: return null
        val title = (entry["title"] as? String)?.trim().orEmpty()
        val month = (entry["month"] as? Number)?.toInt() ?: return null
        val day = (entry["day"] as? Number)?.toInt() ?: return null
        if (title.isEmpty() || month !in 1..12) return null
        if (day !in 1..JalaliDate.monthLength(year, month)) return null
        val source = CalendarSource.fromId(entry["category"] as? String)
            ?: if (entry["holiday"] == true) CalendarSource.OFFICIAL else CalendarSource.CULTURAL
        val id = (entry["id"] as? String)?.takeIf { it.isNotBlank() }
            ?: "$year-%02d-%02d-%s".format(month, day, Integer.toHexString(title.hashCode()))
        return CalendarEvent(
            id = id,
            title = title,
            date = JalaliDate(year, month, day),
            source = source,
            holiday = entry["holiday"] as? Boolean ?: (source == CalendarSource.OFFICIAL),
            colorArgb = colorFromHex(entry["color"] as? String) ?: source.colorArgb,
            description = (entry["description"] as? String).orEmpty(),
            approximate = entry["approximate"] as? Boolean ?: false,
            lunar = entry["lunar"] as? Boolean ?: false,
        )
    }

    /** Accepts `#RRGGBB` and `#AARRGGBB`. */
    fun colorFromHex(raw: String?): Int? {
        val value = raw?.trim()?.removePrefix("#") ?: return null
        if (value.length !in setOf(6, 8)) return null
        val parsed = value.toLongOrNull(16) ?: return null
        return if (value.length == 6) (0xFF000000L or parsed).toInt() else parsed.toInt()
    }

    fun toHex(colorArgb: Int): String = "#%06X".format(colorArgb and 0xFFFFFF)
}
