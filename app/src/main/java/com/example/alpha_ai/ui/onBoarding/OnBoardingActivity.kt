package com.example.alpha_ai.ui.onBoarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.databinding.ActivityOnBoardingBinding
import com.example.alpha_ai.ui.auth.login.LoginActivity
import com.example.alpha_ai.ui.auth.register.RegisterActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class OnBoardingActivity : BaseActivity<ActivityOnBoardingBinding, OnBoardingViewModel>() {
    override val viewModel: OnBoardingViewModel by viewModels()
    override fun inflateBinding(): ActivityOnBoardingBinding {
        return DataBindingUtil.setContentView(this, R.layout.activity_on_boarding)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBinding()
    }

    private fun setupBinding() {
        binding.lifecycleOwner = this
        binding.vm = viewModel
    }

    override fun handleNavigation(destination: NavigationDestination) {
        when(destination){
            is NavigationDestination.Login -> {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
            is NavigationDestination.Register -> {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            }
            else -> {}
        }
    }
}