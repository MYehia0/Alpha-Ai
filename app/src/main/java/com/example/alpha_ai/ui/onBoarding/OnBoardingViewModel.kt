package com.example.alpha_ai.ui.onBoarding

import com.example.alpha_ai.base.BaseViewModel

class OnBoardingViewModel: BaseViewModel<OnBoardingNavigator>() {
    fun goToLogin(){
        navigator?.goToLogin()
    }
    fun goToRegister(){
        navigator?.goToRegister()
    }
}