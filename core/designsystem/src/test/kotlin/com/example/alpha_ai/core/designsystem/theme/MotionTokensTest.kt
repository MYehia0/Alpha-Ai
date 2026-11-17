package com.example.alpha_ai.core.designsystem.theme

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTokensTest {

    @Test
    fun `remove animations turns every motion style into a snap`() {
        val motion = AlphaMotion(reducedMotion = true)
        assertTrue(motion.enter<Float>() is SnapSpec)
        assertTrue(motion.exit<Float>() is SnapSpec)
        assertTrue(motion.change<Float>() is SnapSpec)
    }

    @Test
    fun `normal motion uses the token durations`() {
        val motion = AlphaMotion(reducedMotion = false)
        assertEquals(250, (motion.enter<Float>() as TweenSpec).durationMillis)
        assertEquals(150, (motion.exit<Float>() as TweenSpec).durationMillis)
        assertEquals(250, (motion.change<Float>() as TweenSpec).durationMillis)
        assertEquals(motion.easingEmphasizedDecelerate, (motion.enter<Float>() as TweenSpec).easing)
    }

    @Test
    fun `animator scale zero means reduced motion`() {
        assertTrue(reducedMotionFrom(0f))
        assertFalse(reducedMotionFrom(1f))
        assertFalse(reducedMotionFrom(0.5f))
    }
}
