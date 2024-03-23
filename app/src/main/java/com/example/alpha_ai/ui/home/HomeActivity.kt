package com.example.alpha_ai.ui.home

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.ActivityHomeBinding
import com.example.alpha_ai.base.BaseActivity

class HomeActivity : BaseActivity<ActivityHomeBinding, HomeViewModel>(),HomeNavigator {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding.user = UserProvider.user
//        Log.e("user",binding.user?.uEmail.toString())
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator=this
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_home
    }


    override fun genViewModel(): HomeViewModel {
        return ViewModelProvider(this)[HomeViewModel::class.java]
    }

}