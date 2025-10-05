package com.example.alpha_ai.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.ActivityLoginBinding
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.ui.forgetpassword.sendmail.SendEmailActivity
import com.example.alpha_ai.ui.home.HomeActivity
import com.example.alpha_ai.ui.register.RegisterActivity

class LoginActivity : BaseActivity<ActivityLoginBinding, LoginViewModel>(),LoginNavigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator = this
        binding.content.forgetPassword.setOnClickListener {
            val intent = Intent(this, SendEmailActivity::class.java)
            startActivity(intent)
        }
        if(!viewModel.validateAllForm()){
            binding.content.cardBtnLogin.setCardBackgroundColor(getColor(R.color.blue100))
        }
        else{
            binding.content.cardBtnLogin.setCardBackgroundColor(getColor(R.color.blue))
        }
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_login
    }

    override fun genViewModel(): LoginViewModel {
        return ViewModelProvider(this).get(LoginViewModel::class.java)
    }

    override fun goToRegister() {
        binding.content.textCreateMyAccount.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun goToHome() {
        val intent = Intent(this, StartActivity::class.java)
        startActivity(intent)
        finish()
    }
}