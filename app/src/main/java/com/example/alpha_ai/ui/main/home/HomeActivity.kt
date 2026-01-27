package com.example.alpha_ai.ui.main.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.ActivityHomeBinding
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.data.models.History
import com.example.alpha_ai.ui.main.history.HistoryFragment
import com.example.alpha_ai.data.models.Task
import com.example.alpha_ai.ui.auth.register.RegisterViewModel
import com.example.alpha_ai.ui.main.tasks.TasksFragment
import com.example.alpha_ai.ui.main.tasks.doc.DOCActivity
import com.example.alpha_ai.ui.main.tasks.er.ERActivity
import com.example.alpha_ai.ui.main.tasks.gec.GECActivity
import com.example.alpha_ai.ui.main.tasks.ocr.OCRActivity
import com.example.alpha_ai.ui.main.tasks.stt.STTActivity
import com.example.alpha_ai.ui.main.tasks.wr.WRActivity
import kotlin.getValue

class HomeActivity : BaseActivity<ActivityHomeBinding, HomeViewModel>(), TasksFragment.OnTaskListener ,
    HistoryFragment.OnHistoryListener {
    override val viewModel: HomeViewModel by viewModels()
    override fun inflateBinding(): ActivityHomeBinding {
        return DataBindingUtil.setContentView(this, R.layout.activity_home)
    }

    override fun handleNavigation(destination: NavigationDestination) {

    }

    val tasksFragment = TasksFragment()
    val historyFragment = HistoryFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding.user = UserProvider.user
//        Log.e("user",binding.user?.uEmail.toString())
        binding.vm = viewModel
//        viewModel.navigator=this
//        initializeAdapter()
        binding.bottomNav
            .setOnItemSelectedListener{
                when (it.itemId) {
                    R.id.list1 -> {
                        binding.title = "Tasks"
                        binding.search = false
                        showTasksFragment()
                    }
                    R.id.history -> {
                        binding.title = "History"
                        binding.search = true
                        showHistoryFragment()
                    }
                }
                return@setOnItemSelectedListener true
            }
        binding.bottomNav.selectedItemId = R.id.list1
    }

    fun showTasksFragment(){
        tasksFragment.onTaskListener = this
        supportFragmentManager.beginTransaction()
            .replace(R.id.container,tasksFragment)
            .addToBackStack(null)
            .commit()
    }
    fun showHistoryFragment(){
        historyFragment.onHistoryListener = this
        supportFragmentManager.beginTransaction()
            .replace(R.id.container,historyFragment)
            .commit()
    }

    override fun onTaskClick(task: Task) {
        when(task.id){
            "gec" -> {
                val intent = Intent(this@HomeActivity,
                    GECActivity::class.java)
                startActivity(intent)
            }
            "stt" -> {
                val intent = Intent(this@HomeActivity,
                    STTActivity::class.java)
                startActivity(intent)
            }
            "er" -> {
                val intent = Intent(this@HomeActivity,
                    ERActivity::class.java)
                startActivity(intent)
            }
            "doc" -> {
                val intent = Intent(this@HomeActivity,
                    DOCActivity::class.java)
                startActivity(intent)
            }
            "ocr" -> {
                val intent = Intent(this@HomeActivity,
                    OCRActivity::class.java)
                startActivity(intent)
            }
            "wr" -> {
                val intent = Intent(this@HomeActivity,
                    WRActivity::class.java)
                startActivity(intent)
            }
            else -> {
                null
            }
        }

    }

    override fun onHistoryClick(history: History) {
        Log.e("dr","gec")
        when(history.taskType){
            "gec" -> {
                Log.e("dr","gec")
                val intent = Intent(this@HomeActivity,
                    GECActivity::class.java)
                startActivity(intent)
            }
            "stt" -> {
                Log.e("dr","stt")
                val intent = Intent(this@HomeActivity,
                    STTActivity::class.java)
                startActivity(intent)
            }
            "er" -> {
                Log.e("dr","er")
                val intent = Intent(this@HomeActivity,
                    ERActivity::class.java)
                startActivity(intent)
            }
            "doc" -> {
                Log.e("dr","doc")
                val intent = Intent(this@HomeActivity,
                    DOCActivity::class.java)
                startActivity(intent)
            }
            "ocr" -> {
                Log.e("dr","ocr")
                val intent = Intent(this@HomeActivity,
                    OCRActivity::class.java)
                startActivity(intent)
            }
            "wr" -> {
                Log.e("dr","wr")
                val intent = Intent(this@HomeActivity,
                    WRActivity::class.java)
                startActivity(intent)
            }
            else -> {
                null
            }
        }
    }

}