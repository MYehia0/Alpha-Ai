package com.example.alpha_ai.ui.main.history.search

import com.example.alpha_ai.base.BaseNavigator

interface SearchHistoryNavigator: BaseNavigator {
    fun onEdit(search: String,flag:Boolean){}
}