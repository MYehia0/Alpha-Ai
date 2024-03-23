package com.example.alpha_ai.ui.splash

import android.os.Handler
import android.os.Looper
import com.example.alpha_ai.base.BaseViewModel
import com.example.alpha_ai.database.FireStoreUtils
import com.example.alpha_ai.database.models.User
import com.example.alpha_ai.constants.UserProvider
import com.google.firebase.auth.FirebaseAuth

class SplashViewModel:BaseViewModel<SplashNavigator>() {
    private val auth = FirebaseAuth.getInstance()

    fun checkUser(){

        Handler(Looper.getMainLooper()).postDelayed({
            if(auth.currentUser==null){
                navigator?.goToLogin()
            }
            else{
                FireStoreUtils()
                    .getUserFromFireStore(auth.currentUser?.uid)
                    .addOnCompleteListener{
                        if(it.isSuccessful){
                            val user = User(
                                uid = it.result.data?.get("uid").toString(),
                                uName = it.result.data?.get("uname").toString(),
                                uEmail = it.result.data?.get("uemail").toString()
                            )
//                            val user = it.result.toObject(User::class.java)
                            UserProvider.user = user
                            navigator?.goToHome()
                        }else{
                            navigator?.goToLogin()
                            navigator?.showMessage(it.exception?.localizedMessage!!,"")
                        }
                    }
            }
        },2000)
    }


}