package com.example.alpha_ai.ui.tasks

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.FragmentTasksBinding

class TasksFragment : Fragment() {
    lateinit var binding:FragmentTasksBinding
    lateinit var adapter: TasksAdapter
    var onTaskListener: OnTaskListener? = null

    interface OnTaskListener {
        fun onTaskClick(task: Task)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTasksBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeAdapter()
    }

    private fun initializeAdapter(){
        adapter = TasksAdapter(Task.getTasksList())
        binding.tasksRecycler.adapter = adapter
        adapter.onItemClickListener = object : TasksAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, item: Task) {
                onTaskListener?.let {
                    it.onTaskClick(item)
                }
            }
        }
    }


}