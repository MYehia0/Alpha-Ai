package com.example.alpha_ai.core.network

/** Answers whether the device currently has a usable internet connection. */
interface ConnectivityChecker {
    fun isOnline(): Boolean
}
