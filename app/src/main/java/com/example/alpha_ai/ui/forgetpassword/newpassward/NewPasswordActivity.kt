package com.example.alpha_ai.ui.forgetpassword.newpassward

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivityNewpassBinding
import com.example.alpha_ai.databinding.ActivitySendemailBinding
import com.example.alpha_ai.ui.forgetpassword.otp.OtpActivity
import com.example.alpha_ai.ui.login.LoginActivity
import com.example.alpha_ai.ui.login.StartActivity

class NewPasswordActivity : BaseActivity<ActivityNewpassBinding, NewPasswordViewModel>(),NewPasswordNavigator {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator = this
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_newpass
    }

    override fun genViewModel(): NewPasswordViewModel {
        return ViewModelProvider(this)[NewPasswordViewModel::class.java]
    }

    override fun sucessGoToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

}