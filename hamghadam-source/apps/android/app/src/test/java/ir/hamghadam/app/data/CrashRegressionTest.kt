package ir.hamghadam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for crash audit 0.6.2 (docs/10-crash-audit.fa.md).
 *
 * C-01: the profile list is empty for the implicit first account, and three
 * screens used `profiles.first()` → `NoSuchElementException`.
 * C-07: a corrupt stored minute-of-day reached `LocalTime.of` and threw.
 */
class CrashRegressionTest {

    private val implicitAccount = LocalProfile(id = DEFAULT_PROFILE_ID, name = "حساب من", emoji = "🌿")

    @Test
    fun emptyProfileListFallsBackToTheImplicitAccount() {
        val resolved = ProfileResolver.activeProfile(emptyList(), DEFAULT_PROFILE_ID, implicitAccount)
        assertEquals(implicitAccount, resolved)
    }

    @Test
    fun unknownActiveIdFallsBackInsteadOfThrowing() {
        val other = LocalProfile(id = "p2", name = "کار")
        val resolved = ProfileResolver.activeProfile(listOf(other), "deleted-id", implicitAccount)
        assertEquals(implicitAccount, resolved)
    }

    @Test
    fun matchingActiveIdSelectsThatAccount() {
        val other = LocalProfile(id = "p2", name = "کار")
        val resolved = ProfileResolver.activeProfile(listOf(other), "p2", implicitAccount)
        assertEquals(other, resolved)
    }

    @Test
    fun invalidStoredMinuteDoesNotCrashTheList() {
        val task = TaskEntity(id = "t", title = "خرید", dueMinuteOfDay = 99_999)
        assertNull(task.dueTime)
    }

    @Test
    fun validStoredMinuteIsKept() {
        val task = TaskEntity(id = "t", title = "خرید", dueMinuteOfDay = 9 * 60 + 30)
        assertEquals(9, task.dueTime?.hour)
        assertEquals(30, task.dueTime?.minute)
    }

    @Test
    fun absurdEpochDayDoesNotCrashDateRendering() {
        val task = TaskEntity(id = "t", title = "خرید", dueEpochDay = Long.MIN_VALUE / 2)
        // The getter must swallow the conversion error instead of taking the UI down.
        assertTrue(runCatching { task.dueDate }.isSuccess)
    }
}
