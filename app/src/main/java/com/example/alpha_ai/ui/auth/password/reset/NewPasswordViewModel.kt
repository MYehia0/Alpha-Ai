package com.example.alpha_ai.ui.auth.password.reset

import androidx.databinding.ObservableField
import com.example.alpha_ai.base.BaseViewModel

class NewPasswordViewModel: BaseViewModel<NewPasswordNavigator>() {
    var password = ObservableField<String>()
    var passwordConform = ObservableField<String>()
    var passwordError = ObservableField<String?>()
    var passwordConformError = ObservableField<String?>()

    fun continue_pass(){
        if(!validateForm()){
            return
        }
        navigator?.sucessGoToLogin()
    }

    private var isValid:Boolean = true
    private fun validateForm(): Boolean {
        if(password.get().isNullOrBlank()){
            isValid = false
            passwordError.set("please enter password.")
        }
        else{
            isValid = true
            passwordError.set(null)
        }
        if(passwordConform.get().isNullOrBlank()){
            isValid = false
            passwordConformError.set("please enter password confirm.")
        }
        else if(!password.get().equals(passwordConform.get())){
            isValid = false
            passwordConformError.set("doesn't match")
        }
        else{
            isValid = true
            passwordConformError.set(null)
        }
        return isValid
    }

}