package com.example.alpha_ai.core.base

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.core.common.SnackbarDuration
import com.example.alpha_ai.core.common.UiEvent
import com.example.alpha_ai.core.utils.extensions.showMaterial3Dialog
import com.example.alpha_ai.core.utils.extensions.showMaterial3Snackbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

abstract class BaseActivity<VB:ViewDataBinding,VM: BaseViewModel>:AppCompatActivity() {
    protected lateinit var binding: VB
    protected abstract val viewModel: VM

    protected abstract fun inflateBinding(): VB
    protected abstract fun handleNavigation(destination: NavigationDestination)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.lifecycleOwner = this
        observeUiEvents()
    }

    private fun observeUiEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    handleUiEvent(event)
                }
            }
        }
    }

    private fun handleUiEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ShowSnackbar -> {
                Log.d("BaseActivity", "ShowSnackbar event received: ${event.message}")
                showSnackbar(event)
//                binding.root.showMaterial3Snackbar(
//                    message = event.message,
//                    duration = when (event.duration) {
//                        SnackbarDuration.SHORT -> Snackbar.LENGTH_SHORT
//                        SnackbarDuration.LONG -> Snackbar.LENGTH_LONG
//                        SnackbarDuration.INDEFINITE -> Snackbar.LENGTH_INDEFINITE
//                    },
//                    action = event.action
//                )
            }

            is UiEvent.ShowDialog -> {
                Log.d("BaseActivity", "ShowDialog event received")
                showMaterial3Dialog(
                    title = event.title,
                    message = event.message,
                    positiveButton = event.positiveButton,
                    negativeButton = event.negativeButton,
                    dismissible = event.dismissible
                )
            }

            is UiEvent.Navigate -> {
                Log.d("BaseActivity", "Navigate event received: ${event.destination}")
                handleNavigation(event.destination)
            }
        }
    }

    private fun showSnackbar(event: UiEvent.ShowSnackbar) {
        val anchorView = getSnackbarAnchorView()
        Log.d("BaseActivity", "Anchor view: $anchorView")
        Log.d("BaseActivity", "Anchor view parent: ${anchorView.parent}")

        anchorView.showMaterial3Snackbar(
            message = event.message,
            duration = when (event.duration) {
                SnackbarDuration.SHORT -> Snackbar.LENGTH_SHORT
                SnackbarDuration.LONG -> Snackbar.LENGTH_LONG
                SnackbarDuration.INDEFINITE -> Snackbar.LENGTH_INDEFINITE
            },
            action = event.action
        )
        Log.d("BaseActivity", "Snackbar show() called")
    }

    protected open fun getSnackbarAnchorView(): View {
        return binding.root
    }

    protected fun navigateBack() {
        onBackPressedDispatcher.onBackPressed()
    }
}