package com.example.alpha_ai.core.designsystem.component

/** A button runs its action only when it is enabled and not already busy (FR-012). */
fun buttonAcceptsClick(enabled: Boolean, busy: Boolean): Boolean = enabled && !busy
