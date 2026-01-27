package com.example.alpha_ai.ui.auth.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.databinding.ActivityLoginBinding
import com.example.alpha_ai.core.common.UiState
import com.example.alpha_ai.ui.auth.register.RegisterActivity
import com.example.alpha_ai.ui.main.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding, LoginViewModel>() {

    override val viewModel: LoginViewModel by viewModels()
    override fun inflateBinding(): ActivityLoginBinding {
        return DataBindingUtil.setContentView(this,R.layout.activity_login)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBinding()
        observeAuthState()
    }

    private fun setupBinding() {
         binding.lifecycleOwner = this
         binding.vm = viewModel
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    handleAuthState(state)
                }
            }
        }
    }

    private fun handleAuthState(state: UiState<String>) {
        when (state) {
            is UiState.Idle -> {}
            is UiState.Loading -> {}
            is UiState.Success -> {}
            is UiState.Error -> {}
        }
    }

    override fun handleNavigation(destination: NavigationDestination) {
        when (destination) {
            is NavigationDestination.Home -> {
                startActivity(Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            is NavigationDestination.Register -> {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
            is NavigationDestination.Back -> {
                navigateBack()
            }
            else -> {}
        }
    }
}