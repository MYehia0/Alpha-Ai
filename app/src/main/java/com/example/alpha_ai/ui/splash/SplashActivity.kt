package com.example.alpha_ai.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.databinding.ActivitySplashBinding
import com.example.alpha_ai.ui.main.home.HomeActivity
import com.example.alpha_ai.ui.onBoarding.OnBoardingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>() {
    override val viewModel: SplashViewModel by viewModels()
    override fun inflateBinding(): ActivitySplashBinding {
        return DataBindingUtil.setContentView(this,R.layout.activity_splash)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun handleNavigation(destination: NavigationDestination) {
        when(destination){
            is NavigationDestination.OnBoarding -> {
                val intent = Intent(this, OnBoardingActivity::class.java)
                startActivity(intent)
                finish()
            }
            is NavigationDestination.Home -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
            else -> {}
        }
    }
}