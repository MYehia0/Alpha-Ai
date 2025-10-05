package com.example.alpha_ai.ui.history.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivityLoginBinding
import com.example.alpha_ai.databinding.ActivitySearchHistoryBinding
import com.example.alpha_ai.ui.history.History
import com.example.alpha_ai.ui.history.HistoryFragment
import com.example.alpha_ai.ui.history.NoHistoryFragment
import com.example.alpha_ai.ui.login.LoginNavigator
import com.example.alpha_ai.ui.login.LoginViewModel
import com.example.alpha_ai.ui.tasks.doc.DOCActivity
import com.example.alpha_ai.ui.tasks.er.ERActivity
import com.example.alpha_ai.ui.tasks.gec.GECActivity
import com.example.alpha_ai.ui.tasks.ocr.OCRActivity
import com.example.alpha_ai.ui.tasks.stt.STTActivity
import com.example.alpha_ai.ui.tasks.wr.WRActivity

class SearchHistoryActivity : BaseActivity<ActivitySearchHistoryBinding, SearchHistoryViewModel>(), SearchHistoryNavigator,
    HistoryFragment.OnHistoryListener {
//    val historyFragment = HistoryFragment()
    val noHistoryFragment = NoHistoryFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator=this
        viewModel.search.observe(this) {
            if (it.trim().isNullOrBlank()) {
                viewModel.navigator?.onEdit(it,false)
            } else {
                viewModel.navigator?.onEdit(it,true)
            }
        }
    }

    private fun showNoHistoryFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_history,noHistoryFragment)
            .commit()
    }

    private fun showHistoryFragment(search:String?) {
        val historyFragment = HistoryFragment.getInstance(search)
        historyFragment.onHistoryListener = this
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_history,historyFragment)
            .commit()
    }

    override fun genViewModel(): SearchHistoryViewModel {
        return ViewModelProvider(this).get(SearchHistoryViewModel::class.java)
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_search_history
    }

    override fun onHistoryClick(history: History) {
        when(history.taskType){
            "gec" -> {
                val intent = Intent(this,
                    GECActivity::class.java)
                startActivity(intent)
            }
            "stt" -> {
                val intent = Intent(this,
                    STTActivity::class.java)
                startActivity(intent)
            }
            "er" -> {
                val intent = Intent(this,
                    ERActivity::class.java)
                startActivity(intent)
            }
            "doc" -> {
                val intent = Intent(this,
                    DOCActivity::class.java)
                startActivity(intent)
            }
            "ocr" -> {
                val intent = Intent(this,
                    OCRActivity::class.java)
                startActivity(intent)
            }
            "wr" -> {
                val intent = Intent(this,
                    WRActivity::class.java)
                startActivity(intent)
            }
            else -> {
                null
            }
        }
    }

    override fun onEdit(search: String, flag: Boolean) {
        if (!flag){
            showNoHistoryFragment()
        }
        else {
            showHistoryFragment(search)
        }
    }

}

