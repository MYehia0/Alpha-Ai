package com.example.alpha_ai.ui.onBoarding

import com.example.alpha_ai.core.base.BaseViewModel
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.core.common.UiEvent

class OnBoardingViewModel: BaseViewModel() {
    fun navigateToLogin() {
        sendEvent(UiEvent.Navigate(NavigationDestination.Login))
    }
    fun navigateToRegister() {
        sendEvent(UiEvent.Navigate(NavigationDestination.Register))
    }
}