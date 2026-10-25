package ir.hamghadam.app.domain

import java.util.UUID

/**
 * One checklist line inside a task («زیروظیفه»).
 *
 * Storage: the whole checklist is a JSON array in a single `subtasks` TEXT
 * column of the task row. Rationale (documented in docs/11 §3):
 *  - checklists are small (a handful of lines) and always read with their task,
 *    so a join table would add queries and a second migration for no gain;
 *  - the task stays one aggregate, which keeps backup/restore, undo and future
 *    sync operations atomic;
 *  - unknown JSON keys are ignored on read, so the format can grow later.
 */
data class Subtask(
    val id: String,
    val title: String,
    val done: Boolean = false,
) {
    companion object {
        const val MAX_ITEMS = 50
        const val MAX_TITLE = 120

        fun create(title: String): Subtask? {
            val clean = cleanTitle(title) ?: return null
            return Subtask(id = UUID.randomUUID().toString(), title = clean, done = false)
        }

        fun cleanTitle(raw: String): String? =
            raw.trim().replace(Regex("\\s+"), " ").take(MAX_TITLE).ifBlank { null }
    }
}

/** JSON (de)serialisation for the checklist column; pure JVM, unit-tested. */
object SubtaskCodec {

    fun encode(items: List<Subtask>): String {
        if (items.isEmpty()) return ""
        return SimpleJson.encode(
            items.take(Subtask.MAX_ITEMS).map { item ->
                mapOf(
                    "id" to item.id,
                    "title" to item.title,
                    "done" to item.done,
                )
            },
        )
    }

    /** Never throws: a corrupt column reads as an empty checklist. */
    fun decode(raw: String?): List<Subtask> {
        if (raw.isNullOrBlank()) return emptyList()
        val parsed = runCatching { SimpleJson.parse(raw) as? List<*> }.getOrNull().orEmpty()
        val seen = mutableSetOf<String>()
        return parsed.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val title = Subtask.cleanTitle(map["title"] as? String ?: return@mapNotNull null)
                ?: return@mapNotNull null
            val id = (map["id"] as? String)?.takeIf { it.isNotBlank() && seen.add(it) }
                ?: UUID.randomUUID().toString().also { seen.add(it) }
            Subtask(id = id, title = title, done = map["done"] as? Boolean ?: false)
        }.take(Subtask.MAX_ITEMS)
    }

    /** Returns `done/total` for the progress chip; total 0 when there is none. */
    fun progress(items: List<Subtask>): Pair<Int, Int> =
        items.count { it.done } to items.size
}

/** Number of completed items to total items of a checklist. */
typealias SubtaskProgress = Pair<Int, Int>

/** Persian label used by the editor and the task row, e.g. «۲ از ۵». */
fun SubtaskProgress.label(persianDigits: Boolean): String {
    val (done, total) = this
    return if (total == 0) "" else "${PersianText.number(done, persianDigits)} از ${PersianText.number(total, persianDigits)}"
}
