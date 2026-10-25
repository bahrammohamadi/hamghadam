package ir.hamghadam.app.domain

import java.time.DayOfWeek

/**
 * Persian names and display formatting for Jalali dates.
 *
 * Everything that turns a date into user-visible text lives here (and takes a
 * `persianDigits` flag) so the UI never has to re-implement calendar language.
 */
object PersianCalendar {

    val monthNames: List<String> = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val weekdayByDay: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.SATURDAY to "شنبه",
        DayOfWeek.SUNDAY to "یک‌شنبه",
        DayOfWeek.MONDAY to "دوشنبه",
        DayOfWeek.TUESDAY to "سه‌شنبه",
        DayOfWeek.WEDNESDAY to "چهارشنبه",
        DayOfWeek.THURSDAY to "پنج‌شنبه",
        DayOfWeek.FRIDAY to "جمعه",
    )

    private val weekdayInitialByDay: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.SATURDAY to "ش",
        DayOfWeek.SUNDAY to "ی",
        DayOfWeek.MONDAY to "د",
        DayOfWeek.TUESDAY to "س",
        DayOfWeek.WEDNESDAY to "چ",
        DayOfWeek.THURSDAY to "پ",
        DayOfWeek.FRIDAY to "ج",
    )

    fun monthName(month: Int): String = monthNames[(month - 1).coerceIn(0, 11)]

    fun weekdayName(dayOfWeek: DayOfWeek): String = weekdayByDay.getValue(dayOfWeek)

    fun weekdayInitial(dayOfWeek: DayOfWeek): String = weekdayInitialByDay.getValue(dayOfWeek)

    /** Days of the Persian week, Saturday first. */
    fun weekOrder(): List<DayOfWeek> = listOf(
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    )

    /** First day (Saturday) of the week containing [date]. */
    fun weekStart(date: JalaliDate): JalaliDate {
        val offset = (date.dayOfWeek().value + 1) % 7 // Saturday -> 0, Sunday -> 1, ...
        return date.plusDays(-offset.toLong())
    }

    /** The seven days of the week containing [date], Saturday first. */
    fun weekDays(date: JalaliDate): List<JalaliDate> {
        val start = weekStart(date)
        return (0L..6L).map { start.plusDays(it) }
    }

    /**
     * Month cells for a calendar grid, Saturday first. Leading and trailing
     * cells are null so the grid always has full weeks.
     */
    fun monthGrid(date: JalaliDate): List<List<JalaliDate?>> {
        val first = JalaliDate(date.year, date.month, 1)
        val length = JalaliDate.monthLength(date.year, date.month)
        val leading = (first.dayOfWeek().value + 1) % 7 // Saturday -> 0
        val cells = ArrayList<JalaliDate?>(42)
        repeat(leading) { cells.add(null) }
        for (day in 1..length) cells.add(JalaliDate(date.year, date.month, day))
        while (cells.size % 7 != 0) cells.add(null)
        return cells.chunked(7)
    }

    /** "آبان ۱۴۰۵" */
    fun monthTitle(date: JalaliDate, persianDigits: Boolean = true): String =
        "${monthName(date.month)} ${PersianText.number(date.year, persianDigits)}"

    /** "شنبه ۳ آبان ۱۴۰۵" */
    fun longDate(date: JalaliDate, persianDigits: Boolean = true): String =
        "${weekdayName(date.dayOfWeek())} " +
            "${PersianText.number(date.day, persianDigits)} " +
            "${monthName(date.month)} " +
            PersianText.number(date.year, persianDigits)

    /** "۳ آبان" — used when the year is obvious from context. */
    fun shortDate(date: JalaliDate, persianDigits: Boolean = true): String =
        "${PersianText.number(date.day, persianDigits)} ${monthName(date.month)}"

    /** Today / tomorrow / yesterday get words, everything else gets a short date. */
    fun relativeDate(
        date: JalaliDate,
        today: JalaliDate,
        persianDigits: Boolean = true,
    ): String {
        val diff = date.toEpochDay() - today.toEpochDay()
        return when (diff) {
            0L -> "امروز"
            1L -> "فردا"
            2L -> "پس‌فردا"
            -1L -> "دیروز"
            -2L -> "پریروز"
            else -> {
                val sameYear = date.year == today.year
                if (sameYear) shortDate(date, persianDigits)
                else "${shortDate(date, persianDigits)} ${PersianText.number(date.year, persianDigits)}"
            }
        }
    }

    /** Convenience for a task due label: "امروز ۰۹:۳۰". */
    fun dueLabel(
        date: JalaliDate?,
        minuteOfDay: Int?,
        today: JalaliDate,
        persianDigits: Boolean = true,
    ): String? {
        if (date == null) return null
        val dayPart = relativeDate(date, today, persianDigits)
        if (minuteOfDay == null) return dayPart
        return "$dayPart ${PersianText.clock(minuteOfDay / 60, minuteOfDay % 60, persianDigits)}"
    }

    fun greeting(hourOfDay: Int): String = when (hourOfDay) {
        in 4..11 -> "صبح بخیر"
        in 12..16 -> "ظهر بخیر"
        in 17..19 -> "عصر بخیر"
        else -> "شب بخیر"
    }

    /** "شنبه" style day name plus day number, used in the calendar strip. */
    fun weekdayAndDay(date: JalaliDate, persianDigits: Boolean = true): String =
        "${weekdayName(date.dayOfWeek())} ${PersianText.number(date.day, persianDigits)}"
}
