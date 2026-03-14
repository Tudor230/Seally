package com.example.seally.xp

import kotlin.math.sqrt

/**
 * Maps total XP -> (level, progress in current level, xp in current level, xp needed).
 *
 * This uses a simple quadratic curve so each level requires a bit more XP.
 */
object XpLeveling {
    /** Base XP required for level 1 -> 2 */
    private const val BASE = 100

    /** Additional growth factor */
    private const val GROWTH = 25

    fun levelState(totalXp: Int): XpLevelState {
        val safe = totalXp.coerceAtLeast(0)

        // XP required to reach level L (starting at level 1) is:
        // threshold(L) = BASE*(L-1) + GROWTH*(L-1)^2
        // Find L by solving quadratic for n = (L-1)
        val a = GROWTH.toDouble()
        val b = BASE.toDouble()
        val c = -safe.toDouble()

        val n = if (a == 0.0) {
            (safe / BASE).toDouble()
        } else {
            val disc = (b * b) - 4.0 * a * c
            val root = (-b + sqrt(disc.coerceAtLeast(0.0))) / (2.0 * a)
            root
        }

        val nInt = n.toInt().coerceAtLeast(0)
        var level = nInt + 1

        // Correct in case of rounding
        while (xpToReachLevel(level + 1) <= safe) level++
        while (level > 1 && xpToReachLevel(level) > safe) level--

        val levelStartXp = xpToReachLevel(level)
        val nextLevelXp = xpToReachLevel(level + 1)
        val xpIntoLevel = safe - levelStartXp
        val xpNeeded = (nextLevelXp - levelStartXp).coerceAtLeast(1)
        val progress = (xpIntoLevel.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f)

        return XpLevelState(
            level = level,
            totalXp = safe,
            xpIntoLevel = xpIntoLevel,
            xpNeededForNextLevel = xpNeeded,
            progress = progress,
        )
    }

    private fun xpToReachLevel(level: Int): Int {
        val l = level.coerceAtLeast(1)
        val n = (l - 1)
        return (BASE * n + GROWTH * n * n).coerceAtLeast(0)
    }
}

data class XpLevelState(
    val level: Int,
    val totalXp: Int,
    val xpIntoLevel: Int,
    val xpNeededForNextLevel: Int,
    val progress: Float,
)

