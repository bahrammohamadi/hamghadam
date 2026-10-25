package ir.hamghadam.app.domain

import java.time.LocalTime

/**
 * Result of parsing one line of free-form Persian text such as
 * `فردا ساعت ۹ صبح جلسه با تیم !!`.
 */
data class QuickAddResult(
    val title: String,
    val date: JalaliDate?,
    val time: LocalTime?,
    val priority: Int,
    /** The raw expressions that were understood, e.g. `["فردا", "ساعت ۹"]`. */
    val matches: List<String> = emptyList(),
    /** True when no date/time was recognised at all. */
    val isPlainText: Boolean = false,
)

/**
 * Deterministic, offline quick-capture parser for Persian scheduling phrases.
 *
 * It is intentionally rule based (no model, no network): every extraction is
 * explainable and unit tested. Unrecognised words are left in the title.
 */
object QuickAddParser {

    const val PRIORITY_LOW = 0
    const val PRIORITY_NORMAL = 1
    const val PRIORITY_HIGH = 2

    private const val MASK = '\u0000'

    private val months = PersianCalendar.monthNames.joinToString("|")
    private const val MERIDIEM = "بعد\\s*از\\s*ظهر|بعدازظهر|بامداد|صبح|ظهر|عصر|شب"

    private val numericDate = Regex("(\\d{4})\\s*[/\\-.]\\s*(\\d{1,2})\\s*[/\\-.]\\s*(\\d{1,2})")
    private val dayMonthYear = Regex("(\\d{1,2})\\s+($months)(?:\\s+(\\d{4}))?")
    private val monthDay = Regex("($months)\\s+(\\d{1,2})(?:\\s+(\\d{4}))?")
    private val dayMonthSlash = Regex("(?<![\\d/])(\\d{1,2})\\s*/\\s*(\\d{1,2})(?![\\d/])")
    private val relativeCount = Regex("(\\d{1,3})\\s*(روز|هفته|ماه|سال)\\s*(بعد|دیگر|آینده|بعدی|پیش)")
    private val relativeBare = Regex("(هفته|ماه|سال)\\s*(بعد|آینده|بعدی|پیش)")
    private val wordAfterCount = Regex("(?:^|\\s)(امروز|فردا|پس‌فردا|پس فردا|دیروز|پریروز|پس\\s*فردا)(?=\\s|$)")

    private val timeFraction = Regex("(?:ساعت\\s*)?(?<![\\d:])(\\d{1,2})\\s*و\\s*(نیم|ربع)\\s*($MERIDIEM)?")
    private val timeWithWord = Regex("ساعت\\s*(\\d{1,2})(?::(\\d{1,2}))?\\s*($MERIDIEM)?")
    private val timeColon = Regex("(?<![\\d:])(\\d{1,2}):(\\d{1,2})(?![\\d:])")
    private val timeMeridiem = Regex("(?<![\\d:])(\\d{1,2})\\s*($MERIDIEM)(?![\\u0621-\\u064A])")
    private val timeNoon = Regex("(?<![\\u0621-\\u064A])(ظهر|نصف\\s*شب|نیمه\\s*شب|نیمه‌شب)(?![\\u0621-\\u064A])")

    /** Persian weekday name mapped to [java.time.DayOfWeek.value] (Monday = 1 ... Sunday = 7). */
    private val weekdays = listOf(
        "یک‌شنبه" to 7, "یک شنبه" to 7, "یکشنبه" to 7,
        "دوشنبه" to 1, "دو شنبه" to 1,
        "سه‌شنبه" to 2, "سه شنبه" to 2, "سهشنبه" to 2,
        "چهارشنبه" to 3, "چهار شنبه" to 3,
        "پنج‌شنبه" to 4, "پنج شنبه" to 4, "پنجشنبه" to 4,
        "جمعه" to 5, "آدینه" to 5,
        "شنبه" to 6,
    ).sortedByDescending { it.first.length }

