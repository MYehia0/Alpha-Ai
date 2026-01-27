package com.example.alpha_ai.ui.main.tasks.gec

import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.data.remote.api.ApiManager
import com.example.alpha_ai.databinding.ActivityGecBinding
import com.example.alpha_ai.ui.auth.register.RegisterViewModel
import kotlin.getValue

class GECActivity : BaseActivity<ActivityGecBinding, GECViewModel>() {
    override val viewModel: GECViewModel by viewModels {
        val apiManager = ApiManager.getInstance(applicationContext)
        GECViewModelFactory(apiManager)
    }
    override fun inflateBinding(): ActivityGecBinding {
        return DataBindingUtil.setContentView(this,R.layout.activity_gec)
    }

    override fun handleNavigation(destination: NavigationDestination) {

    }
    private lateinit var inputIntent: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        inputIntent = intent.getStringExtra("IN_CORRECT") ?: ""
        if(inputIntent != ""){
            viewModel.input.set(inputIntent)
            viewModel.submit()
        }
    }
}

