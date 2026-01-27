package com.example.alpha_ai.ui.main.tasks.wr

import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.databinding.ActivityWrBinding
import com.example.alpha_ai.ui.auth.register.RegisterViewModel
import kotlin.getValue

class WRActivity : BaseActivity<ActivityWrBinding, WRViewModel>() {
    override val viewModel: WRViewModel by viewModels()
    override fun inflateBinding(): ActivityWrBinding {
        return DataBindingUtil.setContentView(this,R.layout.activity_wr)
    }
    override fun handleNavigation(destination: NavigationDestination) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.generatedBitmap.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                binding.content.imageView.setImageBitmap(viewModel.generatedBitmap.get())
            }
        })
    }
}