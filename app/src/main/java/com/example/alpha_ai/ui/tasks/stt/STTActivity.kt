package com.example.alpha_ai.ui.tasks.stt

import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.base.BaseActivity
import com.example.alpha_ai.databinding.ActivitySttBinding
import com.example.alpha_ai.ui.tasks.gec.GECActivity
import java.io.IOException

class STTActivity : BaseActivity<ActivitySttBinding, STTViewModel>(), STTNavigator {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording: Boolean = false
    private var audioFilePath: String? = null

    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val requestCode = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        binding.base = viewModel
        viewModel.navigator=this

        binding.content.btnGec.setOnClickListener {
            val intent = Intent(this, GECActivity::class.java)
            intent.putExtra("IN_CORRECT", "${viewModel.output.get()}");
            startActivity(intent)
        }

        binding.content.btnSubmit.setOnClickListener {
            if(binding.content.btnSubmit.text.equals("Record")){
                if (checkPermissions()) {
                    startRecording()
                } else {
                    requestPermissions()
                }
            }
            else {
                stopRecording()
                viewModel.sendAudioToSST(audioFilePath!!)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            return true
        }
        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == this.requestCode) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                startRecording()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startRecording() {
        audioFilePath = "${externalCacheDir?.absolutePath}/recording.wav"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
             setOutputFile(audioFilePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("STTActivity", "prepare() failed")
            }
            start()
            isRecording = true
            binding.content.btnSubmit.text = "Stop"
            binding.content.btnSubmit.icon = getDrawable(R.drawable.video)
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        isRecording = false
        mediaRecorder = null
        binding.content.btnSubmit.text = "Record"
        binding.content.btnSubmit.icon = getDrawable(R.drawable.mic)
    }

    override fun getLayoutID(): Int {
        return R.layout.activity_stt
    }

    override fun genViewModel(): STTViewModel {
        return ViewModelProvider(this)[STTViewModel::class.java]
    }

    override fun correct() {
        val intent = Intent(this, GECActivity::class.java)
        intent.putExtra("IN_CORRECT", "${viewModel.output.get()}");
        startActivity(intent)
    }

}