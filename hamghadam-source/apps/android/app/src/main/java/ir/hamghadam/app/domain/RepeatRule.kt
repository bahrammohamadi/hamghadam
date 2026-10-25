package ir.hamghadam.app.domain

/** How a task repeats once it is completed. */
enum class RepeatRule(val storageValue: String, val label: String) {
    NONE("NONE", "بدون تکرار"),
    DAILY("DAILY", "هر روز"),
    WEEKLY("WEEKLY", "هر هفته"),
    MONTHLY("MONTHLY", "هر ماه");

    /**
     * Next occurrence after [current], or null when the task does not repeat.
     * Monthly repeats clamp to the last day of a shorter month (31 Farvardin -> 31 Ordibehesht,
     * 31 Shahrivar -> 30 Mehr) instead of silently skipping the month.
     */
    fun nextDate(current: JalaliDate): JalaliDate? = when (this) {
        NONE -> null
        DAILY -> current.plusDays(1)
        WEEKLY -> current.plusDays(7)
        MONTHLY -> current.plusMonths(1)
    }

    companion object {
        /** Rules offered in the task editor. */
        val scheduled: List<RepeatRule> = listOf(DAILY, WEEKLY, MONTHLY)

        fun fromStorage(value: String?): RepeatRule =
            entries.firstOrNull { it.storageValue == value } ?: NONE
    }
}

/** Reminder offsets offered in the task editor, in minutes before the due time. */
object ReminderOptions {
    const val AT_TIME = 0
    const val FIVE_MINUTES = 5
    const val FIFTEEN_MINUTES = 15
    const val THIRTY_MINUTES = 30
    const val ONE_HOUR = 60
    const val ONE_DAY = 24 * 60

    val all: List<Int> = listOf(AT_TIME, FIVE_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR, ONE_DAY)

    fun label(minutes: Int?): String = when (minutes) {
        null -> "بدون یادآور"
        AT_TIME -> "در زمان موعد"
        ONE_DAY -> "یک روز قبل"
        ONE_HOUR -> "یک ساعت قبل"
        else -> if (minutes >= 60 && minutes % 60 == 0) {
            "${PersianText.number(minutes / 60)} ساعت قبل"
        } else {
            "${PersianText.number(minutes)} دقیقه قبل"
        }
    }
}
