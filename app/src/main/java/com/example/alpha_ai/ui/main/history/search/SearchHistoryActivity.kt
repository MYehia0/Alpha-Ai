package com.example.alpha_ai.ui.main.history.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.databinding.ActivitySearchHistoryBinding
import com.example.alpha_ai.data.models.History
import com.example.alpha_ai.ui.auth.register.RegisterViewModel
import com.example.alpha_ai.ui.main.history.HistoryFragment
import com.example.alpha_ai.ui.main.history.NoHistoryFragment
import com.example.alpha_ai.ui.main.tasks.doc.DOCActivity
import com.example.alpha_ai.ui.main.tasks.er.ERActivity
import com.example.alpha_ai.ui.main.tasks.gec.GECActivity
import com.example.alpha_ai.ui.main.tasks.ocr.OCRActivity
import com.example.alpha_ai.ui.main.tasks.stt.STTActivity
import com.example.alpha_ai.ui.main.tasks.wr.WRActivity
import kotlin.getValue

class SearchHistoryActivity : BaseActivity<ActivitySearchHistoryBinding, SearchHistoryViewModel>(),
    HistoryFragment.OnHistoryListener {
    override val viewModel: SearchHistoryViewModel by viewModels()
    override fun inflateBinding(): ActivitySearchHistoryBinding {
        return DataBindingUtil.setContentView(this, R.layout.activity_search_history)
    }

    val noHistoryFragment = NoHistoryFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBinding()
        viewModel.search.observe(this) {
            if (it.trim().isNullOrBlank()) {
//                viewModel.navigator?.onEdit(it,false)
            } else {
//                viewModel.navigator?.onEdit(it,true)
            }
        }
    }

    private fun setupBinding() {
        binding.lifecycleOwner = this
        binding.vm = viewModel
    }

    override fun handleNavigation(destination: NavigationDestination) {
        when (destination) {
//            is NavigationDestination.SearchHistoryEdit -> {
//                onEdit(destination.search,destination.flag)
//            }
            else -> {}
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

//    override fun onEdit(search: String, flag: Boolean) {
//        if (!flag){
//            showNoHistoryFragment()
//        }
//        else {
//            showHistoryFragment(search)
//        }
//    }

}

