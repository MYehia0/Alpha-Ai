package com.example.alpha_ai.ui.main.history

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.alpha_ai.core.common.UserProvider
import com.example.alpha_ai.data.models.History
import com.example.alpha_ai.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {
    companion object {
        fun getInstance(search: String?): HistoryFragment {
            val categoryDetailsFragment = HistoryFragment()
            categoryDetailsFragment.search = search
            return categoryDetailsFragment
        }
    }

    var search: String? = null
    private  var historyList: MutableList<History>? = null
    lateinit var binding:FragmentHistoryBinding
    lateinit var adapter: HistoryAdapter
    var onHistoryListener: OnHistoryListener? = null

    interface OnHistoryListener {
        fun onHistoryClick(history: History)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHistoryBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeAdapter()
    }
    private fun initializeAdapter() {
//        loadHistories()
        historyList = History.getHistoryList()
        search?.let { historyList = searchInHistoryList(it) }
        adapter = HistoryAdapter(historyList)
        binding.historiesRecycler.adapter = adapter
        adapter.onItemClickListener = object : HistoryAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, item: History) {
                onHistoryListener?.let {
                    it.onHistoryClick(item)
                }
            }
        }
    }
    var hisList:MutableList<History>?=null
    private fun loadHistories(){
//        FireStoreUtils()
//            .getHistory(UserProvider.user?.uid)
//            .addOnCompleteListener { task->
//                if(task.isSuccessful){
//                    Log.e("user", UserProvider.user?.uid.toString())
//                    var cnt = 0
//                    task.result.documents.forEach{
//                        val history = History(
//                            id = it.data?.get("id").toString(),
//                            uid = it.data?.get("uid").toString(),
//                            taskType = it.data?.get("taskType").toString(),
//                            input = it.data?.get("input").toString(),
//                            response = it.data?.get("response").toString()
//                        )
////                        hisList?.add(history)
//                        hisList?.set(cnt,history)
//                        cnt += 1
//                        Log.e("his",history.toString())
//                    }
////                    hisList?.let { historyList?.addAll(it) }
//                    Log.e("his",hisList.toString())
//                }
//            }
    }

    fun searchInHistoryList(query: String): MutableList<History>? {
        return historyList?.filter { it.response?.contains(query, ignoreCase = true) == true }?.toMutableList()
    }

}