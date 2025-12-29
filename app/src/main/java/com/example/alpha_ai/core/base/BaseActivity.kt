package com.example.alpha_ai.base

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.example.alpha_ai.ui.main.history.search.SearchHistoryActivity

abstract class BaseActivity<VB:ViewDataBinding,VM: BaseViewModel<*>>:AppCompatActivity(),
    BaseNavigator {
    lateinit var binding: VB
    lateinit var viewModel: VM //by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, getLayoutID())
        viewModel = genViewModel()
//        viewModel.navigator = this

    }
    abstract fun getLayoutID():Int
    abstract fun genViewModel():VM

    var alertDialog: AlertDialog?= null
    var progressDialog: ProgressDialog?=null
    override fun showLoading(message: String) {
        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage(message)
        progressDialog?.show()
    }
    override fun showMessage(messageOK: String , message: String) {
        if( message == "" ){
            alertDialog = AlertDialog.Builder(this)
                .setMessage(messageOK)
                .setPositiveButton("ok") { dialog, i ->
                    dialog?.dismiss()
                }.show()
            return
        }
        alertDialog = AlertDialog.Builder(this)
            .setMessage(messageOK)
            .setNegativeButton("ok") { dialog, i ->
                dialog?.dismiss()
            }.setPositiveButton(message) { dialog, i ->
                dialog?.dismiss()
//                viewModel.onBack()
            }.show()

    }
    override fun hideLoading() {
        alertDialog?.dismiss()
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onBack() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onSearch() {
        val intent = Intent(this,
            SearchHistoryActivity::class.java)
        startActivity(intent)
    }

    override fun onLogout() {

    }

    override fun copy(text: String) {
        copyToClipboard(text)
    }
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}