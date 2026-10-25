package ir.hamghadam.app.domain

/**
 * Free-form tags ("برچسب") used to group tasks across projects.
 *
 * Tags are stored as a comma separated string inside one task row: a task has a
 * handful of labels at most, so a join table would add complexity without any
 * benefit for an offline-first single-device app.
 */
object Tags {

    const val MAX_TAGS = 6
    const val MAX_LENGTH = 24

    /** Splits a stored csv value, trimming and normalising every token. */
    fun parseCsv(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',', '،', '؛', ';')
            .mapNotNull { normalizeTag(it) }
            .distinct()
            .take(MAX_TAGS)
    }

    /** Turns "#کار" / " کار " / "کار،" into "کار"; returns null when empty. */
    fun normalizeTag(raw: String): String? {
        val cleaned = raw.trim()
            .trimStart('#', '＃')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty()) return null
        return cleaned.take(MAX_LENGTH)
    }

    fun toCsv(tags: List<String>): String = tags.mapNotNull { normalizeTag(it) }.distinct().take(MAX_TAGS).joinToString(",")

    fun add(rawCsv: String?, tag: String): String {
        val normalized = normalizeTag(tag) ?: return parseCsv(rawCsv).joinToString(",")
        return (parseCsv(rawCsv) + normalized).distinct().take(MAX_TAGS).joinToString(",")
    }

    fun remove(rawCsv: String?, tag: String): String =
        parseCsv(rawCsv).filterNot { it == normalizeTag(tag) }.joinToString(",")

    fun display(tag: String): String = "#$tag"

    /** Tags suggested by the quick-add parser: "#خرید" or "#خرید_هفتگی". */
    val quickAddPattern: Regex = Regex("#([\\p{L}\\p{N}_\\u200C]{1,$MAX_LENGTH})")

    /** Extracts hashtags and returns them together with the text with tags removed. */
    fun extractFromQuickAdd(raw: String): Pair<String, List<String>> {
        val found = quickAddPattern.findAll(raw).mapNotNull { normalizeTag(it.groupValues[1]) }.toList()
        val stripped = raw.replace(quickAddPattern, " ").replace(Regex("\\s+"), " ").trim()
        return stripped to found.distinct().take(MAX_TAGS)
    }

    /** Sorted tag cloud (tag to open task count) for filter chips. */
    fun counts(taskTags: List<String>): List<Pair<String, Int>> =
        taskTags.flatMap { parseCsv(it) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }
}
