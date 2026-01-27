package com.example.alpha_ai.core.utils

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("errorText")
fun bindErrorOnTextInput(textInputLayout:TextInputLayout,error: String?){
    textInputLayout.error = error
}

@BindingAdapter("error")
fun TextInputLayout.setErrorMessage(errorMessage: String?) {
    error = errorMessage
    isErrorEnabled = !errorMessage.isNullOrEmpty()
}

@BindingAdapter("imageID")
fun bindImageByID(imageView: ImageView,imageID: Int?){
    imageView.setImageResource(imageID!!)
}


@BindingAdapter("visible")
fun visible(view: View,visible: Boolean){
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("isVisible")
fun View.setIsVisible(isVisible: Boolean) {
    visibility = if (isVisible) View.VISIBLE else View.GONE
}

@BindingAdapter("enabledWithAlpha")
fun View.setEnabledWithAlpha(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1.0f else 0.5f
}

