package com.example.alpha_ai.ui.history.search

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.example.alpha_ai.base.BaseViewModel
import com.example.alpha_ai.database.FireStoreUtils
import com.example.alpha_ai.ui.history.History
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SearchHistoryViewModel: BaseViewModel<SearchHistoryNavigator>() {
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