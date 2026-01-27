package com.example.alpha_ai.ui.main.history.search

import androidx.lifecycle.MutableLiveData
import com.example.alpha_ai.core.base.BaseViewModel

class SearchHistoryViewModel: BaseViewModel() {
    var search = MutableLiveData<String>()

//    fun updateSearch(){
//        if(search.value?.trim().isNullOrBlank()){
//            navigator?.onEdit(false)
//        }
//        else {
//            navigator?.onEdit(true)
//        }
//    }
}