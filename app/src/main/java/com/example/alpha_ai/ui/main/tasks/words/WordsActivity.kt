package com.example.alpha_ai.ui.main.tasks.words

import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.databinding.ActivityWordsBinding
import com.example.alpha_ai.ui.auth.register.RegisterViewModel
import kotlin.getValue

class WordsActivity : BaseActivity<ActivityWordsBinding, WordsViewModel>() {
    override val viewModel: WordsViewModel by viewModels()
    override fun inflateBinding(): ActivityWordsBinding {
        return DataBindingUtil.setContentView(this,R.layout.activity_words)
    }
    override fun handleNavigation(destination: NavigationDestination) {

    }
    private lateinit var inputIntent: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        inputIntent = intent.getStringExtra("IN_CORRECT") ?: ""
        if(inputIntent != ""){
            viewModel.input.set(inputIntent)
            viewModel.submit()
        }
    }
}