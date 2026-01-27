package com.example.alpha_ai.ui.main.tasks.ocr

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.databinding.ActivityOcrBinding
import com.example.alpha_ai.ui.auth.register.RegisterViewModel
import com.example.alpha_ai.ui.main.tasks.gec.GECActivity
import kotlin.getValue

class OCRActivity : BaseActivity<ActivityOcrBinding, OCRViewModel>() {
    override val viewModel: OCRViewModel by viewModels()
    override fun inflateBinding(): ActivityOcrBinding {
        return DataBindingUtil.setContentView(this,R.layout.activity_ocr)
    }
    override fun handleNavigation(destination: NavigationDestination) {
    }
    var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        selectImage()
    }

    fun selectImage(){
        binding.content.imageView.setOnClickListener {
            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type ="image/*"
            startActivityForResult(intent, 100)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        binding.content.imageView.setImageURI(data?.data)
        var uri: Uri? = data?.data
        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        viewModel.bitmap = bitmap
    }

//    override fun correct() {
//        val intent = Intent(this, GECActivity::class.java)
//        intent.putExtra("IN_CORRECT", "${viewModel.output.get()}");
//        startActivity(intent)
//    }
}