package com.example.alpha_ai.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.ItemTasksBinding

class TasksAdapter(private val tasks:List<Task?>?=null):Adapter<TasksAdapter.TasksViewHolder>() {

    var onItemClickListener: OnItemClickListener? = null

    class TasksViewHolder(val binding:ItemTasksBinding) :ViewHolder(binding.root){
        fun bind (task: Task){
            binding.item = task
        }
    }

    interface OnItemClickListener{
        fun onItemClick(position: Int, item: Task)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TasksViewHolder {
        val binding:ItemTasksBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_tasks,parent,false)
        return TasksViewHolder(binding)
    }

    override fun getItemCount(): Int = tasks?.size?:0

    override fun onBindViewHolder(holder: TasksViewHolder, position: Int) {
        holder.bind(tasks?.get(position)!!)
        holder.binding.root.setOnClickListener{
            onItemClickListener?.onItemClick(position, tasks[position]!!)
        }
    }
}