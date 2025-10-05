package com.example.alpha_ai.ui.tasks.wr

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.Observable
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivityGecBinding
import com.example.alpha_ai.databinding.ActivityWrBinding

class WRActivity : BaseActivity<ActivityWrBinding, WRViewModel>(),WRNavigator {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator=this
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.generatedBitmap.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                binding.content.imageView.setImageBitmap(viewModel.generatedBitmap.get())
            }
        })
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_wr
    }

    override fun genViewModel(): WRViewModel {
        return ViewModelProvider(this)[WRViewModel::class.java]
    }
}