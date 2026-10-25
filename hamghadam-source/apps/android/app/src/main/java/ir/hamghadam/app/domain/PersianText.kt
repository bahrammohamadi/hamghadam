package ir.hamghadam.app.domain

/**
 * Text helpers for Persian input and output.
 *
 * Two normalisation levels exist on purpose:
 *  - [normalizeDisplay] only fixes the Arabic/Persian letter variants so stored
 *    text keeps the user's own wording (ZWNJ, digits, punctuation stay intact).
 *  - [normalizeForMatching] is aggressive and is only used for search and for
 *    parsing quick-add text.
 */
object PersianText {

    private const val PERSIAN_ZERO = '\u06F0' // ۰
    private const val ARABIC_ZERO = '\u0660'  // ٠
    private const val ZWNJ = '\u200C'

    private val DIACRITICS = Regex("[\\u064B-\\u0652\\u0670\\u0640]")

    /** Renders Latin digits as Persian digits. Non-digit characters are untouched. */
    fun toPersianDigits(input: String): String = buildString(input.length) {
        for (ch in input) {
            append(
                when (ch) {
                    in '0'..'9' -> PERSIAN_ZERO + (ch - '0')
                    else -> ch
                }
            )
        }
    }

    /** Renders Persian/Arabic digits as Latin digits so they can be parsed. */
    fun toLatinDigits(input: String): String = buildString(input.length) {
        for (ch in input) {
            append(
                when (ch) {
                    in PERSIAN_ZERO..(PERSIAN_ZERO + 9) -> '0' + (ch - PERSIAN_ZERO)
                    in ARABIC_ZERO..(ARABIC_ZERO + 9) -> '0' + (ch - ARABIC_ZERO)
                    else -> ch
                }
            )
        }
    }

    /** Fixes ي/ك/ة and removes kashida and harakat without changing the words themselves. */
    fun normalizeDisplay(input: String): String = input
        .replace('\u064A', '\u06CC') // ي -> ی
        .replace('\u0643', '\u06A9') // ك -> ک
        .replace('\u0629', '\u0647') // ة -> ه
        .replace(DIACRITICS, "") // kashida + harakat

    /** Normalisation used for searching and parsing only; never shown back to the user. */
    fun normalizeForMatching(input: String): String = normalizeDisplay(input)
        .replace(ZWNJ, ' ')
        .replace('\u200B', ' ')
        .replace('\u00A0', ' ')
        .let { toLatinDigits(it).lowercase() }
        .replace(Regex("\\s+"), " ")
        .trim()

    /** Persian digits optionally applied to a number. */
    fun number(value: Long, persianDigits: Boolean = true): String {
        val raw = value.toString()
        return if (persianDigits) toPersianDigits(raw) else raw
    }

    fun number(value: Int, persianDigits: Boolean = true): String = number(value.toLong(), persianDigits)

    fun twoDigits(value: Int, persianDigits: Boolean = true): String {
        val raw = if (value < 10) "0$value" else value.toString()
        return if (persianDigits) toPersianDigits(raw) else raw
    }

    /** `09:30` style clock text. */
    fun clock(hour: Int, minute: Int, persianDigits: Boolean = true): String =
        "${twoDigits(hour, persianDigits)}:${twoDigits(minute, persianDigits)}"

    /**
     * Trims stray punctuation left behind after removing date/time words from a title.
     * The letter "و" is only dropped when it stands alone (a dangling conjunction),
     * never from the end of a word such as "دارو".
     */
    fun cleanTitle(raw: String): String = raw
        .replace(Regex("\\s+"), " ")
        .trim()
        .replace(Regex("^و\\s+"), "")
        .replace(Regex("\\s+و$"), "")
        .trim(',', '،', ';', '؛', '-', '–', ':', '.', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}
