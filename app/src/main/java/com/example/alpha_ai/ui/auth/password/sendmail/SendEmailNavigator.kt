package com.example.alpha_ai.ui.auth.password.sendmail

import com.example.alpha_ai.base.BaseNavigator

interface SendEmailNavigator: BaseNavigator {
    fun sendCodeAndGetOTP()
}