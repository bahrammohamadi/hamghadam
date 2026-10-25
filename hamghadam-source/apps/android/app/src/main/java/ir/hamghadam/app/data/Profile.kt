package ir.hamghadam.app.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import ir.hamghadam.app.domain.SimpleJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * A local account ("حساب") on this device.
 *
 * Everything is stored on the phone; there is no server account. Each profile
 * owns its own tasks, lists, habits, personal events, templates and focus
 * sessions, so a family can share one install without mixing their data.
 */
data class LocalProfile(
    val id: String,
    val name: String,
    val emoji: String = "🌿",
    val colorArgb: Int = 0xFF6C4EF6.toInt(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

/** Rows created before multi-account support belong to this profile. */
const val DEFAULT_PROFILE_ID = ""

val ProfileEmojis: List<String> = listOf("🌿", "🌙", "☀️", "🐣", "🦊", "🎓", "💼", "🏡", "🎨", "⚽")

val ProfileColors: List<Int> = listOf(
    0xFF6C4EF6.toInt(),
    0xFFE2472F.toInt(),
    0xFF17864B.toInt(),
    0xFF1D63D8.toInt(),
    0xFFC22E7A.toInt(),
    0xFFB4720A.toInt(),
)

/**
 * Persists the list of local profiles plus which one is active.
 *
 * Deliberately tiny (SharedPreferences + the project's own JSON writer): it is
 * a few kilobytes and avoids a second database just for account metadata.
 */
class ProfileStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("hamghadam.profiles", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow(read())
    val profiles: StateFlow<List<LocalProfile>> = _profiles.asStateFlow()

    private val _activeId = MutableStateFlow(
        prefs.getString(KEY_ACTIVE, DEFAULT_PROFILE_ID) ?: DEFAULT_PROFILE_ID,
    )
    val activeId: StateFlow<String> = _activeId.asStateFlow()

    val active: LocalProfile
        get() = _profiles.value.firstOrNull { it.id == _activeId.value } ?: defaultProfile

    /** The implicit first account; always id "" so old rows keep belonging to it. */
    val defaultProfile: LocalProfile = LocalProfile(id = DEFAULT_PROFILE_ID, name = "حساب من", emoji = "🌿")

    fun create(name: String, emoji: String, colorArgb: Int): LocalProfile {
        val clean = name.trim().ifBlank { "حساب تازه" }
        val profile = LocalProfile(
            id = UUID.randomUUID().toString(),
            name = clean.take(24),
            emoji = emoji,
            colorArgb = colorArgb,
        )
        write(_profiles.value + profile)
        return profile
    }

    fun update(id: String, name: String, emoji: String, colorArgb: Int) {
        write(
            _profiles.value.map { profile ->
                if (profile.id != id) profile
                else profile.copy(name = name.trim().take(24).ifBlank { profile.name }, emoji = emoji, colorArgb = colorArgb)
            },
        )
    }

    /** Removes the account. Callers must delete its rows first (see TaskRepository). */
    fun remove(id: String) {
        if (id == DEFAULT_PROFILE_ID) return
        val remaining = _profiles.value.filterNot { it.id == id }
        write(remaining)
        if (_activeId.value == id) setActive(DEFAULT_PROFILE_ID)
    }

    fun setActive(id: String) {
        _activeId.value = id
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    private fun write(list: List<LocalProfile>) {
        _profiles.value = list
        prefs.edit().putString(KEY_PROFILES, encode(list)).apply()
    }

    private fun read(): List<LocalProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        val parsed = runCatching { SimpleJson.parse(raw) as? List<*> }.getOrNull().orEmpty()
        return parsed.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val id = map["id"] as? String ?: return@mapNotNull null
            if (id == DEFAULT_PROFILE_ID) return@mapNotNull null
            LocalProfile(
                id = id,
                name = map["name"] as? String ?: "حساب",
                emoji = map["emoji"] as? String ?: "🌿",
                colorArgb = (map["colorArgb"] as? Number)?.toInt() ?: 0xFF6C4EF6.toInt(),
                createdAtEpochMillis = (map["createdAt"] as? Number)?.toLong() ?: 0L,
            )
        }
    }

    private fun encode(list: List<LocalProfile>): String = SimpleJson.encode(
        list.map { profile ->
            mapOf(
                "id" to profile.id,
                "name" to profile.name,
                "emoji" to profile.emoji,
                "colorArgb" to profile.colorArgb,
                "createdAt" to profile.createdAtEpochMillis,
            )
        },
    )

    companion object {
        private const val KEY_PROFILES = "profiles"
        private const val KEY_ACTIVE = "active"
    }
}

fun LocalProfile.color(): Color = Color(colorArgb)
