package com.example.alpha_ai.ui.history.search

import com.example.alpha_ai.base.BaseNavigator

interface SearchHistoryNavigator: BaseNavigator {
    fun onEdit(search: String,flag:Boolean){}
}