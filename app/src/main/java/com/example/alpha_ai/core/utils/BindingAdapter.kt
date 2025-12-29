package com.example.alpha_ai.core.utils

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
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

@BindingAdapter("imageBID")
fun bindImagebByID(imageButton: ImageButton,imageID: Int?){
    imageButton.setImageResource(imageID!!)
}

@BindingAdapter("visible")
fun visible(view: View,visible: Boolean){
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("backgroundCard")
fun setBackgroundColor(view: CardView, color: Int) {
    view.setCardBackgroundColor(color)
}



