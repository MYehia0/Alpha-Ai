package com.example.alpha_ai.core.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class ActionCooldownTest {

    private val time = TestTimeSource()
    private val cooldown = ActionCooldown(time)

    @Test
    fun `free before anything happened`() {
        assertEquals(Duration.ZERO, cooldown.remaining(CooldownAction.PasswordReset))
    }

    @Test
    fun `counts down over the sixty second window`() {
        cooldown.start(CooldownAction.PasswordReset)
        assertEquals(60.seconds, cooldown.remaining(CooldownAction.PasswordReset))

        time += 59.seconds
        assertEquals(1.seconds, cooldown.remaining(CooldownAction.PasswordReset))

        time += 1.seconds
        assertEquals(Duration.ZERO, cooldown.remaining(CooldownAction.PasswordReset))
    }

    @Test
    fun `actions are independent`() {
        cooldown.start(CooldownAction.PasswordReset)
        assertEquals(Duration.ZERO, cooldown.remaining(CooldownAction.EmailConfirmation))
    }
}
