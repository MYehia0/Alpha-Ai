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
