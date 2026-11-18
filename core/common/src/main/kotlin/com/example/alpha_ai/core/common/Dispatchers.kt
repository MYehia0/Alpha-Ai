package com.example.alpha_ai.core.common

import javax.inject.Qualifier

/** Marks the coroutine dispatcher for blocking input and output. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Marks the coroutine dispatcher for CPU-bound work such as encoding and validation. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Marks the coroutine scope that lives as long as the process: for work that must finish after
 * the screen that started it is gone (constitution III: a scope tied to a lifecycle).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
