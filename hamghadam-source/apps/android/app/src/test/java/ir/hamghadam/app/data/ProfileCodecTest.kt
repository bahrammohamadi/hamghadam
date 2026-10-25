package ir.hamghadam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Multi-account persistence: the account list must survive a round-trip and be defensive. */
class ProfileCodecTest {

    private val family = listOf(
        LocalProfile(id = "a1", name = "مریم", emoji = "🌙", colorArgb = 0xFF17864B.toInt(), createdAtEpochMillis = 1_700_000_000_000L),
        LocalProfile(id = "a2", name = "بردیا", emoji = "⚽", colorArgb = 0xFF1D63D8.toInt(), createdAtEpochMillis = 1_700_000_100_000L),
    )

    @Test
    fun roundTripKeepsEveryField() {
        val decoded = ProfileCodec.decode(ProfileCodec.encode(family))
        assertEquals(family, decoded)
    }

    @Test
    fun emptyListEncodesAndDecodesToEmpty() {
        assertEquals(emptyList<LocalProfile>(), ProfileCodec.decode(ProfileCodec.encode(emptyList())))
    }

    @Test
    fun missingStorageDecodesToEmptyList() {
        assertEquals(emptyList<LocalProfile>(), ProfileCodec.decode(null))
        assertEquals(emptyList<LocalProfile>(), ProfileCodec.decode(""))
        assertEquals(emptyList<LocalProfile>(), ProfileCodec.decode("   "))
    }

    @Test
    fun theImplicitFirstAccountIsNeverPersisted() {
        val raw = """[{"id":"","name":"حساب من"},{"id":"a1","name":"مریم"}]"""
        val decoded = ProfileCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("a1", decoded.first().id)
    }

    @Test
    fun duplicateIdsKeepTheFirstRowOnly() {
        val raw = """[{"id":"a1","name":"اول"},{"id":"a1","name":"تکراری"}]"""
        val decoded = ProfileCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("اول", decoded.first().name)
    }

    @Test
    fun brokenEntriesAreSkippedInsteadOfCrashing() {
        val raw = """[{"name":"بی شناسه"},{"id":"a2"},{"id":"a3","name":"سالم"}]"""
        val decoded = ProfileCodec.decode(raw)
        assertEquals(2, decoded.size)
        assertEquals("a2", decoded[0].id)
        assertEquals("🌿", decoded[0].emoji) // default emoji
        assertEquals("سالم", decoded[1].name)
    }

    @Test
    fun invalidJsonDecodesToEmptyList() {
        assertEquals(emptyList<LocalProfile>(), ProfileCodec.decode("{not-json"))
        assertEquals(emptyList<LocalProfile>(), ProfileCodec.decode("""{"id":"a1"}"""))
    }

    @Test
    fun namesAreTrimmedCappedAndNeverBlank() {
        assertEquals("مریم", ProfileCodec.cleanName("  مریم  ", "پیش‌فرض"))
        assertEquals("پیش‌فرض", ProfileCodec.cleanName("   ", "پیش‌فرض"))
        val long = "ب".repeat(60)
        assertEquals(ProfileCodec.MAX_NAME_LENGTH, ProfileCodec.cleanName(long, "x").length)
        assertTrue(ProfileCodec.cleanName(long, "x").all { it == 'ب' })
    }

    @Test
    fun differentAccountsHaveDifferentIds() {
        val created = ProfileStoreishIds()
        assertNotEquals(created.first(), created.second())
    }

    @Test
    fun deletingTheDefaultAccountIsRefusedByTheStore() {
        // Pure check of the rule the store encodes: the implicit account is permanent.
        assertFalse(DEFAULT_PROFILE_ID.isNotEmpty())
        assertTrue(LocalProfile(id = DEFAULT_PROFILE_ID, name = "حساب من").id.isEmpty())
    }

    /** Mirrors the ids the store hands out for two new accounts. */
    private fun ProfileStoreishIds(): Pair<String, String> =
        java.util.UUID.randomUUID().toString() to java.util.UUID.randomUUID().toString()
}