    fun parse(raw: String, today: JalaliDate): QuickAddResult {
        val normalized = normalizeWithMap(raw)
        val working = StringBuilder(normalized.text)
        var date: JalaliDate? = null
        var time: LocalTime? = null
        val matches = mutableListOf<String>()

        fun consume(range: IntRange, label: String) {
            matches += label
            for (i in range) working.setCharAt(i, MASK)
        }

        // 1) priority markers: "!!!" and "!!" mean high, "!" means low.
        var priority = PRIORITY_NORMAL
        val bangs = Regex("!{1,3}").findAll(working.toString()).toList()
        if (bangs.isNotEmpty()) {
            val longest = bangs.maxOf { it.value.length }
            priority = if (longest >= 2) PRIORITY_HIGH else PRIORITY_LOW
            for (m in bangs) consume(m.range, m.value)
        }

        // 2) weekday names are consumed early so they do not leak into the title,
        //    but they only supply the date when no explicit date was written.
        var weekdayCandidate: JalaliDate? = null
        for ((name, isoDay) in weekdays) {
            val range = findWord(working, name) ?: continue
            val currentIso = today.dayOfWeek().value
            val delta = (isoDay - currentIso + 7) % 7
            weekdayCandidate = today.plusDays(delta.toLong())
            consume(range, name)
            break
        }

        // 3) explicit dates -------------------------------------------------
        numericDate.find(working)?.let { m ->
            val y = m.groupValues[1].toIntOrNull()
            val mo = m.groupValues[2].toIntOrNull()
            val d = m.groupValues[3].toIntOrNull()
            if (y != null && mo != null && d != null && mo in 1..12 && d in 1..31) {
                val candidate = runCatching { JalaliDate(y, mo, d) }.getOrNull()
                if (candidate != null && candidate.isValid) {
                    date = candidate
                    consume(m.range, m.value)
                }
            }
        }

        if (date == null) {
            dayMonthYear.find(working)?.let { m ->
                val d = m.groupValues[1].toIntOrNull()
                val month = monthIndex(m.groupValues[2])
                val year = m.groupValues[3].toIntOrNull()
                if (d != null && month != null && d in 1..31) {
                    val candidate = resolveWithYear(today, d, month, year)
                    if (candidate != null) {
                        date = candidate
                        consume(m.range, m.value)
                    }
                }
            }
        }

        if (date == null) {
            monthDay.find(working)?.let { m ->
                val month = monthIndex(m.groupValues[1])
                val d = m.groupValues[2].toIntOrNull()
                val year = m.groupValues[3].toIntOrNull()
                if (d != null && month != null && d in 1..31) {
                    val candidate = resolveWithYear(today, d, month, year)
                    if (candidate != null) {
                        date = candidate
                        consume(m.range, m.value)
                    }
                }
            }
        }

        if (date == null) {
            dayMonthSlash.find(working)?.let { m ->
                val d = m.groupValues[1].toIntOrNull()
                val mo = m.groupValues[2].toIntOrNull()
                if (d != null && mo != null && mo in 1..12 && d in 1..31) {
                    val candidate = resolveWithYear(today, d, mo, null)
                    if (candidate != null) {
                        date = candidate
                        consume(m.range, m.value)
                    }
                }
            }
        }

        if (date == null) {
            wordAfterCount.find(working)?.let { m ->
                val word = m.groupValues[1].replace(Regex("\\s+"), " ")
                val offset = when (word) {
                    "امروز" -> 0L
                    "فردا" -> 1L
                    "پس‌فردا", "پس فردا", "پسفردا" -> 2L
                    "دیروز" -> -1L
                    "پریروز" -> -2L
                    else -> null
                }
                if (offset != null) {
                    date = today.plusDays(offset)
                    consume(m.range, word)
                }
            }
        }

        if (date == null) {
            relativeCount.find(working)?.let { m ->
                val count = m.groupValues[1].toIntOrNull()
                if (count != null && count > 0) {
                    val candidate = when (m.groupValues[2]) {
                        "روز" -> today.plusDays(count.toLong())
                        "هفته" -> today.plusDays(count * 7L)
                        "ماه" -> today.plusMonths(count)
                        "سال" -> today.plusYears(count)
                        else -> null
                    }
                    if (candidate != null) {
                        date = candidate
                        consume(m.range, m.value)
                    }
                }
            }
        }

        if (date == null) {
            relativeBare.find(working)?.let { m ->
                val candidate = when (m.groupValues[1]) {
                    "هفته" -> today.plusDays(7)
                    "ماه" -> today.plusMonths(1)
                    "سال" -> today.plusYears(1)
                    else -> null
                }
                if (candidate != null) {
                    date = candidate
                    consume(m.range, m.value)
                }
            }
        }

        if (date == null) date = weekdayCandidate

        // 4) times ----------------------------------------------------------
        timeFraction.find(working)?.let { m ->
            val hour = m.groupValues[1].toIntOrNull()
            val minute = if (m.groupValues[2] == "نیم") 30 else 15
            val meridiem = m.groupValues[3].ifBlank { null }
            val resolved = hour?.let { resolveHour(it, meridiem) }
            if (resolved != null) {
                time = LocalTime.of(resolved, minute)
                consume(m.range, m.value)
            }
        }

        if (time == null) {
            timeWithWord.find(working)?.let { m ->
                val hour = m.groupValues[1].toIntOrNull()
                val minute = m.groupValues[2].toIntOrNull() ?: 0
                val meridiem = m.groupValues[3].ifBlank { null }
                val resolved = hour?.let { resolveHour(it, meridiem) }
                if (resolved != null && minute in 0..59) {
                    time = LocalTime.of(resolved, minute)
                    consume(m.range, m.value)
                }
            }
        }

        if (time == null) {
            timeColon.find(working)?.let { m ->
                val hour = m.groupValues[1].toIntOrNull()
                val minute = m.groupValues[2].toIntOrNull()
                if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                    time = LocalTime.of(hour, minute)
                    consume(m.range, m.value)
                }
            }
        }

