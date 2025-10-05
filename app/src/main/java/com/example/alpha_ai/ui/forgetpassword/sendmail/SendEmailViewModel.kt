package com.example.alpha_ai.ui.forgetpassword.sendmail

import androidx.databinding.ObservableField
import com.example.alpha_ai.base.BaseViewModel

class SendEmailViewModel: BaseViewModel<SendEmailNavigator>() {
    var email = ObservableField<String>()
    var emailError = ObservableField<String?>()

    fun send(){
        navigator?.sendCodeAndGetOTP()
    }
}