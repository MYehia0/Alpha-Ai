package com.example.alpha_ai.shell

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.SignInReason
import com.example.alpha_ai.core.domain.auth.StartDestination
import com.example.alpha_ai.core.domain.auth.usecase.ResolveStartDestinationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Whether the start destination is known yet; the splash screen stays while [Resolving]. */
sealed interface StartState {
    data object Resolving : StartState

    data class Resolved(val destination: StartDestination) : StartState
}

/**
 * Decides where the app starts, once per launch (contracts/app-entry.md). A valid `start_reason`
 * extra skips resolution. If local storage is slow, after [RESOLVE_TIMEOUT] the decision is made
 * from the sign-in state alone, so a signed-in user is never sent to sign-in (FR-002).
 */
@HiltViewModel
class StartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resolveStartDestination: ResolveStartDestinationUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<StartState>(StartState.Resolving)
    val state: StateFlow<StartState> = _state.asStateFlow()

    init {
        val reason = savedStateHandle.get<String>(EXTRA_START_REASON)
            ?.let { name -> SignInReason.entries.firstOrNull { it.name == name } }
        if (reason != null) {
            _state.value = StartState.Resolved(StartDestination.SignIn(reason))
        } else {
            viewModelScope.launch {
                val destination = withTimeoutOrNull(RESOLVE_TIMEOUT) { resolveStartDestination() }
                    ?: fallback()
                _state.value = StartState.Resolved(destination)
            }
        }
    }

    private fun fallback(): StartDestination {
        Timber.w("Start resolution timed out; deciding from the sign-in state")
        return if (authRepository.currentUser() != null) {
            StartDestination.Home
        } else {
            StartDestination.SignIn(reason = null)
        }
    }

    companion object {
        /** Intent extra naming a [SignInReason]; anything else is ignored. */
        const val EXTRA_START_REASON = "start_reason"

        val RESOLVE_TIMEOUT: Duration = 2.seconds
    }
}
