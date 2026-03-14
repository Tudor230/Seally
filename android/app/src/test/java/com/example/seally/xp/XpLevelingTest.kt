package com.example.seally.xp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XpLevelingTest {

    @Test
    fun levelState_hasValidProgressRange() {
        val state = XpLeveling.levelState(0)
        assertEquals(1, state.level)
        assertTrue(state.progress in 0f..1f)
        assertTrue(state.xpNeededForNextLevel > 0)
    }

    @Test
    fun increasingXp_increasesOrMaintainsLevel() {
        val s1 = XpLeveling.levelState(0)
        val s2 = XpLeveling.levelState(10_000)
        assertTrue(s2.level >= s1.level)
    }
}

