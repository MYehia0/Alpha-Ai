package com.example.alpha_ai.base

interface BaseNavigator {
    fun showLoading(message:String)
    fun showMessage(messageOK: String , message: String)
    fun hideLoading()
    fun onBack()
}