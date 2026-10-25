package ir.hamghadam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Data isolation between accounts. The most important case is the upgrade path:
 * rows written before 0.4.0 have `profileId == ""` and must stay visible to the
 * implicit first account, never to a newly created one.
 */
class ProfileScopeTest {

    private fun task(id: String, owner: String) = TaskEntity(id = id, title = "کار $id", profileId = owner)

    private val rows = listOf(
        task("legacy-1", DEFAULT_PROFILE_ID),
        task("legacy-2", DEFAULT_PROFILE_ID),
        task("maryam-1", "a1"),
        task("bardia-1", "a2"),
    )

    @Test
    fun legacyRowsBelongToTheImplicitFirstAccount() {
        assertEquals(
            listOf("legacy-1", "legacy-2"),
            rows.ownedBy(DEFAULT_PROFILE_ID) { it.profileId }.map { it.id },
        )
    }

    @Test
    fun aNewAccountSeesOnlyItsOwnRows() {
        assertEquals(1, rows.ownedBy("a1") { it.profileId }.size)
        assertEquals("maryam-1", rows.ownedBy("a1") { it.profileId }.first().id)
    }

    @Test
    fun anUnknownAccountSeesNothing() {
        assertTrue(rows.ownedBy("ghost") { it.profileId }.isEmpty())
    }

    @Test
    fun allAccountsTogetherAccountForEveryRow() {
        val ids = listOf(DEFAULT_PROFILE_ID, "a1", "a2")
            .flatMap { rows.ownedBy(it) { row -> row.profileId } }
            .map { it.id }
        assertEquals(rows.size, ids.size)
        assertEquals(rows.map { it.id }.toSet(), ids.toSet())
    }

    @Test
    fun ownershipCheckMatchesTheFilter() {
        val row = task("x", "a1")
        assertTrue(row.isOwnedBy("a1") { it.profileId })
        assertFalse(row.isOwnedBy(DEFAULT_PROFILE_ID) { it.profileId })
    }

    @Test
    fun blankActiveIdFallsBackToTheImplicitAccount() {
        assertEquals(DEFAULT_PROFILE_ID, newRowProfileId(""))
        assertEquals("a1", newRowProfileId("a1"))
    }

    @Test
    fun deletingOneAccountLeavesTheOthersIntact() {
        val remaining = rows.filterNot { it.profileId == "a1" }
        assertEquals(3, remaining.size)
        assertEquals(2, remaining.ownedBy(DEFAULT_PROFILE_ID) { it.profileId }.size)
        assertEquals(1, remaining.ownedBy("a2") { it.profileId }.size)
    }
}
