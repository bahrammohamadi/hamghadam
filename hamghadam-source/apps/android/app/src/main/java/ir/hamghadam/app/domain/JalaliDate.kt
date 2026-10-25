package ir.hamghadam.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A civil Jalali (Solar Hijri) date.
 *
 * The conversion uses the well-known 33-year break cycle (Borkowski / Khayyam)
 * algorithm that backs `jalaali-js`, cross-checked against `jdatetime` and
 * `convertdate` reference implementations for every day between 1900 and 2100
 * (see `JalaliDateTest`).
 *
 * Dates are civil values, not instants: two devices in different time zones see
 * the same `JalaliDate` for the same wall-clock day. Persisted tasks store the
 * epoch day (`java.time.LocalDate.toEpochDay`) so the day never shifts when the
 * device time zone changes, while *times of day* are stored separately.
 */
data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {

    init {
        require(month in 1..12) { "Invalid Jalali month: $month" }
        require(day in 1..31) { "Invalid Jalali day: $day" }
        require(JalaliMath.isYearSupported(year)) { "Jalali year out of supported range: $year" }
    }

    /** True when the day actually exists in the calendar (e.g. 31 Esfand never does). */
    val isValid: Boolean
        get() = day <= monthLength(year, month)

    /** Day count since 1970-01-01, identical to [LocalDate.toEpochDay]. */
    fun toEpochDay(): Long = JalaliMath.jalaliToJdn(year, month, day) - JDN_EPOCH_OFFSET

    fun toGregorian(): LocalDate = LocalDate.ofEpochDay(toEpochDay())

    fun dayOfWeek(): DayOfWeek = toGregorian().dayOfWeek

    fun plusDays(days: Long): JalaliDate = fromEpochDay(toEpochDay() + days)

    /** Shifts by Jalali months, clamping the day to the last day of the target month. */
    fun plusMonths(months: Int): JalaliDate {
        var y = year
        var m = month + months
        while (m > 12) { m -= 12; y += 1 }
        while (m < 1) { m += 12; y -= 1 }
        return JalaliDate(y, m, day.coerceAtMost(monthLength(y, m)))
    }

    /** Shifts by Jalali years, clamping 30 Esfand to 29 in non-leap years. */
    fun plusYears(years: Int): JalaliDate {
        val y = year + years
        return JalaliDate(y, month, day.coerceAtMost(monthLength(y, month)))
    }

    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun formatSlash(): String = "%04d/%02d/%02d".format(year, month, day)

    override fun toString(): String = formatSlash()

    companion object {
        private const val JDN_EPOCH_OFFSET = 2_440_588L

        fun fromEpochDay(epochDay: Long): JalaliDate = JalaliMath.jdnToJalali(epochDay + JDN_EPOCH_OFFSET)

        fun fromGregorian(date: LocalDate): JalaliDate = fromEpochDay(date.toEpochDay())

        fun today(): JalaliDate = fromGregorian(LocalDate.now())

        fun of(year: Int, month: Int, day: Int): JalaliDate = JalaliDate(year, month, day)

        fun isLeapYear(year: Int): Boolean = JalaliMath.isJalaliLeapYear(year)

        fun monthLength(year: Int, month: Int): Int = when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isLeapYear(year)) 30 else 29
            else -> throw IllegalArgumentException("Invalid Jalali month: $month")
        }

        /** Parses `1404/07/12`, `1404-7-12` and `1404/7/12`. Returns null for anything else. */
        fun parseSlash(text: String): JalaliDate? {
            val parts = text.trim().split('/', '-', '.')
            if (parts.size != 3) return null
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            if (y < 1000 || m !in 1..12 || d !in 1..31) return null
            val candidate = JalaliDate(y, m, d)
            return if (candidate.isValid) candidate else null
        }
    }
}

/** Calendar arithmetic with no Android dependencies so it can be unit-tested on the JVM. */
internal object JalaliMath {

    /** Jalali years supported by the break table below. */
    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    fun isYearSupported(year: Int): Boolean = year >= breaks.first() && year < breaks.last()

    private fun div(a: Int, b: Int): Int = Math.floorDiv(a, b)

    private fun mod(a: Int, b: Int): Int = Math.floorMod(a, b)

    private class CalendarInfo(val leap: Int, val gregorianYear: Int, val march: Int)

    private fun jalCal(jy: Int, withoutLeap: Boolean): CalendarInfo {
        require(isYearSupported(jy)) { "Jalali year out of supported range: $jy" }
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0

        for (i in 1 until bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG

        var leap = 0
        if (!withoutLeap) {
            if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
            leap = mod(mod(n + 1, 33) - 1, 4)
            if (leap == -1) leap = 4
        }
        return CalendarInfo(leap, gy, march)
    }

    fun isJalaliLeapYear(jy: Int): Boolean = jalCal(jy, false).leap == 0

    /**
     * Standard Gregorian -> Julian Day Number conversion
     * (Richards / "Calendrical Calculations"), valid for the whole Gregorian era.
     */
    private fun gregorianToJdn(gy: Int, gm: Int, gd: Int): Long {
        val a = div(14 - gm, 12)
        val y = gy + 4800 - a
        val m = gm + 12 * a - 3
        return gd.toLong() + div(153L * m + 2, 5) + 365L * y +
            div(y.toLong(), 4) - div(y.toLong(), 100) + div(y.toLong(), 400) - 32045
    }

    /** Inverse of [gregorianToJdn]. */
    private fun jdnToGregorian(jdn: Long): Triple<Int, Int, Int> {
        val a = jdn + 32044
        val b = div(4 * a + 3, 146097)
        val c = a - div(146097 * b, 4)
        val d = div(4 * c + 3, 1461)
        val e = c - div(1461 * d, 4)
        val m = div(5 * e + 2, 153)
        val day = (e - div(153 * m + 2, 5) + 1).toInt()
        val month = (m + 3 - 12 * div(m, 10)).toInt()
        val year = (100 * b + d - 4800 + div(m, 10)).toInt()
        return Triple(year, month, day)
    }

    fun jalaliToJdn(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy, true)
        return gregorianToJdn(r.gregorianYear, 3, r.march) +
            (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    fun jdnToJalali(jdn: Long): JalaliDate {
        val gy = jdnToGregorian(jdn).first
        var jy = gy - 621
        var yearStart = jalaliToJdn(jy, 1, 1)
        if (jdn < yearStart) {
            jy -= 1
            yearStart = jalaliToJdn(jy, 1, 1)
        }
        var k = (jdn - yearStart).toInt()
        val month: Int
        val day: Int
        if (k <= 185) {
            month = 1 + div(k, 31)
            day = mod(k, 31) + 1
        } else {
            k -= 186
            month = 7 + div(k, 30)
            day = mod(k, 30) + 1
        }
        return JalaliDate(jy, month, day)
    }

    // Helpers that mirror JavaScript's floor division on Long values.
    private fun div(a: Long, b: Long): Long = Math.floorDiv(a, b)

    private fun mod(a: Long, b: Long): Long = Math.floorMod(a, b)
}
