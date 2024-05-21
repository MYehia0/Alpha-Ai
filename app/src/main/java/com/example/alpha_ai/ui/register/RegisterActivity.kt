package com.example.alpha_ai.ui.register

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.ActivityRegisterBinding
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.ui.login.LoginActivity

class RegisterActivity : BaseActivity<ActivityRegisterBinding, RegisterViewModel>(),RegisterNavigator {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator = this
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_register
    }
    override fun genViewModel(): RegisterViewModel {
        return ViewModelProvider(this).get(RegisterViewModel::class.java)
    }

    override fun goToLogin() {
        binding.content.signIn.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}