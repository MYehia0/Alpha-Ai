package com.example.alpha_ai.ui.forgetpassword.otp

import androidx.databinding.ObservableField
import com.example.alpha_ai.base.BaseViewModel

class OtpViewModel: BaseViewModel<OtpNavigator>() {
    var email = ObservableField<String>()
    var emailError = ObservableField<String?>()

    fun confirm(){
        navigator?.confirm()
    }
}