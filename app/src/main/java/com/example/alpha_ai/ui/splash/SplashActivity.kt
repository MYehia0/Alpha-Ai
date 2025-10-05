package com.example.alpha_ai.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivitySplashBinding
import com.example.alpha_ai.ui.home.HomeActivity
import com.example.alpha_ai.ui.login.LoginActivity
import com.example.alpha_ai.ui.onBoarding.OnBoardingActivity


class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>(),SplashNavigator {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.navigator = this
        Log.e("checkUser","O")
        viewModel.checkUser()
//        Handler(Looper.getMainLooper()).postDelayed({
//            Log.e("BO","O")
//            val intent = Intent(this, OnBoardingActivity::class.java)
//            startActivity(intent)
//            finish()
//        },2000)
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_splash
    }

    override fun genViewModel(): SplashViewModel {
        return ViewModelProvider(this)[SplashViewModel::class.java]
    }

    override fun goToBoarding() {
        Log.e("BO","O")
        val intent = Intent(this, OnBoardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }


}