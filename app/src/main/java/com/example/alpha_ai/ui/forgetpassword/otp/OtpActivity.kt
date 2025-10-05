package com.example.alpha_ai.ui.forgetpassword.otp

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivityOtpBinding
import com.example.alpha_ai.databinding.ActivitySendemailBinding
import com.example.alpha_ai.ui.forgetpassword.newpassward.NewPasswordActivity
import com.example.alpha_ai.ui.login.StartActivity

class OtpActivity : BaseActivity<ActivityOtpBinding, OtpViewModel>(),OtpNavigator {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator = this
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_otp
    }

    override fun genViewModel(): OtpViewModel {
        return ViewModelProvider(this)[OtpViewModel::class.java]
    }

    override fun confirm() {
        val intent = Intent(this, NewPasswordActivity::class.java)
        startActivity(intent)
        finish()
    }

}