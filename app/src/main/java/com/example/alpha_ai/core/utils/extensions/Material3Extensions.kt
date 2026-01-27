package com.example.alpha_ai.core.utils.extensions

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import com.example.alpha_ai.core.common.DialogAction
import com.example.alpha_ai.core.common.SnackbarAction
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

fun View.showMaterial3Snackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    action: SnackbarAction? = null
) {
    Snackbar.make(this, message, duration).apply {
        action?.let {
            setAction(it.label) { _ -> it.action() }
        }
        view.setBackgroundColor(
            context.getColorFromAttr(R.attr.colorSurfaceInverse)
        )
        setTextColor(
            context.getColorFromAttr(R.attr.colorOnSurfaceInverse)
        )
        setActionTextColor(
            context.getColorFromAttr(R.attr.colorPrimaryInverse)
        )
    }.show()
}

fun AppCompatActivity.showMaterial3Dialog(
    title: String,
    message: String,
    positiveButton: DialogAction? = null,
    negativeButton: DialogAction? = null,
    dismissible: Boolean = true
) {
    MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(dismissible)
        .apply {
            positiveButton?.let {
                setPositiveButton(it.label) { dialog, _ ->
                    it.action()
                    dialog.dismiss()
                }
            }
            negativeButton?.let {
                setNegativeButton(it.label) { dialog, _ ->
                    it.action()
                    dialog.dismiss()
                }
            }
        }
        .show()
}

private fun Context.getColorFromAttr(@AttrRes attrColor: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrColor, typedValue, true)
    return typedValue.data
}