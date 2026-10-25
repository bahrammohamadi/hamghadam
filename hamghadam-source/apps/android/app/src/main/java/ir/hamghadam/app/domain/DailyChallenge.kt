package ir.hamghadam.app.domain

/** One line of the purple "برنامهٔ تو" card on the home screen. */
data class ChallengeStep(
    val title: String,
    val unit: String,
    val done: Int,
    val target: Int,
) {
    val ratio: Float get() = if (target <= 0) 1f else (done.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val isComplete: Boolean get() = done >= target && target > 0
    val label: String get() = "${PersianText.number(done)} از ${PersianText.number(target)} $unit"
}

/**
 * A gentle, fully local "daily challenge": three small commitments derived from
 * what the user already planned. No points are stored, nothing is punished —
 * the card only mirrors today's plan (Todoist/TickTick style but calmer).
 */
data class DailyChallenge(
    val steps: List<ChallengeStep>,
    val greeting: String,
) {
    val percent: Int
        get() {
            if (steps.isEmpty()) return 0
            val sum = steps.sumOf { step -> step.ratio.toDouble() }
            return ((sum / steps.size) * 100).toInt().coerceIn(0, 100)
        }

    val isComplete: Boolean get() = steps.isNotEmpty() && steps.all { it.isComplete }

    val subtitle: String
        get() = when {
            steps.isEmpty() -> "امروز هنوز برنامه‌ای نساخته‌ای"
            isComplete -> "چالش امروز کامل شد؛ عالی بود"
            else -> "برنامهٔ تو برای امروز"
        }
}

object DailyChallengeCalculator {

    const val DEFAULT_FOCUS_GOAL_MINUTES = 50

    fun build(
        dueToday: Int,
        doneToday: Int,
        focusMinutesToday: Int,
        focusGoalMinutes: Int = DEFAULT_FOCUS_GOAL_MINUTES,
        habitsTotal: Int,
        habitsDoneToday: Int,
        greeting: String = "سلام",
    ): DailyChallenge {
        val taskTarget = dueToday.coerceIn(0, 3).coerceAtLeast(if (dueToday == 0) 0 else 1)
        val steps = buildList {
            if (taskTarget > 0) {
                add(
                    ChallengeStep(
                        title = "کارهای امروز",
                        unit = "کار",
                        done = doneToday.coerceAtMost(taskTarget),
                        target = taskTarget,
                    ),
                )
            }
            add(
                ChallengeStep(
                    title = "تمرکز",
                    unit = "دقیقه",
                    done = focusMinutesToday,
                    target = focusGoalMinutes.coerceAtLeast(5),
                ),
            )
            if (habitsTotal > 0) {
                val target = habitsTotal.coerceAtMost(2)
                add(
                    ChallengeStep(
                        title = "عادت‌ها",
                        unit = "عادت",
                        done = habitsDoneToday.coerceAtMost(target),
                        target = target,
                    ),
                )
            }
        }
        return DailyChallenge(steps = steps, greeting = greeting)
    }
}
