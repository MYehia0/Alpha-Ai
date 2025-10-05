package com.example.alpha_ai.ui.history

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.alpha_ai.R
import com.example.alpha_ai.databinding.ItemHistoryBinding
import com.example.alpha_ai.databinding.ItemTasksBinding
import com.example.alpha_ai.ui.tasks.TasksAdapter
import com.zerobranch.layout.SwipeLayout

class HistoryAdapter(private var histories:List<History?>?=null):Adapter<HistoryAdapter.HistoryViewHolder>() {

    var onItemClickListener: OnItemClickListener? = null

    class HistoryViewHolder(val binding:ItemHistoryBinding) :ViewHolder(binding.root){
        fun bind (history: History){
            binding.item = history
            binding.imageId = if (history.taskType.equals("gec")){
                R.drawable.gec
            }
            else {
                R.drawable.gec
            }
        }
    }

    interface OnItemClickListener{
        fun onItemClick(position: Int, item: History)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding:ItemHistoryBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_history,parent,false)
        return HistoryViewHolder(binding)
    }

    override fun getItemCount(): Int = histories?.size?:0

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(histories?.get(position)!!)
        holder.binding.dragItem.setOnClickListener {
            Log.e("dr","$position")
            onItemClickListener?.onItemClick(position, histories!![position]!!)
        }
        holder.binding.deleteTask.setOnClickListener{
//                        onDeleteListener.onDelete(position, items?.get(position))
            Log.e("de","$position")
            holder.binding.swipeLayout.close()
        }
//        holder.binding.swipeLayout.setOnActionsListener(object : SwipeLayout.SwipeActionsListener{
//            override fun onOpen(direction: Int, isContinuous: Boolean) {
//                if (direction == SwipeLayout.RIGHT) {
//                    // was executed swipe to the right
//
//                } else if (direction == SwipeLayout.LEFT) {
//                    // was executed swipe to the left
//                    holder.binding.swipeLayout.close()
//                }
//            }
//
//            override fun onClose() {
//
//            }
//        })
    }
    fun updateData(newData: List<History?>) {
        histories = newData
        notifyDataSetChanged()
    }
}