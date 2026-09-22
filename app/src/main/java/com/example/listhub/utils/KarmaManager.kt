package com.example.listhub.utils

/**
 * Basic gamification logic from section 3.8 of the planning doc.
 * We simplified the "5-day streak = 2x multiplier" part for the prototype since
 * tracking streaks properly would need a bit more data modelling (a lastCompletedDate
 * + streakCount on the User table). We left a TODO for that - it's noted in the
 * final report as a feature we deferred to the full POE like the brief allows.
 */
object KarmaManager {

    private const val POINTS_PER_TASK = 10
    private const val POINTS_PER_LEVEL = 100

    fun pointsForCompletingTask(): Int = POINTS_PER_TASK

    // TODO: implement the 5-day streak 2x multiplier for the final POE submission

    fun calculateLevel(totalPoints: Int): Int {
        // level 1 starts at 0 points, level 2 at 100, level 3 at 200, etc.
        return (totalPoints / POINTS_PER_LEVEL) + 1
    }

    fun pointsIntoCurrentLevel(totalPoints: Int): Int {
        return totalPoints % POINTS_PER_LEVEL
    }

    fun pointsNeededForNextLevel(): Int = POINTS_PER_LEVEL
}
