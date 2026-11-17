package com.example.alpha_ai.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {

    @Test
    fun `every pair in the light theme meets WCAG AA`() = assertAllPass(LightColors.let(::contrastPairs), "light")

    @Test
    fun `every pair in the dark theme meets WCAG AA`() = assertAllPass(DarkColors.let(::contrastPairs), "dark")

    @Test
    fun `white text on the primary brand blue passes`() {
        assertEquals(Color(0xFF1976D2), LightColors.primary)
        assertTrue(contrastRatio(Color.White, LightColors.primary) >= MIN_TEXT_CONTRAST)
    }

    @Test
    fun `the brand accent is never used for text`() {
        // #2196F3 measures about 3.1:1 on white, so it must stay decorative (FR-001).
        assertTrue(contrastRatio(BrandAccent, Color.White) < MIN_TEXT_CONTRAST)
        val pairs = contrastPairs(LightColors) + contrastPairs(DarkColors)
        assertTrue(pairs.none { it.foreground == BrandAccent || it.background == BrandAccent })
    }

    @Test
    fun `contrast ratio matches known values`() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(Color.White, Color.White), 0.001)
    }

    private fun assertAllPass(pairs: List<ContrastPair>, theme: String) {
        val failures = pairs
            .map { it to contrastRatio(it.foreground, it.background) }
            .filter { (pair, ratio) -> ratio < pair.minimum }
            .map { (pair, ratio) -> "${pair.name}: %.2f < %.1f".format(ratio, pair.minimum) }
        assertTrue("Contrast failures in $theme theme:\n${failures.joinToString("\n")}", failures.isEmpty())
    }
}
