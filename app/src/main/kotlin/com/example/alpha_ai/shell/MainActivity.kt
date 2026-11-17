package com.example.alpha_ai.shell

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity that hosts every Compose screen. It is an [AppCompatActivity] so spec 005
 * can switch the app language with `AppCompatDelegate.setApplicationLocales` on Android 8–12.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AlphaApp() }
    }
}
