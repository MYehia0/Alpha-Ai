package com.example.alpha_ai.ui.auth.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.ActivityStartBinding
import com.example.alpha_ai.ui.main.home.HomeActivity

class StartActivity : AppCompatActivity() {
    lateinit var binding:ActivityStartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_start)
        binding.btnStart.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}