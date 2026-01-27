package com.example.alpha_ai.ui.main.tasks.doc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.databinding.ActivityDocBinding
import com.example.alpha_ai.ui.main.tasks.gec.GECActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DOCActivity : BaseActivity<ActivityDocBinding, DOCViewModel>() {

    override val viewModel: DOCViewModel by viewModels()
    override fun inflateBinding(): ActivityDocBinding {
        return DataBindingUtil.setContentView(this,R.layout.activity_doc)
    }

    override fun handleNavigation(destination: NavigationDestination) {

    }

    var file: File? = null
    private val PICK_FILE_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
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

//    override fun genViewModel(): DOCViewModel {
//        return ViewModelProvider(this)[DOCViewModel::class.java]
//    }
}