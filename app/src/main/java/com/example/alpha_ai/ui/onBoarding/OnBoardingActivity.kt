package com.example.alpha_ai.ui.onBoarding

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivityOnBoardingBinding
import com.example.alpha_ai.ui.auth.login.LoginActivity
import com.example.alpha_ai.ui.auth.register.RegisterActivity

class OnBoardingActivity : BaseActivity<ActivityOnBoardingBinding, OnBoardingViewModel>(),OnBoardingNavigator {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        viewModel.navigator = this
        binding.btnSignUp.setOnClickListener{
            Log.e("reO","O")
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        binding.btnSignIn.setOnClickListener{
            Log.e("lgO","O")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_on_boarding
    }

    override fun genViewModel(): OnBoardingViewModel {
        return ViewModelProvider(this)[OnBoardingViewModel::class.java]
    }

    override fun goToLogin() {

//        val intent = Intent(this, LoginActivity::class.java)
//        startActivity(intent)
        binding.btnSignIn.setOnClickListener{
            Log.e("lgO","O")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun goToRegister() {
//        val intent = Intent(this, RegisterActivity::class.java)
//        startActivity(intent)
        binding.btnSignUp.setOnClickListener{
            Log.e("reO","O")
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("rO","O")
    }

    override fun onStart() {
        super.onStart()
        Log.e("sO","O")
    }
}