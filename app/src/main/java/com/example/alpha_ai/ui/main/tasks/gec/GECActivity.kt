package com.example.alpha_ai.ui.main.tasks.gec

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivityGecBinding

class GECActivity : BaseActivity<ActivityGecBinding, GECViewModel>(), GECNavigator {
    private lateinit var inputIntent: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator=this
        inputIntent = intent.getStringExtra("IN_CORRECT") ?: ""
        if(inputIntent != ""){
            viewModel.input.set(inputIntent)
            viewModel.submit()
        }
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_gec
    }

    override fun genViewModel(): GECViewModel {
        return ViewModelProvider(this)[GECViewModel::class.java]
    }
}