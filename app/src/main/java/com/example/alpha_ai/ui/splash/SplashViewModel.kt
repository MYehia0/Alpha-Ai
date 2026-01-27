package com.example.alpha_ai.ui.splash

import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.core.base.BaseViewModel
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.core.common.UiEvent
import com.example.alpha_ai.core.common.UserProvider
import com.example.alpha_ai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    companion object {
        private const val SPLASH_DELAY_MS = 2000L
    }

    init {
        checkAuthentication()
    }

    /**
     * Decision tree:
     * 1. Check if user already authenticated in Firebase
     * 2. If yes → Load user data and go to Home
     * 3. If no → Check if "Remember Me" enabled
     * 4. If yes → Attempt auto-login
     * 5. If no or failed → Go to Onboarding
     */
    private fun checkAuthentication() {
        viewModelScope.launch {
            // Show splash screen for minimum duration
            delay(SPLASH_DELAY_MS)

            when {
                authRepository.isUserLoggedIn() -> {
                    handleAuthenticatedUser()
                }
                authRepository.isRememberMeEnabled() -> {
                    handleRememberedUser()
                }
                else -> {
                    sendEvent(UiEvent.Navigate(NavigationDestination.OnBoarding))
                }
            }
        }
    }

    private suspend fun handleAuthenticatedUser() {
        val result = authRepository.getCurrentUserData(forceRefresh = false)

        result.fold(
            onSuccess = { user ->
                UserProvider.user = user
                sendEvent(UiEvent.Navigate(NavigationDestination.Home))
            },
            onFailure = {
                authRepository.signOut()
                sendEvent(UiEvent.Navigate(NavigationDestination.OnBoarding))
            }
        )
    }

    private suspend fun handleRememberedUser() {
        val result = authRepository.attemptAutoLogin()

        if (result != null) {
            result.fold(
                onSuccess = { user ->
                    UserProvider.user = user
                    sendEvent(UiEvent.Navigate(NavigationDestination.Home))
                },
                onFailure = {
                    sendEvent(UiEvent.Navigate(NavigationDestination.OnBoarding))
                }
            )
        } else {
            sendEvent(UiEvent.Navigate(NavigationDestination.OnBoarding))
        }
    }
}