        if (time == null) {
            timeMeridiem.find(working)?.let { m ->
                val hour = m.groupValues[1].toIntOrNull()
                val resolved = hour?.let { resolveHour(it, m.groupValues[2]) }
                if (resolved != null) {
                    time = LocalTime.of(resolved, 0)
                    consume(m.range, m.value)
                }
            }
        }

        if (time == null) {
            timeNoon.find(working)?.let { m ->
                time = when {
                    m.groupValues[1].startsWith("ظهر") -> LocalTime.NOON
                    else -> LocalTime.MIDNIGHT
                }
                consume(m.range, m.value)
            }
        }

        return finish(working, normalized.indexMap, raw, date, time, priority, matches)
    }

    private fun finish(
        working: StringBuilder,
        indexMap: IntArray,
        raw: String,
        date: JalaliDate?,
        time: LocalTime?,
        priority: Int,
        matches: List<String>,
    ): QuickAddResult {
        // Rebuild from the original string so the user's own digits, ZWNJ and
        // letter forms survive; the parser only decides which characters to drop.
        val titleRaw = buildString {
            for (i in working.indices) {
                if (working[i] != MASK) {
                    val originalIndex = indexMap[i]
                    if (originalIndex in raw.indices) append(raw[originalIndex])
                }
            }
        }
        val title = PersianText.cleanTitle(PersianText.normalizeDisplay(titleRaw))
        return QuickAddResult(
            title = title,
            date = date,
            time = time,
            priority = priority,
            matches = matches.filter { it.isNotBlank() }
                .map { PersianText.toPersianDigits(PersianText.normalizeDisplay(it.trim())) },
            isPlainText = date == null && time == null && priority == PRIORITY_NORMAL,
        )
    }

    /** Adds a Jalali year when the text omitted one: past dates roll to next year. */
    private fun resolveWithYear(today: JalaliDate, day: Int, month: Int, year: Int?): JalaliDate? {
        if (year != null) {
            if (year < 1200 || year > 1600) return null
            return runCatching { JalaliDate(year, month, day) }.getOrNull()?.takeIf { it.isValid }
        }
        val thisYear = runCatching { JalaliDate(today.year, month, day) }.getOrNull()
        if (thisYear == null || !thisYear.isValid) return null
        return if (thisYear < today) thisYear.plusYears(1).takeIf { it.isValid } ?: thisYear else thisYear
    }

    private fun resolveHour(hour: Int, meridiem: String?): Int? {
        if (meridiem == null) return hour.takeIf { it in 0..23 }
        if (hour !in 0..12) return null
        val resolved = when {
            meridiem.contains("بعد") -> if (hour in 1..11) hour + 12 else hour
            meridiem == "ظهر" -> if (hour in 1..11) hour + 12 else hour
            meridiem == "عصر" -> if (hour in 1..11) hour + 12 else hour
            meridiem == "شب" -> if (hour == 12) 0 else if (hour in 1..11) hour + 12 else hour
            meridiem == "صبح" || meridiem == "بامداد" -> if (hour == 12) 0 else hour
            else -> hour
        }
        return resolved.takeIf { it in 0..23 }
    }

    private fun monthIndex(name: String): Int? {
        val index = PersianCalendar.monthNames.indexOf(name.replace(Regex("\\s+"), " "))
        return if (index >= 0) index + 1 else null
    }

    private class NormalizedText(val text: String, val indexMap: IntArray)

    /**
     * Normalises the input while remembering where every produced character came
     * from, so the title can be rebuilt from the user's original text.
     */
    private fun normalizeWithMap(raw: String): NormalizedText {
        val text = StringBuilder(raw.length)
        val indexMap = IntArray(raw.length)
        var size = 0
        for (i in raw.indices) {
            val mapped: Char? = when (val ch = raw[i]) {
                in '\u06F0'..'\u06F9' -> '0' + (ch - '\u06F0')
                in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
                '\u064A' -> '\u06CC' // ي -> ی
                '\u0643' -> '\u06A9' // ك -> ک
                '\u0629' -> '\u0647' // ة -> ه
                '\u200B', '\u00A0' -> ' '
                '\u0654', '\u0655', '\u0640', '\u0670' -> null // hamza, kashida, superscript alef
                in '\u064B'..'\u0652' -> null // harakat
                else -> ch
            }
            if (mapped != null) {
                text.append(mapped)
                indexMap[size] = i
                size++
            }
        }
        return NormalizedText(text.toString(), indexMap.copyOf(size))
    }

    /** Finds a whole-word occurrence, rejecting matches inside longer words. */
    private fun findWord(text: CharSequence, word: String): IntRange? {
        var from = 0
        while (true) {
            val index = text.toString().indexOf(word, from)
            if (index < 0) return null
            val before = if (index > 0) text[index - 1] else ' '
            val afterIndex = index + word.length
            val after = if (afterIndex < text.length) text[afterIndex] else ' '
            if (!isWordChar(before) && !isWordChar(after)) return index until afterIndex
            from = index + 1
        }
    }

    private fun isWordChar(ch: Char): Boolean =
        ch.isLetterOrDigit() || ch == '\u200C' || ch == '\u0654'
}
