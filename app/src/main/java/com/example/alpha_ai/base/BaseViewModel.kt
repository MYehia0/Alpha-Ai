package com.example.alpha_ai.base

import androidx.lifecycle.ViewModel

open class BaseViewModel<N: BaseNavigator>:ViewModel() {
    var navigator: N?=null
    fun onBack(){
        navigator?.onBack()
    }
}