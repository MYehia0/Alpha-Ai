package com.example.alpha_ai.ui.main.tasks.ocr

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivityOcrBinding
import com.example.alpha_ai.ui.main.tasks.gec.GECActivity

class OCRActivity : BaseActivity<ActivityOcrBinding, OCRViewModel>(), OCRNavigator {
    var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator=this
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

    override fun getLayoutID(): Int {
        return R.layout.activity_ocr
    }

    override fun genViewModel(): OCRViewModel {
        return ViewModelProvider(this)[OCRViewModel::class.java]
    }

    override fun correct() {
        val intent = Intent(this, GECActivity::class.java)
        intent.putExtra("IN_CORRECT", "${viewModel.output.get()}");
        startActivity(intent)
    }
}