package com.example.alpha_ai

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("errorText")
fun bindErrorOnTextInput(textInputLayout:TextInputLayout,error: String?){
    textInputLayout.error = error
}

@BindingAdapter("textID")
fun bindTextByID(textView: TextView,textID: Int?){
    textView.setText(textID!!)
}

@BindingAdapter("imageID")
fun bindImageByID(imageView: ImageView,imageID: Int?){
    imageView.setImageResource(imageID!!)
}

@BindingAdapter("visible")
fun visible(view: View,visible: Boolean){
    view.visibility = if (visible) View.VISIBLE else View.GONE
}



