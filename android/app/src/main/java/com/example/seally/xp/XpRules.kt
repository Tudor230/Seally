package com.example.seally.xp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

object XpRules {
    const val XP_PER_REP: Int = 2
    const val PLANK_SECONDS_PER_XP: Int = 2
    const val NUTRITION_DAILY_MAX_XP: Int = 30
    const val NUTRITION_DAILY_MIN_XP: Int = -30
    const val WATER_DAILY_MAX_XP: Int = 10
    const val WATER_DAILY_MIN_XP: Int = -10
}

object XpCalculators {
    fun exerciseXpForRepDelta(repDelta: Int): Int {
        return repDelta.coerceAtLeast(0) * XpRules.XP_PER_REP
    }

    fun totalPlankXpForSeconds(totalSeconds: Int): Int {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        return safeSeconds / XpRules.PLANK_SECONDS_PER_XP
    }

    fun nutritionDailyXp(
        calories: Int,
        protein: Int,
        carbs: Int,
        fats: Int,
        sugars: Int,
        fibers: Int,
    ): Int {
        fun pointsByStep(
            value: Int,
            lowMax: Float,
            step: Float,
            maxPoints: Int,
        ): Int {
            val safeValue = value.toFloat().coerceAtLeast(0f)
            if (safeValue <= lowMax) return 0
            val bucket = ((safeValue - lowMax) / step).toInt() + 1
            return bucket.coerceIn(1, maxPoints)
        }

        val caloriesPoints = pointsByStep(value = calories, lowMax = 80f, step = 80f, maxPoints = 10)
        val sugarPoints = pointsByStep(value = sugars, lowMax = 4f, step = 4f, maxPoints = 10)
        val fatPoints = pointsByStep(value = fats, lowMax = 3f, step = 3f, maxPoints = 10)
        val carbPoints = pointsByStep(value = carbs, lowMax = 10f, step = 10f, maxPoints = 10)

        val proteinPoints = when {
            protein <= 2 -> 0
            protein <= 4 -> 1
            protein <= 6 -> 2
            protein <= 8 -> 3
            protein <= 10 -> 4
            else -> 5
        }
        val fiberPoints = when {
            fibers <= 0 -> 0
            fibers <= 1 -> 1
            fibers <= 2 -> 2
            fibers <= 3 -> 3
            fibers <= 4 -> 4
            else -> 5
        }

        val negativeScore = caloriesPoints + sugarPoints + fatPoints + carbPoints
        val positiveScore = proteinPoints + fiberPoints
        val score = negativeScore - positiveScore

        val normalized = when {
            score <= 0 -> 1f
            score >= 40 -> -1f
            else -> 1f - ((score / 40f) * 2f)
        }
        val rawXp = normalized * XpRules.NUTRITION_DAILY_MAX_XP
        return rawXp.roundToInt().coerceIn(XpRules.NUTRITION_DAILY_MIN_XP, XpRules.NUTRITION_DAILY_MAX_XP)
    }

    fun waterDailyXp(consumedMl: Int, targetMl: Int): Int {
        if (targetMl <= 0) return 0
        val safeConsumed = consumedMl.coerceAtLeast(0)
        val relativeError = abs(safeConsumed - targetMl).toFloat() / targetMl.toFloat()
        val normalized = (2f * exp(-2f * relativeError) - 1f).coerceIn(-1f, 1f)
        val rawXp = normalized * XpRules.WATER_DAILY_MAX_XP
        return rawXp.roundToInt().coerceIn(XpRules.WATER_DAILY_MIN_XP, XpRules.WATER_DAILY_MAX_XP)
    }
}

