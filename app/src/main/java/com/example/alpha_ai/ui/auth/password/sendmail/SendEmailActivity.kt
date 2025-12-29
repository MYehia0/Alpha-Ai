package com.example.alpha_ai.ui.auth.password.sendmail

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivitySendemailBinding
import com.example.alpha_ai.ui.auth.password.otp.OtpActivity

class SendEmailActivity : BaseActivity<ActivitySendemailBinding, SendEmailViewModel>(),
    SendEmailNavigator {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator = this
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_sendemail
    }

    override fun genViewModel(): SendEmailViewModel {
        return ViewModelProvider(this)[SendEmailViewModel::class.java]
    }

    override fun sendCodeAndGetOTP() {
        val intent = Intent(this, OtpActivity::class.java)
        startActivity(intent)
        finish()
    }

}