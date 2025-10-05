package com.example.alpha_ai.ui.tasks.doc

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.database.apis.OCRModelApi
import com.example.alpha_ai.databinding.ActivityDocBinding
import com.example.alpha_ai.databinding.ActivityErBinding
import com.example.alpha_ai.databinding.ActivityOcrBinding
import com.example.alpha_ai.ui.tasks.er.ERActivity
import com.example.alpha_ai.ui.tasks.er.ERNavigator
import com.example.alpha_ai.ui.tasks.er.ERViewModel
import com.example.alpha_ai.ui.tasks.gec.GECActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DOCActivity : BaseActivity<ActivityDocBinding, DOCViewModel>(), DOCNavigator {
    var file: File? = null
    private val PICK_FILE_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator=this
        selectImage()
        binding.content.btnGec.setOnClickListener {
            val intent = Intent(this, GECActivity::class.java)
            intent.putExtra("IN_CORRECT", "${viewModel.output.get()}");
            startActivity(intent)
        }
    }

    fun selectImage(){
        binding.content.imageView.setOnClickListener {
            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            uri?.let {
                file = getFileFromUri(uri)
                viewModel.file = file
            }
        }
    }


    private fun getFileFromUri(uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_file")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_ocr
    }

    override fun genViewModel(): DOCViewModel {
        return ViewModelProvider(this)[DOCViewModel::class.java]
    }
}