package com.example.alpha_ai.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonRulesTest {
    @Test
    fun `only an enabled button that is not busy accepts a click`() {
        assertTrue(buttonAcceptsClick(enabled = true, busy = false))
        assertFalse(buttonAcceptsClick(enabled = true, busy = true))
        assertFalse(buttonAcceptsClick(enabled = false, busy = false))
        assertFalse(buttonAcceptsClick(enabled = false, busy = true))
    }
}

class LoadingVisibilityTest {
    @Test
    fun `work that ends before the delay never shows the indicator`() {
        listOf(0L, 100L, 300L, 399L, 1_000L).forEach { at ->
            assertFalse("at $at", loadingVisible(elapsedMs = at, finishedAtMs = 300))
        }
        assertEquals(0, loadingHoldMs(finishedAtMs = 300))
    }

    @Test
    fun `the indicator appears at the delay while work runs`() {
        assertFalse(loadingVisible(elapsedMs = 399, finishedAtMs = null))
        assertTrue(loadingVisible(elapsedMs = 400, finishedAtMs = null))
        assertTrue(loadingVisible(elapsedMs = 60_000, finishedAtMs = null))
    }

    @Test
    fun `once shown it stays the minimum time even if work ends`() {
        // Shown at 400 ms, work ends at 450 ms: visible until 900 ms.
        assertTrue(loadingVisible(elapsedMs = 600, finishedAtMs = 450))
        assertTrue(loadingVisible(elapsedMs = 899, finishedAtMs = 450))
        assertFalse(loadingVisible(elapsedMs = 900, finishedAtMs = 450))
        assertEquals(450, loadingHoldMs(finishedAtMs = 450))
    }

    @Test
    fun `long work hides as soon as it ends`() {
        assertTrue(loadingVisible(elapsedMs = 1_999, finishedAtMs = 2_000))
        assertFalse(loadingVisible(elapsedMs = 2_000, finishedAtMs = 2_000))
        assertEquals(0, loadingHoldMs(finishedAtMs = 2_000))
    }
}
