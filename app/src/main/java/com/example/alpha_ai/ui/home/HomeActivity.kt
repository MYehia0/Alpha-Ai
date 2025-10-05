package com.example.alpha_ai.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.ActivityHomeBinding
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.ui.history.History
import com.example.alpha_ai.ui.history.HistoryFragment
import com.example.alpha_ai.ui.tasks.Task
import com.example.alpha_ai.ui.tasks.TasksFragment
import com.example.alpha_ai.ui.tasks.doc.DOCActivity
import com.example.alpha_ai.ui.tasks.er.ERActivity
import com.example.alpha_ai.ui.tasks.gec.GECActivity
import com.example.alpha_ai.ui.tasks.ocr.OCRActivity
import com.example.alpha_ai.ui.tasks.stt.STTActivity
import com.example.alpha_ai.ui.tasks.wr.WRActivity

class HomeActivity : BaseActivity<ActivityHomeBinding, HomeViewModel>(),HomeNavigator, TasksFragment.OnTaskListener ,
    HistoryFragment.OnHistoryListener {
    val tasksFragment = TasksFragment()
    val historyFragment = HistoryFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding.user = UserProvider.user
//        Log.e("user",binding.user?.uEmail.toString())
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator=this
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

    override fun getLayoutID(): Int {
        return R.layout.activity_home
    }


    override fun genViewModel(): HomeViewModel {
        return ViewModelProvider(this)[HomeViewModel::class.java]
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