package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeTest {

    @Test
    fun `challenge mixes tasks, focus and habits`() {
        val challenge = DailyChallengeCalculator.build(
            dueToday = 5,
            doneToday = 2,
            focusMinutesToday = 25,
            focusGoalMinutes = 50,
            habitsTotal = 3,
            habitsDoneToday = 1,
        )
        assertEquals(3, challenge.steps.size)
        assertEquals("کارهای امروز", challenge.steps[0].title)
        assertEquals(3, challenge.steps[0].target)
        assertEquals(2, challenge.steps[0].done)
        assertFalse(challenge.steps[0].isComplete)
        assertFalse(challenge.isComplete)
        assertTrue(challenge.percent in 0..100)
        assertEquals("برنامهٔ تو برای امروز", challenge.subtitle)
    }

    @Test
    fun `focus step can be larger than the goal`() {
        val challenge = DailyChallengeCalculator.build(
            dueToday = 0,
            doneToday = 0,
            focusMinutesToday = 120,
            focusGoalMinutes = 50,
            habitsTotal = 0,
            habitsDoneToday = 0,
        )
        // No tasks and no habits leaves only the focus commitment.
        assertEquals(1, challenge.steps.size)
        assertTrue(challenge.steps[0].isComplete)
        assertEquals(100, challenge.percent)
        assertTrue(challenge.subtitle.contains("کامل"))
    }

    @Test
    fun `empty day is not a failure`() {
        val challenge = DailyChallengeCalculator.build(
            dueToday = 0,
            doneToday = 0,
            focusMinutesToday = 0,
            habitsTotal = 0,
            habitsDoneToday = 0,
        )
        assertEquals(0, challenge.percent)
        assertFalse(challenge.isComplete)
        assertEquals("امروز هنوز برنامه‌ای نساخته‌ای", challenge.subtitle)
    }

    @Test
    fun `completed task step is capped at its target`() {
        val step = ChallengeStep(title = "کار", unit = "کار", done = 7, target = 3)
        assertTrue(step.isComplete)
        assertEquals(1f, step.ratio)
        assertEquals("۳ از ۳ کار", step.label)
    }

    @Test
    fun `zero target never divides by zero`() {
        val step = ChallengeStep(title = "عادت", unit = "عادت", done = 0, target = 0)
        assertEquals(1f, step.ratio)
        assertFalse(step.isComplete)
    }
}
