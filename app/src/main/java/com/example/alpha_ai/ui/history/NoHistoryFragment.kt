package com.example.alpha_ai.ui.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.FragmentHistoryBinding
import com.example.alpha_ai.databinding.FragmentNohistoryBinding
import com.example.alpha_ai.databinding.FragmentTasksBinding
import com.example.alpha_ai.ui.tasks.Task
import com.example.alpha_ai.ui.tasks.TasksAdapter

class NoHistoryFragment : Fragment() {
    lateinit var binding:FragmentNohistoryBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNohistoryBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}